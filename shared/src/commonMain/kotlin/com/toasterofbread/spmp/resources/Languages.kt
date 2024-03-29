package com.toasterofbread.spmp.resources

import com.toasterofbread.spmp.platform.AppContext
import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser
import java.io.InputStream
import java.util.MissingResourceException

const val DEFAULT_LANGUAGE = "en-GB"

object Languages {
    data class LanguageInfo(val code: String, val readable_name: String)

    fun loadAvailableLanugages(context: AppContext): List<LanguageInfo> {
        val languages: MutableList<LanguageInfo> = mutableListOf()

        iterateValuesDirectories(context) { language, path ->
            val stream: InputStream = try {
                context.openResourceFile("$path/strings.xml")
            }
            catch (e: Throwable) {
                if (e.javaClass != MissingResourceException::class.java) {
                    throw e
                }
                return@iterateValuesDirectories false
            }

            val string = stream.reader().readText()
            stream.close()

            val parser = MiniXmlPullParser(string.iterator())

            while (parser.eventType != EventType.END_DOCUMENT) {
                if (parser.eventType != EventType.START_TAG) {
                    parser.next()
                    continue
                }

                val key = parser.getAttributeValue("", "name")
                if (parser.name == "string" && key == "language_name") {
                    languages.add(LanguageInfo(language ?: DEFAULT_LANGUAGE, parser.nextText()))
                    break
                }

                parser.next()
            }

            stream.close()
            false
        }

        return languages
    }
}
