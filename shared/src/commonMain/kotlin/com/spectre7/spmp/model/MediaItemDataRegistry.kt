package com.spectre7.spmp.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.platform.ProjectPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking

class MediaItemDataRegistry {
    private val entries: MutableMap<String, Entry> = mutableMapOf()
    val size: Int get() = entries.size

    open class Entry {
        @Json(ignored = true)
        var item: MediaItem? = null

        @Json(ignored = true)
        val title_state: MutableState<String?> = mutableStateOf(null)

        var title: String?
            get() = title_state.value
            set(value) {
                title_state.value = value
                item?.also { i ->
                    i.title_listeners.call(i.title)
                }
            }
        var play_count: Int by mutableStateOf(0)
    }

    @Synchronized
    fun getEntry(item: MediaItem): Entry {
        return entries.getOrPut(item.uid) {
            item.getDefaultRegistryEntry()
        }.also { it.item = item }
    }

    @Synchronized
    fun load(prefs: ProjectPreferences = Settings.prefs) {
        val data = prefs.getString("data_registry", null)
        if (data == null) {
            return
        }

        entries.clear()

        val parsed = DataApi.klaxon.parseJsonObject(data.reader())
        runBlocking {
            parsed.entries.map { item ->
                GlobalScope.async {
                    val type = MediaItemType.values()[item.key.take(1).toInt()]
                    entries[item.key] = type.parseRegistryEntry(item.value as JsonObject)
                }
            }.joinAll()
        }
    }

    @Synchronized
    fun save(prefs: ProjectPreferences = Settings.prefs) {
        prefs.edit {
            putString("data_registry", DataApi.klaxon.toJsonString(entries))
        }
    }
}
