/*
 *    Copyright 2017 Trevor Jones
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

package com.trevjonez.kontrast

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.trevjonez.kontrast.adb.Adb
import com.trevjonez.kontrast.adb.AdbDevice
import com.trevjonez.kontrast.adb.AdbStatus
import com.trevjonez.kontrast.adb.getEmulatorName
import com.trevjonez.kontrast.dsl.KontrastExtension
import com.trevjonez.kontrast.internal.readLines
import com.trevjonez.kontrast.internal.testEvents
import com.trevjonez.kontrast.jvm.FileAdapter
import com.trevjonez.kontrast.jvm.PulledOutput
import com.trevjonez.kontrast.report.TestCaseOutput
import com.trevjonez.kontrast.task.CaptureTestKeyTask
import com.trevjonez.kontrast.task.HtmlReportTask
import com.trevjonez.kontrast.task.InstallApkTask
import com.trevjonez.kontrast.task.RenderOnDeviceTask
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okio.Okio.buffer
import okio.Okio.source
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import kotlin.reflect.KClass

class KontrastPlugin : Plugin<Project> {
    companion object {
        const val KONTRAST_CONFIG = "kontrast"
        const val GROUP = "Kontrast"
        const val VERSION = "0.3.1"
    }

    internal lateinit var adb: Adb
    internal lateinit var moshi: Moshi
    internal lateinit var kontrastDsl: KontrastExtension

    override fun apply(project: Project) {

        moshi = Moshi.Builder()
                .add(FileAdapter())
                .build()
        kontrastDsl = project.extensions.create("Kontrast", KontrastExtension::class.java, project)

        if (project.configurations.findByName(KONTRAST_CONFIG) == null) {
            project.configurations.create(KONTRAST_CONFIG)
            project.dependencies.add(KONTRAST_CONFIG, "com.github.trevjonez.Kontrast:unitTestClient:$VERSION")
        }

        project.dependencies.add("androidTestImplementation", "com.github.trevjonez.Kontrast:androidTestClient:$VERSION")


        val unzipTestTask = project.createTask(type = Copy::class,
                                               name = "unpackageKontrastTestJar",
                                               description = "Unzips the kontrast unit test client jar to enable gradle to run a test task on the classes within")

        project.afterEvaluate {
            configureUnzipTask(project, unzipTestTask)

            val androidExt = project.extensions.findByName("android") as AppExtension

            adb = Adb.Impl(androidExt.adbExecutable, project.logger)

            val availableDevices = collectConnectedDevicesWithAliasesCalculated(project)

            this.observeVariants(project, unzipTestTask, androidExt, availableDevices)
        }
    }

    private fun configureUnzipTask(project: Project, unzipTestTask: Copy) {
        val kontrastConfig = project.configurations.findByName(KONTRAST_CONFIG)

        val unitTestClientJar = kontrastConfig.files.find { it.name.contains("unitTestClient") }

        unzipTestTask.apply {
            from(project.zipTree(unitTestClientJar))
            into(File(project.buildDir, "unpackedKontrastJar"))
            dependsOn(kontrastConfig)
        }
    }

    private fun collectConnectedDevicesWithAliasesCalculated(project: Project): List<AdbDevice> {
        return adb.devices()
                .flatMap { devices ->
                    Observable.fromIterable(devices)
                            .filter { it.status == AdbStatus.ONLINE }
                            .flatMapSingle { adbDevice ->
                                if (adbDevice.isEmulator) {
                                    Single.fromCallable { getEmulatorName(adbDevice, project.logger) }
                                            .subscribeOn(Schedulers.io())
                                } else {
                                    kontrastDsl.deviceAliases[adbDevice.id]?.let { alias: String ->
                                        Single.just(adbDevice.copy(alias = alias))
                                    } ?: Single.just(adbDevice)
                                }
                            }
                            .toList()
                }
                .blockingGet()
                .toList()
                .also { ensureNoOverlappingAliases(it) }
    }

    private fun ensureNoOverlappingAliases(availableDevices: List<AdbDevice>) {
        val aliasedDevices = availableDevices.filter { it.alias != null }

        val uniqueAliases = aliasedDevices.fold(mutableSetOf<String>()) { acc, device ->
            acc.apply { add(device.alias!!) }
        }

        if (uniqueAliases.size < aliasedDevices.size) {
            throw IllegalStateException("More than one connected devices share the same alias or AVD name. Disconnect one of the two colliding devices.")
        }
    }

    private fun observeVariants(project: Project, unziptestTask: Copy,
                                androidExt: AppExtension, availableDevices: List<AdbDevice>) {
        androidExt.applicationVariants.all { variant ->
            if (variant.testVariant == null) return@all

            kontrastDsl.targetVariants.let {
                if (it.isNotEmpty() && !it.contains(variant.name)) return@all
            }

            availableDevices.forEach { device ->
                val mainInstall = createMainInstallTask(project, variant, device)
                val testInstall = createTestInstallTask(project, variant, device)
                val render = createRenderTask(project, variant, device, mainInstall, testInstall)
                val keyCapture = createKeyCaptureTask(project, variant, render, device)
                val report = createReportTask(project, variant, device)
                createTestTask(project, variant, render, keyCapture, unziptestTask, report, device)
            }
        }
    }

    private fun createReportTask(project: Project, variant: ApplicationVariant, targetDevice: AdbDevice): HtmlReportTask {
        return project.createTask(type = HtmlReportTask::class,
                                  name = "generate${variant.name.capitalize()}KontrastHtmlReport_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Generate HTML test result report").apply {
            outputDir = File(project.buildDir, "reports${File.separator}Kontrast${File.separator}${variant.name}${File.separator}${targetDevice.alias ?: targetDevice.id}")
            variantName = variant.name
            deviceAlias = targetDevice.alias ?: targetDevice.id
            outputs.upToDateWhen { false }
        }
    }

    private fun createTestTask(project: Project, variant: ApplicationVariant, renderTask: RenderOnDeviceTask,
                               keyTask: CaptureTestKeyTask, unzipTestTask: Copy, reportTask: HtmlReportTask,
                               targetDevice: AdbDevice): Test {
        return project.createTask(type = Test::class,
                                  name = "test${variant.name.capitalize()}KontrastTest_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Compare current captured key with render results",
                                  dependsOn = listOf(renderTask, unzipTestTask)).apply {
            useJUnit()
            systemProperty("KontrastInputDir", renderTask.outputsDir.absolutePath)
            systemProperty("KontrastKeyDir", keyTask.outputsDir.absolutePath)
            val config = project.configurations.findByName(KONTRAST_CONFIG)
            classpath = config
            testClassesDirs = SimpleFileCollection(unzipTestTask.destinationDir)
            keyTask.finalizedBy(this)
            setFinalizedBy(listOf(reportTask))
            outputs.upToDateWhen { false }

            val adapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)).indent("  ")
            reportTask.testCases = testEvents()
                    .withLatestFrom(renderTask.resultSubject.map { it.associateBy { it.output.testKey } }, BiFunction { testEvent: Pair<TestDescriptor, TestResult>, pulledOutputs: Map<String, PulledOutput> ->
                        val (descriptor, result) = testEvent
                        val names = descriptor.name
                                .removePrefix("kontrastCase[")
                                .removeSuffix("]")
                                .split("$$")

                        val inputExtras = try {
                            adapter.fromJson(buffer(source(File(renderTask.outputsDir, "${names.joinToString(File.separator)}${File.separator}extras.json")))) ?: mapOf()
                        } catch (ignore: FileNotFoundException) {
                            mapOf<String, String>()
                        }

                        val keyExtras = try {
                            adapter.fromJson(buffer(source(File(keyTask.outputsDir, "${names.joinToString(File.separator)}${File.separator}extras.json")))) ?: mapOf()
                        } catch (ignore: FileNotFoundException) {
                            mapOf<String, String>()
                        }

                        val className = names[0]
                        val methodName = names[1]
                        val testKey = names[2]
                        val pulledOutput = pulledOutputs[testKey]

                        val instrumentationStatus = pulledOutput?.output?.testStatus

                        val logcatCaptureFile = File(renderTask.outputsDir, "logcatCapture.txt")

                        val logcatFile = File(renderTask.outputsDir, "$className${File.separator}$methodName${File.separator}$testKey${File.separator}logcat.txt")
                        logcatFile.parentFile.apply {
                            if (!exists()) mkdirs()
                        }

                        var linesWritten = 0
                        val bufferedWriter = BufferedWriter(FileWriter(logcatFile))
                        buffer(source(logcatCaptureFile)).readLines()
                                .subscribeOn(Schedulers.io())
                                .skipWhile {
                                    !it.contains("TestRunner: started: ${pulledOutput?.output?.parameterizedName ?: methodName}($className)")
                                }
                                .takeUntil {
                                    it.contains("TestRunner: finished: ${pulledOutput?.output?.parameterizedName ?: methodName}($className)")
                                }
                                .blockingSubscribe {
                                    bufferedWriter.appendln(it)
                                    linesWritten++
                                }

                        bufferedWriter.close()

                        if(linesWritten == 0) {
                            logcatFile.delete()
                        }


                        TestCaseOutput(className, methodName, testKey, inputExtras, keyExtras, result.resultType, instrumentationStatus, renderTask.outputsDir, keyTask.outputsDir)
                    }).toList()
        }
    }

    private fun createKeyCaptureTask(project: Project, variant: ApplicationVariant, renderTask: RenderOnDeviceTask, targetDevice: AdbDevice): CaptureTestKeyTask {
        return project.createTask(type = CaptureTestKeyTask::class,
                                  name = "capture${variant.name.capitalize()}TestKeys_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Capture the current render outputs as new test key",
                                  dependsOn = listOf(renderTask)).apply {
            pulledOutputs = renderTask.resultSubject.firstOrError()
            outputsDir = File(kontrastDsl.testKeyRoot, "${variant.name}${File.separator}${targetDevice.alias ?: targetDevice.id}")
            outputs.upToDateWhen { false }
        }
    }

    private fun createRenderTask(project: Project, variant: ApplicationVariant, targetDevice: AdbDevice, mainInstall: InstallApkTask, testInstall: InstallApkTask): RenderOnDeviceTask {
        return project.createTask(type = RenderOnDeviceTask::class,
                                  name = "render${variant.name.capitalize()}KontrastViews_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Run kontrast rendering step",
                                  dependsOn = listOf(mainInstall, testInstall)).apply {
            device = targetDevice
            testRunner = variant.testRunner
            testPackage = "${variant.applicationId}.test"
            outputsDir = File(project.buildDir, "Kontrast${File.separator}${variant.name}${File.separator}${targetDevice.alias ?: targetDevice.id}")
            extrasAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)).indent("  ")
            outputSetAdapter = moshi.adapter<Set<PulledOutput>>(Types.newParameterizedType(Set::class.java, PulledOutput::class.java)).indent("  ")
            appApk = variant.apk
            testApk = variant.testApk
            outputs.upToDateWhen { false }
        }
    }

    private fun createTestInstallTask(project: Project, variant: ApplicationVariant, targetDevice: AdbDevice): InstallApkTask {
        val assembleTestTask = project.tasks.findByName("assemble${variant.name.capitalize()}AndroidTest")
        return project.createTask(type = InstallApkTask::class,
                                  name = "install${variant.name.capitalize()}TestApk_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Install test apk for variant ${variant.name}",
                                  dependsOn = listOf(assembleTestTask)).apply {
            apk = variant.testApk
            device = targetDevice
        }
    }

    private fun createMainInstallTask(project: Project, variant: ApplicationVariant, targetDevice: AdbDevice): InstallApkTask {
        val assembleTask = project.tasks.findByName("assemble${variant.name.capitalize()}")
        return project.createTask(type = InstallApkTask::class,
                                  name = "install${variant.name.capitalize()}Apk_${targetDevice.alias ?: targetDevice.id}",
                                  description = "Install main apk for variant ${variant.name}",
                                  dependsOn = listOf(assembleTask)).apply {
            apk = variant.apk
            device = targetDevice
        }
    }

    /**
     * TODO: what will this do in an apk split situation
     */
    private val ApplicationVariant.apk: File
        get() = this.outputs.single().outputFile

    private val ApplicationVariant.testApk: File
        get() = this.testVariant.outputs.single().outputFile

    private val ApplicationVariant.testRunner: String
        get() = this.mergedFlavor.testInstrumentationRunner

    private fun <T : DefaultTask> Project.createTask(type: KClass<T>,
                                                     name: String,
                                                     group: String = GROUP,
                                                     description: String? = null,
                                                     dependsOn: List<Task>? = null)
            : T {
        return type.java.cast(project.tasks.create(LinkedHashMap<String, Any>().apply {
            put("name", name)
            put("type", type.java)
            put("group", group)
            description?.let { put("description", it) }
            dependsOn?.let { put("dependsOn", it) }
        }))
    }
}
