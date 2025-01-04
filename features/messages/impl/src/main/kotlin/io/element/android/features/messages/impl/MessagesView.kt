/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package io.element.android.features.messages.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.element.android.features.maprealtime.impl.MapRealtimePresenterState
import io.element.android.features.maprealtime.impl.MapRealtimeView
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheetEvents
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.networkmonitor.api.ui.ConnectivityIndicatorView
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.launch

@Composable
fun MessagesView(
    state: MessagesBottomSheetState,
    mapRealtimeState: MapRealtimePresenterState,
    onBackClick: () -> Unit,
    onRoomDetailsClick: () -> Unit,
    onEventContentClick: (event: TimelineItem.Event) -> Boolean,
    onUserDataClick: (UserId) -> Unit,
    onLinkClick: (String) -> Unit,
    onSendLocationClick: () -> Unit,
    onCreatePollClick: () -> Unit,
    onJoinCallClick: () -> Unit,
    onShowMapClick: () -> Unit,
    onViewAllPinnedMessagesClick: () -> Unit,
    modifier: Modifier = Modifier,
    forceJumpToBottomVisibility: Boolean = false,
    knockRequestsBannerView: @Composable () -> Unit,
) {
//    OnLifecycleEvent { _, event ->
//        state.voiceMessageComposerState.eventSink(VoiceMessageComposerEvents.LifecycleEvent(event))
//    }
//
//    KeepScreenOn(state.voiceMessageComposerState.keepScreenOn)
//
//    HideKeyboardWhenDisposed()
//
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)

//     This is needed because the composer is inside an AndroidView that can't be affected by the FocusManager in Compose
    val localView = LocalView.current

    fun hidingKeyboard(block: () -> Unit) {
        localView.hideKeyboard()
        block()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            ConnectivityIndicatorView(isOnline = state.hasNetworkConnection)
        },
        content = { padding ->
            Box {
//                if (state.isMessagesCollapsed) {
                MapRealtimeView(
                    state = mapRealtimeState,
                    onBackPressed = {
                        // Since the textfield is now based on an Android view, this is no longer done automatically.
                        // We need to hide the keyboard when navigating out of this screen.
                        localView.hideKeyboard()
                        onBackClick()
                    },
                    onJoinCallClick = onJoinCallClick,
                    roomCallState = state.roomCallState,
                    onMessagesPressed = {
                        println("viktor, onMessagePressed")
                        onShowMapClick()
                        state.eventSink(MessagesEvents.ShowMessages)
                    })
//                }
                val isKeyboardVisible by keyboardAsState()
//                if (state.isMessagesCollapsed) {
//                    Modifier
//                        .padding(padding)
//                        .consumeWindowInsets(padding)
//                        .height(if (isKeyboardVisible) 500.dp else 390.dp)
//                        .align(Alignment.BottomCenter)
//                } else {
//                    Modifier
//                        .padding(padding)
//                        .consumeWindowInsets(padding)
//                }

//                val messagesModifier = if (state.isMessagesCollapsed) {
//                    Modifier
//                        .padding(padding)
//                        .consumeWindowInsets(padding)
//                        .height(if (isKeyboardVisible) 500.dp else 300.dp)
//                        .align(Alignment.BottomCenter)
//                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
//                } else {
//                    Modifier
//                        .padding(padding)
//                        .consumeWindowInsets(padding)
//                }

                val scope = rememberCoroutineScope()
                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = false,
                )

                LaunchedEffect(isKeyboardVisible) {
                    println("viktor, isKeybVisible=$isKeyboardVisible")
                    if (isKeyboardVisible) {
                        scope.launch {
                            sheetState.show()
                        }
                    }
                }

                when (state) {
                    is MessagesBottomSheetState.Hidden -> Unit
                    is MessagesBottomSheetState.MessagesState -> {
                        ModalBottomSheet(
                            sheetState = sheetState,
                            onDismissRequest = {
                                state.eventSink(MessagesEvents.HideMessages)
                            }
                        ) {
                            MessagesViewContent(
                                state = state,
                                modifier = Modifier,
                                onUserDataClick = { hidingKeyboard { onUserDataClick(it) } },
                                onLinkClick = onLinkClick,
                                onReadReceiptClick = { event ->
                                    state.readReceiptBottomSheetState.eventSink(ReadReceiptBottomSheetEvents.EventSelected(event))
                                },
                                onSendLocationClick = onSendLocationClick,
                                onCreatePollClick = onCreatePollClick,
                                onSwipeToReply = { targetEvent ->
                                    state.eventSink(MessagesEvents.HandleAction(TimelineItemAction.Reply, targetEvent))
                                },
                                forceJumpToBottomVisibility = forceJumpToBottomVisibility,
                                onJoinCallClick = onJoinCallClick,
                                onViewAllPinnedMessagesClick = onViewAllPinnedMessagesClick,
                                knockRequestsBannerView = knockRequestsBannerView,
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
    )
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(newValue = isImeVisible)
}

@PreviewsDayNight
@Composable
internal fun MessagesViewPreview(@PreviewParameter(MessagesStateProvider::class) state: MessagesBottomSheetState) = ElementPreview {
    MessagesView(
        state = state,
        onBackClick = {},
        onRoomDetailsClick = {},
        onEventContentClick = { false },
        onUserDataClick = {},
        onLinkClick = {},
        onSendLocationClick = {},
        onCreatePollClick = {},
        onJoinCallClick = {},
        onViewAllPinnedMessagesClick = { },
        forceJumpToBottomVisibility = true,
        knockRequestsBannerView = {},
        onShowMapClick = {},
        mapRealtimeState = TODO()
    )
}
