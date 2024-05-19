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

package io.element.android.features.location.impl.all.model

import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import javax.inject.Inject
import io.element.android.libraries.dateformatter.api.DaySeparatorFormatter
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.BeaconShareContent
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.withContext

class ShowLocationItemFactory @Inject constructor(
    private val daySeparatorFormatter: DaySeparatorFormatter,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun create(timelineItems: List<MatrixTimelineItem>) = withContext(dispatchers.computation) {
        val ongoing = ArrayList<ShowLocationItem>()
        for (index in timelineItems.indices.reversed()) {
            val timelineItem = timelineItems[index]
            val locationItem = create(timelineItem) ?: continue

            ongoing.add(locationItem)
        }
        ShowLocationItems(
            ongoing = ongoing.toPersistentList(),
        )
    }

    private suspend fun create(timelineItem: MatrixTimelineItem): ShowLocationItem? {
        return when (timelineItem) {
            is MatrixTimelineItem.Event -> {
                val locationContent = timelineItem.event.content as? BeaconShareContent
                if (locationContent != null) {
                    println()
                }
//                if (pollContent != null) {
//                    val locationContentState = pollContentStateFactory.create(timelineItem.event, pollContent)
//                    ShowLocationItem.kt(
//                        formattedDate = daySeparatorFormatter.format(timelineItem.event.timestamp),
//                        state = pollContentState
//                    )
//                } else {
//                    return null
//                }
                return null
            }
            else -> null
        }
    }
}
