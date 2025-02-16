/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl.cameramode

import io.element.android.features.location.impl.common.MapDefaults
import io.element.android.libraries.maplibre.compose.CameraMode
import io.element.android.libraries.maplibre.compose.CameraPositionState
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

/**
 * Toggles to the next camera mode in the sequence. The sequence follows:
 * - `CameraMode.NONE` -> `CameraMode.TRACKING_GPS_NORTH`
 * - `CameraMode.TRACKING_GPS_NORTH` -> `CameraMode.TRACKING_GPS`
 * - `CameraMode.TRACKING_GPS` -> `CameraMode.NONE`
 *
 * If the current camera mode is unsupported, the cameraMode set will be skipped.
 * If the next camera mode is `CameraMode.TRACKING_GPS_NORTH`, the camera should first be
 * animated to a specific zoom level and location. After the animation finishes, `cameraMode` should be
 * set to prevent animation conflicts.
 *
 */
fun CameraPositionState.toggleNextCameraMode() {
    val nextCameraMode: CameraMode = when (this.cameraMode) {
        CameraMode.NONE -> CameraMode.TRACKING_GPS_NORTH
        CameraMode.TRACKING_GPS_NORTH -> CameraMode.TRACKING_GPS
        CameraMode.TRACKING_GPS -> CameraMode.NONE
        else -> {
            Timber.w("Unsupported selected camera mode=$this")
            return
        }
    }

    if (nextCameraMode == CameraMode.TRACKING_GPS_NORTH) {
        val newCameraPosition: CameraPosition = CameraPosition.Builder()
            .apply {
                this@toggleNextCameraMode.location?.let {
                    target(LatLng(it))
                }
                zoom(MapDefaults.DEFAULT_ZOOM)
            }.build()
        this.animateCameraPosition(
            cameraPosition = newCameraPosition,
            onAnimationFinished = object : MapLibreMap.CancelableCallback {
                override fun onCancel() = Unit

                override fun onFinish() {
                    this@toggleNextCameraMode.cameraMode = nextCameraMode
                }
            },
        )
    } else {
        this.cameraMode = nextCameraMode
    }
}
