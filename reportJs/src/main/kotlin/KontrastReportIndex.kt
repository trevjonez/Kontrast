import kotlinx.html.TagConsumer
import kotlinx.html.dom.append
import kotlinx.html.js.br
import kotlinx.html.js.span
import org.w3c.dom.Element
import org.w3c.dom.HTMLCollection
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.get
import org.w3c.fetch.Request
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass

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

fun main(args: Array<String>) {
    window.onload = {
        setupTabBarListeners()
        setupLogcatToggleListeners()
    }
}

private fun setupLogcatToggleListeners() {
    document.getElementsByClassName("logcat-button").asList()
            .forEach { button ->
                button.addEventListener("click", {
                    button.getLogcatArea().apply {
                        if (classList.contains("logcat-hidden")) {
                            showLogcatArea(button)
                        } else {
                            hideLogcatArea(button)
                        }
                    }
                })
            }
}

private fun setupTabBarListeners() {
    document.getElementsByClassName("AllTab")[0]?.addEventListener("click", {
        document.getElementsByClassName("skipped").showAll()
        document.getElementsByClassName("success").showAll()
        document.getElementsByClassName("failed").showAll()
    })

    document.getElementsByClassName("PassedTab")[0]?.addEventListener("click", {
        document.getElementsByClassName("skipped").hideAll()
        document.getElementsByClassName("success").showAll()
        document.getElementsByClassName("failed").hideAll()
    })

    document.getElementsByClassName("FailedTab")[0]?.addEventListener("click", {
        document.getElementsByClassName("skipped").hideAll()
        document.getElementsByClassName("success").hideAll()
        document.getElementsByClassName("failed").showAll()
    })

    document.getElementsByClassName("SkippedTab")[0]?.addEventListener("click", {
        document.getElementsByClassName("skipped").showAll()
        document.getElementsByClassName("success").hideAll()
        document.getElementsByClassName("failed").hideAll()
    })
}

private fun Element.getLogcatArea(): Element {
    return parentElement?.parentElement?.getElementsByClassName("logcat-area")?.get(0)!!
}

private fun Element.showLogcatArea(button: Element) {
    classList.remove("logcat-hidden")
    button.getElementsByClassName("button-text")[0]!!.apply {
        textContent = "Hide Logcat"
    }
    if (textContent.isNullOrBlank()) {
        val logcatFile = button.getLogcatFile()

        loadLogcatFile(logcatFile, this)
    }
}

private fun Element.hideLogcatArea(button: Element) {
    classList.add("logcat-hidden")
    button.getElementsByClassName("button-text")[0]!!.apply {
        textContent = "Show Logcat"
    }
}

private fun Element.getLogcatFile(): String {
    return getElementsByClassName("logcat-file")[0].let { it!!.textContent!! }
}

private fun loadLogcatFile(logcatFile: String, logcatArea: Element) {
    window.fetch(Request(logcatFile)).then { response ->
        response.text().then { responseText ->
            spanAndAppendLogcatLines(responseText, logcatArea)
        }
    }
}

private fun spanAndAppendLogcatLines(responseText: String, logcatArea: Element) {
    responseText.lines()
            .filter(String::isNotBlank)
            .forEach { line ->
                logcatArea.append {
                    logcatLineSpan(line)
                    br { }
                }
            }
}

private fun TagConsumer<HTMLElement>.logcatLineSpan(line: String) {
    span("${getLogcatClass(line)} logcat-text") {
        text(line)
    }
}

private fun HTMLCollection.hideAll() {
    asList().forEach {
        it.addClass("hidden-test-case")
    }
}

private fun HTMLCollection.showAll() {
    asList().forEach {
        it.removeClass("hidden-test-case")
    }
}

private fun getLogcatClass(line: String): String {
    return when (line.first { it.isLogcatLetter() }) {
        'V'  -> "logcat-verbose"
        'D'  -> "logcat-debug"
        'I'  -> "logcat-info"
        'W'  -> "logcat-warning"
        'E'  -> "logcat-error"
        'A'  -> "logcat-assert"
        else -> throw IllegalArgumentException("Unknown logcat line type")
    }
}

private fun Char.isLogcatLetter(): Boolean {
    //language=RegExp
    return toString().matches("""[vdiweaVDIWEA]""")
}