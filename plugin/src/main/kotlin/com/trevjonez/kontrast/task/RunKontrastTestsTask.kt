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
import io.reactivex.Completable
import io.reactivex.Observable
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

    @get:OutputDirectory
    lateinit var outputsDir: File

    @TaskAction
    fun invoke() {
        if (outputsDir.exists() && !outputsDir.deleteRecursively())
            throw IllegalStateException("Unable to clean output directory: ${outputsDir.absolutePath}")

        if (!outputsDir.mkdirs())
            throw IllegalStateException("Unable to create output directory: ${outputsDir.absolutePath}")

        device.flatMapObservable { adb.shell(it, "am instrument -w -r -e debug false $testPackage/$testRunner") }
                .map(String::trim)
                .takeWhile { !it.startsWith("INSTRUMENTATION_CODE") }
                .parseTestCases()
                .flatMap { testOutput ->
                    device.flatMapCompletable { device ->
                        adb.pull(device,
                                 testOutput.outputDirectory,
                                 File(outputsDir,
                                      "${testOutput.className}${File.separator}${testOutput.methodName}${File.separator}${testOutput.testKey}"),
                                 true)
                    }
                            .andThenObserve { Observable.just(testOutput) }
                }
                .subscribe {
                    //TODO read it and do magical things
                }
    }
}

inline fun <T> Completable.andThenObserve(crossinline func: () -> Observable<T>): Observable<T> =
        andThen(Observable.unsafeCreate { func().subscribe(it::onNext, it::onError, it::onComplete) })

fun Observable<String>.parseTestCases(): Observable<TestOutput> {
    val INST_STAT_CODE = "INSTRUMENTATION_STATUS_CODE"
    return scan(Collector()) { (chunk, closed), next ->
        Collector(if (closed) next else "$chunk${System.lineSeparator()}$next", next.startsWith(INST_STAT_CODE))
    }
            .filter(Collector::closed)
            .filter { it.chunk.contains("$INST_STAT_CODE: 42") }
            .map(Collector::toOutput)
}

data class TestOutput(val testKey: String,
                      val methodName: String,
                      val description: String?,
                      val className: String,
                      val extras: Map<String, String>,
                      val outputDirectory: File)

data class Collector(val chunk: String = "", val closed: Boolean = false) {
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

    fun String.substringBetween(first: String, second: String): String {
        val indexOfFirst = indexOf(first)
        if (indexOfFirst < 0) {
            return ""
        }

        val startIndex = indexOfFirst + first.length
        val endIndex = indexOf(second, startIndex).let { if (it <= 0) length else it }

        return substring(startIndex, endIndex)
    }

    fun String.parseToMap(): Map<String, String> {
        return removeSurrounding("[", "]")
                .split(", ")
                .map { it.split(":") }
                .filter { it.size == 2 }
                .map { it[0].removeSurrounding("\"") to it[1].removeSurrounding("\"") }
                .toMap()
    }

    fun String.toFile() = File(this)
}