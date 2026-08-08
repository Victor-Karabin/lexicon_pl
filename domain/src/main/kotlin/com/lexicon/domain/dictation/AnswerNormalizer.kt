package com.lexicon.domain.dictation

import java.util.Locale
import javax.inject.Inject

class AnswerNormalizer
    @Inject
    constructor() {
        fun matches(
            expected: String,
            submitted: String,
        ): Boolean = normalize(expected) == normalize(submitted)

        private fun normalize(text: String): String = text.trim().lowercase(Locale.ROOT)
    }
