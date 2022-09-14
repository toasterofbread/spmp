package com.spectre7.utils

import android.util.Log
import java.lang.Character.UnicodeBlock.*

enum class JpCharType { KANJI, HIRA, KATA, OTHER }

fun Char.isKanji(): Boolean = Character.UnicodeBlock.of(this) == CJK_UNIFIED_IDEOGRAPHS
fun Char.isHiragana(): Boolean = Character.UnicodeBlock.of(this) == HIRAGANA
fun Char.isKatakana(): Boolean = Character.UnicodeBlock.of(this) == KATAKANA

fun Char.isKatakana(): Boolean {
    return isHalfWidthKatakana() || isFullWidthKatakana()
}

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
    return this;
}

fun Char.jpType(): JpCharType {
    return when(Character.UnicodeBlock.of(this)) {
        CJK_UNIFIED_IDEOGRAPHS -> JpCharType.KANJI
        HIRAGANA -> JpCharType.HIRA
        KATAKANA -> JpCharType.KATA
        else -> JpCharType.OTHER
    }
}

fun String.hasKanjiAndHiragana(): Boolean {
    var has_kanji = false
    var has_hiragana = false
    for (char in this) {
        when (char.jpType()) {
            JpCharType.KANJI -> {
                if (has_hiragana)
                    return true
                has_kanji = true
            }
            JpCharType.HIRA -> {
                if (has_kanji)
                    return true
                has_hiragana = true
            }
            else -> {}
        }
    }
    return false
}

fun String.toHiragana(): String {
    val ret = StringBuilder()
    for (char in this) {
        ret.append(char.toHiragana())
    }
    return ret.toString()
}
