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

import java.io.File
import java.util.stream.Collectors

fun getEmulatorName(adbDevice: AdbDevice): AdbDevice {
    val telnet = ProcessBuilder("telnet", "localhost", adbDevice.id.removePrefix("emulator-")).start()
    val home = System.getProperty("user.home")
    val authToken = File(home, ".emulator_console_auth_token").readText()
    telnet.outputStream.bufferedWriter().apply {
        if (authToken.isNotBlank()) {
            write("auth $authToken"); newLine(); flush()
        }
        write("avd name"); newLine(); flush()
        write("exit"); newLine(); flush()
    }

    val consoleOutput = telnet.inputStream
            .bufferedReader()
            .lines()
            .collect(Collectors.toList())

    return adbDevice.copy(alias = consoleOutput[consoleOutput.size - 2])
}