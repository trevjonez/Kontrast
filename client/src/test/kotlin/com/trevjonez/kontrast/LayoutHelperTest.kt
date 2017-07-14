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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class LayoutHelperTest: RoboTest() {

    @JvmField @Rule val testRule = KontrastRobolectricRule()

    @Test
    fun directoryIsAsExpected() {
        val view = View(RuntimeEnvironment.application)
        assertThat(testRule.ofView(view).outputDirectory.absolutePath)
                .contains("/Kontrast/client/build/Kontrast/com.trevjonez.kontrast.LayoutHelperTest/directoryIsAsExpected/")
        //No full path assertion since the output is based on a random uuid
    }
}