@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalResourceApi::class)

package com.toasterofbread.spmp.resources

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.language_name

private data class Language(val family: String, val region: String?)
private val DEFAULT_LANGUAGE: Language = Language("en", "GB")

//private fun getAvailableLanguages(): List<Language> =
//    Res.string.language_name.items.map { language ->
//        var family: String? = null
//        var locale: String? = null
//
//        for (qualifier in language.qualifiers) {
//            when (qualifier) {
//                is LanguageQualifier -> {
//                    family = qualifier.language
//                }
//                is RegionQualifier -> {
//                    locale = qualifier.region
//                }
//            }
//        }
//
//        return@map Language(family!!, locale)
//    }
//
//private fun List<Language>.getBestMatch(identifier: String): Language? {
//    val split: List<String> = identifier.split('-', limit = 2)
//    check(split.isNotEmpty()) { split }
//
//    for (language in this) {
//        if (language.family == split.first() && language.region == split.getOrNull(1)) {
//            return language
//        }
//    }
//
//    return firstOrNull { it.family == split.first() }
//}
//
//private fun getLanguage(identifier: String): Language =
//    getAvailableLanguages().getBestMatch(identifier) ?: DEFAULT_LANGUAGE
//
//private fun Language.getResourceEnvironment(): ResourceEnvironment {
//    val system_environment: ResourceEnvironment = getSystemResourceEnvironment()
//    return ResourceEnvironment(
//        language = LanguageQualifier(family),
//        region = RegionQualifier(region ?: ""),
//        theme = system_environment.theme,
//        density = system_environment.density
//    )
//}
