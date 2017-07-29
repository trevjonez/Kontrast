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

import io.reactivex.Observable.fromArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class KontrastTest {
    companion object {
        @JvmStatic
        @Parameters(name = "{index}: {0}")
        fun params(): Iterable<Any> {
            val dir = File(System.getProperty("KontrastKeyDir") ?: throw NullPointerException("KontrastKeyDir Required"))
            val inputRoot = File(System.getProperty("KontrastInputDir") ?: throw NullPointerException("KontrastInputDir Required"))
            return childDirectories(dir) //classDirs
                    .concatMap(this::childDirectories) //methodDirs
                    .concatMap(this::childDirectories) //testDirs
                    .map { KontrastCase(it, File(inputRoot, "${it.parentFile.parentFile.name}/${it.parentFile.name}/${it.name}")) }
                    .doOnNext { println("KCase: $it") }
                    .toList()
                    .blockingGet()
        }

        private fun childDirectories(it: File) = fromArray(*it.listFiles()).filter(File::isDirectory)
    }

    @Test
    fun kontrastCase(case: KontrastCase) {

    }
}

class KontrastCase(val keyDir: File, val inputDir: File) {
    override fun toString(): String {
        val className: String = keyDir.parentFile.parentFile.name
        val methodName: String = keyDir.parentFile.name
        val testKey: String = keyDir.name

        return if (methodName == testKey)
            "$className/$methodName"
        else
            "$className/$methodName/$testKey"
    }
}