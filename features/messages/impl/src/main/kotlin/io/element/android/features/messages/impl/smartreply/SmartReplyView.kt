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

package io.element.android.features.messages.impl.smartreply

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.TextMessage
import io.element.android.features.messages.impl.timeline.TimelineState
import io.element.android.features.messages.impl.timeline.model.TimelineItem

import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent

@Composable
fun SmartRepliesView(state: TimelineState) {
//    val smartReplyGenerator = remember { SmartReply.getClient() }
//    val suggestions = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    val conversation = state.timelineItems.mapNotNull { timelineItem ->
        when (timelineItem) {
            is TimelineItem.Event -> {
                when (val timelineEventContent = timelineItem.content) {
                    is TimelineItemTextBasedContent -> {
                        val text = timelineEventContent.body

                        if (timelineItem.isMine) {
                            // Create message for local user
                            TextMessage.createForLocalUser(text, System.currentTimeMillis())
                        } else {
                            // Create message for remote user
                            TextMessage.createForRemoteUser(text, System.currentTimeMillis(), timelineItem.senderId.toString())
                        }
                    }
                    else -> null // Skip non-text based content
                }
            }
            else -> null // Skip all non-event types
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { TODO() }) {
            Text("Reply 1")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { TODO() }) {
            Text("Reply 2")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { TODO() }) {
            Text("Reply 3")
        }
    }
}
