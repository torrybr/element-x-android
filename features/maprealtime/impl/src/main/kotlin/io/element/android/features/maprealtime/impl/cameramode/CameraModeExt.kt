/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl.cameramode

import io.element.android.libraries.maplibre.compose.CameraMode
import timber.log.Timber

/**
 * Toggles to the next camera mode in the sequence. The sequence follows:
 * - `CameraMode.NONE` -> `CameraMode.TRACKING_GPS_NORTH`
 * - `CameraMode.TRACKING_GPS_NORTH` -> `CameraMode.TRACKING_GPS`
 * - `CameraMode.TRACKING_GPS` -> `CameraMode.NONE`
 *
 * If the current camera mode is unsupported, the current mode is returned.
 *
 * @return The next camera mode in the sequence, or the current mode if the mode is unsupported.
 */
fun CameraMode.toggleNextCameraMode(): CameraMode =
    when (this) {
        CameraMode.NONE -> CameraMode.TRACKING_GPS_NORTH
        CameraMode.TRACKING_GPS_NORTH -> CameraMode.TRACKING_GPS
        CameraMode.TRACKING_GPS -> CameraMode.NONE
        else -> {
            Timber.w("Unsupported selected camera mode=$this")
            this
        }
    }
