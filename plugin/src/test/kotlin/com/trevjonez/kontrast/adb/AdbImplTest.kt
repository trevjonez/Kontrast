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

package com.trevjonez.kontrast.adb

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class AdbImplTest {
    val adb = Adb.Impl(File(System.getenv("ANDROID_HOME"), "platform-tools/adb"))

    @Test
    fun devices() {
        assertThat(adb.devices().blockingGet()).containsExactly(AdbDevice("emulator-5554", AdbStatus.ONLINE))
    }

    @Test
    fun shell() {
        adb.shell(adb.devices().blockingGet().first(), "echo 'Hello World'").test().let {
            it.values().forEach { println(it) }
            it.assertValueAt(0) { it == "Hello World" }
            it.assertValueCount(1)
        }
    }
}