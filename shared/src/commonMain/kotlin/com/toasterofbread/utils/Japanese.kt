package com.toasterofbread.utils

import java.lang.Character.UnicodeBlock.*

fun Char.isJP(): Boolean = isKanji() || isHiragana() || isKatakana()
fun Char.isKanji(): Boolean = of(this) == CJK_UNIFIED_IDEOGRAPHS
fun Char.isHiragana(): Boolean = of(this) == HIRAGANA
fun Char.isKatakana(): Boolean = of(this) == KATAKANA

fun Char.isHalfWidthKatakana(): Boolean {
    return ('\uff66' <= this) && (this <= '\uff9d')
}

fun Char.isFullWidthKatakana(): Boolean {
    return ('\u30a1' <= this) && (this <= '\u30fe')
}

fun Char.toHiragana(): Char {
    if (isFullWidthKatakana()) {
        return (this - 0x60)
    }
    else if (isHalfWidthKatakana()) {
        return (this - 0xcf25)
    }
    return this
}

fun String.toHiragana(): String {
    val ret = StringBuilder()
    for (char in this) {
        ret.append(char.toHiragana())
    }
    return ret.toString()
}

fun String.hasKanjiAndHiragana(): Boolean {
    var has_kanji = false
    var has_hiragana = false
    for (char in this) {
        when (of(char)) {
            CJK_UNIFIED_IDEOGRAPHS -> {
                if (has_hiragana)
                    return true
                has_kanji = true
            }
            HIRAGANA -> {
                if (has_kanji)
                    return true
                has_hiragana = true
            }
            else -> {}
        }
    }
    return false
}
