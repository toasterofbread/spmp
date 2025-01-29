package com.toasterofbread.spmp.model

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.resources.Language
import com.toasterofbread.spmp.resources.getResourceEnvironment
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.util.model.Locale
import dev.toastbits.ytmkt.uistrings.RawUiString
import dev.toastbits.ytmkt.uistrings.UiString
import dev.toastbits.ytmkt.uistrings.YoutubeUiString
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.allStringResources

data class AppUiString(
    val string_key: String
): UiString {
    private var strings: MutableMap<String, String> = mutableMapOf()

    override suspend fun getString(language: String): String =
        strings.getOrPut(language) {
            val string: StringResource = Res.allStringResources[string_key] ?: throw RuntimeException("String resource with key '$string_key' not found")
            val resource_environment: ResourceEnvironment = Language.fromIdentifier(language).getResourceEnvironment()
            return@getOrPut org.jetbrains.compose.resources.getString(resource_environment, string)
        }
}

@Composable
fun UiString.observe(): String {
    val player: PlayerState = LocalPlayerState.current
    var string: String by remember { mutableStateOf("") }
    val ui_language: Locale by player.context.observeUiLanguage()

    LaunchedEffect(this, ui_language) {
        string = getString(ui_language.toTag())
    }

    return string
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

suspend fun UiString.getString(context: AppContext): String =
    getString(context.getUiLanguage().toTag())

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
