package io.element.android.features.messages.impl.smartreply

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import io.element.android.features.messages.impl.timeline.TimelineState
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent

@Composable
fun SmartRepliesView(timelineState: TimelineState, onClick: (suggestion: String) -> Unit) {
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
