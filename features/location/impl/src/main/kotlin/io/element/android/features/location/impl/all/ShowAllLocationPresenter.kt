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
import kotlin.math.pow
import kotlin.math.sqrt

class ShowAllLocationPresenter @Inject constructor(
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val room: MatrixRoom,
    private val locationActions: LocationActions,
    private val buildMeta: BuildMeta,
    private val timelineProvider: TimelineProvider,
    private val showLocationItemFactory: ShowLocationItemFactory,
) : Presenter<ShowAllLocationState> {

    private val permissionsPresenter = permissionsPresenterFactory.create(MapDefaults.permissions)

    @Composable
    override fun present(): ShowAllLocationState {
        val timeline by timelineProvider.activeTimelineFlow().collectAsState()
        val permissionsState: PermissionsState = permissionsPresenter.present()
        var isTrackMyLocation by remember { mutableStateOf(false) }
        val appName by remember { derivedStateOf { buildMeta.applicationName } }
        var permissionDialog: ShowAllLocationState.Dialog by remember {
            mutableStateOf(ShowAllLocationState.Dialog.None)
        }

        val roomName by remember { derivedStateOf { room.name } }
        var showTileProviderPicker: Boolean by remember { mutableStateOf(false) }

        val loc by remember { mutableStateOf(Location(38.879366660251435, -77.02429536242268, 4f)) }

        val locationHistoryItemsFlow = remember {
            timeline.timelineItems.map { items ->
                showLocationItemFactory.create(items)
            }
        }

        val locationHistoryItems by locationHistoryItemsFlow.collectAsState(initial = ShowLocationItems())

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
            location = loc,
            description = locationHistoryItems.ongoing.joinToString(", ") { it.state.user },
            hasLocationPermission = permissionsState.isAnyGranted,
            isTrackMyLocation = isTrackMyLocation,
            appName = appName,
            roomName = roomName ?: "",
            showTileProviderPicker = showTileProviderPicker,
            eventSink = ::handleEvents,
        )
    }

    // Calculate the straight line distance between two geo points for simplicity
    fun distanceBetween(point1: GeoPoint, point2: GeoPoint): Double {
        val latDiff = point2.latitude - point1.latitude
        val lonDiff = point2.longitude - point1.longitude
        return sqrt(latDiff.pow(2) + lonDiff.pow(2)) * 111.32 // Approx. conversion to kilometers
    }

    // Interpolate points between two geo points
    fun interpolatePoints(start: GeoPoint, end: GeoPoint, numPoints: Int): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        for (i in 0..numPoints) {
            val fraction = i.toDouble() / numPoints
            val interpolatedLat = start.latitude + (end.latitude - start.latitude) * fraction
            val interpolatedLon = start.longitude + (end.longitude - start.longitude) * fraction
            points.add(GeoPoint(interpolatedLat, interpolatedLon))
        }
        return points
    }

    private suspend fun startBeaconInfo2() {
        val routeTracks = listOf(
            GeoPoint(38.8730, -77.0074), // Nationals Park
            GeoPoint(38.8763, -77.0059), // Navy Yard Metro Station
            GeoPoint(38.8765, -77.0006), // The Yards Park
            GeoPoint(38.8899, -77.0091), // U.S. Capitol
            GeoPoint(38.8913, -77.0300), // National Museum of American History
            GeoPoint(38.8895, -77.0353), // Washington Monument
            GeoPoint(38.8893, -77.0502)  // Lincoln Memorial
        )

        for (i in 0 until routeTracks.size - 1) {
            val currentLocation = routeTracks[i]
            val nextLocation = routeTracks[i + 1]
            val interpolatedPoints = interpolatePoints(currentLocation, nextLocation, 10) // Create 10 intermediate points

            for (point in interpolatedPoints) {
//                updateUserLocation(point.toGeoURI())
                room.updateUserLocation(point.toGeoURI())
                delay(1000L) // Shorter delay for more frequent updates
            }
        }
    }

    private suspend fun startBeaconInfo() {
        room.startBeaconInfo()
        //wait 10 seconds
        Thread.sleep(5000) // pause to test arrival times
        startBeaconInfo2()
    }

    private suspend fun updateLocation() {
    }
}

data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun toGeoURI(): String = "geo:$latitude,$longitude"
}
