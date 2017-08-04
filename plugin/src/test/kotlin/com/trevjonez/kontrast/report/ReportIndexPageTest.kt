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

package com.trevjonez.kontrast.report

import com.trevjonez.kontrast.task.copyFileFromResources
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.testing.TestResult
import org.junit.Test
import java.io.File

class ReportIndexPageTest {
    @Test
    fun generateIndex() {
        val outputDir = File("build${File.separator}htmlReportTestOutputs${File.separator}generateIndex")
        outputDir.mkdirs()

        copyDirectoryFromResources("images", outputDir)

        val jack = TestCaseReportInput("com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       "jackDoeCard",
                                       "jackDoeCard",
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jack Doe"),
                                       mapOf(),
                                       TestResult.ResultType.SKIPPED, //Due to missing test key
                                       File(outputDir, "images"))

        val jane = TestCaseReportInput("com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       "janeDoeCard",
                                       "janeDoeCard",
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jane Doe"),
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jane Doe"),
                                       TestResult.ResultType.SUCCESS,
                                       File(outputDir, "images"))

        val john = TestCaseReportInput("com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       "johnDoeCard",
                                       "johnDoeCard",
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe"),
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe"),
                                       TestResult.ResultType.FAILURE, //Key and input variance
                                       File(outputDir, "images"))

        val josh = TestCaseReportInput("com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       "joshDoeCard",
                                       "joshDoeCard",
                                       mapOf(),
                                       mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe"),
                                       TestResult.ResultType.SKIPPED, //Missing input
                                       File(outputDir, "images"))

        ReportIndexPage(outputDir, "Index page render test", listOf(jack, jane, john, josh)).write()

        copyFileFromResources("kotlin.js", "kotlin.js", File(outputDir, "js"))
        copyFileFromResources("reportJs_main.js", "kontrast.js", File(outputDir, "js"))
        copyFileFromResources("kontrast.css", "kontrast.css", File(outputDir, "css"))
    }

    private fun copyDirectoryFromResources(resourceDirName: String, outputDir: File) {
        val resourceUrl = ClassLoader.getSystemResource(resourceDirName)!!
        val resourceFilePath = resourceUrl.file!!
        FileUtils.copyDirectory(File(resourceFilePath), File(outputDir, resourceDirName))
    }
}