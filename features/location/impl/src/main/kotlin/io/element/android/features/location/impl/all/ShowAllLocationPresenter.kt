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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.location.api.Location
import io.element.android.features.location.impl.all.model.ShowLocationItemFactory
import io.element.android.features.location.impl.all.model.ShowLocationItems
import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.features.location.impl.common.actions.LocationActions
import io.element.android.features.location.impl.common.permissions.PermissionsEvents
import io.element.android.features.location.impl.common.permissions.PermissionsPresenter
import io.element.android.features.location.impl.common.permissions.PermissionsState
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.matrix.api.timeline.TimelineProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import io.element.android.libraries.matrix.api.timeline.Timeline
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.TimeUnit

class ShowAllLocationPresenter @Inject constructor(
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val room: MatrixRoom,
    private val locationActions: LocationActions,
    private val buildMeta: BuildMeta,
    private val timelineProvider: TimelineProvider,
    private val showLocationItemFactory: ShowLocationItemFactory,
) : Presenter<ShowAllLocationState> {

    private val permissionsPresenter = permissionsPresenterFactory.create(MapDefaults.permissions)

    @SuppressLint("MissingPermission", "DefaultLocale")
    @Composable
    override fun present(): ShowAllLocationState {

        val permissionsState: PermissionsState = permissionsPresenter.present()
        var isTrackMyLocation by remember { mutableStateOf(false) }
        val appName by remember { derivedStateOf { buildMeta.applicationName } }
        var permissionDialog: ShowAllLocationState.Dialog by remember {
            mutableStateOf(ShowAllLocationState.Dialog.None)
        }

        val roomName by remember { derivedStateOf { room.displayName } }
        var showTileProviderPicker: Boolean by remember { mutableStateOf(false) }
        
        var isSharingLocation: Boolean by remember {
            mutableStateOf(false)
        }

        val scope = rememberCoroutineScope()

        ///////////////////////////// Location Functions /////////////////////////////
        var locationRequest by remember {
            mutableStateOf<LocationRequest?>(null)
        }

        // Only register the location updates effect when we have a request
        if (locationRequest != null) {
            LocationUpdatesEffect(locationRequest!!) { result ->
                // For each result update the text
                for (currentLocation in result.locations) {
                    scope.launch {
                        locationUpdate(currentLocation)
                    }
                }
            }
        }


        LaunchedEffect(permissionsState.permissions) {
            if (permissionsState.isAnyGranted) {
                permissionDialog = ShowAllLocationState.Dialog.None
            }
        }
        ///////////////////////////// End Location Functions /////////////////////////////

        ///////////////////////////// Timeline Functions /////////////////////////////
        val timeline by timelineProvider.activeTimelineFlow().collectAsState()
        val paginationState by timeline.paginationStatus(Timeline.PaginationDirection.BACKWARDS).collectAsState()

        val locationHistoryItemsFlow = remember {
            timeline.timelineItems.map { items ->
                showLocationItemFactory.create(items)
            }
        }

        val locationHistoryItems by locationHistoryItemsFlow.collectAsState(initial = ShowLocationItems())

        LaunchedEffect(paginationState, locationHistoryItems.size) {
            if (locationHistoryItems.size == 0 && paginationState.canPaginate) loadMore(timeline)
        }

        ///////////////////////////// End Timeline Functions /////////////////////////////

        fun handleEvents(event: ShowAllLocationEvents) {
            when (event) {
                ShowAllLocationEvents.StartBeaconInfo -> {
                    isSharingLocation = true
                    locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(3)).build()
                    scope.launch {
                        startBeaconInfo()

                    }
                }
                ShowAllLocationEvents.StopBeaconInfo -> {
                    isSharingLocation = false
                    locationRequest = null
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
                ShowAllLocationEvents.OpenTileProvider -> {
                    showTileProviderPicker = true
                }
                ShowAllLocationEvents.DismissTileProviderPicker -> {
                    showTileProviderPicker = false
                }
            }
        }

        return ShowAllLocationState(
            permissionDialog = permissionDialog,
            showLocationItems = locationHistoryItems,
            description = "",
            hasLocationPermission = permissionsState.isAnyGranted,
            isTrackMyLocation = isTrackMyLocation,
            appName = appName,
            roomName = roomName,
            showTileProviderPicker = showTileProviderPicker,
            eventSink = ::handleEvents,
            isSharingLocation = isSharingLocation
        )
    }

    private fun CoroutineScope.loadMore(timeline: Timeline) = launch {
        timeline.paginate(Timeline.PaginationDirection.BACKWARDS)
    }

    private suspend fun startBeaconInfo() {
        room.startBeaconInfo((30 * 1000).toULong())
        //wait 5 seconds
        delay(5000) // pause to test arrival times
    }

    private suspend fun locationUpdate(currentLocation: android.location.Location) {
        val matrixLocation = Location(currentLocation.latitude, currentLocation.longitude, currentLocation.accuracy)
        room.updateUserLocation(matrixLocation.toGeoUri())
    }
}

/**
 * An effect that request location updates based on the provided request and ensures that the
 * updates are added and removed whenever the composable enters or exists the composition.
 */
@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
@Composable
fun LocationUpdatesEffect(
    locationRequest: LocationRequest,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onUpdate: (result: LocationResult) -> Unit,
) {
    val context = LocalContext.current
    val currentOnUpdate by rememberUpdatedState(newValue = onUpdate)

    // Whenever on of these parameters changes, dispose and restart the effect.
    DisposableEffect(locationRequest, lifecycleOwner) {
        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                currentOnUpdate(result)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                locationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper(),
                )
            } else if (event == Lifecycle.Event.ON_STOP) {
                locationClient.removeLocationUpdates(locationCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            locationClient.removeLocationUpdates(locationCallback)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
