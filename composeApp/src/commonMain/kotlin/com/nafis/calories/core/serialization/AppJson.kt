package com.nafis.calories.core.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Matches the Gson behaviour of the original app: nulls are omitted, defaults are written,
 * and JSON stored by Gson (which skips null fields) still decodes.
 */
@OptIn(ExperimentalSerializationApi::class)
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
