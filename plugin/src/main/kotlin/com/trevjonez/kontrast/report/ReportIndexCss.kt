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

import azadev.kotlin.css.AUTO
import azadev.kotlin.css.NONE
import azadev.kotlin.css.Stylesheet
import azadev.kotlin.css.backgroundColor
import azadev.kotlin.css.backgroundImage
import azadev.kotlin.css.backgroundPosition
import azadev.kotlin.css.backgroundSize
import azadev.kotlin.css.color
import azadev.kotlin.css.dimens.box
import azadev.kotlin.css.dimens.percent
import azadev.kotlin.css.dimens.px
import azadev.kotlin.css.display
import azadev.kotlin.css.marginBottom
import azadev.kotlin.css.marginLeft
import azadev.kotlin.css.marginRight
import azadev.kotlin.css.marginTop
import azadev.kotlin.css.maxWidth
import azadev.kotlin.css.paddingTop
import java.io.File

class ReportIndexCss(val outputDir: File) : ReportFile {
    override fun write() {
        outputDir.mkdirs()
        Stylesheet {
            ".report-list-root" {
                paddingTop = 16.px
                maxWidth = 1202.px
                marginLeft = AUTO
                marginRight = AUTO
            }

            ".toolbar-row" {
                maxWidth = 1250.px
                marginLeft = AUTO
                marginRight = AUTO
            }

            ".report-body-content" {
                maxWidth = 860.px
                marginLeft = AUTO
                marginRight = AUTO
            }

            ".report-card" {
                backgroundColor = 0xFFFFFF
                marginBottom = 32.px
            }

            ".mdc-toolbar-fixed-adjust" {
                marginTop = 90.px
            }

            ".skipped-title" {
                color = 0xF57F17
            }

            ".failed-title" {
                color = 0xBF360C
            }

            ".hidden-test-case" {
                display = NONE
            }

            ".test-image" {
                maxWidth = 100.percent
                backgroundSize = box(20, 20)
                backgroundPosition = "0px 0px, 10px 10px"
                backgroundImage = "linear-gradient(45deg, #eee 25%, transparent 25%, transparent 75%, #eee 75%, #eee 100%), linear-gradient(45deg, #eee 25%, white 25%, white 75%, #eee 75%, #eee 100%)"
            }

            ".extras-diff" {
                color = 0xF47F17
            }
        }.renderToFile(File(outputDir, "kontrast.css"))
    }
}