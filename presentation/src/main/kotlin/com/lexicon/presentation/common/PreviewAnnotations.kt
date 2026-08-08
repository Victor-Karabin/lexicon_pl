package com.lexicon.presentation.common

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "light", showBackground = true)
@Preview(
    name = "dark",
    showBackground = true,
    backgroundColor = PREVIEW_DARK_BACKGROUND,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class LightDarkPreview

@LightDarkPreview
@Preview(name = "large font", showBackground = true, fontScale = 2.0f)
annotation class LightDarkFontScalePreview

const val PREVIEW_DARK_BACKGROUND: Long = 0xFF121212
