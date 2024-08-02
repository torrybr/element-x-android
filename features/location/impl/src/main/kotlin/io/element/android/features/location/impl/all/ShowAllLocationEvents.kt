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

import io.element.android.features.location.impl.all.model.MapProvider
import org.maplibre.android.geometry.LatLng

sealed interface ShowAllLocationEvents {
    data object DismissDialog : ShowAllLocationEvents
    data object RequestPermissions : ShowAllLocationEvents
    data object OpenAppSettings : ShowAllLocationEvents

    data object StartBeaconInfo : ShowAllLocationEvents

    data object OpenTileProvider : ShowAllLocationEvents

    data object DismissTileProviderPicker : ShowAllLocationEvents

    data object StopBeaconInfo : ShowAllLocationEvents
    data class ChangeProvider(val provider: MapProvider) : ShowAllLocationEvents
    data class MapLongPress(val coordinates: LatLng) : ShowAllLocationEvents
}
