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
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import io.element.android.features.messages.impl.messagecomposer.MessageComposerState
import io.element.android.features.messages.impl.timeline.TimelineState
import io.element.android.features.messages.impl.timeline.model.TimelineItem

import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent

@Composable
fun SmartRepliesView(timelineState: TimelineState) {
    val scrollState = rememberScrollState()

    val conversation = createConversationFromTimelineItems(timelineState.timelineItems)

    val smartReplyGenerator = remember { SmartReply.getClient() }
    val suggestions = remember { mutableStateListOf<String>() }


    if (conversation.isNotEmpty()) {
        LaunchedEffect(conversation) {
            val task = smartReplyGenerator.suggestReplies(conversation)
            task.addOnSuccessListener { result ->
                if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    suggestions.clear()
                    suggestions.addAll(result.suggestions.map { it.text })
                } else {
                    // Handle errors
                }
            }
            task.addOnFailureListener { exception ->
                // Handle exceptions
            }
        }
    }

    // TODO hide this view if there are no suggestions and mimick google messages styling
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (suggestion in suggestions) {
            OutlinedButton(onClick = { fakeClick() }) {
                Text(suggestion)
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

private fun fakeClick() {
    println("Clicked")
}

private fun createConversationFromTimelineItems(timelineItems: List<TimelineItem>): List<TextMessage> {
    return timelineItems.mapNotNull { timelineItem ->
        when (timelineItem) {
            is TimelineItem.Event -> {
                when (val timelineEventContent = timelineItem.content) {
                    is TimelineItemTextBasedContent -> {
                        timelineItem.sentTime
                        val text = timelineEventContent.plainText
                        if (timelineItem.isMine) {
                            TextMessage.createForLocalUser(text, timelineItem.originalTimestamp)
                        } else {
                            TextMessage.createForRemoteUser(text, timelineItem.originalTimestamp, timelineItem.senderId.toString())
                        }
                    }
                    else -> null // Skip non-text based content
                }
            }
            else -> null // Skip all non-event types
        }
    }.sortedBy { it.timestampMillis }
}
