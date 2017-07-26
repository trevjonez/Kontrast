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

import com.trevjonez.kontrast.internal.PulledOutput
import io.reactivex.Single
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CaptureTestKeyTask : DefaultTask() {
    internal lateinit var pulledOutputs: Single<Set<PulledOutput>>
    internal lateinit var outputsDir: File

    @TaskAction
    fun invoke() {
        if (outputsDir.exists() && !outputsDir.deleteRecursively())
            throw IllegalStateException("Unable to clean output directory: ${outputsDir.absolutePath}")

        if (!outputsDir.mkdirs())
            throw IllegalStateException("Unable to create output directory: ${outputsDir.absolutePath}")

        pulledOutputs.blockingGet().forEach {
            val keyRoot = File(outputsDir, it.output.className)
            it.localOutputDir.parentFile.parentFile.copyRecursively(keyRoot)
        }
    }
}