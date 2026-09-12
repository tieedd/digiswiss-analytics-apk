package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class DigimonColor(
    val displayName: String,
    val hexColor: Long,
    val badgeColor: Color,
    val onColor: Color
) {
    RED("Red", 0xFFE53935, Color(0xFFE53935), Color.White),
    BLUE("Blue", 0xFF1E88E5, Color(0xFF1E88E5), Color.White),
    YELLOW("Yellow", 0xFFFDD835, Color(0xFFFDD835), Color(0xFF1A1A1A)),
    GREEN("Green", 0xFF43A047, Color(0xFF43A047), Color.White),
    BLACK("Black", 0xFF475569, Color(0xFF475569), Color.White),
    PURPLE("Purple", 0xFF8E24AA, Color(0xFF8E24AA), Color.White),
    WHITE("White", 0xFFE2E8F0, Color(0xFFE2E8F0), Color(0xFF0F172A));

    companion object {
        fun fromString(colorName: String): DigimonColor {
            return entries.find { it.name.equals(colorName, ignoreCase = true) } ?: RED
        }
    }
}
