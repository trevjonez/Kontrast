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

package com.trevjonez.kontrast.internal

import java.io.File

internal data class TestOutput(val testKey: String,
                      val methodName: String,
                      val description: String?,
                      val className: String,
                      val extras: Map<String, String>,
                      val outputDirectory: File) {

    fun keySubDirectory(): String {
        return className + File.separator + methodName + File.separator + testKey
    }

    fun methodSubDirectory(): String {
        return className + File.separator + methodName
    }
}