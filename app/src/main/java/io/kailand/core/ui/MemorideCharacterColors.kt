package io.kailand.core.ui

import androidx.compose.ui.graphics.Color

/** Character identity surfaces share the selection screen's existing colors. */
data class MemorideCharacterColors(val accent: Color, val soft: Color, val pale: Color) {
    val background: Color get() = soft
}

fun memorideCharacterColors(characterId: String?): MemorideCharacterColors = when (characterId) {
    "tori" -> MemorideCharacterColors(Color(0xFFC9782F), Color(0xFFFFE6CF), Color(0xFFFFF7EE))
    "momo" -> MemorideCharacterColors(Color(0xFF2A7F9E), Color(0xFFD6EDFA), Color(0xFFF0F8FA))
    "lulu" -> MemorideCharacterColors(Color(0xFF7B5BB0), Color(0xFFECE3FA), Color(0xFFF8F4FC))
    else -> MemorideCharacterColors(Color(0xFF1F4E79), Color(0xFFE8F6EE), Color(0xFFE2F0FA))
}
