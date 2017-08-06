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

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.Logger
import java.io.File
import java.util.stream.Collectors

interface Adb {
    val executable: File

    fun devices(): Single<Set<AdbDevice>>

    fun install(device: AdbDevice, apk: File, vararg flags: AdbInstallFlag): Completable

    fun pull(device: AdbDevice, remote: File, local: File, preserveTimestamps: Boolean): Completable

    fun shell(device: AdbDevice, command: String): Observable<String>

    fun deleteDir(device: AdbDevice, directory: File): Completable

    class Impl(override val executable: File, val logger: Logger) : Adb {
        override fun devices(): Single<Set<AdbDevice>> {
            return Single.fromCallable {
                ProcessBuilder(executable.absolutePath, "devices").start()
                        .inputStream.bufferedReader().lines()
                        .map { logger.info(it); it }
                        .filter { it.endsWith("offline") || it.endsWith("device") || it.endsWith("unauthorized") }
                        .map { it.split("""\s""".toRegex()).let { AdbDevice(it[0], AdbStatus.fromString(it[1])) } }
                        .collect(Collectors.toSet())
            }
        }

        override fun install(device: AdbDevice, apk: File, vararg flags: AdbInstallFlag): Completable {
            return Completable.fromAction {
                val process = ProcessBuilder(executable.absolutePath,
                                             "-s", device.id,
                                             "install",
                                             *flags.map(AdbInstallFlag::cliFlag).toTypedArray(),
                                             apk.absolutePath)
                        .start()


                val result = process.waitFor()
                if (result != 0) {
                    val builder = StringBuilder()
                    process.inputStream.bufferedReader().forEachLine { builder.append(it) }
                    throw RuntimeException("install failed with code: $result\n$builder")
                }
            }
        }

        override fun pull(device: AdbDevice, remote: File, local: File, preserveTimestamps: Boolean): Completable {
            return Completable.fromAction {
                if (!local.exists() && !local.mkdirs()) {
                    throw RuntimeException("failed to create local output directory")
                }

                val process = ProcessBuilder(executable.absolutePath,
                                             "-s", device.id,
                                             "pull",
                                             remote.absolutePath,
                                             local.absolutePath,
                                             if (preserveTimestamps) "-a" else "")
                        .start()

                val result = process.waitFor()
                if (result != 0) {
                    val builder = StringBuilder()
                    process.inputStream.bufferedReader().forEachLine { builder.append(it) }
                    throw RuntimeException("pull file failed with code: $result\n$builder")
                }
            }
        }

        override fun shell(device: AdbDevice, command: String): Observable<String> {
            return Observable.create { emitter ->
                try {
                    val process = ProcessBuilder(executable.absolutePath,
                                                 "-s", device.id,
                                                 "shell", command).start()

                    process.inputStream.bufferedReader()
                            .forEachLine {
                                logger.info(it)
                                emitter.onNext(it)
                            }

                    val result = process.waitFor()
                    if (result != 0) {
                        throw RuntimeException("adb shell failed with code: $result, for command: '$command'")
                    }

                    emitter.onComplete()
                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }
        }

        override fun deleteDir(device: AdbDevice, directory: File): Completable {
            return Completable.fromAction {
                val process = ProcessBuilder(executable.absolutePath,
                                             "-s", device.id,
                                             "shell", "rm -rf ${directory.absolutePath}")
                        .start()

                val result = process.waitFor()
                if (result != 0) {
                    val builder = StringBuilder()
                    process.inputStream.bufferedReader().forEachLine { builder.append(it) }
                    throw RuntimeException("directory(${directory.absolutePath}) delete failed with code: $result\n$builder")
                }
            }
        }
    }
}