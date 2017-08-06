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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_WORLD_READABLE
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import java.io.File

@Suppress("DEPRECATION")
@SuppressLint("WorldReadableFiles")
fun getOutputDirectory(context: Context, subDirectoryPath: String): File {
    require(isRunningOnDevice())
    val outputRoot = "${context.packageName}${File.separator}$subDirectoryPath"
    return if (SDK_INT >= LOLLIPOP)
        context.getExternalFilesDir(subDirectoryPath)
    else
        File(context.getDir("Kontrast", MODE_WORLD_READABLE), outputRoot)
}

fun isRunningOnDevice() = System.getProperty("java.vendor").contains("android", true)

fun getProjectBuildDir(): String = File("build").absolutePath