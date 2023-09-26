package com.toasterofbread.utils.composable

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Composable
fun <T> AlignableCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    label: String = "Crossfade",
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (T) -> Unit
) {
    val transition = updateTransition(targetState, label)
    transition.AlignableCrossfade(modifier, animationSpec, contentAlignment = contentAlignment, content = content)
}

@Composable
fun <T> Transition<T>.AlignableCrossfade(
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    contentKey: (targetState: T) -> Any? = { it },
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (targetState: T) -> Unit
) {
    val currentlyVisible = remember { mutableStateListOf<T>().apply { add(currentState) } }
    val contentMap = remember {
        mutableMapOf<T, @Composable () -> Unit>()
    }
    if (currentState == targetState) {
        // If not animating, just display the current state
        if (currentlyVisible.size != 1 || currentlyVisible[0] != targetState) {
            // Remove all the intermediate items from the list once the animation is finished.
            currentlyVisible.removeAll { it != targetState }
            contentMap.clear()
        }
    }
    if (!contentMap.contains(targetState)) {
        // Replace target with the same key if any
        val replacementId = currentlyVisible.indexOfFirst {
            contentKey(it) == contentKey(targetState)
        }
        if (replacementId == -1) {
            currentlyVisible.add(targetState)
        } 
        else {
            currentlyVisible[replacementId] = targetState
        }
        contentMap.clear()
        currentlyVisible.fastForEach { stateForContent ->
            contentMap[stateForContent] = {
                val alpha by animateFloat(
                    transitionSpec = { animationSpec }
                ) { if (it == stateForContent) 1f else 0f }
                Box(Modifier.graphicsLayer { this.alpha = alpha }, contentAlignment = contentAlignment) {
                    content(stateForContent)
                }
            }
        }
    }

    Box(modifier, contentAlignment = contentAlignment) {
        currentlyVisible.fastForEach {
            key(contentKey(it)) {
                contentMap[it]?.invoke()
            }
        }
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}
