package com.toasterofbread.spmp.api.lyrics

import com.toasterofbread.spmp.model.SongLyrics
import com.atilika.kuromoji.ipadic.Tokenizer
import com.toasterofbread.utils.hasKanjiAndHiragana
import com.toasterofbread.utils.isJP
import com.toasterofbread.utils.isKanji

private fun trimOkurigana(term: SongLyrics.Term.Text): List<SongLyrics.Term.Text> {
    if (term.furi == null || !term.text.hasKanjiAndHiragana()) {
        return listOf(term)
    }

    var trim_start: Int = 0
    for (i in 0 until term.furi!!.length) {
        if (term.text[i].isKanji() || term.text[i] != term.furi!![i]) {
            trim_start = i
            break
        }
    }

    var trim_end: Int = 0
    for (i in 1 .. term.furi!!.length) {
        if (term.text[term.text.length - i].isKanji() || term.text[term.text.length - i] != term.furi!![term.furi!!.length - i]) {
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
            term.furi!!.substring(trim_start)
        )
    }
    else {
        last_term = term
    }

    if (trim_end > 0) {
        terms.add(
            SongLyrics.Term.Text(
                last_term.text.substring(0, last_term.text.length - trim_end),
                last_term.furi!!.substring(0, last_term.furi!!.length - trim_end)
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
            terms_to_process.clear()
            ret.add(term)
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
    val line_range = terms.first().line_range!!
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
        var start: Long = Long.MAX_VALUE
        var end: Long = Long.MIN_VALUE

        while (text.length < token_base.length) {
            val term = terms[current_term]
            val subterm = term.subterms.single()
            start = minOf(start, term.start!!)
            end = maxOf(end, term.end!!)

            val needed = token_base.length - text.length
            if (needed < subterm.text.length - term_head) {
                text += subterm.text.substring(term_head, term_head + needed)
                term_head += needed
            }
            else {
                text += subterm.text
                term_head = 0
                current_term++
            }
        }

        val term = SongLyrics.Term(trimOkurigana(SongLyrics.Term.Text(text, token.reading)), line_index, start, end)
        term.line_range = line_range
        ret.add(term)
    }

    return ret
}
