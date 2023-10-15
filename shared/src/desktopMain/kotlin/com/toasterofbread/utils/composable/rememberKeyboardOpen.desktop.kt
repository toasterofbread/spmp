package com.toasterofbread.utils.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

@Composable
actual fun rememberKeyboardOpen(): State<Boolean> = mutableStateOf(false)
