package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.platform.PreferencesProperty
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getFeedCategoryItems
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.PlatformPreferences
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.s_key_feed_show_artists_row
import spmp.shared.generated.resources.s_key_feed_show_song_download_indicators
import spmp.shared.generated.resources.s_key_feed_initial_rows
import spmp.shared.generated.resources.s_sub_feed_initial_rows
import spmp.shared.generated.resources.s_key_feed_square_preview_text_lines
import spmp.shared.generated.resources.s_key_feed_grid_row_count
import spmp.shared.generated.resources.s_key_feed_grid_row_count_expanded
import spmp.shared.generated.resources.s_key_feed_alt_grid_row_count
import spmp.shared.generated.resources.s_key_feed_alt_grid_row_count_expanded
import spmp.shared.generated.resources.s_key_feed_show_radios
import spmp.shared.generated.resources.s_key_hidden_feed_rows
import spmp.shared.generated.resources.s_hidden_feed_rows_dialog_title
import spmp.shared.generated.resources.s_cat_feed
import spmp.shared.generated.resources.s_cat_desc_feed

class FeedSettings(val context: AppContext): SettingsGroup("FEED", context.getPrefs()) {
    val SHOW_ARTISTS_ROW: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_feed_show_artists_row) },
        getDescription = { null },
        getDefaultValue = { true }
    )
    val SHOW_SONG_DOWNLOAD_INDICATORS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_feed_show_song_download_indicators) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val INITIAL_ROWS: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_initial_rows) },
        getDescription = { stringResource(Res.string.s_sub_feed_initial_rows) },
        getDefaultValue = { 4 }
    )
    val SQUARE_PREVIEW_TEXT_LINES: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_square_preview_text_lines) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 2 else 2 }
    )
    val GRID_ROW_COUNT: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_grid_row_count) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 1 else 2 }
    )
    val GRID_ROW_COUNT_EXPANDED: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_grid_row_count_expanded) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 1 else 2 }
    )
    val LANDSCAPE_GRID_ROW_COUNT: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_alt_grid_row_count) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 1 else 1 }
    )
    val LANDSCAPE_GRID_ROW_COUNT_EXPANDED: PreferencesProperty<Int> by property(
        getName = { stringResource(Res.string.s_key_feed_alt_grid_row_count_expanded) },
        getDescription = { null },
        getDefaultValue = { if (Platform.DESKTOP.isCurrent()) 1 else 1 }
    )
    val SHOW_RADIOS: PreferencesProperty<Boolean> by property(
        getName = { stringResource(Res.string.s_key_feed_show_radios) },
        getDescription = { null },
        getDefaultValue = { false }
    )
    val HIDDEN_ROWS: PreferencesProperty<Set<String>> by property(
        getName = { stringResource(Res.string.s_key_hidden_feed_rows) },
        getDescription = { stringResource(Res.string.s_hidden_feed_rows_dialog_title) },
        getDefaultValue = { emptySet<String>() }
    )

    override val page: CategoryPage? =
        SimplePage(
            { stringResource(Res.string.s_cat_feed) },
            { stringResource(Res.string.s_cat_desc_feed) },
            { getFeedCategoryItems(context) },
            { Icons.Outlined.FormatListBulleted }
        )
}
