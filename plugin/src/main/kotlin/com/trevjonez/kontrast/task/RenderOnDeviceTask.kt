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

import com.trevjonez.kontrast.adb.Adb
import com.trevjonez.kontrast.adb.AdbDevice
import com.trevjonez.kontrast.internal.Collector
import com.trevjonez.kontrast.internal.PulledOutput
import com.trevjonez.kontrast.internal.TestOutput
import com.trevjonez.kontrast.internal.andThenEmit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class RenderOnDeviceTask : AdbCommandTask() {

    lateinit var device: Single<AdbDevice>

    internal val resultSubject: BehaviorSubject<Set<PulledOutput>> = BehaviorSubject.create()

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

        //TODO sharding?
        //TODO test orchestrator?
        val pulledOuputs = device.flatMapObservable { adb.shell(it, "am instrument -w -r -e debug false $testPackage/$testRunner") }
                .map(String::trim)
                .takeWhile { !it.startsWith("INSTRUMENTATION_CODE") }
                .parseTestCases()
                .pullOutputsAndDeleteFromDevice(device, outputsDir, adb)
                .writeExtrasToFile(outputsDir)
                .collectInto(mutableSetOf<PulledOutput>()) { set, output -> set.add(output) }
                .blockingGet()

        resultSubject.onNext(pulledOuputs)
    }
}

internal fun Observable<PulledOutput>.writeExtrasToFile(outputsDir: File): Observable<PulledOutput> {
    fun Map<String, String>.toJson(): String {
        return this.map { """"${it.key}": "${it.value}"""" }.joinToString(prefix = "{\n  ", separator = ",\n  ", postfix = "\n}")
    }

    return doOnNext {
        File(File(outputsDir, it.output.keySubDirectory()), "extras.json").apply {
            if (exists()) delete()
            writeText(it.output.extras.toJson())
        }
    }
}

internal fun Observable<TestOutput>.pullOutputsAndDeleteFromDevice(deviceSelection: Single<AdbDevice>, outputsDir: File, adb: Adb): Observable<PulledOutput> {
    return flatMap { testOutput ->
        //don't use sub directory here because the adb pull will copy the leaf directory down
        val localOutputDir = File(outputsDir, testOutput.methodSubDirectory())
        deviceSelection.flatMapObservable { device ->
            adb.pull(device, testOutput.outputDirectory, localOutputDir, true)
                    .subscribeOn(Schedulers.io())
                    .andThen(adb.deleteDir(device, testOutput.outputDirectory).subscribeOn(Schedulers.io()))
                    .andThenEmit(PulledOutput(localOutputDir, testOutput))
        }
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
