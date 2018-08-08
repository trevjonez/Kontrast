/*
 *    Copyright 2018 Trevor Jones
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

package com.trevjonez.kontrast.runner

import android.content.Context
import androidx.test.orchestrator.instrumentationlistener.OrchestratedInstrumentationListener
import com.trevjonez.kontrast.runner.OrchestratingAndroidJunitTestRunner.Args
import org.junit.runner.Description

class AndroidxOrchestratorRunner :
        OrchestratingAndroidJunitTestRunner<Args, AndroidxOrchestratorRunner.Listener>(),
        OrchestratedInstrumentationListener.OnConnectListener {

    override fun onOrchestratorConnect() {
        start()
    }

    override fun makeListener() =
            AndroidxOrchestratorRunner.Listener(OrchestratedInstrumentationListener(this))

    override fun parseRunnerArgs() =
            Args(this, arguments)

    class Listener(val actual: OrchestratedInstrumentationListener) :
            OrchestratingAndroidJunitTestRunner.Listener() {

        override fun connect(context: Context) {
            actual.connect(context)
        }

        override fun addTests(description: Description) {
            actual.addTests(description)
        }
    }
}