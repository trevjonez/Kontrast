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

import java.lang.Math.abs

data class Pixel(val r: Int, val g: Int, val b: Int, val a: Int) {
    companion object {
        val EMPTY = Pixel(0, 0, 0, 0)
        val RED = Pixel(255, 0, 0, 255)
        val TRANS_BLACK = Pixel(1, 1, 1, 128)

        val MIX_FACTOR = .5F
        val INV_MIX_FACTOR = -.5F
    }

    infix fun mixWith(other: Pixel): Pixel {
        return Pixel(
                ((r * MIX_FACTOR) - (abs(r - other.r) / 2 * INV_MIX_FACTOR)).toInt(),
                ((g * MIX_FACTOR) - (abs(g - other.g) / 2 * INV_MIX_FACTOR)).toInt(),
                ((b * MIX_FACTOR) - (abs(b - other.b) / 2 * INV_MIX_FACTOR)).toInt(),
                ((a * MIX_FACTOR) - (abs(a - other.a) / 2 * INV_MIX_FACTOR)).toInt())
    }
}