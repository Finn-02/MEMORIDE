package io.kailand.feature.briefing

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak

private val Korean = LocaleList("ko-KR")

/** Korean headings/summaries wrap on phrase boundaries with balanced lines (API 33+; older
 * platforms fall back to normal wrapping). The locale is explicit so an English device still
 * breaks Korean copy between words instead of between syllables.
 */
internal fun TextStyle.koreanPhrases(): TextStyle =
    copy(localeList = Korean, lineBreak = LineBreak.Heading.copy(wordBreak = LineBreak.WordBreak.Phrase))
