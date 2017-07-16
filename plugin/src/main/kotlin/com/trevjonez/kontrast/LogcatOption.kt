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

sealed class LogcatOption {
    abstract fun toCliString(): String

    object Silent : LogcatOption() {
        override fun toCliString() = "-s"
    }

    data class File(val file: java.io.File) : LogcatOption() {
        override fun toCliString() = "--file=${file.absolutePath}" //escaping on path string?
    }

    data class Rotate(val kilobytes: Long) : LogcatOption() {
        override fun toCliString() = "--rotate-kbytes=$kilobytes"
    }

    data class RotateCount(val count: Int) : LogcatOption() {
        override fun toCliString() = "--rotate-count=$count"
    }

    data class Format(val format: String) : LogcatOption() {
        override fun toCliString() = "--format=$format"
    }

    object Dividers : LogcatOption() {
        override fun toCliString() = "--dividers"
    }

    object Clear : LogcatOption() {
        override fun toCliString() = "--clear"
    }

    object Dump : LogcatOption() {
        override fun toCliString() = "-d"
    }

    data class Regex(val expression: String) : LogcatOption() {
        override fun toCliString() = "--regex=$expression"
    }

    data class MaxCount(val maxCount: Long) : LogcatOption() {
        override fun toCliString() = "--max-count=$maxCount"
    }

    object Print : LogcatOption() {
        override fun toCliString() = "--print"
    }

    data class TailCount(val count: Long, val dump: Boolean) : LogcatOption() {
        override fun toCliString(): String {
            return if (dump) {
                "-t $count"
            } else {
                "-T $count"
            }
        }
    }

    data class TailTime(val time: String, val dump: Boolean) : LogcatOption() {
        override fun toCliString(): String {
            return if (dump) {
                "-t '$time'"
            } else {
                "-T '$time"
            }
        }
    }

    data class BufferSize(val size: String?) : LogcatOption() {
        override fun toCliString(): String {
            return if (size == null) {
                "--buffer-size"
            } else {
                "--buffer-size=$size"
            }
        }
    }

    object Last : LogcatOption() {
        override fun toCliString() = "-last"
    }

    data class Buffer(val options: Set<Buffer.Option>) : LogcatOption() {
        enum class Option(val cliString: String) {
            MAIN("main"),
            SYSTEM("system"),
            RADIO("radio"),
            EVENTS("events"),
            CRASH("crash"),
            DEFAULT("default"),
            ALL("all")
        }

        override fun toCliString(): String {
            return options.joinToString(prefix = "--buffer=", separator = ",") { it.cliString }
        }
    }

    object Binary : LogcatOption() {
        override fun toCliString() = "--binary"
    }

    object Statistics : LogcatOption() {
        override fun toCliString() = "--statistics"
    }

    data class Prune(val param: String?) : LogcatOption() {
        override fun toCliString(): String {
            return if (param == null) {
                "--prune"
            } else {
                "--prune='$param'"
            }
        }
    }

    data class Pid(val pid: Int) : LogcatOption() {
        override fun toCliString() = "--pid=$pid"
    }

    object Wrap : LogcatOption() {
        override fun toCliString() = "--wrap"
    }
}

