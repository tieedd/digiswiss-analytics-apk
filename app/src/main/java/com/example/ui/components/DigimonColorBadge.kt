package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigimonColor

@Composable
fun DigimonColorBadge(
    colorName: String,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val colors = colorName.split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .map { DigimonColor.fromString(it) }

    if (colors.isEmpty()) return

    if (colors.size == 1) {
        val digiColor = colors[0]
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(digiColor.badgeColor.copy(alpha = 0.18f))
                .border(1.dp, digiColor.badgeColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(digiColor.badgeColor)
                )
                if (showLabel) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = digiColor.displayName,
                        color = digiColor.badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    } else {
        // Multi-color badge: show combination chip or row of dots
        val primaryColor = colors[0].badgeColor
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(primaryColor.copy(alpha = 0.16f))
                .border(1.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Render colored circles for each color
                colors.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(col.badgeColor)
                    )
                }

                if (showLabel) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = colors.joinToString("/") { it.displayName },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
