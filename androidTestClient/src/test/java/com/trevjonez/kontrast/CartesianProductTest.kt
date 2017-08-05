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

import org.assertj.core.api.Assertions
import org.junit.Test

class CartesianProductTest {
    @Test
    fun minimumCase() {
        Assertions.assertThat(CartesianProduct.create(listOf("A", "B"), listOf(1, 2)))
                .containsExactly(listOf("A", 1), listOf("A", 2), listOf("B", 1), listOf("B", 2))
    }

    @Test
    fun polyCase() {
        Assertions.assertThat(CartesianProduct.create(listOf("A", "B"), listOf(1, 2), listOf("X", "Y", "Z")))
                .containsExactly(listOf("A", 1, "X"), listOf("A", 1, "Y"), listOf("A", 1, "Z"),
                                 listOf("A", 2, "X"), listOf("A", 2, "Y"), listOf("A", 2, "Z"),
                                 listOf("B", 1, "X"), listOf("B", 1, "Y"), listOf("B", 1, "Z"),
                                 listOf("B", 2, "X"), listOf("B", 2, "Y"), listOf("B", 2, "Z"))
    }
}