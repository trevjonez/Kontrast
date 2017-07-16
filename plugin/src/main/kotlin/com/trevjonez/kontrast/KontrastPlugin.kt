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

import org.gradle.api.Plugin
import org.gradle.api.Project

class KontrastPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        //PTV -> Per testable variant
        //PCD -> Per connected device

        //A: Install main apk task (PTV)

        //B: Install test apk task (PTV)

        //C: Run kontrast test task (PTV)
        //      Clear any previous test run data from build directory (PCD)
        //      Scan the adb output and pull test outputs as produced
        //      Delete on device test outputs once they have been pulled

        //D: Create task to record current test run output as the test key (PTV)

        //E: Create task to perform image diffing and report gen inputs (PTV)

        //F: Create task to generate report html page and junit formatted xml report (PTV)
    }
}