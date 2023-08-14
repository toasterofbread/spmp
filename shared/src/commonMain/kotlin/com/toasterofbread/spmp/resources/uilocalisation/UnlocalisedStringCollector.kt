package com.toasterofbread.spmp.resources.uilocalisation

import androidx.compose.runtime.mutableStateListOf

class UnlocalisedStringCollector {
    data class UnlocalisedString(
        val type: String, 
        val key: String?, 
        val source_language: String
    ) {
        val stacktrace = Throwable().getStackTrace().takeLast(5)
        
        companion object {
            fun fromLocalised(string: LocalisedYoutubeString) =
                UnlocalisedString("LocalisedYoutubeString.${string.type}", string.key, string.source_language)
        }
    }
    
    val unlocalised_strings: MutableList<UnlocalisedString> = mutableStateListOf()

    fun add(string: UnlocalisedString): Boolean {
        for (item in unlocalised_strings) {
            if (item == string) {
                return false
            }
        }

        unlocalised_strings.add(string)
        return true
    }
    fun add(string: LocalisedYoutubeString) = add(UnlocalisedString.fromLocalised(string))

    fun getStrings(): List<UnlocalisedString> = unlocalised_strings
}
