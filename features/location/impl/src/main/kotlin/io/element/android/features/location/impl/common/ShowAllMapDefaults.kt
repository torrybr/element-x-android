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

package io.element.android.features.location.impl.common

import android.Manifest
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.maplibre.compose.MapLocationSettings
import io.element.android.libraries.maplibre.compose.MapSymbolManagerSettings
import io.element.android.libraries.maplibre.compose.MapUiSettings
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

//import org.ramani.compose.LocationRequestProperties

/**
 * Common configuration values for the map.
 */
object ShowAllMapDefaults {
    val uiSettings: MapUiSettings
        @Composable
        @ReadOnlyComposable
        get() = MapUiSettings(
            compassEnabled = true,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = true,
            logoGravity = Gravity.BOTTOM,
            attributionGravity = Gravity.BOTTOM,
            attributionTintColor = ElementTheme.colors.iconPrimary
        )

    val symbolManagerSettings: MapSymbolManagerSettings
        get() = MapSymbolManagerSettings(
            iconAllowOverlap = true
        )

    val locationSettings: MapLocationSettings
        get() = MapLocationSettings(
            locationEnabled = true,
            backgroundTintColor = Color.White,
            foregroundTintColor = Color(0xFF2496F9),
            backgroundStaleTintColor = Color.White,
            foregroundStaleTintColor = Color(0xFF2496F9),
            accuracyColor = Color(0xFF2496F9),
            pulseEnabled = true,
            pulseColor = Color(0xFF2496F9),
        )

    val centerCameraPosition = CameraPosition.Builder()
        .target(LatLng(49.843, 9.902056))
        .zoom(2.7)
        .build()

    const val DEFAULT_ZOOM = 15.0

    val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
}
