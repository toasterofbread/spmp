package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalView
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

@Composable
fun rememberKeyboardOpen(): State<Boolean> {
	val keyboard_open = remember { mutableStateOf(false) }

	val view = LocalView.current
	val view_tree_observer = view.viewTreeObserver

	DisposableEffect(view_tree_observer) {
		val listener = ViewTreeObserver.OnGlobalLayoutListener {
			keyboard_open.value = ViewCompat.getRootWindowInsets(view)
				?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
		}
	
		view_tree_observer.addOnGlobalLayoutListener(listener)
		onDispose {
			view_tree_observer.removeOnGlobalLayoutListener(listener)
		}
	}

	return keyboard_open
}
