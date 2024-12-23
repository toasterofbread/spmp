package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsGroupImpl
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getLyricsCategoryItems
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.SettingsItem
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_cat_desc_lyrics
import spmp.shared.generated.resources.s_cat_lyrics
import spmp.shared.generated.resources.s_key_lyrics_default_furigana
import spmp.shared.generated.resources.s_key_lyrics_default_source
import spmp.shared.generated.resources.s_key_lyrics_enable_word_sync
import spmp.shared.generated.resources.s_key_lyrics_extra_padding
import spmp.shared.generated.resources.s_key_lyrics_follow_enabled
import spmp.shared.generated.resources.s_key_lyrics_follow_offset
import spmp.shared.generated.resources.s_key_lyrics_font_size
import spmp.shared.generated.resources.s_key_lyrics_romanise_furigana
import spmp.shared.generated.resources.s_key_lyrics_sync_delay
import spmp.shared.generated.resources.s_key_lyrics_sync_delay_bluetooth
import spmp.shared.generated.resources.s_key_lyrics_sync_delay_topbar
import spmp.shared.generated.resources.s_key_lyrics_text_alignment
import spmp.shared.generated.resources.s_sub_lyrics_enable_word_sync
import spmp.shared.generated.resources.s_sub_lyrics_extra_padding
import spmp.shared.generated.resources.s_sub_lyrics_follow_enabled
import spmp.shared.generated.resources.s_sub_lyrics_follow_offset
import spmp.shared.generated.resources.s_sub_lyrics_sync_delay
import spmp.shared.generated.resources.s_sub_lyrics_sync_delay_bluetooth
import spmp.shared.generated.resources.s_sub_lyrics_sync_delay_topbar

class LyricsSettings(val context: AppContext): SettingsGroupImpl("LYRICS", context.getPrefs()) {
    val FOLLOW_ENABLED: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lyrics_follow_enabled) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_follow_enabled) },
        getDefaultValue = { true }
    )
    val FOLLOW_OFFSET: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_lyrics_follow_offset) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_follow_offset) },
        getDefaultValue = { 0.25f }
    )
    val ROMANISE_FURIGANA: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lyrics_romanise_furigana) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val DEFAULT_FURIGANA: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lyrics_default_furigana) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val TEXT_ALIGNMENT: PlatformSettingsProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_lyrics_text_alignment) },
        getDescription = { null },
        getDefaultValue = { 0 } // Left, center, right
    )
    val EXTRA_PADDING: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lyrics_extra_padding) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_extra_padding) },
        getDefaultValue = { true }
    )
    val ENABLE_WORD_SYNC: PlatformSettingsProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_lyrics_enable_word_sync) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_enable_word_sync) },
        getDefaultValue = { false }
    )
    val FONT_SIZE: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_lyrics_font_size) },
        getDescription = { null },
        getDefaultValue = { 0.5f }
    )
    val DEFAULT_SOURCE: PlatformSettingsProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_lyrics_default_source) },
        getDescription = { null },
        getDefaultValue = { 0 }
    )
    val SYNC_DELAY: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_lyrics_sync_delay) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_sync_delay) },
        getDefaultValue = { 0f }
    )
    val SYNC_DELAY_TOPBAR: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_lyrics_sync_delay_topbar) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_sync_delay_topbar) },
        getDefaultValue = { -0.5f }
    )
    val SYNC_DELAY_BLUETOOTH: PlatformSettingsProperty<Float> by property(
        getName = { stringResource(Res.string.s_key_lyrics_sync_delay_bluetooth) },
        getDescription = { stringResource(Res.string.s_sub_lyrics_sync_delay_bluetooth) },
        getDefaultValue = { 0.3f }
    )

    @Composable
    override fun getTitle(): String = stringResource(Res.string.s_cat_lyrics)

    @Composable
    override fun getDescription(): String = stringResource(Res.string.s_cat_desc_lyrics)

    @Composable
    override fun getIcon(): ImageVector = Icons.Outlined.MusicNote

    override fun getConfigurationItems(): List<SettingsItem> = getLyricsCategoryItems(context)
}
