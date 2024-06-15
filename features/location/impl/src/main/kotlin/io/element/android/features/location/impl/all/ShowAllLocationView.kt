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
import androidx.compose.material.icons.filled.AirlineStops
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AirlineStops
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.TypographyTokens
import io.element.android.features.location.api.Location.Companion.fromGeoUri
import io.element.android.features.location.api.internal.rememberTileStyleUrl
import io.element.android.features.location.impl.all.model.ShowLocationItem

import io.element.android.features.location.impl.common.PermissionDeniedDialog
import io.element.android.features.location.impl.common.PermissionRationaleDialog
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
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.RenderMode
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.MapLibre
import org.ramani.compose.CameraPosition
import org.ramani.compose.CircleWithItem
import java.util.Locale

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

    val cameraPosition = rememberSaveable {
        mutableStateOf(CameraPosition())
    }

    val userLocation = rememberSaveable { mutableStateOf(Location(null)) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.roomName,
                        style = ElementTheme.typography.aliasScreenTitle,
                    )
                },
                navigationIcon = {
                    BackButton(
                        onClick = onBackPressed,
                    )
                },
                actions = {
                    /// Uncommeting this will crash the app
                    IconButton(onClick = { /*TODO*/ }) {

                    }
                }
            )
        },
        floatingActionButton = {
            // TODO (tb): should probably hoist this state to the presenter
            FloatingActionButton(
                onClick = {
                    cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                        this.target = LatLng(
                            userLocation.value.latitude,
                            userLocation.value.longitude
                        )
                        this.zoom = 12.0
                    }
                },
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {

            MapLibre(
                modifier = Modifier.fillMaxSize(),
                styleUrl = state.styleUrl,
                locationRequestProperties = LocationRequestProperties(interval = 250L),
                cameraPosition = cameraPosition.value,
                renderMode = RenderMode.COMPASS,
                userLocation = userLocation
            ) {
                // TODO (tb): this is bad, just have the sdk leave out your own dot
                state.showLocationItems.ongoing.filter { !it.state.isMine }.map { item ->
                    LocationSymbol(item)
                }
            }

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
                RoundedIconButton(icon = if (state.isSharingLocation) Icons.Filled.Stop else Icons.Outlined.PlayArrow,
                    onClick = {
                        if (!state.isSharingLocation) {
                            state.eventSink(ShowAllLocationEvents.StartBeaconInfo)
                        } else {
                            state.eventSink(ShowAllLocationEvents.StopBeaconInfo)
                        }
                    })
                RoundedIconButton(icon = Icons.Outlined.Layers, onClick = { state.eventSink(ShowAllLocationEvents.OpenTileProvider) })
                RoundedIconButton(icon = Icons.Outlined.AirlineStops, onClick = { /* Handle Settings click */ })
            }
            TileProviderBottomSheet(state = state, onTileProviderSelected = { provider ->
                state.eventSink(
                    ShowAllLocationEvents.ChangeProvider(
                        provider
                    )
                )
            })
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

@Composable
fun LocationSymbol(item: ShowLocationItem) {
    val location = fromGeoUri(item.state.location)
    location?.let {
        val latLng = LatLng(it.lat, it.lon)
        DrawableWithItem(
            center = latLng,
            radius = 10.0F,
            color = "White",
            text = item.state.user.substring(1, 2).uppercase(Locale.getDefault()),
            isDraggable = false,
            zIndex = 1,
            itemSize = 12F,
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
