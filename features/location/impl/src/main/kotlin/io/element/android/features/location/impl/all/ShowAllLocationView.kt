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

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.location.impl.all.composables.LocationSymbol
import io.element.android.features.location.impl.all.composables.MapToolbar
import io.element.android.features.location.impl.common.PermissionDeniedDialog
import io.element.android.features.location.impl.common.PermissionRationaleDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.RenderMode
import org.ramani.compose.CameraPosition
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.MapLibre

@Composable
fun ShowAllLocationView(
    state: ShowAllLocationState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.permissionDialog) {
        ShowAllLocationState.Dialog.None -> Unit
        ShowAllLocationState.Dialog.PermissionDenied -> PermissionDeniedDialog(
            onContinue = { state.eventSink(ShowAllLocationEvents.OpenAppSettings) },
            onDismiss = { state.eventSink(ShowAllLocationEvents.DismissDialog) },
            appName = state.appName,
        )
        ShowAllLocationState.Dialog.PermissionRationale -> PermissionRationaleDialog(
            onContinue = { state.eventSink(ShowAllLocationEvents.RequestPermissions) },
            onDismiss = { state.eventSink(ShowAllLocationEvents.DismissDialog) },
            appName = state.appName,
        )
    }

    val cameraPosition = rememberSaveable {
        mutableStateOf(CameraPosition())
    }

    val userLocation = rememberSaveable { mutableStateOf(Location(null)) }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            MapToolbar(onBackPressed = onBackPressed, title = state.roomName)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {

            MapLibre(
                modifier = Modifier
                    .fillMaxSize(),
                styleUrl = state.styleUrl,
                locationRequestProperties = LocationRequestProperties(interval = 250L),
                cameraPosition = cameraPosition.value,
                renderMode = RenderMode.COMPASS,
                userLocation = userLocation,
            ) {
                // TODO (tb): this is bad, just have the sdk leave out your own dot
                state.showLocationItems.ongoing.filter { !it.state.isMine }.map { item ->
                    LocationSymbol(item)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp), // Adds padding on the right side
                verticalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
            ) {
                RoundedIconButton(icon = if (state.isSharingLocation) Icons.Filled.Stop else Icons.Outlined.PlayArrow,
                    onClick = {
                        if (!state.isSharingLocation) {
                            state.eventSink(ShowAllLocationEvents.StartBeaconInfo)
                        } else {
                            state.eventSink(ShowAllLocationEvents.StopBeaconInfo)
                        }
                    })
                RoundedIconButton(icon = Icons.Outlined.Layers, onClick = { state.eventSink(ShowAllLocationEvents.OpenTileProvider) })
                RoundedIconButton(icon = Icons.Outlined.LocationSearching, onClick = {
                    cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                        this.target = LatLng(
                            userLocation.value.latitude,
                            userLocation.value.longitude
                        )
                        this.zoom = 17.0
                    }
                })
            }
            TileProviderBottomSheet(state = state, onTileProviderSelected = { provider ->
                state.eventSink(
                    ShowAllLocationEvents.ChangeProvider(
                        provider
                    )
                )
            })
        }
    }
}

@Composable
fun RoundedIconButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.White, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Icon",
            tint = Color.Black
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ShowAllLocationViewPreview(@PreviewParameter(ShowAllLocationStateProvider::class) state: ShowAllLocationState) = ElementPreview {
    ShowAllLocationView(
        state = state,
        onBackPressed = {},
    )
}
