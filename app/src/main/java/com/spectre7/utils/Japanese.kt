package com.spectre7.utils

import android.util.Log
import java.lang.Character.UnicodeBlock.*

enum class JpCharType { KANJI, HIRA, KATA, OTHER }

fun Char.isKanji(): Boolean = Character.UnicodeBlock.of(this) == CJK_UNIFIED_IDEOGRAPHS
fun Char.isHiragana(): Boolean = Character.UnicodeBlock.of(this) == HIRAGANA
fun Char.isKatakana(): Boolean = Character.UnicodeBlock.of(this) == KATAKANA

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