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

import android.content.Context
import java.io.File

fun getOutputDirectory(context: Context, subDirectoryPath: String): File {
    return if(isRunningOnDevice()) {
        context.getExternalFilesDir(subDirectoryPath)
    } else { /* Probably Robolectric? */
        TODO("Robolectric does not support view rendering or real bitmap behaviour this must be ran on device/emu.\n" +
             "I would love for this to work in the JVM but the work needed at this point is out of scope of MVP")
        File("${getProjectBuildDir()}${File.separator}$subDirectoryPath")
    }
}

fun isRunningOnDevice() = System.getProperty("java.vendor").contains("android", true)

fun getProjectBuildDir(): String = File("build").absolutePath