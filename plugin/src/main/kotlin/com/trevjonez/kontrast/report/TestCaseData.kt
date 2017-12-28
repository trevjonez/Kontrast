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

import com.trevjonez.kontrast.jvm.InstrumentationTestStatus
import org.gradle.api.tasks.testing.TestResult
import java.io.File

interface TestCaseData {
    val className: String
    val methodName: String
    val testKey: String
    val inputExtras: Map<String, String>
    val keyExtras: Map<String, String>
    val status: TestResult.ResultType
    val inputImage: File
    val keyImage: File
    val diffImage: File
    val logcatFile: File
    val instrumentationStatus: InstrumentationTestStatus?

    fun subDirectory() = "$className${File.separator}$methodName${File.separator}$testKey"
}