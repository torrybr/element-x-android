package io.element.android.x.node

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.composable.Children
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.node.ParentNode
import com.bumble.appyx.navmodel.backstack.BackStack
import io.element.android.x.architecture.createNode
import io.element.android.x.features.messages.MessagesNode
import io.element.android.x.matrix.core.RoomId
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class RoomFlowNode(
    buildContext: BuildContext,
    private val roomId: RoomId,
    private val backstack: BackStack<NavTarget> = BackStack(
        initialElement = NavTarget.Messages,
        savedStateMap = buildContext.savedStateMap,
    ),
) : ParentNode<RoomFlowNode.NavTarget>(
    navModel = backstack,
    buildContext = buildContext
) {

    init {
        lifecycle.subscribe(
            onCreate = { Timber.v("OnCreate") },
            onDestroy = { Timber.v("OnDestroy") }
        )
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        return when (navTarget) {
            NavTarget.Messages -> createNode<MessagesNode>(buildContext)
        }
    }

    sealed interface NavTarget : Parcelable {
        @Parcelize
        object Messages : NavTarget
    }

    @Composable
    override fun View(modifier: Modifier) {
        Children(navModel = backstack)
    }
}
