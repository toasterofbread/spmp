package com.toasterofbread.spmp.platform.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SplashExtraLoadingContent(modifier: Modifier = Modifier, server_executable_path: String? = null)
