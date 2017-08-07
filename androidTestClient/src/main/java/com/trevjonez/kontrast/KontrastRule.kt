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

import android.support.test.internal.runner.junit4.statement.UiThreadStatement
import android.support.test.rule.ActivityTestRule
import android.view.LayoutInflater
import android.view.View
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class KontrastRule(val activityTestRule: ActivityTestRule<*>) : TestRule {
    lateinit var className: String
    lateinit var methodName: String

    val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(activityTestRule.activity)

    override fun apply(base: Statement, description: Description): Statement {
        className = description.className
        methodName = description.methodName.let {
            //language=RegExp
            if (it.endsWith(']')) {
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
        return LayoutHelper(view, className, methodName, testKey.removeWhiteSpace())
    }

    fun <T> inflateOnMainThread(inflationBlock: (inflater: LayoutInflater) -> T): T {
        var result: T? = null
        UiThreadStatement.runOnUiThread {
            result = inflationBlock(layoutInflater)
        }
        return result ?: throw NullPointerException("Didn't receive anything from work on main thread")
    }

    private fun String.removeWhiteSpace(): String {
        return split(' ').joinToString("")
    }
}