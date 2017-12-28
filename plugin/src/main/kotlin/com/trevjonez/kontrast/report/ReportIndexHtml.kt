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

import ar.com.hjg.pngj.PngReader
import kotlinx.html.ScriptType
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
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
import kotlinx.html.unsafe
import org.gradle.api.tasks.testing.TestResult
import java.io.File
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus.ERROR as ON_DEVICE_ERROR
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus.FAILED_ASSUMPTION as ON_DEVICE_FAILED_ASSUMPTION
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus.FAILURE as ON_DEVICE_FAILURE
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus.IGNORED as ON_DEVICE_IGNORED

class ReportIndexHtml(private val outputDir: File,
                      private val variantName: String,
                      private val deviceAlias: String,
                      private val testCases: List<TestCaseData>) : ReportFile {
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
                                    span("mdc-toolbar__title") { text("Kontrast Test Report: $variantName - $deviceAlias") }
                                }
                                section("mdc-toolbar__section mdc-toolbar__section--align-end") {
                                    nav("mdc-tab-bar") {
                                        autoInit("MDCTabBar")
                                        span(classes = "mdc-tab mdc-tab--active AllTab") { text("All: ${testCases.size}") }
                                        span(classes = "mdc-tab PassedTab") { text("Passed: ${testCases.passed().size}") }
                                        span(classes = "mdc-tab FailedTab") { text("Failed: ${testCases.failed().size}") }
                                        span(classes = "mdc-tab SkippedTab") { text("Skipped: ${testCases.skipped().size}") }
                                        span("mdc-tab-bar__indicator")
                                    }
                                }
                            }
                        }

                        h3("mdc-typography--headline mdc-toolbar-fixed-adjust report-body-content") { text("Test Cases") }

                        testCases.forEach { testCase ->
                            div("mdc-card report-card report-body-content ${testCase.cardClass()}") {
                                section("mdc-card__primary") {
                                    h1("mdc-card__title mdc-card__title--large ${testCase.titleClass()}") {
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
                                            a(imgPath) {
                                                img("input", "", "test-image lazyload") {
                                                    val size = testCase.inputImage.imageSize()
                                                    width = size.width.toString()
                                                    height = size.height.toString()
                                                    attributes.put("data-src", imgPath)
                                                }
                                            }
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
                                            a(imgPath) {
                                                img("key", "", "test-image lazyload") {
                                                    val size = testCase.keyImage.imageSize()
                                                    width = size.width.toString()
                                                    height = size.height.toString()
                                                    attributes.put("data-src", imgPath)
                                                }
                                            }
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
                                            a(imgPath) {
                                                img("diff", "", "test-image lazyload") {
                                                    val size = testCase.diffImage.imageSize()
                                                    width = size.width.toString()
                                                    height = size.height.toString()
                                                    attributes.put("data-src", imgPath)
                                                }
                                            }
                                        }

                                        val diffExtras = mapDiff(testCase.inputExtras, testCase.keyExtras)
                                        section("mdc-card__supporting-text ${if (diffExtras.isNotEmpty()) "extras-diff" else ""}") {
                                            text("Extras Difference: ${diffExtras.prettyPrint()}")
                                        }
                                    }
                                }

                                if (testCase.logcatFile.exists()) {
                                    section("mdc-card__supporting-text logcat-button-section") {
                                        button(classes = "mdc-button mdc-button--raised logcat-button") {
                                            span("button-text") {
                                                text("Show Logcat")
                                            }
                                            span("logcat-file") {
                                                text("logcat${File.separator}${testCase.subDirectory()}${File.separator}logcat.txt")
                                            }
                                        }
                                    }

                                    section("mdc-card__supporting-text logcat-text-section") {
                                        div(classes = "logcat-area logcat-hidden") {}
                                    }
                                }
                            }
                        }

                        script { src = "js/material-components-web.min.js" }
                        script(type = ScriptType.textJavaScript) { unsafe { raw("window.mdc.autoInit();") } }
                        script { src = "js/kotlin.js" }
                        script { src = "js/kotlinx-html-js.js" }
                        script { src = "js/kontrast.js" }
                        script { src = "js/lazyload.js" }
                        script(type = ScriptType.textJavaScript) { unsafe { raw("lazyload();") } }
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

    private fun TestCaseData.cardClass(): String {
        return when {
            wasSkipped() -> "skipped"
            didPass()    -> "success"
            didFail()    -> "failed"
            else         -> throw IllegalStateException("unhandled condition $this")
        }
    }

    private fun TestCaseData.titleClass(): String {
        return when {
            wasSkipped() -> "skipped-title"
            didPass()    -> "success-title"
            didFail()    -> "failed-title"
            else         -> throw IllegalStateException("unhandled condition $this")
        }
    }

    private fun TestCaseData.cardBodyMessage(): String {
        return when (status) {
            TestResult.ResultType.SKIPPED -> {
                instrumentationStatus?.let {
                    when (it) {
                        ON_DEVICE_FAILURE           -> "Instrumentation portion of test had a failure, inspect logcat for details"
                        ON_DEVICE_ERROR             -> "Instrumentation portion of test had an error, inspect logcat for details"
                        ON_DEVICE_IGNORED           -> "Instrumentation portion of test was ignored"
                        ON_DEVICE_FAILED_ASSUMPTION -> "Instrumentation portion of test had a failed assumption"
                        else                        -> null
                    }
                } ?: testSkipCauseMessage()
            }
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

private fun List<TestCaseData>.passed(): List<TestCaseData> {
    return filter { it.didPass() }
}

private fun List<TestCaseData>.failed(): List<TestCaseData> {
    return filter { it.didFail() }
}

private fun List<TestCaseData>.skipped(): List<TestCaseData> {
    return filter { it.wasSkipped() }
}

private fun TestCaseData.didPass(): Boolean {
    return status == TestResult.ResultType.SUCCESS
}

private fun TestCaseData.didFail(): Boolean {
    return status == TestResult.ResultType.FAILURE ||
           (status == TestResult.ResultType.SKIPPED &&
            ((instrumentationStatus == ON_DEVICE_FAILURE) || (instrumentationStatus == ON_DEVICE_ERROR)))
}

private fun TestCaseData.wasSkipped(): Boolean {
    return status == TestResult.ResultType.SKIPPED &&
           !(((instrumentationStatus == ON_DEVICE_FAILURE) == true) ||
             ((instrumentationStatus == ON_DEVICE_ERROR) == true))
}

private fun File.imageSize(): Resolution {
    val reader = PngReader(this)
    val width = reader.imgInfo.cols
    val height = reader.imgInfo.rows
    reader.close()
    return Resolution(width, height)
}

private data class Resolution(val width: Int, val height: Int)