package com.spectre7.spmp.ui.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.utils.*
import com.spectre7.utils.getString
import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.ptl.Ptl

@Composable
fun DownloadMenu(song: Song, on_close_request: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Download")
    }
}