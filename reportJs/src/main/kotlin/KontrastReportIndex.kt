import org.w3c.dom.HTMLCollection
import org.w3c.dom.asList
import org.w3c.dom.get
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
