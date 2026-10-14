package com.lexicon.interactors.numbers

enum class NumberKind { CARDINAL, ORDINAL, ENUMERATION }

enum class NumberRule {
    UNITS,
    TEENS,
    TENS_TO_FORTY,
    TENS_FROM_FIFTY,
    TENS_AND_UNITS,
    HUNDREDS,
    MILLION,
    MILLIONS_FEW,
    MILLIONS_MANY,
    THOUSAND,
    THOUSANDS_FEW,
    THOUSANDS_MANY,
    ORDINAL_UNITS,
    ORDINAL_TEENS,
    ORDINAL_TENS,
    ORDINAL_TENS_AND_UNITS,
    ORDINAL_HUNDREDS,
    ORDINAL_THOUSAND,
    ORDINAL_LAST_PART,
    ENUMERATION,
}

object PolishNumbers {
    const val MAX: Long = 999_999_999L

    private const val GROUPED_FROM = 10_000L
    private const val THOUSAND = 1_000
    private const val MILLION = 1_000_000
    private const val ENUMERATED_UP_TO = 10L
    private const val ENUMERATION_WORD = "po"

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

    private val ordinalUnits = listOf(
        "", "pierwszy", "drugi", "trzeci", "czwarty", "piąty", "szósty", "siódmy", "ósmy", "dziewiąty",
    )

    private val ordinalTeens = listOf(
        "dziesiąty", "jedenasty", "dwunasty", "trzynasty", "czternasty", "piętnasty", "szesnasty", "siedemnasty",
        "osiemnasty", "dziewiętnasty",
    )

    private val ordinalTens = listOf(
        "", "", "dwudziesty", "trzydziesty", "czterdziesty", "pięćdziesiąty", "sześćdziesiąty", "siedemdziesiąty",
        "osiemdziesiąty", "dziewięćdziesiąty",
    )

    private val ordinalHundreds = listOf(
        "", "setny", "dwusetny", "trzechsetny", "czterechsetny", "pięćsetny", "sześćsetny", "siedemsetny", "osiemsetny",
        "dziewięćsetny",
    )

