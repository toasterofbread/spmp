package com.toasterofbread.spmp.youtubeapi.lyrics

//import PlatformIO
//import com.github.wanasit.kotori.Dictionary
//import com.github.wanasit.kotori.TermDictionary
//import com.github.wanasit.kotori.TermEntry
//import com.github.wanasit.kotori.Tokenizer
//import com.github.wanasit.kotori.mecab.MeCabConnectionCost
//import com.github.wanasit.kotori.mecab.MeCabDictionary
//import com.github.wanasit.kotori.mecab.MeCabLikeTermFeatures
//import com.github.wanasit.kotori.mecab.MeCabTermFeatures
//import com.github.wanasit.kotori.mecab.MeCabUnknownTermExtractionStrategy
//import com.github.wanasit.kotori.optimized.PlainTermDictionary
//import io.ktor.utils.io.charsets.Charset
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import spmp.shared.generated.resources.Res
//
//internal typealias Kotori = Tokenizer<MeCabTermFeatures>
//
//internal suspend fun loadKotori(): Kotori = withContext(Dispatchers.PlatformIO) {
//    val charset: Charset = MeCabDictionary.DEFAULT_CHARSET
//
//    val term_dictionary: TermDictionary<MeCabTermFeatures> = loadTermDictionary(charset)
//    val term_connection = MeCabConnectionCost.readFromByteArray(
//        getFile(MeCabDictionary.FILE_NAME_CONNECTION_COST),
//        charset = charset
//    )
//    val unknown_term_dictionary =
//        MeCabUnknownTermExtractionStrategy.readFromByteArrays(
//            getFile(MeCabDictionary.FILE_NAME_UNKNOWN_ENTRIES),
//            getFile(MeCabDictionary.FILE_NAME_CHARACTER_DEFINITION),
//            charset
//        )
//
//    val dictionary: Dictionary<MeCabTermFeatures> =
//        Dictionary(
//            term_dictionary,
//            term_connection,
//            unknown_term_dictionary
//        )
//
//    return@withContext Tokenizer.create(dictionary)
//}
//
//suspend fun loadTermDictionary(charset: Charset): TermDictionary<MeCabTermFeatures> = withContext(Dispatchers.PlatformIO) {
//    val entries: List<TermEntry<MeCabLikeTermFeatures>> =
//        FILE_LIST
//            .filter { it.endsWith(".csv") }
//            .sorted()
//            .flatMap { MeCabTermFeatures.readTermEntriesFromByteArray(getFile(it), charset) }
//
//    return@withContext PlainTermDictionary(entries.toTypedArray())
//}
//
//private suspend fun getFile(path: String): ByteArray =
//    Res.readBytes("files/mecab-ipadic-2.7.0-20070801/$path")
//
//private val FILE_LIST: List<String> =
//    listOf(
//        "aclocal.m4",
//        "Adj.csv",
//        "Adnominal.csv",
//        "Adverb.csv",
//        "AUTHORS",
//        "Auxil.csv",
//        "ChangeLog",
//        "char.def",
//        "config.guess",
//        "config.sub",
//        "configure",
//        "configure.in",
//        "Conjunction.csv",
//        "COPYING",
//        "dicrc",
//        "feature.def",
//        "Filler.csv",
//        "INSTALL",
//        "install-sh",
//        "Interjection.csv",
//        "left-id.def",
//        "Makefile.am",
//        "Makefile.in",
//        "matrix.def",
//        "missing",
//        "mkinstalldirs",
//        "NEWS",
//        "Noun.adjv.csv",
//        "Noun.adverbal.csv",
//        "Noun.csv",
//        "Noun.demonst.csv",
//        "Noun.nai.csv",
//        "Noun.name.csv",
//        "Noun.number.csv",
//        "Noun.org.csv",
//        "Noun.others.csv",
//        "Noun.place.csv",
//        "Noun.proper.csv",
//        "Noun.verbal.csv",
//        "Others.csv",
//        "pos-id.def",
//        "Postp-col.csv",
//        "Postp.csv",
//        "Prefix.csv",
//        "README",
//        "RESULT",
//        "rewrite.def",
//        "right-id.def",
//        "Suffix.csv",
//        "Symbol.csv",
//        "unk.def",
//        "Verb.csv"
//    )
