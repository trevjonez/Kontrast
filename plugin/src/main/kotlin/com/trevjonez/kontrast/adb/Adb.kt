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

import com.trevjonez.kontrast.internal.never
import com.trevjonez.kontrast.internal.readLines
import com.trevjonez.kontrast.internal.toCompletable
import com.trevjonez.kontrast.internal.toObservable
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import java.io.File

interface Adb {
    val executable: File

    fun devices(): Single<Set<AdbDevice>>

    fun install(device: AdbDevice, apk: File, vararg flags: AdbInstallFlag): Completable

    fun pull(device: AdbDevice, remote: File, local: File, preserveTimestamps: Boolean): Completable

    fun shell(device: AdbDevice, command: String): Observable<String>

    fun deleteDir(device: AdbDevice, directory: File): Completable

    fun logcat(device: AdbDevice): Observable<String>

    class Impl(override val executable: File, val logger: Logger) : Adb {
        override fun devices(): Single<Set<AdbDevice>> {
            return ProcessBuilder(executable.absolutePath, "devices")
                    .toObservable("adb", logger, Observable.never())
                    .subscribeOn(Schedulers.io())
                    .doOnError { logger.info("adb devices root observable threw") }
                    .flatMap { (stdOut, stdErr) ->
                        Observable.merge(
                                stdErr.readLines()
                                        .subscribeOn(Schedulers.io())
                                        .doOnNext { logger.info("stdErr: $it") }
                                        .doOnError { logger.info("stdErr threw: adb devices") }
                                        .never(),
                                stdOut.readLines()
                                        .subscribeOn(Schedulers.io())
                                        .doOnNext { logger.info("stdOut: $it") }
                                        .doOnError { logger.info("stdOut threw: adb devices") }
                                        .onErrorResumeNext { _: Throwable -> Observable.empty<String>() }
                                        )
                    }
                    .map { it.trim() }
                    .filter { it.endsWith("offline") || it.endsWith("device") || it.endsWith("unauthorized") }
                    .map { it.split("""\s""".toRegex()).let { AdbDevice(it[0], AdbStatus.fromString(it[1])) } }
                    .collectInto(mutableSetOf<AdbDevice>()) { set, device -> set.add(device) }
                    .map { it.toSet() }
        }

        override fun install(device: AdbDevice, apk: File, vararg flags: AdbInstallFlag): Completable {
            return ProcessBuilder(executable.absolutePath,
                                  "-s", device.id,
                                  "install",
                                  *flags.map(AdbInstallFlag::cliFlag).toTypedArray(),
                                  apk.absolutePath)
                    .toCompletable("adb -s ${device.id} install ${apk.absolutePath}", logger)
        }

        override fun pull(device: AdbDevice, remote: File, local: File, preserveTimestamps: Boolean): Completable {
            return ProcessBuilder(executable.absolutePath,
                                  "-s", device.id,
                                  "pull",
                                  remote.absolutePath,
                                  local.absolutePath,
                                  if (preserveTimestamps) "-a" else "")
                    .toCompletable("adb -s ${device.id} pull ${remote.absolutePath} ${local.absolutePath}", logger)
                    .doOnSubscribe {
                        if (!local.exists() && !local.mkdirs()) {
                            throw RuntimeException("failed to create local output directory")
                        }
                    }
        }

        override fun shell(device: AdbDevice, command: String): Observable<String> {
            return ProcessBuilder(executable.absolutePath,
                                  "-s", device.id,
                                  "shell", command)
                    .toObservable("adb -s ${device.id} shell $command", logger, Observable.never())
                    .flatMap { (stdOut, stdErr) ->
                        Observable.merge(
                                stdOut.readLines().subscribeOn(Schedulers.io()),
                                stdErr.readLines().subscribeOn(Schedulers.io())
                                        .doOnNext { logger.info("adb -s ${device.id} shell $command stdErr: $it") }
                                        .never())
                    }
        }


        override fun deleteDir(device: AdbDevice, directory: File): Completable {
            return ProcessBuilder(executable.absolutePath,
                                  "-s", device.id,
                                  "shell", "rm -rf ${directory.absolutePath}")
                    .toCompletable("adb -s ${device.id} shell rm -rf ${directory.absolutePath}", logger)
        }

        override fun logcat(device: AdbDevice): Observable<String> {
            return ProcessBuilder(executable.absolutePath,
                                  "-s", device.id,
                                  "logcat")
                    .toObservable("logcat", logger, Observable.never())
                    .flatMap { (stdOut, stdErr) ->
                        Observable.merge(
                                stdOut.readLines().subscribeOn(Schedulers.io()),
                                stdErr.readLines().subscribeOn(Schedulers.io())
                                        .doOnNext { logger.info("adb logcat stdErr: $it") }
                                        .never())
                    }
        }
    }
}