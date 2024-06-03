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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.location.api.Location
import io.element.android.features.location.impl.all.model.ShowLocationItem
import io.element.android.features.location.impl.all.model.ShowLocationItemState
import io.element.android.features.location.impl.all.model.ShowLocationItems

private const val APP_NAME = "ApplicationName"

/**
 *  inject or provide the state for previews or testing, ensuring that the UI can be rendered with various
 *  predefined states for development or testing purposes
 */
class ShowAllLocationStateProvider : PreviewParameterProvider<ShowAllLocationState> {
    override val values: Sequence<ShowAllLocationState>
        get() = sequenceOf(
            aShowAllLocationState(),
            aShowAllLocationState(
                permissionDialog = ShowAllLocationState.Dialog.PermissionDenied,
            ),
            aShowAllLocationState(
                permissionDialog = ShowAllLocationState.Dialog.PermissionRationale,
            ),
            aShowAllLocationState(
                hasLocationPermission = true,
            ),
            aShowAllLocationState(
                hasLocationPermission = true,
                isTrackMyLocation = true,
            ),
            aShowAllLocationState(
                description = "My favourite place!",
            ),
            aShowAllLocationState(
                description = "For some reason I decided to to write a small essay that wraps at just two lines!",
            ),
            aShowAllLocationState(
                description = "For some reason I decided to write a small essay in the location description. " +
                    "It is so long that it will wrap onto more than two lines!",
            ),
        )
}

fun aShowAllLocationState(
    permissionDialog: ShowAllLocationState.Dialog = ShowAllLocationState.Dialog.None,
    location: Location = Location(1.23, 2.34, 4f),
    description: String? = null,
    hasLocationPermission: Boolean = false,
    isTrackMyLocation: Boolean = false,
    appName: String = APP_NAME,
    eventSink: (ShowAllLocationEvents) -> Unit = {},

    ) = ShowAllLocationState(
    showLocationItems = TODO(),
    permissionDialog = permissionDialog,
    description = description,
    hasLocationPermission = hasLocationPermission,
    isTrackMyLocation = isTrackMyLocation,
    appName = appName,
    roomName = "RoomName",
    showTileProviderPicker = false,
    eventSink = eventSink,
    isSharingLocation = false,
)

//internal fun aLocationHistoryItem(
//    formattedDate: String = "01/12/2023",
//    state: ShowLocationItemState = aLocationContentState(),
//) = ShowLocationItem(
//    formattedDate = formattedDate,
//    state = state,
//)
