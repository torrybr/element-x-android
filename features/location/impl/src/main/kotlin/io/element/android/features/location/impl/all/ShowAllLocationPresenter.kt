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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.features.location.api.Location
import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.features.location.impl.common.actions.LocationActions
import io.element.android.features.location.impl.common.permissions.PermissionsEvents
import io.element.android.features.location.impl.common.permissions.PermissionsPresenter
import io.element.android.features.location.impl.common.permissions.PermissionsState
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.room.MatrixRoom
import kotlinx.coroutines.launch

class ShowAllLocationPresenter @AssistedInject constructor(
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val room: MatrixRoom,
    private val locationActions: LocationActions,
    private val buildMeta: BuildMeta,
    @Assisted private val location: Location,
    @Assisted private val description: String?
) : Presenter<ShowAllLocationState> {
    @AssistedFactory
    interface Factory {
        fun create(location: Location, description: String?): ShowAllLocationPresenter
    }

    private val permissionsPresenter = permissionsPresenterFactory.create(MapDefaults.permissions)

    @Composable
    override fun present(): ShowAllLocationState {
        val permissionsState: PermissionsState = permissionsPresenter.present()
        var isTrackMyLocation by remember { mutableStateOf(false) }
        val appName by remember { derivedStateOf { buildMeta.applicationName } }
        var permissionDialog: ShowAllLocationState.Dialog by remember {
            mutableStateOf(ShowAllLocationState.Dialog.None)
        }

        val scope = rememberCoroutineScope()

        LaunchedEffect(permissionsState.permissions) {
            if (permissionsState.isAnyGranted) {
                permissionDialog = ShowAllLocationState.Dialog.None
            }
        }

        fun handleEvents(event: ShowAllLocationEvents) {
            when (event) {
                ShowAllLocationEvents.StartBeaconInfo -> scope.launch {
                    startBeaconInfo()
                }
                is ShowAllLocationEvents.TrackMyLocation -> {
                    if (event.enabled) {
                        when {
                            permissionsState.isAnyGranted -> isTrackMyLocation = true
                            permissionsState.shouldShowRationale -> permissionDialog = ShowAllLocationState.Dialog.PermissionRationale
                            else -> permissionDialog = ShowAllLocationState.Dialog.PermissionDenied
                        }
                    } else {
                        isTrackMyLocation = false
                    }
                }
                ShowAllLocationEvents.DismissDialog -> permissionDialog = ShowAllLocationState.Dialog.None
                ShowAllLocationEvents.OpenAppSettings -> {
                    locationActions.openSettings()
                    permissionDialog = ShowAllLocationState.Dialog.None
                }
                ShowAllLocationEvents.RequestPermissions -> permissionsState.eventSink(PermissionsEvents.RequestPermissions)
                ShowAllLocationEvents.Share -> TODO()
            }
        }

        return ShowAllLocationState(
            permissionDialog = permissionDialog,
            location = location,
            description = description,
            hasLocationPermission = permissionsState.isAnyGranted,
            isTrackMyLocation = isTrackMyLocation,
            appName = appName,
            eventSink = ::handleEvents,
        )
    }

    private suspend fun startBeaconInfo() {
        room.startBeaconInfo()
        //wait 10 seconds
        Thread.sleep(5000) // pause to test arrival times
        room.updateUserLocation("geo:51.5008,0.1247;u=35")
//        print("Beacon info started")
    }
}
