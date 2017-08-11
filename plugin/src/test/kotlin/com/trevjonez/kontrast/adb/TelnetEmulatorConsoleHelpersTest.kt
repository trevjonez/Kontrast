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

class TelnetEmulatorConsoleHelpersTest {
    @Test
    fun getEmulatorName() {
        val emulatorInput = AdbDevice("emulator-5554", AdbStatus.ONLINE)
        val emulatorName = getEmulatorName(emulatorInput)
        assertThat(emulatorName.alias).isEqualTo("Nexus_5X_API_O")
    }
}