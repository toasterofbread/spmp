package com.toasterofbread.spmp.model.settings

import com.toasterofbread.spmp.model.settings.category.*
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.Language

class Settings(val context: AppContext, available_languages: List<Language>) {
    val youtube_auth: YoutubeAuthSettings = YoutubeAuthSettings(context)
    val system: SystemSettings = SystemSettings(context, available_languages)
    val behaviour: BehaviourSettings = BehaviourSettings(context)
    val layout: LayoutSettings = LayoutSettings(context)
    val state: PlayerSettings = PlayerSettings(context)
    val feed: FeedSettings = FeedSettings(context)
    val theme: ThemeSettings = ThemeSettings(context)
    val lyrics: LyricsSettings = LyricsSettings(context)
    val discord: DiscordSettings = DiscordSettings(context)
    val discord_auth: DiscordAuthSettings = DiscordAuthSettings(context)
    val filter: FilterSettings = FilterSettings(context)
    val streaming: StreamingSettings = StreamingSettings(context)
    val shortcut: ShortcutSettings = ShortcutSettings(context)
    val platform: PlatformSettings = PlatformSettings(context)
    val misc: MiscSettings = MiscSettings(context)
    val deps: DependencySettings = DependencySettings(context)
    val search: SearchSettings = SearchSettings(context)
    val ytapi: YTApiSettings = YTApiSettings(context.getPrefs())

    val all_groups: Map<String, SettingsGroup> =
        listOf(
            youtube_auth,

            system,
            behaviour,
            layout,
            state,
            feed,
            theme,
            lyrics,
            discord,
            discord_auth,
            filter,
            streaming,
            shortcut,
            platform,
            misc,
            deps,

            ytapi
        ).associateBy { it.group_key }

    val groups_with_page: List<SettingsGroup> get() =
        all_groups.values.filter { it.page != null && it !is DependencySettings }

    val group_pages: List<SettingsGroup.CategoryPage> get() =
        all_groups.values.mapNotNull { if (it is DependencySettings) null else it.page }

    fun groupFromKey(key: String): SettingsGroup? =
        all_groups[key]
}
