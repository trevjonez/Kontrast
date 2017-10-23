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

package com.trevjonez.kontrast.adb

import com.trevjonez.kontrast.internal.doOnFirst
import com.trevjonez.kontrast.internal.toObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.net.Socket

fun getEmulatorName(adbDevice: AdbDevice): AdbDevice {
    val sendSubject = PublishSubject.create<String>()
    val emulatorName = Socket("localhost", adbDevice.portNumber)
            .toObservable(sendSubject)
            .subscribeOn(Schedulers.io())
            .skipWhile { it.trim() != "OK" }
            .doOnFirst { sendSubject.onNext("avd name") }
            .filter { it != "OK" }
            .firstOrError()
            .blockingGet()

    return adbDevice.copy(alias = emulatorName)
}