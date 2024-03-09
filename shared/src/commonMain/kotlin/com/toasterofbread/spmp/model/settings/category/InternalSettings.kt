package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spmp.model.settings.SettingsKey

data object InternalSettings: SettingsCategory("internal") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): CategoryPage? = null

    enum class Key: SettingsKey {
        TOPBAR_MODE_HOME,
        TOPBAR_MODE_NOWPLAYING;

        override val category: SettingsCategory get() = InternalSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                TOPBAR_MODE_HOME -> MusicTopBarMode.LYRICS.ordinal
                TOPBAR_MODE_NOWPLAYING -> MusicTopBarMode.LYRICS.ordinal
            } as T
    }
}

enum class MusicTopBarMode {
    VISUALISER, LYRICS;

    fun getIcon(): ImageVector = when (this) {
        LYRICS -> Icons.Default.Lyrics
        VISUALISER -> Icons.Default.GraphicEq
    }

    fun getNext(can_show_visualiser: Boolean): MusicTopBarMode {
        val next =
            if (ordinal == 0) entries.last()
            else entries[ordinal - 1]

        if (!can_show_visualiser && next == VISUALISER) {
            return next.getNext(false)
        }

        return next
    }

    companion object {
        val default: MusicTopBarMode get() = LYRICS
    }
}
