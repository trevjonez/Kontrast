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

import android.util.Log
import android.view.View
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

abstract class KontrastRule : TestRule {
    lateinit var className: String
    lateinit var methodName: String

    override fun apply(base: Statement, description: Description): Statement {
        className = description.className
        methodName = description.methodName
        return base
    }

    abstract fun ofView(view: View): LayoutHelper
}

class KontrastAndroidTestRule : KontrastRule() {
    override fun ofView(view: View) = LayoutHelper(view, className, methodName) {
        Log.i("LayoutHelper", "KontrastCapture[${it.absolutePath}]")
    }
}

class KontrastRobolectricRule: KontrastRule() {
    init {
        TODO("Robolectric is not supported.\n" +
             "The only limitation I am aware of is that the bitmap implementation doesn't actually do anything.\n" +
             "PR welcome from anyone that wants to do a custom bitmap shadow that might make it work.")
        try {
            Class.forName("org.robolectric.RuntimeEnvironment")
        } catch (error: ClassNotFoundException) {
            throw IllegalStateException("Robolectric rule should not be used for test runs without robolectric")
        }
    }

    override fun ofView(view: View) = LayoutHelper(view, className, methodName) {
        TODO("Figure out how we want to signal back for the plugin to see what tests were just in the run. If we even get this far")
    }
}