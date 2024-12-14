@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalResourceApi::class)

package com.toasterofbread.spmp.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.allStringResources
import spmp.shared.generated.resources.language_name

// TODO | Replace with ComposeKit Locale
data class Language(
    val family: String,
    val locale: String?,
    val readable_name: String
) {
    val identifier: String
        get() =
            if (locale == null) family
            else "$family-$locale"

    companion object {
        suspend fun fromIdentifier(identifier: String): Language =
            getAvailableLanguages().getBestMatch(identifier) ?: DEFAULT

        suspend fun getSystem(): Language =
            fromIdentifier(Locale.current.toLanguageTag())

        val DEFAULT: Language = Language("en", "GB", "English (GB)")
    }
}

private var available_languages: List<Language>? = null

suspend fun getAvailableLanguages(): List<Language> {
    if (available_languages == null) {
        available_languages =
            Res.string.language_name.items.mapNotNull { language ->
                if (language.qualifiers.isEmpty()) {
                    return@mapNotNull null
                }

                var family: String? = null
                var locale: String? = null

                for (qualifier in language.qualifiers) {
                    when (qualifier) {
                        is LanguageQualifier -> {
                            family = qualifier.language
                        }
                        is RegionQualifier -> {
                            locale = qualifier.region
                        }
                    }
                }

                checkNotNull(family)

                val readable_name: String = getString(getResourceEnvironment(family, locale), Res.string.language_name)
                return@mapNotNull Language(family, locale, readable_name)
            }
    }
    return available_languages!!
}

private fun List<Language>.getBestMatch(identifier: String): Language? {
    val split: List<String> = identifier.split('-', limit = 2)
    check(split.isNotEmpty()) { split }

    for (language in this) {
        if (language.family == split.first() && language.locale == split.getOrNull(1)) {
            return language
        }
    }

    return firstOrNull { it.family == split.first() }
}

fun getResourceEnvironment(language_family: String, language_region: String?): ResourceEnvironment {
    val system_environment: ResourceEnvironment = getSystemResourceEnvironment()
    return ResourceEnvironment(
        language = LanguageQualifier(language_family),
        region = RegionQualifier(language_region ?: ""),
        theme = system_environment.theme,
        density = system_environment.density
    )
}

fun Language.getResourceEnvironment(): ResourceEnvironment =
    getResourceEnvironment(family, locale)

@Composable
fun rememberStringResourceByKey(key: String): StringResource {
    return remember(key) {
        Res.allStringResources[key] ?: throw RuntimeException("String resource with key '$key' not found")
    }
}

suspend fun getStringTODO(string: String): String = "TODO($string)"

@Composable
fun stringResourceTODO(string: String): String = "TODO($string)"
