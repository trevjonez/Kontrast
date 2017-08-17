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

object CartesianProduct {
    /**
     * Create a cartesian product of items.
     * Intended for creating permutations for parameterized test input.
     */
    @JvmStatic fun create(vararg items: List<Any>): List<Array<Any>> {
        require(items.size >= 2)
        return items.toList()
                .drop(2)
                .fold(items[0] * items[1]) { cp, ls -> cp * ls }
                .map { flattenList(it).toTypedArray() }
    }

    private operator fun List<Any>.times(other: List<Any>): List<List<Any>> {
        return mutableListOf<List<Any>>().also { result ->
            this.forEach { thisItem ->
                other.forEach { otherItem ->
                    result.add(listOf(thisItem, otherItem))
                }
            }
        }
    }

    private fun flattenList(nestList: List<Any>): List<Any> {
        val result = mutableListOf<Any>()

        fun flatten(list: List<Any>) {
            list.forEach {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    !is List<*> -> result.add(it)
                    else        -> flatten(it as List<Any>)
                }
            }
        }

        flatten(nestList)
        return result
    }
}