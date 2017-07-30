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
import com.trevjonez.kontrast.adb.Adb
import com.trevjonez.kontrast.task.CaptureTestKeyTask
import com.trevjonez.kontrast.task.InstallApkTask
import com.trevjonez.kontrast.task.RenderOnDeviceTask
import com.trevjonez.kontrast.task.SelectDeviceTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import java.io.File
import kotlin.reflect.KClass

//PTV -> Per testable variant

//A: Get first connected or selected device

//B: Install main apk task (PTV)

//C: Install test apk task (PTV)

//D: Run kontrast rendering task (PTV)
//      Clear any previous render run data from build directory
//      Scan the adb output and pull render outputs as produced
//      Delete on device test outputs once they have been pulled

//E: Create task to record current rendering run output as the test key (PTV)

//F: Create task to perform image diffing and report gen inputs (PTV)
//      Custom test task that gets configured via properties passed in and uses parametrized test runner

//G: Create task to generate report html page (PTV)

/**
 * if we move the adb reading to the configuration phase (only do it if we know we are going to run a kontrast task?)
 * from there we can create install, render, record, and test tasks per variant per connected device
 * this will help with the differences in rendering when ran on different screen config devices
 */
class KontrastPlugin : Plugin<Project> {
    companion object {
        const val KONTRAST_CONFIG = "kontrast"
        const val GROUP = "Kontrast"
    }

    internal lateinit var adb: Adb

    override fun apply(project: Project) {
        //TODO configuration DSL?

        if (project.configurations.findByName(KONTRAST_CONFIG) == null) {
            project.configurations.create(KONTRAST_CONFIG)
//            project.dependencies.add(KONTRAST_CONFIG, ) unit test client
        }

        val deviceSelectTask = project.createTask(type = SelectDeviceTask::class,
                                                  name = "selectKontrastDevice",
                                                  description = "Get adb devices and select one for kontrast tasks to target")

        val unzipTestTask = project.createTask(type = Copy::class,
                                               name = "unpackageKontrastTestJar",
                                               description = "Unzips the kontrast unit test client jar to enable gradle to run a test task on the classes within")

        project.afterEvaluate {
            val kontrastConfig = project.configurations.findByName(KONTRAST_CONFIG)
            val unitTestClientJar = kontrastConfig.files.find { it.name.contains("unitTestClient") }
            unzipTestTask.apply {
                from(project.zipTree(unitTestClientJar))
                into(File(project.buildDir, "unpackedKontrastJar"))
                dependsOn(kontrastConfig)
            }

            val androidExt = project.extensions.findByName("android") as AppExtension
            adb = Adb.Impl(androidExt.adbExecutable)
            this.observeVariants(it, deviceSelectTask, unzipTestTask, androidExt)
        }
    }

    fun observeVariants(project: Project, selectTask: SelectDeviceTask, unziptestTask: Copy, androidExt: AppExtension) {
        androidExt.applicationVariants.all { variant ->
            if (variant.testVariant == null) return@all

            val mainInstall = createMainInstallTask(project, variant, selectTask)
            val testInstall = createTestInstallTask(project, variant, selectTask)
            val render = createRenderTask(project, variant, selectTask, mainInstall, testInstall)
            val keyCapture = createKeyCaptureTask(project, variant, render)
            val test = createTestTask(project, variant, render, keyCapture, unziptestTask)
        }
    }

    private fun createTestTask(project: Project, variant: ApplicationVariant, renderTask: RenderOnDeviceTask, keyTask: CaptureTestKeyTask, unziptestTask: Copy): Test {
        return project.createTask(type = Test::class,
                                  name = "test${variant.name.capitalize()}KontrastTest",
                                  description = "Compare current captured key with render results",
                                  dependsOn = listOf(renderTask, unziptestTask)).apply {
            useJUnit()
            systemProperty("KontrastInputDir", renderTask.outputsDir.absolutePath)
            systemProperty("KontrastKeyDir", keyTask.outputsDir.absolutePath)
            val config = project.configurations.findByName(KONTRAST_CONFIG)
            classpath = config
            testClassesDirs = SimpleFileCollection(unziptestTask.destinationDir)
        }
    }

    private fun createKeyCaptureTask(project: Project, variant: ApplicationVariant, renderTask: RenderOnDeviceTask): CaptureTestKeyTask {
        return project.createTask(type = CaptureTestKeyTask::class,
                                  name = "capture${variant.name.capitalize()}TestKeys",
                                  description = "Capture the current render outputs as new test key",
                                  dependsOn = listOf(renderTask)).apply {
            pulledOutputs = renderTask.resultSubject.firstOrError()
            outputsDir = File(project.projectDir, "Kontrast${File.separator}${variant.name}")
        }
    }

    private fun createRenderTask(project: Project, variant: ApplicationVariant, selectTask: SelectDeviceTask, mainInstall: InstallApkTask, testInstall: InstallApkTask): RenderOnDeviceTask {
        return project.createTask(type = RenderOnDeviceTask::class,
                                  name = "render${variant.name.capitalize()}KontrastViews",
                                  description = "Run kontrast rendering step",
                                  dependsOn = listOf(mainInstall, testInstall)).apply {
            device = selectTask.resultSubject.firstOrError()
            testRunner = variant.testRunner
            testPackage = "${variant.applicationId}.test"
            outputsDir = File(project.buildDir, "Kontrast${File.separator}${variant.name}")
        }
    }

    private fun createTestInstallTask(project: Project, variant: ApplicationVariant, selectTask: SelectDeviceTask): InstallApkTask {
        val assembleTestTask = project.tasks.findByName("assemble${variant.name.capitalize()}AndroidTest")
        return project.createTask(type = InstallApkTask::class,
                                  name = "install${variant.name.capitalize()}TestApk",
                                  description = "Install test apk for variant ${variant.name}",
                                  dependsOn = listOf(selectTask, assembleTestTask)).apply {
            apk = variant.testApk
            device = selectTask.resultSubject.firstOrError()
        }
    }

    private fun createMainInstallTask(project: Project, it: ApplicationVariant, selectTask: SelectDeviceTask): InstallApkTask {
        val assembleTask = project.tasks.findByName("assemble${it.name.capitalize()}")
        return project.createTask(type = InstallApkTask::class,
                                  name = "install${it.name.capitalize()}Apk",
                                  description = "Install main apk for variant ${it.name}",
                                  dependsOn = listOf(selectTask, assembleTask)).apply {
            apk = it.apk
            device = selectTask.resultSubject.firstOrError()
        }
    }

    private val ApplicationVariant.apk: File
        get() = this.outputs.map { it.mainOutputFile }.single().outputFile

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