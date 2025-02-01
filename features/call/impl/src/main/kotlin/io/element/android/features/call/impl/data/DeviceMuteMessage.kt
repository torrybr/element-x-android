/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceMuteMessage(
    @SerialName("audio_enabled")
    val isAudioEnabled: Boolean?,
    @SerialName("video_enabled")
    val isVideoEnabled: Boolean?,
)
