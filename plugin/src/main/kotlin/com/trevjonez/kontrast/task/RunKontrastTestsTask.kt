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

import com.trevjonez.kontrast.AdbDevice
import io.reactivex.Single
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class RunKontrastTestsTask : AdbCommandTask() {

    lateinit var device: Single<AdbDevice>

    @get:Input
    lateinit var testPackage: String

    @get:Input
    lateinit var testRunner: String

    @OutputDirectory
    lateinit var outputsDir: File

    @TaskAction
    fun invoke() {
        if (outputsDir.exists() && !outputsDir.deleteRecursively())
            throw IllegalStateException("Unable to clean output directory: ${outputsDir.absolutePath}")

        if (!outputsDir.mkdirs())
            throw IllegalStateException("Unable to create output directory: ${outputsDir.absolutePath}")

        device.flatMapObservable { adb.shell(it, "am instrument -w -r -e debug false $testPackage/$testRunner") }
                .subscribe {
                    //TODO read it and do magical things
                }
    }
}