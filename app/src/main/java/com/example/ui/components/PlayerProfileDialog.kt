package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.DigiRed
import com.example.ui.theme.TextMuted

@Composable
fun PlayerProfileDialog(
    player: PlayerEntity,
    allPlayers: List<PlayerEntity>,
    matches: List<MatchEntity>,
    onDismiss: () -> Unit
) {
    val playerMap = remember(allPlayers) { allPlayers.associateBy { it.id } }
    val playerMatches = remember(matches, player.id) {
        matches.filter { it.player1Id == player.id || it.player2Id == player.id }
            .sortedByDescending { it.roundNumber }
    }

    val totalWins = player.totalWins + playerMatches.count { it.isReported && it.winnerId == player.id }
    val totalLosses = player.totalLosses + playerMatches.count { it.isReported && it.winnerId != null && it.winnerId != player.id && !it.isDraw }
    val totalDraws = player.totalDraws + playerMatches.count { it.isReported && it.isDraw }
    val totalMatchesPlayed = totalWins + totalLosses + totalDraws
    val allTimeWinRate = if (totalMatchesPlayed > 0) (totalWins.toDouble() / totalMatchesPlayed.toDouble()) * 100.0 else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("player_profile_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DigiCyan.copy(alpha = 0.2f))
                                .border(1.5.dp, DigiCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.name.take(2).uppercase(),
                                color = DigiCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = player.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = player.handle,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Deck & Trophies Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DigimonColorBadge(colorName = player.deckColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.deckArchetype,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (player.trophies > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${player.trophies} Juara",
                                color = DigiGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WIN RATE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${String.format("%.1f", allTimeWinRate)}%",
                            color = if (allTimeWinRate >= 50.0) DigiGreen else DigiRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL RECORD", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${totalWins}W - ${totalLosses}L",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TURNAMEN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${player.totalTournaments}",
                            color = DigiCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Match History Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = DigiCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Riwayat Pertandingan Turnamen",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Match History List
                if (playerMatches.isEmpty()) {
                    Text(
                        text = "Belum ada pertandingan pada turnamen aktif.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(playerMatches) { m ->
                            val isP1 = m.player1Id == player.id
                            val oppId = if (isP1) m.player2Id else m.player1Id
                            val opponent = oppId?.let { playerMap[it] }

                            val myScore = if (isP1) m.p1Score else m.p2Score
                            val oppScore = if (isP1) m.p2Score else m.p1Score
                            val isWin = m.winnerId == player.id || m.isBye
                            val isDraw = m.isDraw

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCardElevated)
                                    .border(
                                        1.dp,
                                        if (isWin) DigiGreen.copy(alpha = 0.4f) else CyberCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Ronde ${m.roundNumber} • Meja ${if (m.isBye) "BYE" else m.tableNumber}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (m.isBye) "BYE Otomatis" else "vs ${opponent?.name ?: "Opponent"}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (opponent != null) {
                                            Text(
                                                text = opponent.deckArchetype,
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (m.isReported) "$myScore - $oppScore" else "Belum Selesai",
                                            color = if (isWin) DigiGreen else if (isDraw) DigiGold else DigiRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        if (m.matchDurationMinutes > 0) {
                                            Text(
                                                text = "${m.matchDurationMinutes} Menit",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
