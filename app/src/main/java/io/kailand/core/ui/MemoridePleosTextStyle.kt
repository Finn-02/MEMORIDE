package io.kailand.core.ui

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

/** Korean word/phrase boundaries for the Pleos screens; explicit newlines remain authoritative. */
val MemoridePleosTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    localeList = LocaleList("ko-KR"),
    lineBreak = LineBreak(
        strategy = LineBreak.Strategy.HighQuality,
        strictness = LineBreak.Strictness.Strict,
        wordBreak = LineBreak.WordBreak.Phrase,
    ),
    hyphens = Hyphens.None,
)
