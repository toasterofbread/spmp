package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getContrasted
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EditMenu(song: Song, close: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text("Edit song details", fontWeight = FontWeight.Light, fontSize = 20.sp)

        val tab_state = rememberPagerState()
        val scope = rememberCoroutineScope()

        Column(verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {

            TabRow(
                tab_state.targetPage,
                Modifier.clip(RoundedCornerShape(16.dp)).height(30.dp).fillMaxWidth(0.75f),
                containerColor = MainActivity.theme.getOnBackground(true).setAlpha(0.75),
                contentColor = MainActivity.theme.getOnBackground(true).getContrasted(),
                indicator = { tab_positions ->
                    val offset =
                        if (tab_state.currentPageOffset == 0.0f) tab_positions[tab_state.currentPage].left
                        else tab_positions[tab_state.currentPage].left + (tab_positions[1].left - tab_positions[0].left) * tab_state.currentPageOffset

                    Box(
                        Modifier
                            .requiredWidth(tab_positions[tab_state.targetPage].width)
                            .offset(offset - (tab_positions[tab_state.targetPage].width / 2))
                            .background(
                                MainActivity.theme.getOnBackground(true).getContrasted()
                                    .setAlpha(0.15), RoundedCornerShape(16.dp)
                            )
                    )
                }
            ) {
                for (i in 0 until 2) {
                    Tab(true, {
                        scope.launch {
                            tab_state.animateScrollToPage(i)
                        }
                    }) {
                        Text(
                            when (i) {
                                0 -> "Details"
                                else -> "Lyrics"
                            }
                        )
                    }
                }
            }

            HorizontalPager(2, state = tab_state) {
                Text("Yo wassup $it")
            }

        }
    }
}