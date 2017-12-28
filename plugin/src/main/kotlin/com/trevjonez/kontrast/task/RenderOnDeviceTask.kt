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
import com.trevjonez.kontrast.internal.andThenEmit
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus
import com.trevjonez.kontrast.jvm.PulledOutput
import com.trevjonez.kontrast.jvm.TestOutput
import io.reactivex.Completable
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

open class RenderOnDeviceTask : AdbCommandTask() {

    internal val resultSubject: BehaviorSubject<Set<PulledOutput>> = BehaviorSubject.create()

    lateinit var extrasAdapter: JsonAdapter<Map<String, String>>

    internal lateinit var outputSetAdapter: JsonAdapter<Set<PulledOutput>>

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

        val logcatFile = File(outputsDir, "logcatCapture.txt")

        if (!logcatFile.createNewFile())
            throw IllegalStateException("Failed to create logcat capture file")

        val bufferedWriter = BufferedWriter(FileWriter(logcatFile))
        val logcatSub = adb.logcat(device)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnDispose { bufferedWriter.close() }
                .subscribe { bufferedWriter.appendln(it) }


        val pulledOutputs = adb.shell(device, "am instrument -w -r -e debug false -e annotation com.trevjonez.kontrast.KontrastTest $testPackage/$testRunner")
                .map(String::trim)
                .takeWhile { !it.startsWith("INSTRUMENTATION_CODE") }
                .parseTestCases(logger)
                .pullOutputsAndDeleteFromDevice(device, outputsDir, adb, logger)
                .writeExtrasToFile(outputsDir, extrasAdapter, logger)
                .collectInto(mutableSetOf<PulledOutput>()) { set, output -> set.add(output) }
                .blockingGet()

        logcatSub.dispose()

        val ambiguousCases = mutableListOf<String>()

        val outputByKeySubDir = mutableMapOf<String, PulledOutput>()

        pulledOutputs.forEach {
            if (it.output.testStatus == InstrumentationTestStatus.OK) {
                outputByKeySubDir.put(it.output.keySubDirectory(), it)?.let {
                    ambiguousCases.add(it.output.keySubDirectory())
                }
            }
        }

        if (ambiguousCases.isNotEmpty())
            throw IllegalStateException("There where ambiguous test outputs, use kontrastRule.ofView(View, String) to disambiguate.${ambiguousCases.joinToString(",\n", "\n")}")

        val testCaseListingFile = File(outputsDir, "test-cases.json")
        val buffer = buffer(sink(testCaseListingFile))
        outputSetAdapter.toJson(buffer, pulledOutputs)
        buffer.close()

        logger.info("found results from ${pulledOutputs.size} kontrast tests")
        resultSubject.onNext(pulledOutputs)
    }
}

internal fun Observable<PulledOutput>.writeExtrasToFile(outputsDir: File, adapter: JsonAdapter<Map<String, String>>, logger: Logger): Observable<PulledOutput> {
    return doOnNext { pulledOutput ->
        if (pulledOutput.output.testStatus == InstrumentationTestStatus.OK) {
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
}

internal fun Observable<TestOutput>.pullOutputsAndDeleteFromDevice(device: AdbDevice, outputsDir: File, adb: Adb, logger: Logger): Observable<PulledOutput> {
    return flatMap { testOutput ->
        //don't use sub directory here because the adb pull will copy the leaf directory down
        val localOutputDir = File(outputsDir, testOutput.methodSubDirectory())

        val testOutputDir = testOutput.outputDirectory
        if (testOutputDir != null) {
            logger.info("attempting to pull and delete [${testOutputDir.absolutePath}]")
            adb.pull(device, testOutputDir, localOutputDir, true)
                    .subscribeOn(Schedulers.io())
                    .andThen(adb.deleteDir(device, testOutputDir)
                                     .subscribeOn(Schedulers.io()))
        } else {
            logger.info("nothing to pull/delete")
            Completable.complete()
        }
                .andThenEmit(PulledOutput(localOutputDir, testOutput))
    }
}

private const val INST_STAT_CODE = "INSTRUMENTATION_STATUS_CODE"

internal fun Observable<String>.parseTestCases(logger: Logger): Observable<TestOutput> {
    return scan(Collector()) { (chunk, closed, latestCode, read42), next ->
        var code42Hit = read42
        val statusCode = when {
            closed                          -> {
                logger.info("Collector closed, resetting status code scan. next line: $next")
                code42Hit = false
                1
            }
            next.startsWith(INST_STAT_CODE) -> {
                val code = next.removePrefix("$INST_STAT_CODE: ").trim().toInt()
                logger.info("Read code: $code: $next")
                if (code == 42) code42Hit = true
                if (code != 42 && code != 1) code else latestCode
            }
            else                            -> {
                logger.info("no status code update for this line: $next")
                latestCode
            }
        }

        logger.info("Retaining latest code: $statusCode")

        Collector(if (closed) next else "$chunk${System.lineSeparator()}$next",
                  statusCode == 0 && code42Hit || statusCode.isOneOf(-1, -2, -3, -4),
                  statusCode, code42Hit)
    }
            .filter(Collector::closed)
            .map({ collector -> collector.toOutput(logger) })
}

private fun Int.isOneOf(vararg values: Int): Boolean {
    return values.contains(this)
}
