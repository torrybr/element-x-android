/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("io.element.android-library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.anvil)
}

anvil {
    generateDaggerFactories.set(true)
}

android {
    namespace = "io.element.android.libraries.androidtools.impl"

    dependencies {
        anvil(projects.anvilcodegen)
        implementation(libs.dagger)
        implementation(projects.libraries.di)
        implementation(projects.anvilannotations)

        api(projects.libraries.androidtools.api)

        testImplementation(libs.test.junit)
        testImplementation(libs.test.truth)
        testImplementation(projects.libraries.androidtools.test)
    }
}
