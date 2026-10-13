package com.lexicon.interactors.numbers

sealed interface NumberSelection {
    data class Valid(
        val numbers: List<Long>,
    ) : NumberSelection

    data object Empty : NumberSelection

    data class Unreadable(
        val part: String,
    ) : NumberSelection

    data object TooLarge : NumberSelection

    data object TooMany : NumberSelection

    companion object {
        const val MAX_COUNT = 100_000

        private val separators = Regex("""[,;\n]+""")
        private val digitGroupSpace = Regex("""(\d)[\s\u00A0]+(?=\d{3}(?!\d))""")
        private val single = Regex("""^\d+$""")
        private val range = Regex("""^(\d+)\s*[-–—]\s*(\d+)$""")

        fun parse(text: String): NumberSelection {
            val parts = text.split(separators).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return Empty

            val numbers = mutableSetOf<Long>()
            for (part in parts) {
                val compact = part.replace(digitGroupSpace, "$1")
                val bounds = range.find(compact)?.groupValues?.let { it[1] to it[2] }
                    ?: compact.takeIf { single.matches(it) }?.let { it to it }
                    ?: return Unreadable(part)

                val first = bounds.first.toLongOrNull() ?: return TooLarge
                val second = bounds.second.toLongOrNull() ?: return TooLarge
                val low = minOf(first, second)
                val high = maxOf(first, second)
                if (high > PolishNumbers.MAX) return TooLarge
                if (high - low + 1 > MAX_COUNT) return TooMany

                for (value in low..high) numbers += value
                if (numbers.size > MAX_COUNT) return TooMany
            }
            return Valid(numbers.sorted())
        }
    }
}
