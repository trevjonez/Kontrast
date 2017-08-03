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

import org.gradle.api.tasks.testing.TestResult
import java.io.File

data class TestCaseReportInput(
        override val className: String,
        override val methodName: String,
        override val testKey: String,
        override val inputExtras: Map<String, String>,
        override val keyExtras: Map<String, String>,
        override val status: TestResult.ResultType,
        val reportContentRoot: File) : TestCaseData {
    override val inputImage = File(reportContentRoot, "${subDirectory()}${File.separator}input.png")
    override val keyImage = File(reportContentRoot, "${subDirectory()}${File.separator}key.png")
    override val diffImage = File(reportContentRoot, "${subDirectory()}${File.separator}diff.png")
}