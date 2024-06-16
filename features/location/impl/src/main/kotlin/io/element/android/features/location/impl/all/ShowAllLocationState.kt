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

package io.element.android.features.location.impl.all

import io.element.android.features.location.impl.all.model.MapProvider
import io.element.android.features.location.impl.all.model.ShowLocationItems

/**
 * defines the state for the "Show All Location" feature, including all the variables and data structures
 * that represent the UI state at any given time
 */
data class ShowAllLocationState(
    val permissionDialog: Dialog,
    val hasLocationPermission: Boolean,
    val showTileProviderPicker: Boolean,
    val appName: String,
    val roomName: String,
    val eventSink: (ShowAllLocationEvents) -> Unit,
    val showLocationItems: ShowLocationItems,
    val isSharingLocation: Boolean,
    val mapTileProvider: MapProvider,
) {
    sealed interface Dialog {
        data object None : Dialog
        data object PermissionRationale : Dialog
        data object PermissionDenied : Dialog
    }

    val styleUrl: String
        get() = "https://api.maptiler.com/maps/" + mapTileProvider.mapKey + "/style.json?key=" + "4N19bSbSelzpOSfUibeB"
}
