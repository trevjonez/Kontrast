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

package com.trevjonez.kontrast.app

import android.annotation.SuppressLint
import android.support.v4.content.ContextCompat
import android.widget.FrameLayout
import com.trevjonez.kontrast.KontrastTest
import com.trevjonez.kontrast.KontrastTestBase
import com.trevjonez.kontrast.app.databinding.CardLayoutBinding
import org.junit.Test

@SuppressLint("SetTextI18n")
class CardLayoutKontrastTest : KontrastTestBase() {

    @Test
    @KontrastTest
    fun johnDoeCard() {
        val view = kontrastRule.processOnMainThread {
            FrameLayout(activity).also {
                CardLayoutBinding.inflate(layoutInflater, it, true).apply {
                    username.text = "John Doe"
                }
            }
        }

        kontrastRule.ofView(view)
                .setWidthDp(320)
                .extra("Username", "John Doe")
                .capture()
    }

    @Test
    @KontrastTest
    fun janeDoeCard() {
        val view = kontrastRule.processOnMainThread {
            FrameLayout(activity).also {
                CardLayoutBinding.inflate(layoutInflater, it, true).apply {
                    username.text = "Jane Doe"
                    avatar.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_accessibility_black_24dp))
                }
            }
        }

        kontrastRule.ofView(view)
                .setWidthDp(320)
                .extra("Username", "Jane Doe")
                .capture()
    }

    @Test
    @KontrastTest
    fun jackDoeCard() {
        val view = kontrastRule.processOnMainThread {
            FrameLayout(activity).also {
                CardLayoutBinding.inflate(layoutInflater, it, true).apply {
                    username.text = "Jack Doe"
                    avatar.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_assignment_ind_black_24dp))
                }
            }
        }

        kontrastRule.ofView(view)
                .setWidthDp(320)
                .extra("Username", "Jack Doe")
                .capture()
    }
}