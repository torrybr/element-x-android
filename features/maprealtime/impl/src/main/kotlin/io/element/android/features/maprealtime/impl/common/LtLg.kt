/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.maprealtime.impl.common

import android.location.Location

data class LtLg(
    val latitude: Double,
    val longitude: Double,
)

fun Location.toLtLg() = LtLg(latitude = latitude, longitude = longitude)
