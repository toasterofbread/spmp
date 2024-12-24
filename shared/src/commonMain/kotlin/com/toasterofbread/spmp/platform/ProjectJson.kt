package com.toasterofbread.spmp.platform

import kotlinx.serialization.json.Json

object ProjectJson {
    val instance: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            useArrayPolymorphism = true
        }
}
