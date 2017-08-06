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

        val jack = TestCaseReportInput(className = "com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       methodName = "jackDoeCard",
                                       testKey = "jackDoeCard",
                                       inputExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jack Doe"),
                                       keyExtras = mapOf(),
                                       status = TestResult.ResultType.SKIPPED, //Due to missing test key
                                       reportImageDir = File(outputDir, "images"))

        val jane = TestCaseReportInput(className = "com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       methodName = "janeDoeCard",
                                       testKey = "janeDoeCard",
                                       inputExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jane Doe"),
                                       keyExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "Jane Doe"),
                                       status = TestResult.ResultType.SUCCESS,
                                       reportImageDir = File(outputDir, "images"))

        val john = TestCaseReportInput(className = "com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       methodName = "johnDoeCard",
                                       testKey = "johnDoeCard",
                                       inputExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe"),
                                       keyExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe",
                                                         "Something" to "Different",
                                                         "anotherLongWindedThing" to "so that it wraps lines",
                                                         "yet another" to "long extra"),
                                       status = TestResult.ResultType.FAILURE, //Key and input variance
                                       reportImageDir = File(outputDir, "images"))

        val josh = TestCaseReportInput(className = "com.trevjonez.kontrast.app.CardLayoutKontrastTest",
                                       methodName = "joshDoeCard",
                                       testKey = "joshDoeCard",
                                       inputExtras = mapOf(),
                                       keyExtras = mapOf("Width" to "320dp", "Height" to "wrap_content", "Username" to "John Doe"),
                                       status = TestResult.ResultType.SKIPPED, //Missing input
                                       reportImageDir = File(outputDir, "images"))

        ReportIndex(outputDir, "Index page render test", listOf(jack, jane, john, josh)).write()
    }

    private fun copyDirectoryFromResources(resourceDirName: String, outputDir: File) {
        val resourceUrl = ClassLoader.getSystemResource(resourceDirName)!!
        val resourceFilePath = resourceUrl.file!!
        FileUtils.copyDirectory(File(resourceFilePath), File(outputDir, resourceDirName))
    }
}