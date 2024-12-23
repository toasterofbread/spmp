package com.toasterofbread.spmp.model.settings

import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.model.settings.category.DependencySettings
import com.toasterofbread.spmp.model.settings.category.DiscordAuthSettings
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.model.settings.category.ExperimentalSettings
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.model.settings.category.FilterSettings
import com.toasterofbread.spmp.model.settings.category.InterfaceSettings
import com.toasterofbread.spmp.model.settings.category.LayoutSettings
import com.toasterofbread.spmp.model.settings.category.LyricsSettings
import com.toasterofbread.spmp.model.settings.category.MiscSettings
import com.toasterofbread.spmp.model.settings.category.PlatformSettings
import com.toasterofbread.spmp.model.settings.category.PlayerSettings
import com.toasterofbread.spmp.model.settings.category.SearchSettings
import com.toasterofbread.spmp.model.settings.category.ShortcutSettings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.model.settings.category.WidgetSettings
import com.toasterofbread.spmp.model.settings.category.YTApiSettings
import com.toasterofbread.spmp.model.settings.category.YoutubeAuthSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.Language
import dev.toastbits.composekit.commonsettings.impl.ComposeKitSettings
import dev.toastbits.composekit.settings.ComposeKitSettingsGroup
import dev.toastbits.composekit.settings.ui.screen.PlatformSettingsGroupScreen

class Settings(
    private val context: AppContext,
    available_languages: List<Language>
): ComposeKitSettings {
    val YoutubeAuth: YoutubeAuthSettings = YoutubeAuthSettings(context)
    val Behaviour: BehaviourSettings = BehaviourSettings(context)
    val Layout: LayoutSettings = LayoutSettings(context)
    val Player: PlayerSettings = PlayerSettings(context)
    val Feed: FeedSettings = FeedSettings(context)
    override val Theme: ThemeSettings = ThemeSettings(context)
    val Lyrics: LyricsSettings = LyricsSettings(context)
    val Discord: DiscordSettings = DiscordSettings(context)
    val DiscordAuth: DiscordAuthSettings = DiscordAuthSettings(context)
    val Filter: FilterSettings = FilterSettings(context)
    val Streaming: StreamingSettings = StreamingSettings(context)
    val Shortcut: ShortcutSettings = ShortcutSettings(context)
    val Platform: PlatformSettings = PlatformSettings(context)
    val Misc: MiscSettings = MiscSettings(context)
    val Widget: WidgetSettings = WidgetSettings(context)
    val Deps: DependencySettings = DependencySettings(context)
    val Search: SearchSettings = SearchSettings(context)
    val Experimental: ExperimentalSettings = ExperimentalSettings(context)
    val YtApi: YTApiSettings = YTApiSettings(context.getPrefs())
    override val Interface: InterfaceSettings = InterfaceSettings(context)

    override val preferences: dev.toastbits.composekit.settings.PlatformSettings
        get() = context.getPrefs()

    override val allGroups: List<ComposeKitSettingsGroup>
        get() = all_groups.values.toList()

    val all_groups: Map<String, SettingsGroup> =
        listOf(
            YoutubeAuth,

            Interface,
            Behaviour,
            Layout,
            Player,
            Feed,
            Theme,
            Lyrics,
            Discord,
            DiscordAuth,
            Filter,
            Streaming,
            Shortcut,
            Platform,
            Widget,
            Misc,
            Deps,
            Experimental,

            YtApi
        ).associateBy { it.groupKey }

    val groups_with_page: List<SettingsGroup> get() =
        all_groups.values.filter { !it.hidden && it !is DependencySettings }

    val group_pages: List<PlatformSettingsGroupScreen> get() =
        all_groups.values.mapNotNull { if (it is DependencySettings) null else it.takeIf { !it.hidden }?.let { PlatformSettingsGroupScreen(it) } }

    fun groupFromKey(key: String): SettingsGroup? =
        all_groups[key]
}
