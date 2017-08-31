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

package com.trevjonez.kontrast.internal

import java.io.File

internal data class Collector(val chunk: String = "", val closed: Boolean = false) {
    fun toOutput(): TestOutput {
        val INST_STAT = "INSTRUMENTATION_STATUS"
        return TestOutput(chunk.substringBetween("$INST_STAT: Kontrast:TestKey=", INST_STAT).trim(),
                          chunk.substringBetween("$INST_STAT: Kontrast:MethodName=", INST_STAT).trim(),
                          chunk.substringBetween("$INST_STAT: Kontrast:Description=", INST_STAT).trim()
                                  .let { if (it == "null") null else it },
                          chunk.substringBetween("$INST_STAT: Kontrast:ClassName=", INST_STAT).trim(),
                          chunk.substringBetween("$INST_STAT: Kontrast:Extras=", INST_STAT).trim().parseToMap(),
                          chunk.substringBetween("$INST_STAT: Kontrast:OutputDir=", INST_STAT).trim().toFile())
    }

    private fun String.substringBetween(first: String, second: String): String {
        val indexOfFirst = indexOf(first)
        if (indexOfFirst < 0) {
            return ""
        }

        val startIndex = indexOfFirst + first.length
        val endIndex = indexOf(second, startIndex).let { if (it <= 0) length else it }

        return substring(startIndex, endIndex)
    }

    private fun String.parseToMap(): Map<String, String> {
        return removeSurrounding("[", "]")
                .split("EXTRA_DELIMITER")
                .map { it.split("KVP_DELIMITER") }
                .filter { it.size == 2 }
                .map { it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"") }
                .toMap()
    }

    /**
     * File pull issues on api 19 require some hackery on the file path
     */
    private fun String.toFile(): File {
        return if (startsWith("/sdcard/storage/sdcard/Android/data"))
            File(removePrefix("/sdcard/storage"))
        else
            File(this)
    }
}