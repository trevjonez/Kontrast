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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.support.test.InstrumentationRegistry
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import java.io.File

class LayoutHelper(val view: View, val className: String, val methodName: String, val testKey: String) {
    companion object {
        const val KONTRAST_SIGNAL_CODE = 42
        const val CLASS_NAME = "ClassName"
        const val METHOD_NAME = "MethodName"
        const val TEST_KEY = "TestKey"
        const val EXTRAS = "Extras"
        const val DESCRIPTION = "Description"
        const val OUTPUT_DIR = "OutputDir"
        const val KONTRAST = "Kontrast"

    }

    val outputDirectory: File by lazy {
        val subDirectoryPath = "Kontrast${File.separator}$className${File.separator}$methodName${File.separator}$testKey"
        view.context.getExternalFilesDir(subDirectoryPath)
    }

    var widthSpec = makeMeasureSpec(0, UNSPECIFIED)

    var heightSpec = makeMeasureSpec(0, UNSPECIFIED)

    var description: String? = null

    val extras = mutableMapOf<String, String>().apply {
        put("Width", "wrap_content")
        put("Height", "wrap_content")
    }

    fun setWidthDp(dp: Int) = apply {
        widthSpec = makeMeasureSpec(dp.dp2Px(), EXACTLY)
        extra("Width", "${dp}dp")
    }

    fun setWidthPx(px: Int) = apply {
        widthSpec = makeMeasureSpec(px, EXACTLY)
        extra("Width", "${px}px")
    }

    fun setHeightDp(dp: Int) = apply {
        heightSpec = makeMeasureSpec(dp.dp2Px(), EXACTLY)
        extra("Height", "${dp}dp")
    }

    fun setHeightPx(px: Int) = apply {
        heightSpec = makeMeasureSpec(px, EXACTLY)
        extra("Height", "${px}px")
    }

    fun description(value: String) = apply {
        description = value
    }

    fun extra(key: String, value: String) = apply {
        extras.put(key, value)
    }

    fun layout() = apply {
        do {
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        } while (view.isLayoutRequested)
    }

    fun draw(): Bitmap {
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).apply {
            WindowAttachment.emulateAttach(view).use {
                view.draw(Canvas(this))
            }
        }
    }

    @SuppressLint("SetWorldReadable", "NewApi", "SdCardPath")
    fun capture() {
        val snapshot = layout().draw()
        if (!outputDirectory.mkdirs() && !outputDirectory.exists()) {
            Log.e("LayoutHelper", "Unable to create output dir(s) ${outputDirectory.absolutePath}")
        }
        File(outputDirectory, "image.png").apply {
            createNewFile()
            outputStream().use {
                snapshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            setReadable(true, false)
        }

        //TODO layout hierarchy //uiautomator dump [file]

        val outputPath = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            "/sdcard" + outputDirectory.absolutePath.removePrefix("/storage/emulated/0")
        else
            outputDirectory.absolutePath

        val data = Bundle().apply {
            putString("$KONTRAST:$CLASS_NAME", className)
            putString("$KONTRAST:$METHOD_NAME", methodName)
            putString("$KONTRAST:$TEST_KEY", testKey)
            putString("$KONTRAST:$EXTRAS", extras.map({ (k, v) -> """"$k"KVP_DELIMITER"$v"""" }).joinToString(prefix = "[", postfix = "]", separator = "EXTRA_DELIMITER"))
            putString("$KONTRAST:$DESCRIPTION", description)
            putString("$KONTRAST:$OUTPUT_DIR", outputPath)
        }

        InstrumentationRegistry.getInstrumentation().sendStatus(KONTRAST_SIGNAL_CODE, data)
    }

    private fun Int.dp2Px(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                         this.toFloat(),
                                         view.context.resources.displayMetrics).toInt()
    }
}