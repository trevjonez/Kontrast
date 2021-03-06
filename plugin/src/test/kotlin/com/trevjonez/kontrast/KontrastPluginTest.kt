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

import org.apache.commons.io.FileUtils.copyDirectory
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class KontrastPluginTest {
    @Test
    fun renderAndCaptureTestKey() {
        val kontrastVersion = System.getProperty("KontrastVersion")

        val projectDir = File("build/pluginTestProjects/renderAndCaptureTestKey").apply {
            deleteRecursively()
            mkdirs()
            copyDirectory(File(".", "../app"), File(this, "app"))
            File(this, "local.properties").writeText("sdk.dir=${System.getenv("ANDROID_HOME")}")
            File(this, "settings.gradle").writeText("include ':app'")
            File(this, "gradle.properties").writeText(rootGradlePropertiesContents())
            File(this, "build.gradle").writeText(rootGradleFileContents(kontrastVersion))
            File(this, "app/kontrast.gradle").writeText(kontrastGradleContents(kontrastVersion))
        }


        GradleRunner.create()
                .withProjectDir(projectDir)
                .forwardOutput()
                .withArguments("app:captureDebugTestKeys_Nexus_5X_API_O")
                .build()

        assertThat(File(projectDir, "app/Kontrast/debug/Nexus_5X_API_O/com.trevjonez.kontrast.app.CardLayoutKontrastTest")).isDirectory().satisfies {
            assertThat(File(it, "janeDoeCard/janeDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "image.png")).exists()
                assertThat(File(it, "extras.json")).exists()
            }
            assertThat(File(it, "johnDoeCard/johnDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "image.png")).exists()
                assertThat(File(it, "extras.json")).exists()
            }
            assertThat(File(it, "jackDoeCard/jackDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "image.png")).exists()
                assertThat(File(it, "extras.json")).exists()
            }
        }
    }

    @Test
    fun compareRenderWithTestKeys() {
        val kontrastVersion = System.getProperty("KontrastVersion")
        val projectDir = File("build/pluginTestProjects/compareRenderWithTestKeys").apply {
            deleteRecursively()
            mkdirs()
            copyDirectory(File(".", "../app"), File(this, "app"))
            copyDirectory(File(javaClass.getResource("/Kontrast").path), File(this, "app/Kontrast"))
            File(this, "local.properties").writeText("sdk.dir=${System.getenv("ANDROID_HOME")}")
            File(this, "settings.gradle").writeText("include ':app'")
            File(this, "gradle.properties").writeText(rootGradlePropertiesContents())
            File(this, "build.gradle").writeText(rootGradleFileContents(kontrastVersion))
            File(this, "app/kontrast.gradle").writeText(kontrastGradleContents(kontrastVersion))
        }

        GradleRunner.create()
                .withProjectDir(projectDir)
                .forwardOutput()
                .withArguments("app:testDebugKontrastTest_Nexus_5X_API_O")
                .buildAndFail()

        assertThat(File(projectDir, "app/build/reports/Kontrast/debug/Nexus_5X_API_O/images/com.trevjonez.kontrast.app.CardLayoutKontrastTest")).isDirectory().satisfies {
            assertThat(File(it, "jackDoeCard/jackDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "input.png")).exists()
            }
            assertThat(File(it, "janeDoeCard/janeDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "diff.png")).exists()
                assertThat(File(it, "input.png")).exists()
                assertThat(File(it, "key.png")).exists()
            }
            assertThat(File(it, "johnDoeCard/johnDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "diff.png")).exists()
                assertThat(File(it, "input.png")).exists()
                assertThat(File(it, "key.png")).exists()
            }
            assertThat(File(it, "joshDoeCard/joshDoeCard")).isDirectory().satisfies {
                assertThat(File(it, "key.png")).exists()
            }
        }
    }

    @Test
    fun runWithKnownBadTestsOnDevice() {
        val kontrastVersion = System.getProperty("KontrastVersion")
        val projectDir = File("build/pluginTestProjects/runWithKnownBadTestsOnDevice").apply {
            deleteRecursively()
            mkdirs()
            copyDirectory(File(".", "../app"), File(this, "app"))
            copyDirectory(File(javaClass.getResource("/Kontrast").path), File(this, "app/Kontrast"))
            File(this, "local.properties").writeText("sdk.dir=${System.getenv("ANDROID_HOME")}")
            File(this, "settings.gradle").writeText("include ':app'")
            File(this, "gradle.properties").writeText(rootGradlePropertiesContents())
            File(this, "build.gradle").writeText(rootGradleFileContents(kontrastVersion))
            File(this, "app/kontrast.gradle").writeText(kontrastGradleContents(kontrastVersion))
            File(this, "app/src/androidTest/java/com/trevjonez/kontrast/app/CardLayoutKontrastTest.kt").apply {
                val lines = readLines()
                val keep = lines.subList(0, lines.size - 1)
                delete()
                keep.forEach { appendText(it + System.lineSeparator()) }
                appendText(extraFailingTests)
                appendText("}")
            }
        }

        GradleRunner.create()
                .withProjectDir(projectDir)
                .forwardOutput()
                .withArguments("app:testDebugKontrastTest_Nexus_5X_API_O", "--info", "--stacktrace")
                .buildAndFail()
    }

    val extraFailingTests = """
    @Test
    @KontrastTest
    fun failingAssertOnDeviceTest() {
        org.junit.Assert.fail("Failed assertion test")
    }

    @Test
    @KontrastTest
    fun failAssumeOnDeviceTest() {
        org.junit.Assume.assumeTrue("Failed assumption test", false)
    }

    @Test
    @KontrastTest
    fun errorOnDeviceTest() {
        requireNotNull(null)
    }
    """

    fun rootGradleFileContents(kontrastVersion: String) = """
buildscript {
    ext.kotlin_version = '1.3.41'
    ext.android_plugin_version = '3.4.2'

    ext.min_sdk = 16
    ext.target_sdk = 28

    repositories {
        google()
        jcenter()
        mavenLocal()
    }
    dependencies {
        classpath group: 'org.jetbrains.kotlin', name: 'kotlin-gradle-plugin', version: kotlin_version
        classpath group: 'com.android.tools.build', name: 'gradle', version: android_plugin_version
        classpath group: 'com.github.trevjonez.Kontrast', name: 'plugin', version: '$kontrastVersion'
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
    }
}
"""

    fun kontrastGradleContents(kontrastVersion: String) = """
apply plugin: 'kontrast'

dependencies {
    debugApi group: 'com.github.trevjonez.Kontrast', name: 'appClient', version: '$kontrastVersion'
}
"""

    fun rootGradlePropertiesContents() = """android.useAndroidX=true
android.enableJetifier=true
"""
}
