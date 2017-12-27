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

import ar.com.hjg.pngj.ImageInfo
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.PngWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.trevjonez.kontrast.jvm.FileAdapter
import com.trevjonez.kontrast.jvm.InstrumentationTestStatus
import com.trevjonez.kontrast.jvm.PulledOutput
import okio.Okio.buffer
import okio.Okio.source
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.lang.Math.max

@RunWith(Parameterized::class)
class KontrastTest(val case: KontrastCase) {
    companion object {

        @JvmStatic
        @Parameters(name = "{0}")
        fun params(): Iterable<Any> {
            val keyRoot = File(System.getProperty("KontrastKeyDir") ?: throw NullPointerException("KontrastKeyDir Required"))
            val inputRoot = File(System.getProperty("KontrastInputDir") ?: throw NullPointerException("KontrastInputDir Required"))
            println("KeyRoot: ${keyRoot.absolutePath}")
            println("InputRoot: ${inputRoot.absolutePath}")

            val keyCases = if (keyRoot.exists())
                childDirectories(keyRoot) //classDirs
                        .flatMap(this::childDirectories) //methodDirs
                        .flatMap(this::childDirectories) //testDirs
                        .map { KontrastCase(it, File(inputRoot, "${it.parentFile.parentFile.name}${File.separator}${it.parentFile.name}${File.separator}${it.name}"), null) }
            else emptyList()

            val moshi = Moshi.Builder().add(FileAdapter()).build()
            val testCaseSetAdapter = moshi.adapter<Set<PulledOutput>>(Types.newParameterizedType(Set::class.java, PulledOutput::class.java))
            val inputCasesRaw = buffer(source(File(inputRoot, "test-cases.json"))).use {
                testCaseSetAdapter.fromJson(it)
            } ?: throw NullPointerException("can't run test cases without any input.")

            val inputCases = inputCasesRaw.map {
                KontrastCase(File(keyRoot, it.output.keySubDirectory()), File(inputRoot, it.output.keySubDirectory()), it)
            }

            return keyCases.mergeWith(inputCases)
        }

        private fun childDirectories(it: File) = it.listFiles().filter(File::isDirectory)

        private fun List<KontrastCase>.mergeWith(other: List<KontrastCase>): List<KontrastCase> {
            val result = associateBy { it.toString() }.toMutableMap()

            other.forEach {
                result[it.toString()] = result[it.toString()]?.copy(pulledOutput = it.pulledOutput!!) ?: it
            }

            return result.map { it.value }
        }
    }

    @Test
    fun kontrastCase() {
        val testStatus = case.pulledOutput?.output?.testStatus
        if(testStatus != null && testStatus != InstrumentationTestStatus.OK) {
            assumeTrue("On device portion of test run failed with status: $testStatus", false)
        }

        assumeTrue("Input missing for case with test key", case.inputDir.exists())
        assumeTrue("Key missing for case with input", case.keyDir.exists())

        val keyReader = PngReader(File(case.keyDir, "image.png"))
        val inputReader = PngReader(File(case.inputDir, "image.png"))

        val diffInfo = ImageInfo(max(keyReader.imgInfo.cols, inputReader.imgInfo.cols),
                                 max(keyReader.imgInfo.rows, inputReader.imgInfo.rows),
                                 keyReader.imgInfo.bitDepth, true)

        val diffWriter = PngWriter(File(case.inputDir, "diff.png"), diffInfo)

        var diffCount = 0
        var rowIndex = 0

        while (keyReader.hasMoreRows() && inputReader.hasMoreRows()) {
            val keyRow = keyReader.readRow() as ImageLineInt
            val inputRow = inputReader.readRow() as ImageLineInt
            val diffRow = ImageLineInt(diffInfo)

            val keyPixels = keyRow.scanline.toPixels()
            val inputPixels = inputRow.scanline.toPixels()

            mapFromEachWithPad(keyPixels, inputPixels, Pixel.EMPTY) { key, input ->
                if (key != input) {
                    diffCount++
                    Pixel.RED
                } else {
                    key mixWith Pixel.TRANS_BLACK
                }
            }.forEachIndexed { offset, pixel ->
                diffRow.writePixelAtOffset(offset, pixel)
            }

            diffWriter.writeRow(diffRow)
            rowIndex++
        }

        val redRow = ImageLineInt(diffInfo)
        for (offset in 0 until diffInfo.cols) {
            redRow.writePixelAtOffset(offset, Pixel.RED)
        }
        while (rowIndex < diffInfo.rows) {
            diffWriter.writeRow(redRow)
        }

        keyReader.end()
        inputReader.end()
        diffWriter.end()

        Assert.assertTrue("There were $diffCount(${prettyPrintPercent(diffCount, diffInfo.rows * diffInfo.cols)}) variant pixels in processed images", diffCount == 0)
    }

    private fun prettyPrintPercent(num: Int, div: Int): String {
        val percentage = (num.toFloat() / div.toFloat()) * 100F
        return "%.2f%%%n".format(percentage).trim()
    }

    private fun ImageLineInt.writePixelAtOffset(offset: Int, pixel: Pixel) {
        scanline[offset * 4] = pixel.r
        scanline[offset * 4 + 1] = pixel.g
        scanline[offset * 4 + 2] = pixel.b
        scanline[offset * 4 + 3] = pixel.a
    }

    private fun IntArray.toPixels(): List<Pixel> {
        return asIterable()
                .buffer(4)
                .map { Pixel(it[0], it[1], it[2], it[3]) }
    }

    private fun <T, R> mapFromEachWithPad(first: List<T>, second: List<T>, pad: T, action: (first: T, second: T) -> R): List<R> {
        val size = max(first.size, second.size)
        val result = ArrayList<R>(size)
        for (index in 0 until size) {
            val f = first.getOrElse(index) { pad }
            val s = second.getOrElse(index) { pad }
            result.add(action(f, s))
        }

        return result.toList()
    }
}

private fun <T> Iterable<T>.buffer(bufferSize: Int): List<List<T>> {
    require(bufferSize > 0)
    val iter = iterator()
    val result = ArrayList<List<T>>(count() / bufferSize + 1)
    var nextBuf = ArrayList<T>(bufferSize)
    while (iter.hasNext()) {
        nextBuf.add(iter.next())
        if (nextBuf.size == bufferSize) {
            result.add(nextBuf)
            nextBuf = ArrayList<T>(bufferSize)
        }
    }
    return result
}
