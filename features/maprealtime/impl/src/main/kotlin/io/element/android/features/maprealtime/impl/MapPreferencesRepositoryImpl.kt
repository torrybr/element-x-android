/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl

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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.maplibre.compose.CameraMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Also accessed via reflection by the instrumentation tests @see [im.vector.app.ClearCurrentSessionRule].
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "map_type_store")

/**
 * Local storage for:
 * - map tile provider (String).
 */
interface MapPreferencesRepository {
    val mapTileProviderFlow: Flow<String>
    val selectedCameraModeFlow: Flow<CameraMode>

    suspend fun setMapTileProvider(provider: String)
    suspend fun setCameraMode(cameraMode: CameraMode)
}

@ContributesBinding(RoomScope::class)
class MapPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
    dispatchers: CoroutineDispatchers,
) : MapPreferencesRepository {

    private companion object {
        private const val KEY_MAP_TILE_PROVIDER = "map_tile_provider"
        private const val KEY_SELECTED_CAMERA_MODE = "selected_camera_mode_key"
    }

    private val mapTileProvider by lazy { stringPreferencesKey(KEY_MAP_TILE_PROVIDER) }
    private val selectedCameraModeKey by lazy { intPreferencesKey(KEY_SELECTED_CAMERA_MODE) }

    override val mapTileProviderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[mapTileProvider] ?: "streets-v2"
    }.flowOn(dispatchers.io)

    override val selectedCameraModeFlow: Flow<CameraMode> = context.dataStore.data.map { preferences ->
        preferences[selectedCameraModeKey]?.let {
            CameraMode.fromInternal(it)
        } ?: CameraMode.TRACKING_GPS_NORTH
    }.flowOn(dispatchers.io)

    override suspend fun setMapTileProvider(provider: String) {
        Timber.d("MapPreferencesRepository setMapTileProvider=$provider")
        context.dataStore.edit { settings ->
            settings[mapTileProvider] = provider
        }
    }

    override suspend fun setCameraMode(cameraMode: CameraMode) {
        Timber.d("MapPreferencesRepository setCameraMode=$cameraMode")
        context.dataStore.edit { settings ->
            settings[selectedCameraModeKey] = cameraMode.toInternal()
        }
    }
}
