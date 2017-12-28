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

import android.view.View
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class KontrastRule : TestRule {
    private lateinit var className: String
    private lateinit var methodName: String
    private var parameterizedMethodName: String? = null

    override fun apply(base: Statement, description: Description): Statement {
        className = description.className
        methodName = description.methodName.let {
            if (it.endsWith(']')) {
                parameterizedMethodName = it
                val start = it.indexOf('[')
                it.removeRange(start, it.length)
            } else it
        }
        return base
    }

    fun ofView(view: View): LayoutHelper {
        return ofView(view, methodName)
    }

    fun ofView(view: View, testKey: String): LayoutHelper {
        return LayoutHelper(view, className, methodName, testKey.removeWhiteSpace(), parameterizedMethodName)
    }

    private fun String.removeWhiteSpace(): String {
        return split(' ').joinToString("")
    }
}