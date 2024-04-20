package com.toasterofbread.spmp.model.settings

import dev.toastbits.composekit.platform.PlatformFile
import dev.toastbits.composekit.platform.PlatformPreferences
import com.toasterofbread.spmp.model.settings.category.SettingsGroup
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.json.encodeToJsonElement

object SettingsImportExport {
    @Serializable
    data class SettingsExportData(
        val included_groups: List<String>?,
        val values: JsonObject?
    ) {
        fun getGroups(context: AppContext): List<SettingsGroup>? =
            included_groups?.mapNotNull { context.settings.groupFromKey(it) }
    }

    fun exportSettingsData(
        prefs: PlatformPreferences,
        groups: List<SettingsGroup>
    ): SettingsExportData {
        val values: MutableMap<String, JsonElement> = mutableMapOf()

        for (category in groups) {
            for (property in category.getAllProperties()) {
                val value: Any? = property.get()
                if (value != property.getDefaultValue()) {
                    values[property.key] = property.serialise(value)
                }
            }
        }

        return SettingsExportData(
            included_groups = groups.map { it.group_key },
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
        context: AppContext,
        data: SettingsExportData,
        groups: List<SettingsGroup>?
    ): ImportResult {
        var directly_imported: Int = 0
        var default_imported: Int = 0

        if (data.values != null) {
            context.getPrefs().edit {
                val all_groups: Collection<SettingsGroup> = context.settings.all_groups.values
                val included_groups: List<SettingsGroup>? = data.included_groups?.map { key ->
                    context.settings.groupFromKey(key) ?: throw RuntimeException(key)
                }

                for (category in included_groups ?: all_groups) {
                    if (groups != null && !groups.contains(category)) {
                        continue
                    }

                    for (property in category.getAllProperties()) {
                        val value: JsonElement? = data.values[property.key]
                        if (value != null) {
                            property.set(value, this)
                            directly_imported++
                        }
                        else {
                            remove(property.key)
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
        // null -> JsonPrimitive(null)
        // is String -> JsonPrimitive(value)
        is Set<*> -> JsonArray((value as Set<String>).map { JsonPrimitive(it) })
        // is Int -> JsonPrimitive(value)
        // is Long -> JsonPrimitive(value)
        // is Float -> JsonPrimitive(value)
        // is Boolean -> JsonPrimitive(value)
        else -> Json.encodeToJsonElement(value)
        // else -> throw NotImplementedError(value::class.toString())
    }

private fun jsonElementToPrefsValue(element: JsonElement?): Any? =
    when (element) {
        null, is JsonNull -> null
        is JsonArray -> element.jsonArray.map { it.jsonPrimitive.content }.toSet()
        is JsonPrimitive -> element.booleanOrNull ?: element.intOrNull ?: element.longOrNull ?: element.floatOrNull ?: element.content
        else -> throw NotImplementedError(element::class.toString())
    }
