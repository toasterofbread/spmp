package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
expect fun rememberKeyboardOpen(): State<Boolean>
