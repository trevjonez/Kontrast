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

import com.trevjonez.kontrast.adb.AdbDevice
import io.reactivex.subjects.BehaviorSubject
import org.gradle.api.tasks.TaskAction

open class SelectDeviceTask : AdbCommandTask() {

    internal val resultSubject: BehaviorSubject<AdbDevice> = BehaviorSubject.create()

    @TaskAction
    fun invoke() {
        //TODO provide optional dsl to override this selection
        adb.devices()
                .map { it.first() }
                .subscribe(resultSubject::onNext) {
            throw IllegalStateException("No devices found", it)
        }
    }
}