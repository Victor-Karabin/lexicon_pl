package com.lexicon.interactors.numbers

object PolishNumbers {
    const val MAX: Long = 999_999_999L

    private const val GROUPED_FROM = 10_000L
    private const val THOUSAND = 1_000
    private const val MILLION = 1_000_000

    private val units = listOf("zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć")

    private val teens = listOf(
        "dziesięć", "jedenaście", "dwanaście", "trzynaście", "czternaście", "piętnaście", "szesnaście", "siedemnaście",
        "osiemnaście", "dziewiętnaście",
    )

    private val tens = listOf(
        "", "", "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt", "sześćdziesiąt", "siedemdziesiąt",
        "osiemdziesiąt", "dziewięćdziesiąt",
    )

    private val hundreds = listOf(
        "", "sto", "dwieście", "trzysta", "czterysta", "pięćset", "sześćset", "siedemset", "osiemset", "dziewięćset",
    )

    private val thousandForms = GroupForms(one = "tysiąc", few = "tysiące", many = "tysięcy")

    private val millionForms = GroupForms(one = "milion", few = "miliony", many = "milionów")

    private val ipa = mapOf(
        "zero" to "ˈzɛrɔ",
        "jeden" to "ˈjɛdɛn",
        "dwa" to "dva",
        "trzy" to "tʂɨ",
        "cztery" to "ˈt͡ʂtɛrɨ",
        "pięć" to "pjɛɲt͡ɕ",
        "sześć" to "ʂɛɕt͡ɕ",
        "siedem" to "ˈɕɛdɛm",
        "osiem" to "ˈɔɕɛm",
        "dziewięć" to "ˈd͡ʑɛvjɛɲt͡ɕ",
        "dziesięć" to "ˈd͡ʑɛɕɛɲt͡ɕ",
        "jedenaście" to "jɛdɛˈnaɕt͡ɕɛ",
        "dwanaście" to "dvaˈnaɕt͡ɕɛ",
        "trzynaście" to "tʂɨˈnaɕt͡ɕɛ",
        "czternaście" to "t͡ʂtɛrˈnaɕt͡ɕɛ",
        "piętnaście" to "pjɛtˈnaɕt͡ɕɛ",
        "szesnaście" to "ʂɛsˈnaɕt͡ɕɛ",
        "siedemnaście" to "ɕɛdɛmˈnaɕt͡ɕɛ",
        "osiemnaście" to "ɔɕɛmˈnaɕt͡ɕɛ",
        "dziewiętnaście" to "d͡ʑɛvjɛtˈnaɕt͡ɕɛ",
        "dwadzieścia" to "dvaˈd͡ʑɛɕt͡ɕa",
        "trzydzieści" to "tʂɨˈd͡ʑɛɕt͡ɕi",
        "czterdzieści" to "t͡ʂtɛrˈd͡ʑɛɕt͡ɕi",
        "pięćdziesiąt" to "pjɛɲˈd͡ʑɛɕɔnt",
        "sześćdziesiąt" to "ʂɛʑˈd͡ʑɛɕɔnt",
        "siedemdziesiąt" to "ɕɛdɛmˈd͡ʑɛɕɔnt",
        "osiemdziesiąt" to "ɔɕɛmˈd͡ʑɛɕɔnt",
        "dziewięćdziesiąt" to "d͡ʑɛvjɛɲˈd͡ʑɛɕɔnt",
        "sto" to "stɔ",
        "dwieście" to "ˈdvjɛɕt͡ɕɛ",
        "trzysta" to "ˈtʂɨsta",
        "czterysta" to "t͡ʂtɛˈrɨsta",
        "pięćset" to "ˈpjɛɲt͡ɕsɛt",
        "sześćset" to "ˈʂɛɕt͡ɕsɛt",
        "siedemset" to "ɕɛˈdɛmsɛt",
        "osiemset" to "ɔˈɕɛmsɛt",
        "dziewięćset" to "d͡ʑɛˈvjɛɲt͡ɕsɛt",
        "tysiąc" to "ˈtɨɕɔnt͡s",
        "tysiące" to "tɨˈɕɔnt͡sɛ",
        "tysięcy" to "tɨˈɕɛnt͡sɨ",
        "milion" to "ˈmiljɔn",
        "miliony" to "miˈljɔnɨ",
        "milionów" to "miˈljɔnuf",
    )

    fun words(value: Long): String = spell(value, explicitOne = false)

    fun answers(value: Long): List<String> = listOf(words(value), spell(value, explicitOne = true)).distinct()

    fun transcription(value: Long): String = words(value).split(' ').joinToString(" ") { ipa.getValue(it) }

    fun digits(value: Long): String =
        if (value < GROUPED_FROM) {
            value.toString()
        } else {
            value.toString().reversed().chunked(3).joinToString(" ").reversed()
        }

    private fun spell(
        value: Long,
        explicitOne: Boolean,
    ): String {
        require(value in 0..MAX) { "$value is outside 0..$MAX" }
        if (value == 0L) return units.first()

        val millions = (value / MILLION).toInt()
        val thousands = (value / THOUSAND % THOUSAND).toInt()
        val rest = (value % THOUSAND).toInt()

        return listOfNotNull(
            group(millions, millionForms, explicitOne),
            group(thousands, thousandForms, explicitOne),
            rest.takeIf { it > 0 }?.let(::belowThousand),
        ).joinToString(" ")
    }

    private fun group(
        count: Int,
        forms: GroupForms,
        explicitOne: Boolean,
    ): String? =
        when (count) {
            0 -> null
            1 -> if (explicitOne) "${units[1]} ${forms.one}" else forms.one
            else -> "${belowThousand(count)} ${forms.of(count)}"
        }

    private fun belowThousand(value: Int): String {
        val rest = value % 100
        val tail = when {
            rest == 0 -> null
            rest < 10 -> units[rest]
            rest < 20 -> teens[rest - 10]
            rest % 10 == 0 -> tens[rest / 10]
            else -> "${tens[rest / 10]} ${units[rest % 10]}"
        }
        return listOfNotNull(hundreds[value / 100].ifEmpty { null }, tail).joinToString(" ")
    }

    private class GroupForms(
        val one: String,
        val few: String,
        val many: String,
    ) {
        fun of(count: Int): String = if (count % 10 in 2..4 && count % 100 !in 12..14) few else many
    }
}
