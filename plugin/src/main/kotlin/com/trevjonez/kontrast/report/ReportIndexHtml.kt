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

import kotlinx.html.ScriptType
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.title
import org.gradle.api.tasks.testing.TestResult
import java.io.File

class ReportIndexHtml(val outputDir: File, val variantName: String, val testCases: List<TestCaseData>) : ReportFile {
    override fun write() {
        require(outputDir.exists()) { "Invalid output dir, must be pre-existing. ${outputDir.absolutePath}" }

        File(outputDir, "index.html").apply {
            if (exists()) delete()
            if (!createNewFile()) throw IllegalStateException("Unable to create new report index.html")
            writer().use {
                it.appendln("<!DOCTYPE html>")
                it.appendHTML().html {
                    attributes.put("class", "mdc-typography")
                    head {
                        meta { charset = "utf-8" }
                        meta { name = "viewport"; content = "width=device-width,initial-scale=1" }
                        title("Kontrast Report: $variantName")
                        link {
                            rel = "stylesheet"
                            href = "css/material-components-web.min.css"
                        }
                        link {
                            rel = "stylesheet"
                            href = "css/kontrast.css"
                        }
                    }
                    body {
                        style = "background-color: #f5f5f5;"
                        header("mdc-toolbar mdc-toolbar--fixed mdc-toolbar--waterfall") {
                            autoInit("MDCToolbar")
                            div("mdc-toolbar__row toolbar-row") {
                                section("mdc-toolbar__section mdc-toolbar__section--align-start") {
                                    span("mdc-toolbar__title") { text("Kontrast Test Report: $variantName") }
                                }
                                section("mdc-toolbar__section mdc-toolbar__section--align-end") {
                                    nav("mdc-tab-bar") {
                                        autoInit("MDCTabBar")
                                        span(classes = "mdc-tab mdc-tab--active AllTab") { text("All") }
                                        span(classes = "mdc-tab PassedTab") { text("Passed") }
                                        span(classes = "mdc-tab FailedTab") { text("Failed") }
                                        span(classes = "mdc-tab SkippedTab") { text("skipped") }
                                        span("mdc-tab-bar__indicator")
                                    }
                                }
                            }
                        }

                        h3("mdc-typography--headline mdc-toolbar-fixed-adjust report-body-content") { text("Test Cases") }

                        testCases.forEach { testCase ->
                            div("mdc-card report-card report-body-content ${cardClass(testCase.status)}") {
                                section("mdc-card__primary") {
                                    h1("mdc-card__title mdc-card__title--large ${titleClass(testCase.status)}") {
                                        text(testCase.testKey)
                                    }
                                    h2("mdc-card__subtitle") { text("${testCase.className}#${testCase.methodName}") }
                                }
                                section("mdc-card__supporting-text") {
                                    text(testCase.cardBodyMessage())
                                }
                                section("mdc-card__media") {
                                    if (testCase.inputImage.exists()) {
                                        section("mdc-card__supporting-text") {
                                            text("Actual:")
                                        }
                                        "images${File.separator}${testCase.subDirectory()}${File.separator}input.png".let { imgPath ->
                                            a(imgPath) { img("input", imgPath, "test-image") }
                                        }
                                    }

                                    if (testCase.inputExtras.isNotEmpty()) {
                                        section("mdc-card__supporting-text") {
                                            text("Extras: ${testCase.inputExtras.prettyPrint()}")
                                        }
                                    }

                                    if (testCase.keyImage.exists()) {
                                        section("mdc-card__supporting-text") {
                                            text("Expected:")
                                        }
                                        "images${File.separator}${testCase.subDirectory()}${File.separator}key.png".let { imgPath ->
                                            a(imgPath) { img("key", imgPath, "test-image") }
                                        }
                                    }

                                    if (testCase.keyExtras.isNotEmpty()) {
                                        section("mdc-card__supporting-text") {
                                            text("Extras: ${testCase.keyExtras.prettyPrint()}")
                                        }
                                    }

                                    if (testCase.diffImage.exists()) {
                                        section("mdc-card__supporting-text") {
                                            text("Difference:")
                                        }
                                        "images${File.separator}${testCase.subDirectory()}${File.separator}diff.png".let { imgPath ->
                                            a(imgPath) { img("diff", imgPath, "test-image") }
                                        }

                                        val diffExtras = mapDiff(testCase.inputExtras, testCase.keyExtras)
                                        section("mdc-card__supporting-text ${if (diffExtras.isNotEmpty()) "extras-diff" else ""}") {
                                            text("Extras Difference: ${diffExtras.prettyPrint()}")
                                        }
                                    }
                                }
                            }
                        }

                        script { src = "js/material-components-web.min.js" }
                        script(type = ScriptType.textJavaScript) { text("window.mdc.autoInit();") }
                        script { src = "js/kotlin.js" }
                        script { src = "js/kontrast.js" }
                    }
                }
            }
        }
    }

    private fun String?.diffWrap(): String {
        return this?.let {
            """$this"""
        } ?: "null"
    }

    private fun Map<String, String>.prettyPrint(): String {
        return this.entries.joinToString(prefix = "{", postfix = "}", separator = ",\n") { """"${it.key}":"${it.value}"""" }
    }

    private fun mapDiff(inputSet: Map<String, String>, keySet: Map<String, String>): Map<String, String> {
        val differentPairs = mutableMapOf<String, String>()
        inputSet.entries.forEach { (inKey, inVal) ->
            val keyVal = keySet[inKey]
            if (inVal != keyVal) {
                differentPairs.put(inKey, "${inVal.diffWrap()}:${keyVal.diffWrap()}")
            }
        }
        keySet.entries.forEach { (keyKey, keyVal) ->
            val inVal = inputSet[keyKey]
            if (inVal != keyVal) {
                differentPairs.put(keyKey, "${inVal.diffWrap()} : ${keyVal.diffWrap()}")
            }
        }
        return differentPairs
    }

    private fun cardClass(status: TestResult.ResultType): String {
        return when (status) {
            TestResult.ResultType.SKIPPED -> "skipped"
            TestResult.ResultType.SUCCESS -> "success"
            TestResult.ResultType.FAILURE -> "failed"
        }
    }

    private fun titleClass(status: TestResult.ResultType): String {
        return when (status) {
            TestResult.ResultType.SKIPPED -> "skipped-title"
            TestResult.ResultType.SUCCESS -> "success-title"
            TestResult.ResultType.FAILURE -> "failed-title"
        }
    }

    private fun TestCaseData.cardBodyMessage(): String {
        return when (status) {
            TestResult.ResultType.SKIPPED -> testSkipCauseMessage()
            TestResult.ResultType.SUCCESS -> ""
            TestResult.ResultType.FAILURE -> "Test failed due to variant pixels"
        }
    }

    private fun TestCaseData.testSkipCauseMessage(): String {
        return when {
            inputImage.exists() && keyImage.doesNotExist() ->
                "Test skipped due to missing key files where an input file did exist."

            inputImage.doesNotExist() && keyImage.exists() ->
                "Test skipped due to missing input files where a key file did exist."

            else                                           ->
                throw IllegalArgumentException("Only relevant for skipped test cases which should fit either of the combinations above")
        }
    }

    private fun File.doesNotExist(): Boolean {
        return !this.exists()
    }

    private fun Tag.autoInit(className: String) {
        attributes.put("data-mdc-auto-init", className)
    }
}