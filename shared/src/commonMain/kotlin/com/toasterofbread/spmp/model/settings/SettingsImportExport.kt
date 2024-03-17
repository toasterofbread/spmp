package com.toasterofbread.spmp.model.settings

import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.putAny
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement

object SettingsImportExport {
    @Serializable
    data class SettingsExportData(
        val included_categories: List<String>?,
        val values: JsonObject?
    ) {
        fun getCategories(): List<SettingsCategory>? =
            included_categories?.mapNotNull { SettingsCategory.fromIdOrNull(it) }
    }

    suspend fun exportSettings(
        context: AppContext,
        file: PlatformFile,
        categories: List<SettingsCategory>
    ) = withContext(Dispatchers.IO) {
        val prefs: PlatformPreferences = context.getPrefs()
        val values: MutableMap<String, JsonElement> = mutableMapOf()

        for (category in categories) {
            for (key in category.keys) {
                val value: Any = key.get(prefs)
                if (value != key.getDefaultValue()) {
                    values[key.getName()] = Json.encodeToJsonElement(value)
                }
            }
        }

        val data: SettingsExportData = SettingsExportData(
            included_categories = categories.map { it.id },
            values = JsonObject(values)
        )

        file.outputStream().writer().use { writer ->
            writer.write(Json.encodeToString(data))
            writer.flush()
        }
    }

    suspend fun loadSettingsFile(file: PlatformFile): SettingsExportData = withContext(Dispatchers.IO) {
        return@withContext file.inputStream().use { stream ->
            Json.decodeFromStream(stream)
        }
    }

    data class ImportResult(
        val directly_imported_count: Int,
        val default_imported_count: Int
    )

    fun importData(context: AppContext, data: SettingsExportData, categories: List<SettingsCategory>?): ImportResult {
        var directly_imported: Int = 0
        var default_imported: Int = 0

        if (data.values != null) {
            context.getPrefs().edit {
                val all_categories: List<SettingsCategory> = SettingsCategory.all
                val included_categories: List<SettingsCategory>? = data.included_categories?.map { id ->
                    SettingsCategory.fromId(id)
                }

                for (category in included_categories ?: all_categories) {
                    if (categories != null && !categories.contains(category)) {
                        continue
                    }

                    for (key in category.keys) {
                        val name: String = key.getName()
                        val value: Any? = data.values[name]
                        putAny(name, data.values[name], key.getDefaultValue())

                        if (value != null) {
                            directly_imported++
                        }
                        else {
                            default_imported++
                        }
                    }
                }
            }
        }

        return ImportResult(
            directly_imported,
            default_imported
        )
    }
}
