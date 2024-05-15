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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AirlineStops
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.compound.tokens.generated.TypographyTokens
import io.element.android.features.location.api.internal.rememberTileStyleUrl

import io.element.android.features.location.impl.common.PermissionDeniedDialog
import io.element.android.features.location.impl.common.PermissionRationaleDialog
import io.element.android.features.location.impl.common.ShowAllMapDefaults
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.FloatingActionButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.maplibre.compose.CameraMode
import io.element.android.libraries.maplibre.compose.CameraMoveStartedReason
import io.element.android.libraries.maplibre.compose.rememberCameraPositionState
import io.element.android.libraries.ui.strings.CommonStrings
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.compose.UiSettings

@OptIn(ExperimentalMaterial3Api::class)
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

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(LatLng(state.location.lat, state.location.lon))
            .zoom(ShowAllMapDefaults.DEFAULT_ZOOM)
            .build()
    }

    LaunchedEffect(state.isTrackMyLocation) {
        when (state.isTrackMyLocation) {
            false -> cameraPositionState.cameraMode = CameraMode.NONE
            true -> {
                cameraPositionState.position = CameraPosition.Builder()
                    .zoom(ShowAllMapDefaults.DEFAULT_ZOOM)
                    .build()
                cameraPositionState.cameraMode = CameraMode.TRACKING
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            state.eventSink(ShowAllLocationEvents.TrackMyLocation(false))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Show Room Name Here",
                        style = ElementTheme.typography.aliasScreenTitle,
                    )
                },
                navigationIcon = {
                    BackButton(
                        onClick = onBackPressed,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { state.eventSink(ShowAllLocationEvents.Share) }
                    ) {
                        Icon(
                            imageVector = CompoundIcons.ShareAndroid(),
                            contentDescription = stringResource(CommonStrings.action_share),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { state.eventSink(ShowAllLocationEvents.TrackMyLocation(true)) },
            ) {
                when (state.isTrackMyLocation) {
                    false -> Icon(imageVector = Icons.Default.LocationSearching, contentDescription = null)
                    true -> Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                styleUrl = rememberTileStyleUrl(),
                locationRequestProperties = LocationRequestProperties(interval = 250L),
            )
            state.description?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TypographyTokens.fontBodyMdRegular,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp), // Adds padding on the right side
                verticalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
            ) {
                RoundedIconButton(icon = Icons.Filled.Settings, onClick = { /* Handle Star click */ })
                RoundedIconButton(icon = Icons.Filled.Layers, onClick = { /* Handle Place click */ })
                RoundedIconButton(icon = Icons.Filled.AirlineStops, onClick = { /* Handle Settings click */ })
                RoundedIconButton(icon = Icons.Filled.AcUnit, onClick = {
                    state.eventSink(ShowAllLocationEvents.StartBeaconInfo)
                })

            }
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

private const val PIN_ID = "pin"
