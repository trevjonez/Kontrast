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
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import java.io.File
import java.util.UUID

class LayoutHelper(val view: View, val className: String, val methodName: String, val testRunnerNotifier: (File) -> Unit) {
    val outputDirectory: File = getOutputDirectory(view.context,
            "Kontrast${File.separator}$className${File.separator}$methodName${File.separator}${UUID.randomUUID()}")

    var widthSpec = makeMeasureSpec(0, UNSPECIFIED)
    var heightSpec = makeMeasureSpec(0, UNSPECIFIED)
    var description: String? = null

    val extras = mutableMapOf<String, String>()

    fun setWidthDp(dp: Int) = setWidthPx(dp.dp2Px())

    fun setWidthPx(px: Int) = apply {
        widthSpec = makeMeasureSpec(px, EXACTLY)
    }

    fun setHeightDp(dp: Int) = setHeightPx(dp.dp2Px())

    fun setHeightPx(px: Int) = apply {
        heightSpec = makeMeasureSpec(px, EXACTLY)
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

    @SuppressLint("SetWorldReadable")
    fun capture() {
        val snapshot = layout().draw()
        outputDirectory.mkdirs()
        File(outputDirectory, "image.png").apply {
            createNewFile()
            outputStream().use {
                snapshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            setReadable(true, false)
        }

        //TODO meta info //json file?

        //TODO layout hierarchy //uiautomator dump [file]

        testRunnerNotifier(outputDirectory)
    }

    private fun Int.dp2Px(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                this.toFloat(),
                view.context.resources.displayMetrics).toInt()
    }
}