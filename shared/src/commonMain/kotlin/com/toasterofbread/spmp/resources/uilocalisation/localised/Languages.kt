package com.toasterofbread.spmp.resources.uilocalisation.localised

class Languages(getLanguage: (String) -> Int) {
    val en = getLanguage("en")
    val ja = getLanguage("ja")
}
