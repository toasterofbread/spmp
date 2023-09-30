package com.toasterofbread.spmp.youtubeapi.lyrics

import com.atilika.kuromoji.ipadic.Tokenizer
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.utils.common.hasKanjiAndHiragana
import com.toasterofbread.utils.common.isHiragana
import com.toasterofbread.utils.common.isJP
import com.toasterofbread.utils.common.isKanji
import com.toasterofbread.utils.common.isKatakana
import java.nio.channels.ClosedByInterruptException

fun createTokeniser(): Tokenizer {
    try {
        return Tokenizer()
    }
    catch (e: RuntimeException) {
        if (e.cause is ClosedByInterruptException) {
            throw InterruptedException()
        }
        else {
            throw e
        }
    }
}

fun mergeAndFuriganiseTerms(tokeniser: Tokenizer, terms: List<SongLyrics.Term>): List<SongLyrics.Term> {
    if (terms.isEmpty()) {
        return emptyList()
    }

    val ret: MutableList<SongLyrics.Term> = mutableListOf()
    val terms_to_process: MutableList<SongLyrics.Term> = mutableListOf()

    for (term in terms) {
        val text = term.subterms.single().text
        if (text.any { it.isJP() }) {
            terms_to_process.add(term)
        }
        else {
            ret.addAll(_mergeAndFuriganiseTerms(tokeniser, terms_to_process))
            ret.add(term)
            terms_to_process.clear()
        }
    }

    ret.addAll(_mergeAndFuriganiseTerms(tokeniser, terms_to_process))

    return ret
}

private fun _mergeAndFuriganiseTerms(tokeniser: Tokenizer, terms: List<SongLyrics.Term>): List<SongLyrics.Term> {
    if (terms.isEmpty()) {
        return emptyList()
    }

    val ret: MutableList<SongLyrics.Term> = mutableListOf()
    val line_range = terms.first().line_range
    val line_index = terms.first().line_index

    var terms_text: String = ""
    for (term in terms) {
        terms_text += term.subterms.single().text
    }

    val tokens = tokeniser.tokenize(terms_text)

    var current_term: Int = 0
    var term_head: Int = 0

    for (token in tokens) {
        val token_base = token.surface

        var text: String = ""
        var start: Long? = null
        var end: Long? = null

        while (text.length < token_base.length) {
            val term = terms[current_term]
            val subterm = term.subterms.single()

            if (term.start != null && (start == null || term.start < start)) {
                start = term.start
            }
            if (term.end != null && (end == null || term.end > end)) {
                end = term.end
            }

            val needed = token_base.length - text.length
            if (needed < subterm.text.length - term_head) {
                text += subterm.text.substring(term_head, term_head + needed)
                term_head += needed
            }
            else {
                text += subterm.text.substring(term_head)
                term_head = 0
                current_term++
            }
        }

        val term = SongLyrics.Term(
            trimOkurigana(SongLyrics.Term.Text(text, token.reading))
                .flatMap {
                    removeHiraganaReadings(it)
                }
                .flatMap {
                    splitCombinedReading(it)
                },
            line_index,
            start,
            end
        )
        term.line_range = line_range
        ret.add(term)
    }

    return ret
}

private fun splitCombinedReading(term: SongLyrics.Term.Text): List<SongLyrics.Term.Text> {
    val reading = term.reading ?: return listOf(term)

    if (term.text.length != reading.length || !term.text.all { it.isKanji() }) {
        return listOf(term)
    }

    return term.text.mapIndexed { i, char ->
        SongLyrics.Term.Text(char.toString(), reading[i].toString())
    }
}

private fun removeHiraganaReadings(term: SongLyrics.Term.Text): List<SongLyrics.Term.Text> {
    val reading = term.reading ?: return listOf(term)

    val terms: MutableList<SongLyrics.Term.Text> = mutableListOf()
    var reading_head: Int = 0

    var kanji_section: String = ""
    var hira_section: String = ""

    fun checkHiraSection() {
        if (hira_section.isNotEmpty()) {
            val index = reading.indexOf(hira_section, reading_head)
            if (index != -1) {
                val second_index = term.text.indexOf(hira_section, index + 1)
                if (second_index == -1) {
                    terms.add(
                        SongLyrics.Term.Text(
                            kanji_section,
                            reading.substring(reading_head, index)
                        )
                    )

                    terms.add(
                        SongLyrics.Term.Text(
                            hira_section
                        )
                    )

                    reading_head += index + hira_section.length

                    hira_section = ""
                    kanji_section = ""
                }
            }
        }
    }

    for (char in term.text) {
        if (char.isHiragana() || char.isKatakana()) {
            hira_section += char
        }
        else {
            checkHiraSection()

            if (char.isKanji()) {
                kanji_section += char
            }
        }
    }

    checkHiraSection()

    if (kanji_section.isNotEmpty()) {
        terms.add(
            SongLyrics.Term.Text(
                kanji_section,
                reading.substring(reading_head)
            )
        )
    }

    return terms
}

private fun trimOkurigana(term: SongLyrics.Term.Text): List<SongLyrics.Term.Text> {
    if (term.reading == null || !term.text.hasKanjiAndHiragana()) {
        return listOf(term)
    }

    var trim_start: Int = 0
    for (i in 0 until term.reading!!.length) {
        if (term.text[i].isKanji() || term.text[i] != term.reading!![i]) {
            trim_start = i
            break
        }
    }

    var trim_end: Int = 0
    for (i in 1 .. term.reading!!.length) {
        if (term.text[term.text.length - i].isKanji() || term.text[term.text.length - i] != term.reading!![term.reading!!.length - i]) {
            trim_end = i - 1
            break
        }
    }

    val terms: MutableList<SongLyrics.Term.Text> = mutableListOf()
    val last_term: SongLyrics.Term.Text

    if (trim_start > 0) {
        terms.add(
            SongLyrics.Term.Text(
                term.text.substring(0, trim_start),
                null
            )
        )

        last_term = SongLyrics.Term.Text(
            term.text.substring(trim_start),
            term.reading!!.substring(trim_start)
        )
    }
    else {
        last_term = term
    }

    if (trim_end > 0) {
        terms.add(
            SongLyrics.Term.Text(
                last_term.text.substring(0, last_term.text.length - trim_end),
                last_term.reading!!.substring(0, last_term.reading!!.length - trim_end)
            )
        )
        terms.add(
            SongLyrics.Term.Text(
                last_term.text.takeLast(trim_end),
                null
            )
        )
    }
    else {
        terms.add(last_term)
    }

    return terms
}
