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

import android.view.View
import android.view.ViewGroup
import java.io.Closeable
import java.util.WeakHashMap

class WindowAttachment {

    enum class AttachMethod(val methodName: String) {
        ATTACH("onAttachedToWindow"), DETACH("onDetachedFromWindow");

        operator fun invoke(view: View) {
            View::class.java.getDeclaredMethod(methodName).apply {
                isAccessible = true
            }.invoke(view)

            if (view is ViewGroup) {
                for (childIndex in 0 until view.childCount) {
                    this(view.getChildAt(childIndex))
                }
            }
        }
    }

    companion object {
        val attachmentSet = WeakHashMap<View, Unit>()

        fun emulateAttach(view: View): Closeable {
            if (view.windowToken != null || attachmentSet.containsKey(view)) return NoopDetach

            attachmentSet.put(view, Unit)
            AttachMethod.ATTACH(view)

            return EmulatedDetach(view)
        }

    }

    object NoopDetach : Closeable {
        override fun close() {

        }
    }

    class EmulatedDetach(val view: View) : Closeable {
        override fun close() {
            AttachMethod.DETACH(view)
            attachmentSet.remove(view)
        }
    }


}