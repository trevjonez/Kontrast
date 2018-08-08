/*
 *    Copyright 2018 Trevor Jones
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.trevjonez.kontrast.runner

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.util.Log
import androidx.test.internal.runner.TestExecutor
import androidx.test.internal.runner.TestRequestBuilder
import androidx.test.internal.runner.TestSize
import androidx.test.internal.runner.listener.*
import androidx.test.internal.util.ReflectionUtil.reflectivelyInvokeRemoteMethod
import androidx.test.runner.MonitoringInstrumentation
import androidx.test.runner.lifecycle.ApplicationLifecycleCallback
import androidx.test.runner.lifecycle.ApplicationLifecycleMonitorRegistry
import androidx.test.runner.screenshot.ScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot.addScreenCaptureProcessors
import org.junit.runner.Description
import org.junit.runner.Request
import org.junit.runner.manipulation.Filter
import org.junit.runner.notification.RunListener
import org.junit.runners.model.RunnerBuilder
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

abstract class OrchestratingAndroidJunitTestRunner<
        RunnerArgs : OrchestratingAndroidJunitTestRunner.Args,
        OrchestrationListener : OrchestratingAndroidJunitTestRunner.Listener
        > : MonitoringInstrumentation() {

    lateinit var arguments: Bundle
    val runnerArgs: RunnerArgs by lazy { parseRunnerArgs() }
    var orchestrationListener: OrchestrationListener? = null
    private val instrumentationResultPrinter = InstrumentationResultPrinter()

    open val logTag: String = "AbsOrchestratingRunner"
    abstract fun parseRunnerArgs(): RunnerArgs
    abstract fun makeListener(): OrchestrationListener

    override fun onCreate(arguments: Bundle) {
        this.arguments = arguments

        if (runnerArgs.waitForDebugger) {
            Log.i(logTag, "Waiting for debugger to connect...")
            Debug.waitForDebugger()
            Log.i(logTag, "Debugger connected.")
        }

        super.onCreate(arguments)

        runnerArgs.appListeners.forEach {
            ApplicationLifecycleMonitorRegistry.getInstance().addLifecycleCallback(it)
        }

        addScreenCaptureProcessors(runnerArgs.screenCaptureProcessors.toSet())

        if (runnerArgs.orchestratorService != null
            && isPrimaryInstrProcess(runnerArgs.targetProcess)) {
            orchestrationListener = makeListener()
            orchestrationListener!!.connect(context)
        }

        //if orch service provided and primary inst process
        //connect and wait to be called
        //else
        start()
    }

    override fun onStart() {
        setJsBridgeClassName("androidx.test.espresso.web.bridge.JavaScriptBridge")
        super.onStart()

        if (runnerArgs.listTestsForOrchestrator && isPrimaryInstrProcess(runnerArgs.targetProcess)) {
            orchestrationListener!!.addTests(buildRequest().runner.description)
            finish(Activity.RESULT_OK, Bundle.EMPTY)
            return
        }

        runnerArgs.remoteMethod?.let { (className, methodName) ->
            reflectivelyInvokeRemoteMethod(className, methodName)
        }

        if (!isPrimaryInstrProcess(runnerArgs.targetProcess)) {
            Log.i(logTag, "Runner is idle...")
            return
        }

        val results = try {
            TestExecutor.Builder(this).addListeners().build().execute(buildRequest())
        } catch (error: RuntimeException) {
            val msg = "Fatal exception when running tests"
            Log.e(logTag, msg, error)
            Bundle().apply {
                putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                          "$msg\n${Log.getStackTraceString(error)}")
            }
        }

        finish(Activity.RESULT_OK, results)
    }

    override fun onException(obj: Any, e: Throwable): Boolean {
        instrumentationResultPrinter.reportProcessCrash(e)
        return super.onException(obj, e)
    }

    fun buildRequest(): Request {
        return TestRequestBuilder(this, arguments).apply {
            addPathsToScan(runnerArgs.classpathToScan)
            if (runnerArgs.classpathToScan.isEmpty()) {
                addPathToScan(context.packageCodePath)
            }

            runnerArgs.configureRequest(this)
        }.build()
    }

    fun TestExecutor.Builder.addListeners() = apply {
        if (runnerArgs.newRunListenerMode) addListenersNewOrder(this)
        else addListenersLegacyOrder(this)
    }

    fun TestExecutor.Builder.addArgListeners() = apply {
        runnerArgs.listeners.forEach { addRunListener(it) }
    }

    fun TestExecutor.Builder.addDelayListener() = apply {
        if (runnerArgs.delayInMillis > 0) {
            addRunListener(DelayInjector(runnerArgs.delayInMillis))
        }
    }

    fun TestExecutor.Builder.addCoverageListener() = apply {
        if (runnerArgs.codeCoverage) {
            addRunListener(CoverageListener(runnerArgs.codeCoveragePath))
        }
    }

    fun addListenersNewOrder(builder: TestExecutor.Builder) {
        builder.addArgListeners()
        when {
            runnerArgs.logOnly -> builder.addRunListener(this.instrumentationResultPrinter)
            runnerArgs.suiteAssignment -> builder.addRunListener(SuiteAssignmentPrinter())
            else -> {
                builder.addRunListener(LogRunListener())
                builder.addDelayListener()
                builder.addCoverageListener()
                builder.addRunListener(orchestrationListener ?: instrumentationResultPrinter)
                builder.addRunListener(ActivityFinisherRunListener(this, ActivityFinisher()) {
                    waitForActivitiesToComplete()
                })
            }
        }
    }

    fun addListenersLegacyOrder(builder: TestExecutor.Builder) {
        when {
            runnerArgs.logOnly -> builder.addRunListener(this.instrumentationResultPrinter)
            runnerArgs.suiteAssignment -> builder.addRunListener(SuiteAssignmentPrinter())
            else -> {
                builder.addRunListener(LogRunListener())
                builder.addRunListener(orchestrationListener ?: instrumentationResultPrinter)
                builder.addRunListener(ActivityFinisherRunListener(this, ActivityFinisher()) {
                    waitForActivitiesToComplete()
                })
                builder.addDelayListener()
                builder.addCoverageListener()
            }
        }
        builder.addArgListeners()
    }

    open class Args(instrumentation: Instrumentation, bundle: Bundle) {

        val manifestBundle: Bundle

        init {
            val packageManager = instrumentation.context.packageManager
            val instInfo = packageManager.getInstrumentationInfo(instrumentation.componentName, GET_META_DATA)
            manifestBundle = instInfo.metaData
        }

        open val waitForDebugger: Boolean
            get() = debug && !listTestsForOrchestrator

        open val debug: Boolean by lazy {
            cascadeBoolean(DEBUG, bundle, manifestBundle)
        }

        open val suiteAssignment by lazy {
            cascadeBoolean(SUITE_ASSIGNMENT, bundle, manifestBundle)
        }

        open val codeCoverage by lazy {
            cascadeBoolean(COVERAGE, bundle, manifestBundle)
        }

        open val codeCoveragePath by lazy {
            cascadeString(COVERAGE_PATH, bundle, manifestBundle)
        }

        open val delayInMillis by lazy {
            cascadeUnsignedInt(DELAY_IN_MILLIS, bundle, manifestBundle)
        }

        open val logOnly by lazy {
            cascadeBoolean(LOG_ONLY, bundle, manifestBundle)
        }

        open val testPackages by lazy {
            coalesceCsv(TEST_PACKAGE, bundle, manifestBundle)
                    .plus(testsFile.packages)
        }

        open val notTestPackages by lazy {
            coalesceCsv(NOT_TEST_PACKAGE, bundle, manifestBundle)
                    .plus(notTestsFile.packages)
        }

        open val testSize by lazy {
            cascadeString(TEST_SIZE, bundle, manifestBundle)
        }

        open val annotation by lazy {
            cascadeString(ANNOTATION, bundle, manifestBundle)
        }

        open val notAnnotations by lazy {
            coalesceCsv(NOT_ANNOTATION, bundle, manifestBundle)
        }

        open val testTimeout by lazy {
            cascadeUnsignedLong(TIMEOUT, bundle, manifestBundle)
        }

        open val listeners by lazy {
            coalesceCsv(LISTENER, bundle, manifestBundle)
                    .map { instantiate<RunListener>(it) }
        }

        open val filters by lazy {
            coalesceCsv(FILTER, bundle, manifestBundle)
                    .map { instantiateFilter(it, bundle) }
        }

        open val runnerBuilderClasses by lazy {
            coalesceCsv(RUNNER_BUILDER, bundle, manifestBundle)
                    .map { Class.forName(it) as Class<out RunnerBuilder> }
        }

        open val testsFile by lazy {
            coalesceTestFile(instrumentation, TEST_FILE, bundle, manifestBundle)
        }

        open val tests by lazy {
            coalesceCsv(TEST_CLASS, bundle, manifestBundle)
                    .map { it.asTestArg() }
                    .plus(testsFile.tests)
        }

        open val notTestsFile by lazy {
            coalesceTestFile(instrumentation, NOT_TEST_FILE, bundle, manifestBundle)
        }

        open val notTests by lazy {
            coalesceCsv(NOT_TEST_CLASS, bundle, manifestBundle)
                    .map { it.asTestArg() }
                    .plus(notTestsFile.tests)
        }

        open val numShards by lazy {
            cascadeUnsignedInt(NUM_SHARDS, bundle, manifestBundle)
        }

        open val shardIndex by lazy {
            cascadeUnsignedInt(SHARD_INDEX, bundle, manifestBundle)
        }

        open val disableAnalytics by lazy {
            cascadeBoolean(DISABLE_ANALYTICS, bundle, manifestBundle)
        }

        open val appListeners by lazy {
            coalesceCsv(APP_LISTENER, bundle, manifestBundle)
                    .map { instantiate<ApplicationLifecycleCallback>(it) }
        }

        open val classLoader by lazy {
            coalesceCsv(CLASS_LOADER, bundle, manifestBundle)
                    .singleOrNull()?.let {
                        instantiate<ClassLoader>(it)
                    }
        }

        open val classpathToScan by lazy {
            coalesceClasspath(CLASSPATH_TO_SCAN, bundle, manifestBundle).toSet()
        }

        open val remoteMethod by lazy {
            cascadeString(REMOTE_INIT_METHOD, bundle, manifestBundle)?.asTestArg()
        }

        open val targetProcess by lazy {
            cascadeString(TARGET_PROCESS, bundle, manifestBundle)
        }

        open val screenCaptureProcessors by lazy {
            coalesceCsv(SCREENSHOT_PROCESSORS, bundle, manifestBundle)
                    .map { instantiate<ScreenCaptureProcessor>(it) }
        }

        open val orchestratorService by lazy {
            cascadeString(ORCHESTRATOR_SERVICE, bundle, manifestBundle)
        }

        open val listTestsForOrchestrator by lazy {
            cascadeBoolean(LIST_TESTS_FOR_ORCHESTRATOR, bundle, manifestBundle)
        }

        open val shellExecBinderKey by lazy {
            cascadeString(SHELL_EXEC_BINDER_KEY, bundle, manifestBundle)
        }

        open val newRunListenerMode by lazy {
            cascadeBoolean(RUN_LISTENER_NEW_ORDER, bundle, manifestBundle)
        }

        fun cascadeBoolean(key: String, vararg bundles: Bundle, default: Boolean = false): Boolean {
            return cascadeToDefault(default, *bundles) { getString(key)?.toBoolean() }
        }

        fun cascadeString(key: String, vararg bundles: Bundle, default: String? = null): String? {
            return cascadeToDefault(default, *bundles) { getString(key) }
        }

        fun cascadeUnsignedInt(key: String, vararg bundles: Bundle, default: Int = -1): Int {
            return cascadeToDefault(default, *bundles) {
                get(key)?.toString()?.toInt()?.also { require(it >= 0) }
            }
        }

        fun cascadeUnsignedLong(key: String, vararg bundles: Bundle, default: Long = -1L): Long {
            return cascadeToDefault(default, *bundles) {
                get(key)?.toString()?.toLong()?.also { require(it >= 0) }
            }
        }

        inline fun <T> cascadeToDefault(default: T, vararg bundles: Bundle, crossinline get: Bundle.() -> T?): T {
            for (it in bundles) {
                it.get()?.let { return it }
            }
            return default
        }

        fun coalesceCsv(key: String, vararg bundles: Bundle): List<String> {
            return coalesce(*bundles) {
                getString(key, "").split(CLASS_SEPARATOR)
            }
        }

        fun coalesceTestFile(instrumentation: Instrumentation, key: String, vararg bundles: Bundle): TestFileArg {
            return bundles.map { bundle ->
                bundle.getString(key)?.let { filePath ->
                    instrumentation.openFile(filePath).useLines { lines ->
                        lines.fold(mutableListOf<TestArg>() to mutableListOf<String>()) { collector, line ->
                            collector.apply {
                                if (line.isClassOrMethod) {
                                    first.add(line.asTestArg())
                                } else {
                                    second.addAll(line.split(CLASS_SEPARATOR))
                                }
                            }
                        }
                    }
                } ?: emptyList<TestArg>() to emptyList<String>()
            }.let { fileContents ->
                TestFileArg(fileContents.map { it.first }.flatten(), fileContents.map { it.second }.flatten())
            }
        }

        val String.isClassOrMethod: Boolean
            get() = matches(CLASS_OR_METHOD_REGEX)

        val String.validatePackage: String
            get() = apply { require(matches(VALID_PACKAGE_REGEX)) }

        @SuppressLint("NewApi")
        fun Instrumentation.openFile(path: String): BufferedReader {
            val isInstantApp = Build.VERSION.SDK_INT >= 26 && context.packageManager.isInstantApp
            val readerImpl = if (isInstantApp) {
                InputStreamReader(AutoCloseInputStream(uiAutomation.executeShellCommand("cat $path")))
            } else {
                FileReader(File(path))
            }

            return BufferedReader(readerImpl)
        }

        fun coalesceClasspath(key: String, vararg bundles: Bundle): List<String> {
            return coalesce(*bundles) {
                getString(key, "").split(CLASSPATH_SEPARATOR)
            }
        }

        inline fun <T : Any> coalesce(vararg bundles: Bundle, crossinline get: Bundle.() -> List<T>): List<T> {
            return bundles.map { it.get() }.flatten()
        }

        fun <T : Any> instantiate(className: String): T {
            return Class.forName(className)
                    .getConstructor().apply {
                        isAccessible = true
                    }.newInstance() as T
        }

        fun instantiateFilter(className: String, bundle: Bundle): Filter {
            val classRef = Class.forName(className)
            val (constructor, args) = try {
                classRef.getConstructor() to emptyArray<Any>()
            } catch (noSuchMethod: NoSuchMethodException) {
                classRef.getConstructor(Bundle::class.java) to arrayOf(bundle)
            }
            constructor.isAccessible = true
            return constructor.newInstance(args) as Filter
        }

        open fun configureRequest(builder: TestRequestBuilder) {
            builder.apply {
                tests.forEach { (className, methodName) ->
                    if (methodName.isNullOrBlank()) addTestClass(className)
                    else addTestMethod(className, methodName)
                }

                notTests.forEach { (className, methodName) ->
                    if (methodName.isNullOrBlank()) removeTestClass(className)
                    else removeTestMethod(className, methodName)
                }

                testPackages.forEach { addTestPackage(it) }

                notTestPackages.forEach { removeTestPackage(it) }

                testSize?.let { addTestSizeFilter(TestSize.fromString(it)) }

                annotation?.let { addAnnotationInclusionFilter(it) }

                notAnnotations.forEach { addAnnotationExclusionFilter(it) }

                filters.forEach { addFilter(it) }

                if (testTimeout > 0) setPerTestTimeout(testTimeout)

                if (numShards > 0 && shardIndex >= 0 && shardIndex < numShards)
                    addShardingFilter(numShards, shardIndex)

                setSkipExecution(logOnly)

                classLoader?.let { setClassLoader(it) }

                runnerBuilderClasses.forEach { addCustomRunnerBuilderClass(it) }
            }
        }

        companion object {
            const val TEST_CLASS = "class"
            const val CLASSPATH_TO_SCAN = "classpathToScan"
            const val NOT_TEST_CLASS = "notClass"
            const val TEST_SIZE = "size"
            const val LOG_ONLY = "log"
            const val ANNOTATION = "annotation"
            const val NOT_ANNOTATION = "notAnnotation"
            const val NUM_SHARDS = "numShards"
            const val SHARD_INDEX = "shardIndex"
            const val DELAY_IN_MILLIS = "delay_msec"
            const val COVERAGE = "coverage"
            const val COVERAGE_PATH = "coverageFile"
            const val SUITE_ASSIGNMENT = "suiteAssignment"
            const val DEBUG = "debug"
            const val LISTENER = "listener"
            const val FILTER = "filter"
            const val RUNNER_BUILDER = "runnerBuilder"
            const val TEST_PACKAGE = "package"
            const val NOT_TEST_PACKAGE = "notPackage"
            const val TIMEOUT = "timeout_msec"
            const val TEST_FILE = "testFile"
            const val NOT_TEST_FILE = "notTestFile"
            const val DISABLE_ANALYTICS = "disableAnalytics"
            const val APP_LISTENER = "appListener"
            const val CLASS_LOADER = "classLoader"
            const val REMOTE_INIT_METHOD = "remoteMethod"
            const val TARGET_PROCESS = "targetProcess"
            const val SCREENSHOT_PROCESSORS = "screenCaptureProcessors"
            const val ORCHESTRATOR_SERVICE = "orchestratorService"
            const val LIST_TESTS_FOR_ORCHESTRATOR = "listTestsForOrchestrator"
            const val SHELL_EXEC_BINDER_KEY = "shellExecBinderKey"
            const val RUN_LISTENER_NEW_ORDER = "newRunListenerMode"

            const val CLASS_SEPARATOR = ','
            const val CLASSPATH_SEPARATOR = ':'

            val CLASS_OR_METHOD_REGEX =
                    """^([\p{L}_$][\p{L}\p{N}_$]*\.)*[\p{Lu}_$][\p{L}\p{N}_$]*(#[\p{L}_$][\p{L}\p{N}_$]*)?$""".toRegex()

            val VALID_PACKAGE_REGEX =
                    """^([\p{L}_$][\p{L}\p{N}_$]*\.)*[\p{L}_$][\p{L}\p{N}_$]*$""".toRegex()
        }
    }

    abstract class Listener : RunListener() {
        abstract fun connect(context: Context)
        abstract fun addTests(description: Description)
    }
}
