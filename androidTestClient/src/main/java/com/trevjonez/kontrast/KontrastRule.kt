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

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.view.View
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

abstract class KontrastRule : TestRule {
    lateinit var className: String
    lateinit var methodName: String

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

    abstract fun ofView(view: View, testKey: String): LayoutHelper
}

class KontrastAndroidTestRule(val activityRule: ActivityTestRule<*>) : KontrastRule() {
    companion object {
        const val KONTRAST_SIGNAL_CODE = 42
        const val CLASS_NAME = "ClassName"
        const val METHOD_NAME = "MethodName"
        const val TEST_KEY = "TestKey"
        const val EXTRAS = "Extras"
        const val DESCRIPTION = "Description"
        const val OUTPUT_DIR = "OutputDir"
        const val KONTRAST = "Kontrast"
    }

    @SuppressLint("NewApi")
    override fun ofView(view: View, testKey: String): LayoutHelper {
        return LayoutHelper(view, className, methodName, testKey.removeWhiteSpace()) { helper ->
            val data = Bundle().apply {
                putString("$KONTRAST:$CLASS_NAME", helper.className)
                putString("$KONTRAST:$METHOD_NAME", helper.methodName)
                putString("$KONTRAST:$TEST_KEY", helper.testKey)
                putString("$KONTRAST:$EXTRAS", helper.extras.map({ (k, v) -> """"$k":"$v"""" }).joinToString(prefix = "[", postfix = "]"))
                putString("$KONTRAST:$DESCRIPTION", helper.description)
                putString("$KONTRAST:$OUTPUT_DIR", helper.outputDirectory.absolutePath)
            }

            InstrumentationRegistry.getInstrumentation().sendStatus(KONTRAST_SIGNAL_CODE, data)
            /*
INSTRUMENTATION_STATUS: Kontrast:TestKey=johnDoeCard
INSTRUMENTATION_STATUS: Kontrast:MethodName=johnDoeCard
INSTRUMENTATION_STATUS: Kontrast:Description=null
INSTRUMENTATION_STATUS: Kontrast:ClassName=com.trevjonez.kontrast.CardLayoutKontrastTest
INSTRUMENTATION_STATUS: Kontrast:Extras=[]
INSTRUMENTATION_STATUS: Kontrast:OutputDir=/storage/emulated/0/Android/data/com.trevjonez.kontrast.app/files/Kontrast/com.trevjonez.kontrast.CardLayoutKontrastTest/johnDoeCard/johnDoeCard
INSTRUMENTATION_STATUS_CODE: 42
            */
        }
    }

    fun <T> processOnMainThread(processor: () -> T): T {
        var result: T? = null
        activityRule.runOnUiThread {
            result = processor()
        }
        return result ?: throw NullPointerException("Didn't receive anything from work on main thread")
    }
}

private fun String.removeWhiteSpace(): String {
    return split(' ').joinToString("")
}