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

package com.trevjonez.kontrast.task

import com.trevjonez.kontrast.adb.AdbDevice
import com.trevjonez.kontrast.adb.AdbInstallFlag.ALLOW_TEST_PACKAGES
import com.trevjonez.kontrast.adb.AdbInstallFlag.REPLACE_EXISTING
import io.reactivex.Single
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

open class InstallApkTask : AdbCommandTask() {
    @get:InputFile
    lateinit var apk: File

    @get:Input
    lateinit var device: Single<AdbDevice>

    @TaskAction
    fun invoke() {
        device.flatMapCompletable { adb.install(it, apk, ALLOW_TEST_PACKAGES, REPLACE_EXISTING) }
                .blockingAwait(60, TimeUnit.SECONDS)
    }
}
