package com.toasterofbread.spmp.resources

import SpMp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getUiLanguage
import kotlinx.coroutines.runBlocking
import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser
import java.io.InputStream
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private var _strings: Map<String, String>? = null
private var _string_arrays: Map<String, List<String>>? = null

private val strings: Map<String, String> get() = _strings!!
private val string_arrays: Map<String, List<String>> get() = _string_arrays!!

private val resource_load_lock = ReentrantLock()

@Suppress("BlockingMethodInNonBlockingContext")
fun initResources(language: String, context: AppContext) {
    fun formatText(text: String): String = text.replace("\\\"", "\"").replace("\\'", "'")

    resource_load_lock.withLock {
        runBlocking {
            val strs = mutableMapOf<String, String>()
            val str_arrays = mutableMapOf<String, List<String>>()

            fun loadFile(path: String) {
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

                println("Loading strings.xml at $path")

                val string: String = stream.reader().readText()
                stream.close()

                val parser = MiniXmlPullParser(string.iterator())

                while (parser.eventType != EventType.END_DOCUMENT) {
                    if (parser.eventType != EventType.START_TAG) {
                        parser.next()
                        continue
                    }

                    val key = parser.getAttributeValue("", "name")
                    if (key != null) {
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

            _strings = strs
            _string_arrays = str_arrays
        }
    }
}

fun getString(key: String): String = strings[key] ?: throw NotImplementedError(key)
fun getStringOrNull(key: String): String? = _strings?.get(key)
fun getStringTODO(temp_string: String): String = "$temp_string // TODO" // String to be localised
fun getStringArray(key: String): List<String> = string_arrays[key] ?: throw NotImplementedError(key)

fun getStringSafe(key: String, context: AppContext): String {
    resource_load_lock.withLock {
        if (_strings == null) {
            initResources(context.getUiLanguage(), context)
        }
        return strings[key] ?: throw NotImplementedError(key)
    }
}

fun getStringArraySafe(key: String, context: AppContext): List<String> {
    resource_load_lock.withLock {
        if (_string_arrays == null) {
            initResources(context.getUiLanguage(), context)
        }
        return string_arrays[key] ?: throw NotImplementedError(key)
    }
}

inline fun iterateValuesDirectories(context: AppContext, action: (language: String?, path: String) -> Boolean) {
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
