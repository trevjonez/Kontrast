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

package com.trevjonez.kontrast.adb

enum class AdbInstallFlag(val cliFlag: String) {
    FORWARD_LOCK_APPLICATION("-l"),
    REPLACE_EXISTING("-r"),
    ALLOW_TEST_PACKAGES("-t"),
    INSTALL_ON_SDCARD("-s"),
    ALLOW_VERSION_DOWNGRADE("-d"),
    GRANT_ALL_RUNTIME_PERMISSIONS("-g")
}