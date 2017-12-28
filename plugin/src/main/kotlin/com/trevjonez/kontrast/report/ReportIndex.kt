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

import org.apache.commons.io.IOUtils
import java.io.File

class ReportIndex(val outputDir: File, val variantName: String, val deviceAlias: String, val testCases: List<TestCaseData>) : ReportFile {
    override fun write() {
        ReportIndexHtml(outputDir, variantName, deviceAlias, testCases).write()
        ReportIndexCss(File(outputDir, "css")).write()
        copyFileFromResources("material-components-web.min.css", "material-components-web.min.css", File(outputDir, "css"))
        copyFileFromResources("kotlin.js", "kotlin.js", File(outputDir, "js"))
        copyFileFromResources("material-components-web.min.js", "material-components-web.min.js", File(outputDir, "js"))
        copyFileFromResources("reportJs.js", "kontrast.js", File(outputDir, "js"))
        copyFileFromResources("kotlinx-html-js.js", "kotlinx-html-js.js", File(outputDir, "js"))
        copyFileFromResources("lazyload.js", "lazyload.js", File(outputDir, "js"))
        println("Kontrast report: file://${outputDir.absolutePath}/index.html")
    }

    private fun copyFileFromResources(resName: String, destFileName: String, outputDir: File) {
        if (!outputDir.exists()) outputDir.mkdirs()
        val resource = javaClass.classLoader.getResourceAsStream(resName)
                       ?: throw NullPointerException("res file didn't exist: $resName")

        val dest = File(outputDir, destFileName)
        IOUtils.copy(resource, dest.outputStream())
    }
}
