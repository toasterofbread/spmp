package com.toasterofbread.spmp.resources.uilocalisation.localised

object UILanguages {
    val en: String = "en-GB"
    val ja: String = "ja-JP"
    val pl: String = "pl-PL"
    val es: String = "es-US"
}

fun <T> Map<String, T>.getByLanguage(language: String): IndexedValue<Map.Entry<String, T>>? {
    val exact: IndexedValue<Map.Entry<String, T>>? = entries.withIndex().firstOrNull { it.value.key == language }
    if (exact != null) {
        return exact
    }

    val lang: String = language.split('-', limit = 2).first()
    for (entry in entries.withIndex()) {
        if (entry.value.key.split('-', limit = 2).first() == lang) {
            return entry
        }
    }

    return null
}
