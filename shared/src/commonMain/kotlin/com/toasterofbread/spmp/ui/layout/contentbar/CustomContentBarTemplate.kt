package com.toasterofbread.spmp.ui.layout.contentbar

import LocalPlayerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.components.platform.composable.platformClickable
import com.toasterofbread.spmp.model.appaction.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.contentbar.element.*
import com.toasterofbread.spmp.ui.theme.appHover
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.vibrantAccent
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.content_bar_template_navigation
import spmp.shared.generated.resources.content_bar_template_lyrics
import spmp.shared.generated.resources.content_bar_template_song_actions
import spmp.shared.generated.resources.content_bar_template_default_portrait_top_upper
import spmp.shared.generated.resources.content_bar_template_default_portrait_top_lower
import spmp.shared.generated.resources.content_bar_template_desc_navigation
import spmp.shared.generated.resources.content_bar_template_desc_lyrics
import spmp.shared.generated.resources.content_bar_template_desc_song_actions
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.content_bar_editor_template_dialog_title

enum class CustomContentBarTemplate {
    NAVIGATION,
    LYRICS,
    SONG_ACTIONS,
    DEFAULT_PORTRAIT_TOP_UPPER,
    DEFAULT_PORTRAIT_TOP_LOWER;

    fun getContentBar(): ContentBar =
        TemplateCustomContentBar(this)

    @Composable
    fun getName(): String =
        when (this) {
            NAVIGATION -> stringResource(Res.string.content_bar_template_navigation)
            LYRICS -> stringResource(Res.string.content_bar_template_lyrics)
            SONG_ACTIONS -> stringResource(Res.string.content_bar_template_song_actions)
            DEFAULT_PORTRAIT_TOP_UPPER -> stringResource(Res.string.content_bar_template_default_portrait_top_upper)
            DEFAULT_PORTRAIT_TOP_LOWER -> stringResource(Res.string.content_bar_template_default_portrait_top_lower)
        }

    @Composable
    fun getDescription(): String? =
        when (this) {
            NAVIGATION -> stringResource(Res.string.content_bar_template_desc_navigation)
            LYRICS -> stringResource(Res.string.content_bar_template_desc_lyrics)
            SONG_ACTIONS -> stringResource(Res.string.content_bar_template_desc_song_actions)
            DEFAULT_PORTRAIT_TOP_UPPER -> null
            DEFAULT_PORTRAIT_TOP_LOWER -> null
        }

    fun getIcon(): ImageVector =
        when (this) {
            NAVIGATION -> Icons.Default.Widgets
            LYRICS -> Icons.Default.Lyrics
            SONG_ACTIONS -> Icons.Default.MusicNote
            DEFAULT_PORTRAIT_TOP_UPPER -> Icons.Default.VerticalAlignTop
            DEFAULT_PORTRAIT_TOP_LOWER -> Icons.Default.VerticalAlignTop
        }

    fun getDefaultHeight(): Dp =
        when (this) {
            NAVIGATION -> 50.dp
            LYRICS -> 40.dp
            SONG_ACTIONS -> 50.dp
            DEFAULT_PORTRAIT_TOP_UPPER -> 50.dp
            DEFAULT_PORTRAIT_TOP_LOWER -> 50.dp
        }

    fun getElements(): List<ContentBarElement> =
        when (this) {
            NAVIGATION -> listOf(
                ContentBarElementButton.ofAppPage(AppPage.Type.SONG_FEED),
                ContentBarElementButton.ofAppPage(AppPage.Type.LIBRARY),
                ContentBarElementButton.ofAppPage(AppPage.Type.SEARCH),
                ContentBarElementButton.ofAppPage(AppPage.Type.RADIO_BUILDER),
                ContentBarElementButton(OtherAppAction(OtherAppAction.Action.RELOAD_PAGE)),
                ContentBarElementPinnedItems(config = ContentBarElementConfig(size_mode = ContentBarElement.SizeMode.FILL)),
                ContentBarElementButton.ofAppPage(AppPage.Type.PROFILE),
                ContentBarElementButton.ofAppPage(AppPage.Type.CONTROL_PANEL),
                ContentBarElementButton.ofAppPage(AppPage.Type.SETTINGS)
            )
            LYRICS -> listOf(
                ContentBarElementCrossfade(
                    ContentBarElementConfig(size_mode = ContentBarElement.SizeMode.FILL),
                    elements = listOf(
                        ContentBarElementLyrics(),
                        ContentBarElementVisualiser()
                    )
                )
            )
            SONG_ACTIONS -> listOf(
                ContentBarElementButton(SongAppAction(SongAppAction.Action.OPEN_EXTERNALLY)),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.TOGGLE_LIKE)),
                ContentBarElementSpacer(config = ContentBarElementConfig(size_mode = ContentBarElement.SizeMode.FILL)),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.DOWNLOAD)),
                ContentBarElementButton(SongAppAction(SongAppAction.Action.START_RADIO))
            )
            DEFAULT_PORTRAIT_TOP_UPPER -> listOf(
                ContentBarElementButton.ofAppPage(AppPage.Type.SETTINGS),
                ContentBarElementCrossfade(
                    ContentBarElementConfig(size_mode = ContentBarElement.SizeMode.FILL),
                    elements = listOf(
                        ContentBarElementLyrics(),
                        ContentBarElementVisualiser()
                    )
                ),
                ContentBarElementButton.ofAppPage(AppPage.Type.LIBRARY)
            )
            DEFAULT_PORTRAIT_TOP_LOWER -> listOf(
                ContentBarElementButton.ofAppPage(AppPage.Type.SEARCH),
                ContentBarElementContentBar(
                    config = ContentBarElementConfig(
                        size_mode = ContentBarElement.SizeMode.FILL,
                        hide_bar_when_empty = true
                    ),
                    bar = ContentBarReference.ofInternalBar(InternalContentBar.PRIMARY)
                )
            )
        }

    @Composable
    private fun BarPreview(modifier: Modifier = Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val bar: CustomContentBar = remember { CustomContentBar("", elements = getElements()) }

        Column(
            modifier
                .background(player.theme.card, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(getIcon(), null)
                Text(getName())
            }

            bar.CustomBarContent(
                modifier = Modifier.background(player.theme.vibrantAccent, RoundedCornerShape(16.dp)),
                background_colour = ThemeValues.Slot.Extension.VIBRANT_ACCENT,
                vertical = false,
                always_display = true,
                content_padding = PaddingValues(5.dp),
                buttonContent = { _, element, size ->
                    element.Element(false, null, size, onPreviewClick = {})
                }
            )
        }
    }

    companion object {
        @Composable
        fun SelectionDialog(modifier: Modifier = Modifier, onSelected: (CustomContentBarTemplate?) -> Unit) {
            val player: PlayerState = LocalPlayerState.current

            AlertDialog(
                { onSelected(null) },
                modifier = modifier,
                confirmButton = {
                    Button(
                        { onSelected(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = player.theme.background,
                            contentColor = player.theme.onBackground
                        ),
                        modifier = Modifier.appHover(true)
                    ) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
                title = {
                    Text(stringResource(Res.string.content_bar_editor_template_dialog_title))
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { template ->
                            template.BarPreview(
                                Modifier
                                    .fillMaxWidth()
                                    .platformClickable(
                                        onClick = { onSelected(template) }
                                    )
                                    .appHover(true)
                            )
                        }
                    }
                }
            )
        }
    }
}
