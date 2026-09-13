package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.TextMuted

@Composable
fun ScoreReportDialog(
    match: MatchEntity,
    player1: PlayerEntity,
    player2: PlayerEntity,
    isBo1: Boolean = false,
    onDismiss: () -> Unit,
    onConcedePlayer: ((playerId: Long) -> Unit)? = null,
    onDropPlayer: ((playerId: Long) -> Unit)? = null,
    onConfirmReport: (p1Score: Int, p2Score: Int, isDraw: Boolean, winnerId: Long?, durationMinutes: Int, firstTurnPlayerId: Long?) -> Unit
) {
    var matchFormatBo1 by remember { mutableStateOf(isBo1) }

    val maxScore = if (matchFormatBo1) 1 else 2
    val defaultP1 = if (match.isReported) match.p1Score.coerceAtMost(maxScore) else if (matchFormatBo1) 1 else 2
    val defaultP2 = if (match.isReported) match.p2Score.coerceAtMost(maxScore) else 0

    var p1Score by remember { mutableIntStateOf(defaultP1) }
    var p2Score by remember { mutableIntStateOf(defaultP2) }
    var isDraw by remember { mutableStateOf(match.isDraw) }
    var durationMins by remember {
        mutableIntStateOf(
            if (match.matchDurationMinutes > 0) match.matchDurationMinutes
            else if (matchFormatBo1) 18 else 28
        )
    }
    var firstTurnPlayerId by remember { mutableStateOf<Long?>(match.firstTurnPlayerId ?: player1.id) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("score_report_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lapor Hasil Meja #${match.tableNumber}",
                            color = DigiCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (matchFormatBo1) "Digimon Card Game Best of 1" else "Digimon Card Game Best of 3",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bo1 / Bo3 Format Switch Selector in Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCardElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bo1 button
                    val selectedBo1 = matchFormatBo1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedBo1) DigiCyan else Color.Transparent)
                            .clickable {
                                matchFormatBo1 = true
                                if (p1Score > 1) p1Score = 1
                                if (p2Score > 1) p2Score = 0
                                durationMins = 18
                            }
                            .padding(vertical = 6.dp)
                            .testTag("dialog_switch_bo1"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Best of 1 (1 Game)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBo1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBo1) Color(0xFF0F172A) else Color.White
                        )
                    }

                    // Bo3 button
                    val selectedBo3 = !matchFormatBo1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedBo3) DigiCyan else Color.Transparent)
                            .clickable {
                                matchFormatBo1 = false
                                durationMins = 28
                            }
                            .padding(vertical = 6.dp)
                            .testTag("dialog_switch_bo3"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Best of 3 (First to 2)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBo3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBo3) Color(0xFF0F172A) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = player1.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        DigimonColorBadge(colorName = player1.deckColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (p1Score > 0) p1Score-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = DigiCyan)
                            }
                            Text(
                                text = "$p1Score",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (p1Score < maxScore) p1Score++
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = DigiCyan)
                            }
                        }
                    }

                    Text(
                        text = "VS",
                        color = DigiGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )

                    // Player 2
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = player2.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        DigimonColorBadge(colorName = player2.deckColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (p2Score > 0) p2Score-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = DigiCyan)
                            }
                            Text(
                                text = "$p2Score",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (p2Score < maxScore) p2Score++
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = DigiCyan)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick preset buttons based on format
                Text(
                    text = "Preset Cepat (${if (matchFormatBo1) "Best of 1" else "Best of 3"}):",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (matchFormatBo1) {
                    // Bo1 Presets: 1-0, 0-1, 0-0 (Draw)
                    val bo1Presets = listOf(
                        "${player1.name.take(6)} Menang (1-0)" to (1 to 0),
                        "${player2.name.take(6)} Menang (0-1)" to (0 to 1),
                        "Seri / Draw (0-0)" to (0 to 0)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bo1Presets.forEach { (label, scores) ->
                            val isSelected = p1Score == scores.first && p2Score == scores.second
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) DigiCyan else CyberCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        p1Score = scores.first
                                        p2Score = scores.second
                                        isDraw = (scores.first == scores.second)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DigiCyan else Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    // Bo3 Presets: 2-0, 2-1, 1-2, 0-2, 1-1 Draw
                    val bo3Presets = listOf(
                        "2-0" to (2 to 0),
                        "2-1" to (2 to 1),
                        "1-2" to (1 to 2),
                        "0-2" to (0 to 2),
                        "1-1 (Draw)" to (1 to 1)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bo3Presets.forEach { (label, scores) ->
                            val isSelected = p1Score == scores.first && p2Score == scores.second
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) DigiCyan else CyberCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        p1Score = scores.first
                                        p2Score = scores.second
                                        isDraw = (scores.first == scores.second)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DigiCyan else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Concession & Drop Quick Actions
                if (onConcedePlayer != null || onDropPlayer != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aksi Cepat Concede / Drop:",
                        color = Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onConcedePlayer != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onConcedePlayer(player1.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${player1.name.split(" ").first()} Concede",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onConcedePlayer(player2.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${player2.name.split(" ").first()} Concede",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (onDropPlayer != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberCardElevated)
                                    .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onDropPlayer(player1.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Drop P1",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_score_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                    ) {
                        Text("Batal", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val winner = when {
                                p1Score > p2Score -> player1.id
                                p2Score > p1Score -> player2.id
                                else -> null
                            }
                            val draw = (p1Score == p2Score)
                            onConfirmReport(p1Score, p2Score, draw, winner, durationMins, firstTurnPlayerId)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_score_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DigiCyan)
                    ) {
                        Text(
                            "Simpan Skor",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
