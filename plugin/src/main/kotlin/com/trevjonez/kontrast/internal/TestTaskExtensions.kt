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

package com.trevjonez.kontrast.internal

import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

fun Test.testEvents(): Observable<Pair<TestDescriptor, TestResult>> {
    val subject = ReplaySubject.create<Pair<TestDescriptor, TestResult>>()

    addTestListener(object : TestListener {
        override fun beforeSuite(descriptor: TestDescriptor) {}
        override fun afterSuite(descriptor: TestDescriptor, result: TestResult) {
            if (descriptor.name.contains("Gradle Test Run"))
                subject.onComplete()
        }

        override fun beforeTest(descriptor: TestDescriptor) {}
        override fun afterTest(descriptor: TestDescriptor, result: TestResult) {
            subject.onNext(descriptor to result)
        }
    })

    return subject
}