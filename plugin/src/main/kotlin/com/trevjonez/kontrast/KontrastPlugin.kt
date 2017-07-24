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
import com.trevjonez.kontrast.task.InstallApkTask
import com.trevjonez.kontrast.task.RunKontrastTestsTask
import com.trevjonez.kontrast.task.SelectDeviceTask
import io.reactivex.Single
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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

class KontrastPlugin : Plugin<Project> {
    companion object {
        const val GROUP = "Kontrast"
    }

    val adb: Adb by lazy { TODO() }

    lateinit var selectedDevice: Single<AdbDevice>

    override fun apply(project: Project) {
        //TODO configuration DSL?

        val deviceSelectTask = project.createTask(type = SelectDeviceTask::class,
                                                  name = "selectKontrastDevice",
                                                  description = "Get adb devices and select one for kontrast tasks to target").apply {
            selectedDevice = resultSubject.firstOrError().cache()
        }

        project.afterEvaluate { this.observeVariants(it, deviceSelectTask) }
    }

    fun observeVariants(project: Project, selectTask: SelectDeviceTask) {
        val androidExt = project.extensions.findByName("android") as AppExtension
        androidExt.applicationVariants.all { variant ->
            if (variant.testVariant == null) return@all

            val mainInstall = createMainInstallTask(project, variant, selectTask)
            val testInstall = createTestInstallTask(project, variant, selectTask)
            val runTests = createRunTestTask(project, variant, mainInstall, testInstall)
        }
    }

    private fun createRunTestTask(project: Project, variant: ApplicationVariant, mainInstall: InstallApkTask, testInstall: InstallApkTask): RunKontrastTestsTask {
        return project.createTask(type = RunKontrastTestsTask::class,
                                  name = "test${variant.name.capitalize()}KontrastTest",
                                  description = "Run kontrast tests",
                                  dependsOn = listOf(mainInstall, testInstall)).apply {
            device = selectedDevice
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
            device = selectedDevice
        }
    }

    private fun createMainInstallTask(project: Project, it: ApplicationVariant, selectTask: SelectDeviceTask): InstallApkTask {
        val assembleTask = project.tasks.findByName("assemble${it.name.capitalize()}")
        return project.createTask(type = InstallApkTask::class,
                                  name = "install${it.name.capitalize()}Apk",
                                  description = "Install main apk for variant ${it.name}",
                                  dependsOn = listOf(selectTask, assembleTask)).apply {
            apk = it.apk
            device = selectedDevice
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
