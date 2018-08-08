/*
 *    Copyright 2018 Trevor Jones
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

package com.trevjonez.kontrast.runner

data class TestArg(val className: String, val methodName: String? = null) {
    companion object {
        const val METHOD_SEPARATOR = '#'

        @JvmStatic
        fun from(arg: String): TestArg {
            val methodIndex = arg.indexOf(METHOD_SEPARATOR)
            return if (methodIndex > 0) {
                val className = arg.substring(0, methodIndex)
                val methodName = arg.substring(methodIndex + 1)
                TestArg(className, methodName)
            } else {
                TestArg(arg)
            }
        }
    }
}

fun String.asTestArg() = TestArg.from(this)