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

import android.app.Activity
import android.app.Instrumentation
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Bundle
import androidx.test.internal.runner.TestRequestBuilder
import androidx.test.runner.MonitoringInstrumentation
import org.junit.runner.Request
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.notification.RunListener
import org.junit.runners.model.RunnerBuilder

class KontrastTestParser {
}

class KontrastTestRunner : MonitoringInstrumentation() {
    private lateinit var arguments: Bundle
    private lateinit var runnerArgs: KontrastTestRunner.Args

    override fun onCreate(arguments: Bundle) {
        this.arguments = arguments
        this.runnerArgs = Args.from(this, arguments)
        //TODO Parse arguments
        //TODO Wait for debugger?
        //TODO Usage analytics?

        super.onCreate(arguments)


        //if orch service provided and primary inst process
        //connect and wait to be called
        //else
        start()
    }

    override fun onStart() {
        //TODO JS bridge?
        super.onStart()

        if (/* List tests for orch && isPrimaryInst*/isPrimaryInstrProcess(runnerArgs.targetProcess)) {
            val testRequest = buildRequest()
            //listener.addTests(testRequest.getRunner().getDescription())
            finish(Activity.RESULT_OK, Bundle.EMPTY)
            return
        }
    }

    fun buildRequest(): Request {
        return TestRequestBuilder(this, arguments).apply {
            addPathToScan(runnerArgs.classpathToScan)

            if (runnerArgs.classpathToScan.isEmpty()) {
                addPathToScan(context.packageCodePath)
            }

            runnerArgs.configureRequest(this)
        }.build()
    }

    abstract class Args(instrumentation: Instrumentation, bundle: Bundle) {

        val manifestBundle: Bundle

        init {
            val packageManager = instrumentation.context.packageManager
            val instInfo = packageManager.getInstrumentationInfo(instrumentation.componentName, GET_META_DATA)
            manifestBundle = instInfo.metaData
        }

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
        }

        open val notTestPackages by lazy {
            coalesceCsv(NOT_TEST_PACKAGE, bundle, manifestBundle)
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
                    .map { instantiateListener(it) }
        }

        open val filters by lazy {
            coalesceCsv(FILTER, bundle, manifestBundle)
                    .map { instantiateFilter(it, bundle) }
        }

        open val runnerBuilderClasses by lazy {
            coalesceCsv(RUNNER_BUILDER, bundle, manifestBundle)
                    .map { Class.forName(it) as Class<out RunnerBuilder> }
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
                getString(key, "").split(',')
            }
        }

        inline fun <T : Any> coalesce(vararg bundles: Bundle, crossinline get: Bundle.() -> List<T>): List<T> {
            return bundles.map { it.get() }.flatten()
        }

        fun instantiateListener(className: String): RunListener {
            return Class.forName(className)
                    .getConstructor().apply {
                        isAccessible = true
                    }.newInstance() as RunListener
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
        }
    }
}
