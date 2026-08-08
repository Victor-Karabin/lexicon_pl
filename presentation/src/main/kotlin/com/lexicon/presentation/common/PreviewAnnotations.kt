package com.lexicon.presentation.common

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview in both colour schemes.
 *
 * `uiMode` drives `isSystemInDarkTheme()`, which is what `LexiconTheme` reads by default — so a
 * preview only picks up the dark variant if its content is wrapped in `LexiconTheme`, as every
 * screen preview here is.
 *
 * Catching contrast problems in the IDE is cheap; catching them on a device is not. Prefer this
 * over a bare `@Preview` for anything that renders colour, but still check real devices for
 * anything subtle.
 */
@Preview(name = "light", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LightDarkPreview

/**
 * [LightDarkPreview] at the largest font scale users can select, where text-heavy layouts are most
 * likely to clip or overflow.
 */
@LightDarkPreview
@Preview(name = "large font", showBackground = true, fontScale = 2.0f)
annotation class LightDarkFontScalePreview
