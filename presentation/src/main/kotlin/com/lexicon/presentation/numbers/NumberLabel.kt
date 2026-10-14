package com.lexicon.presentation.numbers

import com.lexicon.interactors.numbers.NumberKind
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TextResource

private val listPoints = listOf(
    R.string.numbers_list_point_1,
    R.string.numbers_list_point_2,
    R.string.numbers_list_point_3,
    R.string.numbers_list_point_4,
    R.string.numbers_list_point_5,
    R.string.numbers_list_point_6,
    R.string.numbers_list_point_7,
    R.string.numbers_list_point_8,
    R.string.numbers_list_point_9,
    R.string.numbers_list_point_10,
)

fun NumberQuestion.label(): TextResource =
    when (kind) {
        NumberKind.CARDINAL -> TextResource(R.string.numbers_cardinal_label, listOf(digits))
        NumberKind.ORDINAL -> TextResource(ordinalSuffix(value), listOf(digits))
        NumberKind.ENUMERATION -> TextResource(listPoints[value.toInt() - 1])
    }

private fun ordinalSuffix(value: Long): Int =
    when {
        value % 100 in 11L..13L -> R.string.numbers_ordinal_th
        value % 10 == 1L -> R.string.numbers_ordinal_st
        value % 10 == 2L -> R.string.numbers_ordinal_nd
        value % 10 == 3L -> R.string.numbers_ordinal_rd
        else -> R.string.numbers_ordinal_th
    }
