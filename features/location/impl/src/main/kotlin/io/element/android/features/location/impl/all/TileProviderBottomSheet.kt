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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.location.impl.all.model.MapProvider
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TileProviderBottomSheet(
    state: ShowAllLocationState,
    onTileProviderSelected: (MapProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(state.showTileProviderPicker) }

    BackHandler {
        isVisible = false
    }

    LaunchedEffect(state.showTileProviderPicker) {
        isVisible = if (state.showTileProviderPicker) {
            // We need to use this instead of `LocalFocusManager.clearFocus()` to hide the keyboard when focus is on an Android View
            localView.hideKeyboard()
            true
        } else {
            false
        }
    }

    // Send 'DismissAttachmentMenu' event when the bottomsheet was just hidden
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            state.eventSink(ShowAllLocationEvents.DismissTileProviderPicker)
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            onDismissRequest = { isVisible = false }
        ) {
            Row {
                Column(Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp)) {
                    Text(
                        text = "CHOOSE MAP "
                    )
                    Text(
                        text = state.mapTileProvider.displayName,
                    )
                }

            }
            TileProviderPickerMenu(
                state = state,
                onTileProviderSelected = onTileProviderSelected
            )
        }
    }
}

@Composable
private fun ProviderItem(
    provider: MapProvider,
    onTileProviderSelected: (MapProvider) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTileProviderSelected(provider) }) {
        Box(
            modifier = Modifier
                .background(Color.Gray)
                .size(72.dp)
        )
        Text(text = provider.displayName.uppercase(), modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun TileProviderPickerMenu(
    state: ShowAllLocationState,
    onTileProviderSelected: (MapProvider) -> Unit
) {
    // TODO (tb): create a single source of truth for the list of Map providers
    val mapTileProviders = listOf(
        MapProvider("OSM", "openstreetmap"), MapProvider("Satellite", "satellite"),
        MapProvider("Streets", "streets-v2"), MapProvider("TOPO", "topo-v2")
    )

    // A row of 4 options with an image and text below each
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier
        .fillMaxWidth()
        .padding(0.dp, 0.dp, 0.dp, 16.dp)
    ) {
        for (i in mapTileProviders) {
            ProviderItem(
                provider = i,
                onTileProviderSelected = onTileProviderSelected
            )
        }
    }
}


