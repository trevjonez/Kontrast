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
import org.junit.Test
import java.io.File

class ReportIndexPageTest {
    @Test
    fun generateIndex() {
        val outputDir = File("build${File.separator}htmlReportTestOutputs${File.separator}generateIndex")
        outputDir.mkdirs()
        copyFileFromResources("kotlin.js", "js${File.separator}kotlin.js", outputDir)
        copyFileFromResources("reportJs_main.js", "js${File.separator}kontrast.js", outputDir)
        val page = ReportIndexPage(outputDir, "Index page render test")
        page.write()
    }
}