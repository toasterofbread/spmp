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
import kotlinx.serialization.json.*

object SettingsImportExport {
    @Serializable
    data class SettingsExportData(
        val included_categories: List<String>?,
        val values: JsonObject?
    ) {
        fun getCategories(): List<SettingsCategory>? =
            included_categories?.mapNotNull { SettingsCategory.fromIdOrNull(it) }
    }

    fun exportSettingsData(
        prefs: PlatformPreferences,
        categories: List<SettingsCategory>
    ): SettingsExportData {
        val values: MutableMap<String, JsonElement> = mutableMapOf()

        for (category in categories) {
            for (key in category.keys) {
                val value: Any = key.get(prefs)
                if (value != key.getDefaultValue()) {
                    values[key.getName()] = prefsValueToJsonElement(value)
                }
            }
        }

        return SettingsExportData(
            included_categories = categories.map { it.id },
            values = JsonObject(values)
        )
    }

    suspend fun loadSettingsFile(file: PlatformFile): SettingsExportData =
        withContext(Dispatchers.IO) {
            return@withContext file.inputStream().use { stream ->
                Json.decodeFromStream(stream)
            }
        }

    data class ImportResult(
        val directly_imported_count: Int,
        val default_imported_count: Int
    )

    fun importSettingsData(
        prefs: PlatformPreferences,
        data: SettingsExportData,
        categories: List<SettingsCategory>?
    ): ImportResult {
        var directly_imported: Int = 0
        var default_imported: Int = 0

        if (data.values != null) {
            prefs.edit {
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
                        val value: Any? = jsonElementToPrefsValue(data.values[name])
                        putAny(name, value, key.getDefaultValue())

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

private fun prefsValueToJsonElement(value: Any?): JsonElement =
    when (value) {
        null -> JsonPrimitive(null)
        is String -> JsonPrimitive(value)
        is Set<*> -> JsonArray((value as Set<String>).map { JsonPrimitive(it) })
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        else -> throw NotImplementedError(value::class.toString())
    }

private fun jsonElementToPrefsValue(element: JsonElement?): Any? =
    when (element) {
        null, is JsonNull -> null
        is JsonArray -> element.jsonArray.map { it.jsonPrimitive.content }.toSet()
        is JsonPrimitive -> element.booleanOrNull ?: element.intOrNull ?: element.longOrNull ?: element.floatOrNull ?: element.content
        else -> throw NotImplementedError(element::class.toString())
    }