    private const val THOUSANDTH = "tysięczny"

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
        "pierwszy" to "ˈpjɛrfʂɨ",
        "drugi" to "ˈdrugi",
        "trzeci" to "ˈtʂɛt͡ɕi",
        "czwarty" to "ˈt͡ʂfartɨ",
        "piąty" to "ˈpjɔntɨ",
        "szósty" to "ˈʂustɨ",
        "siódmy" to "ˈɕudmɨ",
        "ósmy" to "ˈusmɨ",
        "dziewiąty" to "d͡ʑɛˈvjɔntɨ",
        "dziesiąty" to "d͡ʑɛˈɕɔntɨ",
        "jedenasty" to "jɛdɛˈnastɨ",
        "dwunasty" to "dvuˈnastɨ",
        "trzynasty" to "tʂɨˈnastɨ",
        "czternasty" to "t͡ʂtɛrˈnastɨ",
        "piętnasty" to "pjɛtˈnastɨ",
        "szesnasty" to "ʂɛsˈnastɨ",
        "siedemnasty" to "ɕɛdɛmˈnastɨ",
        "osiemnasty" to "ɔɕɛmˈnastɨ",
        "dziewiętnasty" to "d͡ʑɛvjɛtˈnastɨ",
        "dwudziesty" to "dvuˈd͡ʑɛstɨ",
        "trzydziesty" to "tʂɨˈd͡ʑɛstɨ",
        "czterdziesty" to "t͡ʂtɛrˈd͡ʑɛstɨ",
        "pięćdziesiąty" to "pjɛɲd͡ʑɛˈɕɔntɨ",
        "sześćdziesiąty" to "ʂɛʑd͡ʑɛˈɕɔntɨ",
        "siedemdziesiąty" to "ɕɛdɛmd͡ʑɛˈɕɔntɨ",
        "osiemdziesiąty" to "ɔɕɛmd͡ʑɛˈɕɔntɨ",
        "dziewięćdziesiąty" to "d͡ʑɛvjɛɲd͡ʑɛˈɕɔntɨ",
        "setny" to "ˈsɛtnɨ",
        "dwusetny" to "dvuˈsɛtnɨ",
        "trzechsetny" to "tʂɛxˈsɛtnɨ",
        "czterechsetny" to "t͡ʂtɛrɛxˈsɛtnɨ",
        "pięćsetny" to "pjɛɲt͡ɕˈsɛtnɨ",
        "sześćsetny" to "ʂɛɕt͡ɕˈsɛtnɨ",
        "siedemsetny" to "ɕɛdɛmˈsɛtnɨ",
        "osiemsetny" to "ɔɕɛmˈsɛtnɨ",
        "dziewięćsetny" to "d͡ʑɛvjɛɲt͡ɕˈsɛtnɨ",
        "tysięczny" to "tɨˈɕɛnt͡ʂnɨ",
        "po" to "pɔ",
        "pierwsze" to "ˈpjɛrfʂɛ",
        "drugie" to "ˈdrugʲɛ",
        "trzecie" to "ˈtʂɛt͡ɕɛ",
        "czwarte" to "ˈt͡ʂfartɛ",
        "piąte" to "ˈpjɔntɛ",
        "szóste" to "ˈʂustɛ",
        "siódme" to "ˈɕudmɛ",
        "ósme" to "ˈusmɛ",
        "dziewiąte" to "d͡ʑɛˈvjɔntɛ",
        "dziesiąte" to "d͡ʑɛˈɕɔntɛ",
    )

    fun hasForm(
        value: Long,
        kind: NumberKind,
    ): Boolean =
        when (kind) {
            NumberKind.CARDINAL -> value in 0..MAX
            NumberKind.ORDINAL -> value in 1..MAX && (value % THOUSAND != 0L || value == THOUSAND.toLong())
            NumberKind.ENUMERATION -> value in 1..ENUMERATED_UP_TO
        }

    fun words(
        value: Long,
        kind: NumberKind = NumberKind.CARDINAL,
    ): String = spell(value, kind, explicitOne = false)

    fun answers(
        value: Long,
        kind: NumberKind = NumberKind.CARDINAL,
    ): List<String> = listOf(words(value, kind), spell(value, kind, explicitOne = true)).distinct()

    fun transcription(
        value: Long,
        kind: NumberKind = NumberKind.CARDINAL,
    ): String = words(value, kind).split(' ').joinToString(" ") { ipa.getValue(it) }

    fun rules(
        value: Long,
        kind: NumberKind = NumberKind.CARDINAL,
    ): List<NumberRule> {
        require(hasForm(value, kind)) { "$value has no ${kind.name.lowercase()} form here" }
        if (kind == NumberKind.ENUMERATION) return listOf(NumberRule.ENUMERATION)
        if (kind == NumberKind.ORDINAL) return ordinalRules(value)
        if (value == 0L) return listOf(NumberRule.UNITS)

        val rules = linkedSetOf<NumberRule>()
        groupRules(value, rules)
        rules += belowThousandRules((value % THOUSAND).toInt())
        return rules.toList()
    }

    private fun ordinalRules(value: Long): List<NumberRule> {
        if (value == THOUSAND.toLong()) return listOf(NumberRule.ORDINAL_THOUSAND)

        val rules = linkedSetOf<NumberRule>()
        if (value > THOUSAND) rules += NumberRule.ORDINAL_LAST_PART
        groupRules(value, rules)
        rules += ordinalBelowThousandRules((value % THOUSAND).toInt())
        return rules.toList()
    }

    fun digits(
        value: Long,
        kind: NumberKind = NumberKind.CARDINAL,
    ): String =
        grouped(value) +
            when (kind) {
                NumberKind.CARDINAL -> ""
                NumberKind.ORDINAL -> "."
                NumberKind.ENUMERATION -> ")"
            }

    private fun grouped(value: Long): String =
        if (value < GROUPED_FROM) {
            value.toString()
        } else {
            value.toString().reversed().chunked(3).joinToString(" ").reversed()
        }

    private fun spell(
        value: Long,
        kind: NumberKind,
        explicitOne: Boolean,
    ): String {
        require(hasForm(value, kind)) { "$value has no ${kind.name.lowercase()} form here" }
        if (kind == NumberKind.ENUMERATION) return "$ENUMERATION_WORD ${neuter(ordinalBelowThousand(value.toInt()))}"
        if (kind == NumberKind.ORDINAL && value == THOUSAND.toLong()) return THOUSANDTH
        if (value == 0L) return units.first()

        val millions = (value / MILLION).toInt()
        val thousands = (value / THOUSAND % THOUSAND).toInt()
        val rest = (value % THOUSAND).toInt()
        val tail = when {
            rest == 0 -> null
            kind == NumberKind.ORDINAL -> ordinalBelowThousand(rest)
            else -> belowThousand(rest)
        }

        return listOfNotNull(
            group(millions, millionForms, explicitOne),
            group(thousands, thousandForms, explicitOne),
            tail,
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

    private fun groupRules(
        value: Long,
        into: MutableSet<NumberRule>,
    ) {
        groupRules((value / MILLION).toInt(), NumberRule.MILLION, NumberRule.MILLIONS_FEW, NumberRule.MILLIONS_MANY, into)
        groupRules((value / THOUSAND % THOUSAND).toInt(), NumberRule.THOUSAND, NumberRule.THOUSANDS_FEW, NumberRule.THOUSANDS_MANY, into)
    }

    private fun groupRules(
        count: Int,
        one: NumberRule,
        few: NumberRule,
        many: NumberRule,
        into: MutableSet<NumberRule>,
    ) {
        when {
            count == 0 -> return
            count == 1 -> into += one
            else -> {
                into += if (takesFew(count)) few else many
                into += belowThousandRules(count)
            }
        }
    }

    private fun belowThousandRules(value: Int): List<NumberRule> {
        val rest = value % 100
        return listOfNotNull(
            NumberRule.HUNDREDS.takeIf { value >= 100 },
            when {
                rest == 0 -> null
                rest <= 10 -> NumberRule.UNITS
                rest < 20 -> NumberRule.TEENS
                rest < 50 -> NumberRule.TENS_TO_FORTY
                else -> NumberRule.TENS_FROM_FIFTY
            },
            NumberRule.TENS_AND_UNITS.takeIf { rest > 20 && rest % 10 != 0 },
        )
    }

    private fun ordinalBelowThousandRules(value: Int): List<NumberRule> {
        val rest = value % 100
        return listOfNotNull(
            NumberRule.ORDINAL_HUNDREDS.takeIf { value >= 100 },
            when {
                rest == 0 -> null
                rest <= 10 -> NumberRule.ORDINAL_UNITS
                rest < 20 -> NumberRule.ORDINAL_TEENS
                else -> NumberRule.ORDINAL_TENS
            },
            NumberRule.ORDINAL_TENS_AND_UNITS.takeIf { rest > 20 && rest % 10 != 0 },
        )
    }

    private fun takesFew(count: Int): Boolean = count % 10 in 2..4 && count % 100 !in 12..14

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

    private fun ordinalBelowThousand(value: Int): String {
        val rest = value % 100
        if (rest == 0) return ordinalHundreds[value / 100]
        val tail = when {
            rest < 10 -> ordinalUnits[rest]
            rest < 20 -> ordinalTeens[rest - 10]
            rest % 10 == 0 -> ordinalTens[rest / 10]
            else -> "${ordinalTens[rest / 10]} ${ordinalUnits[rest % 10]}"
        }
        return listOfNotNull(hundreds[value / 100].ifEmpty { null }, tail).joinToString(" ")
    }

    private fun neuter(ordinal: String): String = ordinal.dropLast(1) + if (ordinal.endsWith('i')) "ie" else "e"

    private class GroupForms(
        val one: String,
        val few: String,
        val many: String,
    ) {
        fun of(count: Int): String = if (takesFew(count)) few else many
    }
}
