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

import androidx.compose.runtime.Composable
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.room.MatrixRoom

/**
 * A presenter for androids ML KIt smart reply feature.

 */
class SmartReplyPresenter @AssistedInject constructor(
    private val room: MatrixRoom,
    @Assisted private val inputs: Inputs,
) : Presenter<SmartReplyState> {
    data class Inputs(
        val messages: Array<String>,
    )

    @AssistedFactory
    interface Factory {
        fun create(inputs: Inputs): SmartReplyPresenter
    }

    private val smartReplyClient = SmartReply.getClient()

    private fun fetchSmartReplies(messages: List<TextMessage>): List<String> {
        var replies = listOf<String>()
        smartReplyClient.suggestReplies(messages.toList())
            .addOnSuccessListener { result ->
                if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    replies = result.suggestions.map { it.text }
                }
            }
        return replies
    }

    @Composable
    override fun present(): SmartReplyState {

        fun handleEvents(event: SmartReplyEvents) {
            when (event) {
                is SmartReplyEvents.OnReplySelected -> TODO()
            }
        }

        return SmartReplyState(
            loading = false,
            eventSink = ::handleEvents
        )
    }
}
