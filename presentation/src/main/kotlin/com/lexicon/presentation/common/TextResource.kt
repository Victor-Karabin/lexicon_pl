package com.lexicon.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class TextResource(
    @param:StringRes val id: Int,
    val args: List<Any> = emptyList(),
)

@Composable
fun TextResource.text(): String = stringResource(id, *args.toTypedArray())
