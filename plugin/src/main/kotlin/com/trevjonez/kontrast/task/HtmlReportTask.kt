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

package com.trevjonez.kontrast.task

import com.trevjonez.kontrast.report.ReportIndexPage
import com.trevjonez.kontrast.report.TestCaseOutput
import com.trevjonez.kontrast.report.TestCaseReportInput
import io.reactivex.Single
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class HtmlReportTask : DefaultTask() {

    lateinit var testCases: Single<List<TestCaseOutput>>

    @OutputDirectory
    lateinit var outputDir: File

    lateinit var variantName: String

    @TaskAction
    fun invoke() {
        if (outputDir.exists())
            if (!outputDir.deleteRecursively()) throw IllegalStateException("Unable to delete old report directory")

        if (!outputDir.mkdirs()) throw IllegalStateException("Unable to create new report directory")

        testCases.map {
            it.map { testCase ->
                TestCaseReportInput(testCase.className, testCase.methodName,
                                    testCase.testKey, testCase.inputExtras,
                                    testCase.keyExtras, testCase.status,
                                    outputDir)
                        .also { reportCase ->
                            if (testCase.inputImage.exists())
                                FileUtils.copyFile(testCase.inputImage, reportCase.inputImage)

                            if (testCase.keyImage.exists())
                                FileUtils.copyFile(testCase.keyImage, reportCase.keyImage)

                            if (testCase.diffImage.exists())
                                FileUtils.copyFile(testCase.diffImage, reportCase.diffImage)
                        }
            }
        }
                .map { ReportIndexPage(outputDir, variantName, it) }
                .blockingGet()
                .write()

        copyFileFromResources("kotlin.js", "kotlin.js", File(outputDir, "js"))
        copyFileFromResources("reportJs_main.js", "kontrast.js", File(outputDir, "js"))
        copyFileFromResources("kontrast.css", "kontrast.css", File(outputDir, "css"))
    }
}

fun copyFileFromResources(resName: String, destFileName: String, outputDir: File) {
    if (!outputDir.exists()) outputDir.mkdirs()
    val resource = Thread.currentThread().contextClassLoader.getResourceAsStream(resName)
    val dest = File(outputDir, destFileName)
    IOUtils.copy(resource, dest.outputStream())
}
