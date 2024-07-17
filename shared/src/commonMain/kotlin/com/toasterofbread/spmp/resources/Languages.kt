package com.toasterofbread.spmp.resources

import com.toasterofbread.spmp.platform.AppContext
import java.io.InputStream
import java.util.MissingResourceException

const val DEFAULT_LANGUAGE = "en-GB"

object Languages {
    data class LanguageInfo(val code: String, val readable_name: String)

    fun loadAvailableLanugages(context: AppContext): List<LanguageInfo> {
        val languages: MutableList<LanguageInfo> = mutableListOf()

        iterateValuesDirectories(context) { language, path ->
            val stream: InputStream =
                try {
                    context.openResourceFile("$path/strings.xml")
                }
                catch (e: Throwable) {
                    if (e.javaClass != MissingResourceException::class.java) {
                        throw e
                    }
                    return@iterateValuesDirectories false
                }

            stream.reader().useLines { lines ->
                for (line in lines) {
                    val trimmed: String = line.trim()
                    if (!trimmed.startsWith("<string name=\"language_name\">")) {
                        continue
                    }

                    val file_language: String = trimmed.substring(29, trimmed.length - 9)
                    languages.add(LanguageInfo(language ?: DEFAULT_LANGUAGE, file_language))

                    break
                }
            }

            return@iterateValuesDirectories false
        }

        return languages
    }
}
