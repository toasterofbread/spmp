package com.toasterofbread.spmp.platform.splash

import ProgramArguments
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SplashExtraLoadingContent(modifier: Modifier = Modifier, arguments: ProgramArguments)
