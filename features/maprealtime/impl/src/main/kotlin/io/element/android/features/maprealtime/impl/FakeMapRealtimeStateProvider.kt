/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.maplibre.compose.CameraMode

open class FakeMapRealtimeStateProvider : PreviewParameterProvider<MapRealtimePresenterState> {
    override val values: Sequence<MapRealtimePresenterState>
        get() = sequenceOf(
            MapRealtimePresenterState(
                eventSink = { },
                permissionDialog = MapRealtimePresenterState.Dialog.None,
                hasLocationPermission = true,
                hasGpsEnabled = true,
                showMapTypeDialog = false,
                appName = "Test",
                roomName = "TestRoom",
                isSharingLocation = true,
                mapType = MapType("OSM", "openstreetmap"),
                liveLocationShares = emptyList(),
                isWaitingForLocation = false,
                selectedCameraMode = CameraMode.TRACKING_GPS_NORTH,
                lastKnownPosition = null,
            )
        )
}
