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

package com.example.tjones.myapplication

import com.example.tjones.myapplication.databinding.DatabindingUserListItemBinding
import com.trevjonez.kontrast.CartesianProduct
import com.trevjonez.kontrast.KontrastTest
import com.trevjonez.kontrast.KontrastTestBase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class DatabindingListItemTest(val width: Int, val name: String) : KontrastTestBase() {
    companion object {
        @JvmStatic
        @Parameters
        fun params(): Collection<Array<Any>> {
            return CartesianProduct.create(listOf(320), listOf("Josh Doe", "Jerry Doe", "Jake Doe"))
        }
    }

    @Test
    @KontrastTest
    fun databoundCard() {
        val view = inflateOnMainThread { inflater ->
            DatabindingUserListItemBinding.inflate(inflater).apply {
                username.text = name
            }
        }.root

        kontrastRule.ofView(view, "databoundCard-$width-$name")
                .setWidthDp(width)
                .extra("name", name)
                .capture()
    }
}