/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.features.maprealtime.impl.cameramode.toggleNextCameraMode
import io.element.android.features.maprealtime.impl.common.PermissionDeniedDialog
import io.element.android.features.maprealtime.impl.common.PermissionRationaleDialog
import io.element.android.features.roomcall.api.RoomCallState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.utils.KeepScreenOn
import io.element.android.libraries.maplibre.compose.CameraMode
import io.element.android.libraries.maplibre.compose.CameraPositionState
import io.element.android.libraries.maplibre.compose.MapLibreMap
import io.element.android.libraries.maplibre.compose.rememberCameraPositionState
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import timber.log.Timber

@Composable
fun MapRealtimeView(
    state: MapRealtimePresenterState,
    onBackPressed: () -> Unit,
    onMessagesPressed: () -> Unit,
    onJoinCallClick: () -> Unit,
    roomCallState: RoomCallState,
) {
    val cameraPositionState = rememberCameraPositionState { cameraMode = state.selectedCameraMode }

    LaunchedEffect(cameraPositionState.cameraMode) {
        state.eventSink(MapRealtimeEvents.ToggleNextCameraMode(cameraPositionState.cameraMode))
    }

    KeepScreenOn()

    LaunchedEffect(Unit) {
        state.eventSink(MapRealtimeEvents.RequestPermissions)

        if (state.hasGpsEnabled && state.hasLocationPermission) {
            val builder = CameraPosition.Builder().zoom(MapDefaults.DEFAULT_ZOOM)

            cameraPositionState.position = if (cameraPositionState.cameraMode == CameraMode.NONE) {
                if (cameraPositionState.location != null) {
                    builder.target(LatLng(checkNotNull(cameraPositionState.location)))
                } else {
                    builder.target(LatLng(checkNotNull(MapDefaults.fallbackCameraPosition.target)))
                }
                builder.build()
            } else {
                MapDefaults.fallbackCameraPosition
            }
        } else {
            cameraPositionState.position = MapDefaults.fallbackCameraPosition
        }
    }

    when (state.permissionDialog) {
        MapRealtimePresenterState.Dialog.None -> Unit
        MapRealtimePresenterState.Dialog.PermissionDenied -> PermissionDeniedDialog(
            onContinue = { state.eventSink(MapRealtimeEvents.OpenAppSettings) },
            onDismiss = { state.eventSink(MapRealtimeEvents.DismissDialog) },
            appName = state.appName,
        )
        MapRealtimePresenterState.Dialog.PermissionRationale -> PermissionRationaleDialog(
            onContinue = { state.eventSink(MapRealtimeEvents.RequestPermissions) },
            onDismiss = { state.eventSink(MapRealtimeEvents.DismissDialog) },
            appName = state.appName,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibreMap(
            modifier = Modifier.fillMaxSize(),
            styleUri = state.styleUrl,
            cameraPositionState = cameraPositionState,
            uiSettings = MapDefaults.mapRealtimeSettings,
            symbolManagerSettings = MapDefaults.symbolManagerSettings,
            locationSettings = MapDefaults.mapRealtimeLocationSettings.copy(
                locationEnabled = state.hasLocationPermission,
            ),
            onMapLongClick = {
                state.eventSink(MapRealtimeEvents.MapLongPress(it))
            }
        ) {
            state.liveLocationShares.map { item ->
                val st = if (state.mapType.mapKey == "satellite") "White" else "Black"
                LocationSymbol(item, st)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .windowInsetsPadding(WindowInsets.statusBars), // Adds padding on the right side
            verticalArrangement = Arrangement.spacedBy(8.dp), // Space between buttons,
            horizontalAlignment = Alignment.End
        ) {
            MapToolbar(
                onBackPressed = onBackPressed,
                title = state.roomName,
                onMessagesPressed = onMessagesPressed,
                onJoinCallClicked = onJoinCallClick,
                roomCallState = roomCallState,
            )
            LocationButton(
                state = state,
                onClick = {
                    if (state.isSharingLocation) {
                        state.eventSink(MapRealtimeEvents.StopLiveLocationShare)
                    } else {
                        state.eventSink(MapRealtimeEvents.StartLiveLocationShare)
                    }
                }
            )
            RoundedIconButton(
                icon = Icons.Outlined.Layers,
                onClick = { state.eventSink(MapRealtimeEvents.OpenMapTypeDialog) })
            RoundedIconButton(
                icon = Icons.Outlined.LocationSearching,
                onClick = {
                    if (cameraPositionState.cameraMode == CameraMode.NONE) {
                        cameraPositionState.animateCameraPosition(
                            CameraPosition.Builder()
                                .apply {
                                    cameraPositionState.location?.let {
                                        target(LatLng(it))
                                    }
                                    zoom(MapDefaults.DEFAULT_ZOOM)
                                }.build()

                        )
                    } else {
                        Timber.d("MapRealtimeView Camera tracking is active, skipping camera move...")
                    }
                })
            TrackingLocationButton(
                cameraPositionState = cameraPositionState,
            ) {
                cameraPositionState.cameraMode = cameraPositionState.cameraMode.toggleNextCameraMode()
            }
        }
        MapTypeBottomSheet(state = state, onTileProviderSelected = { provider ->
            state.eventSink(
                MapRealtimeEvents.MapTypeSelected(
                    provider
                )
            )
        })
    }
}

@Composable
fun LocationButton(
    state: MapRealtimePresenterState,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp)
    ) {
        if (state.isWaitingForLocation) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawCircle(color = Color.Green)
            }

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            val (imageVector, backgroundColor) = if (state.isSharingLocation) {
                Icons.Outlined.Stop to Color.Red
            } else {
                Icons.Outlined.PlayArrow to Color.Green
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(backgroundColor, CircleShape)
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = "Icon",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun TrackingLocationButton(
    cameraPositionState: CameraPositionState,
    onClick: () -> Unit,
) {
    val cameraMode = cameraPositionState.cameraMode
    val rotation = if (cameraMode != CameraMode.NONE) -cameraPositionState.position.bearing.toFloat() else 0f
    RoundedIconButton(
        modifier = Modifier.rotate(rotation),
        icon = painterResource(R.drawable.ic_cardinal_point),
        iconTint = if (cameraMode == CameraMode.NONE) Color.Black else Color.Unspecified,
        onClick = onClick,
    )
}

@Composable
fun RoundedIconButton(
    modifier: Modifier = Modifier,
    icon: Any,
    backgroundColor: Color = Color.White,
    iconTint: Color = Color.Black,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(backgroundColor, CircleShape)
    ) {
        when (icon) {
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = "Icon",
                    tint = iconTint,
                )
            }
            is Painter -> {
                Icon(
                    painter = icon,
                    contentDescription = "Icon",
                    tint = iconTint,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun MapRealtimeViewPreview(
    @PreviewParameter(MapRealtimePresenterStateProvider::class) state: MapRealtimePresenterState
) = ElementPreview {
    MapRealtimeView(
        state = state,
        onBackPressed = TODO(),
        onMessagesPressed = TODO(),
        onJoinCallClick = TODO(),
        roomCallState = RoomCallState.StandBy(canStartCall = true)
    )
}

