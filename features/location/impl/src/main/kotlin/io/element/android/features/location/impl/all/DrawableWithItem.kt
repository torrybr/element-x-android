/*
 * Copyright (c) 2024 New Vector Ltd
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.Circle
import org.ramani.compose.Symbol

@Composable
fun UpdateCenter(coord: LatLng, centerUpdated: (LatLng) -> Unit) {
    centerUpdated(coord)
}

@Composable
fun DrawableWithItem(
    center: LatLng,
    radius: Float,
    dragRadius: Float = radius,
    isDraggable: Boolean,
    color: String,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    opacity: Float = 1.0f,
    zIndex: Int = 0,
    imageId: Int? = null,
    itemSize: Float = 0.0f,
    text: String? = null,
) {
    val draggableCenterState = remember { mutableStateOf(center) }

    UpdateCenter(coord = center, centerUpdated = { draggableCenterState.value = it })

    // Display circle
    Circle(
        center = center,
        radius = radius,
        isDraggable = false,
        color = "Black",
        opacity = opacity,
        zIndex = zIndex,
        borderColor = "White",
        borderWidth = 2.0F,
        onCenterDragged = {}
    )

    imageId?.let {
        Symbol(
            center = center,
            color = "Black",
            isDraggable = false,
            imageId = imageId,
            size = itemSize,
            zIndex = zIndex + 1,
        )
    }

    text?.let {
        Symbol(
            center = center,
            color = "White",
            isDraggable = false,
            text = text,
            size = itemSize,
            zIndex = zIndex + 1
        )
    }
}
