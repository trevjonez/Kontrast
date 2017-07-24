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

import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File


class RunKontrastTestsTaskTest {

    @Test
    fun testParsingIntegrationCheck() {
        val basicInput = """
INSTRUMENTATION_STATUS: numtests=1
INSTRUMENTATION_STATUS: stream=
com.trevjonez.kontrast.CardLayoutKontrastTest:
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: test=johnDoeCard
INSTRUMENTATION_STATUS: class=com.trevjonez.kontrast.CardLayoutKontrastTest
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: numtests=1
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
INSTRUMENTATION_STATUS: test=johnDoeCard
INSTRUMENTATION_STATUS: class=com.trevjonez.kontrast.CardLayoutKontrastTest
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: Kontrast:TestKey=johnDoeCard
INSTRUMENTATION_STATUS: Kontrast:MethodName=johnDoeCard
INSTRUMENTATION_STATUS: Kontrast:Description=null
INSTRUMENTATION_STATUS: Kontrast:ClassName=com.trevjonez.kontrast.CardLayoutKontrastTest
INSTRUMENTATION_STATUS: Kontrast:Extras=[]
INSTRUMENTATION_STATUS: Kontrast:OutputDir=/storage/emulated/0/Android/data/com.trevjonez.kontrast.app/files/Kontrast/com.trevjonez.kontrast.CardLayoutKontrastTest/johnDoeCard/johnDoeCard
INSTRUMENTATION_STATUS_CODE: 42
INSTRUMENTATION_RESULT: stream=

Time: 0.695

OK (1 test)


INSTRUMENTATION_CODE: -1
"""
        val testSub = Observable.fromIterable(basicInput.split('\n')).parseTestCases().test()

        assertThat(testSub.values()[0]).isEqualTo(TestOutput("johnDoeCard",
                                                             "johnDoeCard",
                                                             null,
                                                             "com.trevjonez.kontrast.CardLayoutKontrastTest",
                                                             mapOf(),
                                                             File("/storage/emulated/0/Android/data/com.trevjonez.kontrast.app/files/Kontrast/com.trevjonez.kontrast.CardLayoutKontrastTest/johnDoeCard/johnDoeCard")))
    }
}