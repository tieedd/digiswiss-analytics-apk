package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.TextMuted

// 7 official Digimon colors in Indonesian
val AVAILABLE_DIGIMON_COLORS = listOf(
    DigimonColor.RED to "Merah",
    DigimonColor.BLUE to "Biru",
    DigimonColor.YELLOW to "Kuning",
    DigimonColor.GREEN to "Hijau",
    DigimonColor.BLACK to "Hitam",
    DigimonColor.PURPLE to "Ungu",
    DigimonColor.WHITE to "Putih"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiColorSelector(
    selectedColors: List<String>,
    onColorsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pilih Warna Deck (Bisa lebih dari 1):",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (selectedColors.isNotEmpty()) {
                Text(
                    text = "${selectedColors.size} warna dipilih",
                    color = DigiCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AVAILABLE_DIGIMON_COLORS.forEach { (digiColor, indonesianName) ->
                val colorKey = digiColor.name
                val isSelected = selectedColors.contains(colorKey)

                val chipBg = if (isSelected) digiColor.badgeColor.copy(alpha = 0.25f) else CyberCardElevated
                val borderColor = if (isSelected) digiColor.badgeColor else CyberCardBorder

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            val newColors = if (isSelected) {
                                if (selectedColors.size > 1) selectedColors - colorKey else selectedColors
                            } else {
                                selectedColors + colorKey
                            }
                            onColorsChanged(newColors)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(digiColor.badgeColor)
                                .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )

                        Text(
                            text = indonesianName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = digiColor.badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
