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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.element.android.features.location.api.Location
import io.element.android.features.location.impl.LocationForegroundService
import io.element.android.features.location.impl.all.model.MapProvider
import io.element.android.features.location.impl.all.model.ShowLocationItemFactory
import io.element.android.features.location.impl.all.model.ShowLocationItems
import io.element.android.features.location.impl.all.store.DefaultMapDataStore
import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.features.location.impl.common.actions.LocationActions
import io.element.android.features.location.impl.common.permissions.PermissionsEvents
import io.element.android.features.location.impl.common.permissions.PermissionsPresenter
import io.element.android.features.location.impl.common.permissions.PermissionsState
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.TimelineProvider
import io.element.android.libraries.matrix.impl.room.RustMatrixRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ShowAllLocationPresenter @Inject constructor(
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val room: MatrixRoom,
    private val locationActions: LocationActions,
    private val buildMeta: BuildMeta,
    private val timelineProvider: TimelineProvider,
    private val showLocationItemFactory: ShowLocationItemFactory,
    private val mapDataStore: DefaultMapDataStore
) : Presenter<ShowAllLocationState> {

    private val permissionsPresenter = permissionsPresenterFactory.create(MapDefaults.permissions)

    @SuppressLint("MissingPermission", "DefaultLocale")
    @Composable
    override fun present(): ShowAllLocationState {

        // create a list of map tile providers to choose from
        val mapTileProviders = listOf(
            MapProvider("OSM", "openstreetmap"), MapProvider("Satellite", "satellite"),
            MapProvider("Streets", "streets-v2"), MapProvider("TOPO", "topo-v2")
        )

        val permissionsState: PermissionsState = permissionsPresenter.present()
        val appName by remember { derivedStateOf { buildMeta.applicationName } }
        var permissionDialog: ShowAllLocationState.Dialog by remember {
            mutableStateOf(ShowAllLocationState.Dialog.None)
        }

        val roomName by remember { derivedStateOf { room.displayName } }
        var showTileProviderPicker: Boolean by remember { mutableStateOf(false) }

        var isSharingLocation: Boolean by remember {
            mutableStateOf(false)
        }

        // TODO (tb): This takes a few moments before i get the actual value in the datastore which
        // results in teh wrong map being loaded and trigger the else condition on line 213
        val mapTile = mapDataStore.mapTileProviderFlow.collectAsState(initial = "").value

        val scope = rememberCoroutineScope()

        ///////////////////////////// Location Functions /////////////////////////////

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

        fun handleEvents(event: ShowAllLocationEvents, context: Context) {
            when (event) {
                ShowAllLocationEvents.StartBeaconInfo -> {
                    isSharingLocation = true
                    //locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(3)).build()
                    scope.launch {
                        startBeaconInfo()
                    }
                    LocationForegroundService.start(context, room as RustMatrixRoom)
                }
                ShowAllLocationEvents.StopBeaconInfo -> {
                    isSharingLocation = false
                    //locationRequest = null
                    LocationForegroundService.stop(context)
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
                is ShowAllLocationEvents.ChangeProvider -> {
                    scope.launch {
                        setMapTileProvider(event.provider.mapKey)
                    }
                }
            }
        }

        val context = LocalContext.current

        return ShowAllLocationState(
            permissionDialog = permissionDialog,
            showLocationItems = locationHistoryItems,
            hasLocationPermission = permissionsState.isAnyGranted,
            appName = appName,
            roomName = roomName,
            showTileProviderPicker = showTileProviderPicker,
            eventSink = { event ->
                handleEvents(event, context)
            },
            isSharingLocation = isSharingLocation,
            mapTileProvider = mapTileProviders.find { it.mapKey == mapTile } ?: mapTileProviders[2]
        )
    }

    private fun CoroutineScope.loadMore(timeline: Timeline) = launch {
        timeline.paginate(Timeline.PaginationDirection.BACKWARDS)
    }

    // TODO (tb): Not sure why this works but it does, used AnalyticsStore as a reference
    private fun CoroutineScope.setMapTileProvider(mapProvider: String) = launch {
        mapDataStore.setMapTileProvider(mapProvider)
    }

    private suspend fun startBeaconInfo() {
        room.startBeaconInfo((30 * 1000).toULong())
        //wait 5 seconds
        delay(5000) // pause to test arrival times
    }

    private suspend fun locationUpdate(currentLocation: android.location.Location) {
        val matrixLocation = Location(currentLocation.latitude, currentLocation.longitude, currentLocation.accuracy)
        room.sendUserLocationBeacon(matrixLocation.toGeoUri())
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
