package com.spectre7.spmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spectre7.spmp.model.YoutubeMusicAuthInfo

const val YOUTUBE_MUSIC_URL = "https://music.youtube.com/"
const val YOUTUBE_MUSIC_LOGIN_URL = "https://accounts.google.com/v3/signin/identifier?dsh=S1527412391%3A1678373417598386&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den-GB%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%253Fcbrd%253D1%26feature%3D__FEATURE__&hl=en-GB&ifkv=AWnogHfK4OXI8X1zVlVjzzjybvICXS4ojnbvzpE4Gn_Pfddw7fs3ERdfk-q3tRimJuoXjfofz6wuzg&ltmpl=music&passive=true&service=youtube&uilel=3&flowName=GlifWebSignIn&flowEntry=ServiceLogin"

@Composable
expect fun YoutubeMusicLogin(modifier: Modifier = Modifier, onFinished: (Result<YoutubeMusicAuthInfo>) -> Unit)
