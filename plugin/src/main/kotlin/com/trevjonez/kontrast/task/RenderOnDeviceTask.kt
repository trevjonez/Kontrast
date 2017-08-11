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

import com.squareup.moshi.JsonAdapter
import com.trevjonez.kontrast.adb.Adb
import com.trevjonez.kontrast.adb.AdbDevice
import com.trevjonez.kontrast.internal.Collector
import com.trevjonez.kontrast.internal.PulledOutput
import com.trevjonez.kontrast.internal.TestOutput
import com.trevjonez.kontrast.internal.andThenEmit
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okio.Okio.buffer
import okio.Okio.sink
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import java.io.File

open class RenderOnDeviceTask : AdbCommandTask() {

    internal val resultSubject: BehaviorSubject<Set<PulledOutput>> = BehaviorSubject.create()

    lateinit var extrasAdapter: JsonAdapter<Map<String, String>>

    lateinit var device: AdbDevice

    @get:InputFile
    lateinit var appApk: File

    @get:InputFile
    lateinit var testApk: File

    @get:Input
    lateinit var testPackage: String

    @get:Input
    lateinit var testRunner: String

    @get:OutputDirectory
    lateinit var outputsDir: File

    @TaskAction
    fun invoke() {
        if (outputsDir.exists() && !outputsDir.deleteRecursively())
            throw IllegalStateException("Unable to clean output directory: ${outputsDir.absolutePath}")

        if (!outputsDir.mkdirs())
            throw IllegalStateException("Unable to create output directory: ${outputsDir.absolutePath}")

        val pulledOutputs = adb.shell(device, "am instrument -w -r -e debug false -e annotation com.trevjonez.kontrast.KontrastTest $testPackage/$testRunner")
                .map(String::trim)
                .doOnEach { logger.info("$it") }
                .takeWhile { !it.startsWith("INSTRUMENTATION_CODE") }
                .parseTestCases()
                .pullOutputsAndDeleteFromDevice(device, outputsDir, adb, logger)
                .writeExtrasToFile(outputsDir, extrasAdapter, logger)
                .collectInto(mutableSetOf<PulledOutput>()) { set, output -> set.add(output) }
                .blockingGet()

        resultSubject.onNext(pulledOutputs)
    }
}

internal fun Observable<PulledOutput>.writeExtrasToFile(outputsDir: File, adapter: JsonAdapter<Map<String, String>>, logger: Logger): Observable<PulledOutput> {
    return doOnNext { pulledOutput ->
        File(File(outputsDir, pulledOutput.output.keySubDirectory()), "extras.json").apply {
            logger.info("writing extras file [${this.absolutePath}]")
            if (exists()) delete()
            createNewFile()
            buffer(sink(this)).use {
                adapter.toJson(it, pulledOutput.output.extras)
            }
        }
    }
}

internal fun Observable<TestOutput>.pullOutputsAndDeleteFromDevice(device: AdbDevice, outputsDir: File, adb: Adb, logger: Logger): Observable<PulledOutput> {
    return flatMap { testOutput ->
        //don't use sub directory here because the adb pull will copy the leaf directory down
        val localOutputDir = File(outputsDir, testOutput.methodSubDirectory())

        logger.info("attempting to pull and delete [${testOutput.outputDirectory.absolutePath}]")
        adb.pull(device, testOutput.outputDirectory, localOutputDir, true)
                .subscribeOn(Schedulers.io())
                .andThen(adb.deleteDir(device, testOutput.outputDirectory).subscribeOn(Schedulers.io()))
                .andThenEmit(PulledOutput(localOutputDir, testOutput))
    }
}

internal fun Observable<String>.parseTestCases(): Observable<TestOutput> {
    val INST_STAT_CODE = "INSTRUMENTATION_STATUS_CODE"
    return scan(Collector()) { (chunk, closed), next ->
        Collector(if (closed) next else "$chunk${System.lineSeparator()}$next", next.startsWith(INST_STAT_CODE))
    }
            .filter(Collector::closed)
            .filter { it.chunk.contains("$INST_STAT_CODE: 42") }
            .map(Collector::toOutput)
}
