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

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.rule.ActivityTestRule
import org.junit.Rule


abstract class KontrastTestBase(hostActivity: Class<out Activity> = KontrastTestBase.defaultActivity()) {
    companion object {
        fun defaultActivity(): Class<Activity> {
            return try {
                @Suppress("UNCHECKED_CAST")
                Class.forName("com.trevjonez.kontrast.KontrastActivity") as Class<Activity>
            } catch (notFound: ClassNotFoundException) {
                throw IllegalArgumentException("Either include the appClient as a debug only dependency to your app, " +
                                               "or specify an existing activity in the constructor", notFound)
            }
        }
    }

    @JvmField @Rule val activityRule: ActivityTestRule<out Activity> = ActivityTestRule(hostActivity)
    @JvmField @Rule val kontrastRule: KontrastRule = KontrastRule()

    val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(activityRule.activity)

    fun <T> inflateOnMainThread(inflationBlock: (inflater: LayoutInflater) -> T): T {
        var result: T? = null
        UiThreadStatement.runOnUiThread {
            result = inflationBlock(layoutInflater)
        }
        return result ?: throw NullPointerException("Didn't receive anything from work on main thread")
    }

    fun View.toKontrastHelper() = kontrastRule.ofView(this)
    fun View.toKontrastHelper(testKey: String) = kontrastRule.ofView(this, testKey)
}
