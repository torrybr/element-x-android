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

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

class SmartReplyState(
    val messages: PersistentList<String> = persistentListOf(),
    val loading: Boolean = false,
    val predictions: PersistentList<String> = persistentListOf(),
    val eventSink: (SmartReplyEvents) -> Unit
)
