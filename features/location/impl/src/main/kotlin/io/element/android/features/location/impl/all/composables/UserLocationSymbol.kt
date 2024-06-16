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

package io.element.android.features.location.impl.all.composables

import androidx.compose.runtime.Composable
import io.element.android.features.location.api.Location.Companion.fromGeoUri
import io.element.android.features.location.impl.all.model.ShowLocationItem
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CircleWithItem
import java.util.Locale

@Composable
fun LocationSymbol(item: ShowLocationItem) {
    val location = fromGeoUri(item.state.location)
    location?.let {
        val latLng = LatLng(it.lat, it.lon)

        CircleWithItem(
            center = latLng,
            radius = 10.0F,
            isDraggable = false,
            color = "White",
            text = item.state.user.substring(1, 2).uppercase(Locale.getDefault()),
            zIndex = 1,
            itemSize = 12F,
            borderColor = "Black",
            borderWidth = 3.0F,
        )
    }
}
