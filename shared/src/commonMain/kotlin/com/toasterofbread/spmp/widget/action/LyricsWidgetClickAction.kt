package com.toasterofbread.spmp.widget.action

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_click_action_lyrics_hide_until_next_song
import spmp.shared.generated.resources.widget_click_action_lyrics_toggle_furigana

@Serializable
enum class LyricsWidgetClickAction(val nameResource: StringResource): TypeWidgetClickAction {
    TOGGLE_FURIGANA(Res.string.widget_click_action_lyrics_toggle_furigana),
    HIDE_UNTIL_NEXT_SONG(Res.string.widget_click_action_lyrics_hide_until_next_song);
}
