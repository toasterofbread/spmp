package com.toasterofbread.spmp.resources

import com.toasterofbread.spmp.platform.AppContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.MissingResourceException

private const val DEFAULT_LANGUAGE = "en-GB"

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

            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(stream.reader())

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    parser.next()
                    continue
                }

                val key = parser.getAttributeValue(null, "name")
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
