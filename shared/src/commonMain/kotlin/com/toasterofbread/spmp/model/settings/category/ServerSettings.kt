package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getServerCategoryItems

data object ServerSettings: SettingsCategory("server") {
    override val keys: List<SettingsKey> = Key.values().toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_server"),
            getString("s_cat_desc_server"),
            { getServerCategoryItems() }
        ) { Icons.Outlined.Dns }

    enum class Key: SettingsKey {
        IP_ADDRESS,
        PORT,
        LOCAL_COMMAND,
        KILL_CHILD_ON_EXIT;

        override val category: SettingsCategory get() = ServerSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                IP_ADDRESS -> "127.0.0.1"
                PORT -> 3973
                LOCAL_COMMAND -> "spms"
                KILL_CHILD_ON_EXIT -> true
            } as T
    }
}
