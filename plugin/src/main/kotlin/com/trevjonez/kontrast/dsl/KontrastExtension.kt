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

package com.trevjonez.kontrast.dsl

import org.gradle.api.Project
import java.io.File

open class KontrastExtension(project: Project) {

    var testKeyRoot = File(project.projectDir, "Kontrast")

    fun testKeyRoot(file: File) {
        testKeyRoot = file
    }

    val deviceAliases = mutableMapOf<String, String>()

    fun deviceAlias(deviceId: String, alias: String) {
        deviceAliases.put(deviceId, alias)
    }

    fun deviceAlias(aliasPair: Pair<String, String>) {
        deviceAliases.put(aliasPair.first, aliasPair.second)
    }

    val targetVariants = mutableListOf<String>()

    fun targetVariants(variantNames: List<String>) {
        targetVariants.addAll(variantNames)
    }

    fun targetVariant(variantName: String) {
        targetVariants.add(variantName)
    }
}