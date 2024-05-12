package com.toasterofbread.spmp.model

import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.UiString
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage

data class AppUiString(
    val string_key: String
): UiString {
    override fun getString(language: String): String =
        com.toasterofbread.spmp.resources.getString(string_key)
}

fun UiString.serialise(): String =
    when (this) {
        is AppUiString -> "A,$string_key"
        is RawUiString -> "R,$raw_string"
        is YoutubeUiString -> "Y,${type.ordinal},$index"
        else -> throw NotImplementedError(this::class.toString())
    }

fun UiString.Companion.deserialise(data: String): UiString {
    val split: List<String> = data.split(",", limit = 3)

    try {
        var type: String = split[0]

        // For backwards-compatibility
        type.toIntOrNull()?.also { int ->
            when (int) {
                0 -> type = "R"
                1 -> type = "A"
                2 -> type = "Y"
            }
        }

        when (type) {
            "R" -> return RawUiString(split[1])
            "A" -> return AppUiString(split[1])
            "Y" -> {
                return YoutubeUiString(
                    YoutubeUiString.Type.entries[split[1].toInt()],
                    split[2].toInt()
                )
            }
            else -> throw NotImplementedError("Unknown type '$type'")
        }
    }
    catch (e: Throwable) {
        val exception = RuntimeException("UiString deserialisation failed '$data' $split", e)
        exception.printStackTrace()
        throw exception
    }
}

fun UiString.getString(context: AppContext): String =
    getString(context.getUiLanguage())

//    companion object {
//        fun mediaItemPage(key: String, item_type: MediaItemType, context: AppContext, source_language: String = context.getDataLanguage()): UiString =
//            when (item_type) {
//                MediaItemType.ARTIST -> Type.ARTIST_PAGE.createFromKey(key, context)
//                else -> {
//                    SpMp.onUnlocalisedStringFound(item_type.toString(), key, source_language)
//                    RawLocalisedString(key)
//                }
//            }
//    }
