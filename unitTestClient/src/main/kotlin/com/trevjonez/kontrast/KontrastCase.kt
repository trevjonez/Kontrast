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

package com.trevjonez.kontrast

import java.io.File

class KontrastCase(val keyDir: File, val inputDir: File) {
    override fun toString(): String {
        val className: String = keyDir.parentFile.parentFile.name
        val methodName: String = keyDir.parentFile.name
        val testKey: String = keyDir.name

        return if (methodName == testKey)
            "$className/$methodName"
        else
            "$className/$methodName/$testKey"
    }
}