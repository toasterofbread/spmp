package com.toasterofbread.spmp.resources

import com.toasterofbread.spmp.platform.PlatformContext
import kotlinx.coroutines.runBlocking
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.*

private lateinit var strings: Map<String, String>
private lateinit var string_arrays: Map<String, List<String>>

@Suppress("BlockingMethodInNonBlockingContext")
fun initResources(language: String, context: PlatformContext) {
    fun formatText(text: String): String = text.replace("\\\"", "\"").replace("\\'", "'")

    runBlocking {
        val strs = mutableMapOf<String, String>()
        val str_arrays = mutableMapOf<String, List<String>>()

        suspend fun loadFile(path: String) {
            val stream: InputStream
            try {
                stream = context.openResourceFile(path)
            }
            catch (e: Throwable) {
                if (e.javaClass != MissingResourceException::class.java) {
                    throw e
                }
                return
            }

            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(stream.reader())

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    parser.next()
                    continue
                }

                val key = parser.getAttributeValue(null, "name")
                when (parser.name) {
                    "string" -> {
                        strs[key] = formatText(parser.nextText())
                    }
                    "string-array" -> {
                        val array = mutableListOf<String>()

                        parser.nextTag()
                        while (parser.name == "item") {
                            array.add(formatText(parser.nextText()))
                            parser.nextTag()
                        }

                        str_arrays[key] = array
                    }
                    "resources" -> {}
                    else -> throw NotImplementedError(parser.name)
                }

                parser.next()
            }

            stream.close()

            println("Loaded strings.xml at $path")
        }

        var language_best_match: String? = null
        val language_family = language.split('-', limit = 2).first()

        iterateValuesDirectories(context) { file_language, path ->
            if (file_language == null) {
                return@iterateValuesDirectories false
            }

            if (file_language == language) {
                language_best_match = path
                return@iterateValuesDirectories true
            }

            if (file_language.split('-', limit = 2).first() == language_family) {
                language_best_match = path
            }

            return@iterateValuesDirectories false
        }

        loadFile("values/strings.xml")
        if (language_best_match != null) {
            loadFile("$language_best_match/strings.xml")
        }
        loadFile("values/ytm.xml")

        strings = strs
        string_arrays = str_arrays
    }
}

fun getString(key: String): String = strings[key] ?: throw NotImplementedError(key)
fun getStringOrNull(key: String): String? = strings[key]
fun getStringTODO(temp_string: String): String = "$temp_string // TODO" // String to be localised
fun getStringArray(key: String): List<String> = string_arrays[key] ?: throw NotImplementedError(key)

inline fun iterateValuesDirectories(context: PlatformContext, action: (language: String?, path: String) -> Boolean) {
    for (file in context.listResourceFiles("") ?: emptyList()) {
        if (!file.startsWith("values")) {
            continue
        }

        val file_language = if (file.length == 6) null else file.substring(7)
        if (action(file_language, file)) {
            break
        }
    }
}
