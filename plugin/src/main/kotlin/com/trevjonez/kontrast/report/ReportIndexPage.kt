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

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import java.io.File

class ReportIndexPage(val outputDir: File, val variantName: String) : ReportPage {

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
                        meta {
                            charset = "utf-8"
                        }
                        meta {
                            name = "viewport"
                            content = "width=device-width,initial-scale=1"
                        }
                        title("Kontrast Report: $variantName")
                        link {
                            rel = "stylesheet"
                            href = "https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css"
                        }
                    }
                    body {
                        header("mdc-toolbar mdc-toolbar--fixed mdc-toolbar--waterfall") {
                            mdcAutoInit = "MDCToolbar"
                            div("mdc-toolbar__row") {
                                section("mdc-toolbar__section mdc-toolbar__section--align-start") {
                                    span("mdc-toolbar__title") {
                                        text("Kontrast Test Report: $variantName")
                                    }
                                }
                                section("mdc-toolbar__section mdc-toolbar__section--align-end") {
                                    nav("mdc-tab-bar") {
                                        mdcAutoInit = "MDCTabBar"
                                        a(href = "#", classes = "mdc-tab mdc-tab--active") {
                                            text("All")
                                        }
                                        a(href = "#", classes = "mdc-tab") {
                                            text("Passed")
                                        }
                                        a(href = "#", classes = "mdc-tab") {
                                            text("Failed")
                                        }
                                        a(href = "#", classes = "mdc-tab") {
                                            text("skipped")
                                        }
                                        span("mdc-tab-bar__indicator") {}
                                    }
                                }
                            }
                        }

                        script {
                            src = "https://unpkg.com/material-components-web@latest/dist/material-components-web.js"
                        }
                        script {
                            text("window.mdc.autoInit();")
                        }
                    }
                }
                it.flush()
            }
        }
    }
}