@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.utils.composable

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun LoadActionIconButton(
    performLoad: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    uncancellable: Boolean = false,
    load_on_launch: Boolean = false,
    content: @Composable () -> Unit,
) {
    val coroutine_scope = rememberCoroutineScope()
    var load_in_progress: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!load_on_launch || load_in_progress) {
            return@LaunchedEffect
        }

        coroutine_scope.launch(
            if (uncancellable) NonCancellable
            else EmptyCoroutineContext
        ) {
            load_in_progress = true
            performLoad()
            load_in_progress = false
        }
    }

    Crossfade(
        load_in_progress,
        modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonTokens.StateLayerSize)
    ) { loading ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                SubtleLoadingIndicator()
            }
            else {
                IconButton(
                    {
                        if (load_in_progress) {
                            return@IconButton
                        }

                        coroutine_scope.launch(
                            if (uncancellable) NonCancellable
                            else EmptyCoroutineContext
                        ) {
                            load_in_progress = true
                            performLoad()
                            load_in_progress = false
                        }
                    },
                    content = content,
                    enabled = enabled
                )
            }
        }
    }
}
