/*
 *    Copyright 2018 Trevor Jones
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

import com.trevjonez.kontrast.KontrastPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Turns out that much of the flake we see in CI is segfaults in ART (on emulators) that are very indicative of a race condition.
 * The speculation is that attempting to invoke the instrumentation command too soon after the adb install command is causing ART to crash.
 * This task is intended to give ART some time to settle and hopefully not segfault.
 */
open class ArtSettlingTask: DefaultTask() {

    var waitingPeriodMilli: Long = KontrastPlugin.DEFAULT_SETTLING_TIME

    @TaskAction
    fun invoke() {
        Thread.sleep(waitingPeriodMilli)
    }
}