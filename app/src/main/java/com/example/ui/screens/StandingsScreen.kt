package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerEntity
import com.example.ui.components.DigimonColorBadge
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.TextMuted
import com.example.util.StandingEntry

@Composable
fun StandingsScreen(
    standings: List<StandingEntry>,
    tournamentName: String,
    currentRound: Int,
    totalRounds: Int,
    matchFormat: String = "BO3",
    onPlayerClick: (PlayerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .testTag("standings_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Klasemen Real-Time",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        val formatLabel = if (matchFormat == "BO1") "Best of 1" else "Best of 3"
                        Text(
                            text = "$tournamentName • $formatLabel • Ronde $currentRound/$totalRounds",
                            color = DigiCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DigiGold.copy(alpha = 0.15f))
                            .border(1.dp, DigiGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Swiss Ranking",
                                color = DigiGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top Leader Spotlight
                if (standings.isNotEmpty()) {
                    val leader = standings.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(leader.player) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DigiGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(DigiGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#1",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = leader.player.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${leader.player.deckArchetype} • ${leader.player.handle}",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${leader.matchPoints} Poin",
                                    color = DigiGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${leader.matchesWon}W - ${leader.matchesLost}L",
                                    color = DigiGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Table Column Header Bar (DigiLabs style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCardSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANK",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(38.dp)
                    )
                    Text(
                        text = "PEMAIN & DECK",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "W-L-D",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp)
                    )
                    Text(
                        text = "OMW%",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        text = "PTS",
                        color = DigiCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }

        // Leaderboard Rows
        items(standings) { entry ->
            val isTopRank = entry.rank <= 3
            val rankBg = when (entry.rank) {
                1 -> DigiGold
                2 -> Color(0xFFCBD5E1)
                3 -> Color(0xFFCD7F32)
                else -> CyberCardBorder
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCardElevated)
                    .border(
                        1.dp,
                        if (entry.rank == 1) DigiGold.copy(alpha = 0.5f) else CyberCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlayerClick(entry.player) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("standing_row_rank_${entry.rank}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(rankBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${entry.rank}",
                            color = if (isTopRank) Color(0xFF0F172A) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Player & Deck
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.player.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DigimonColorBadge(colorName = entry.player.deckColor, showLabel = false)
                        }
                        Text(
                            text = entry.player.deckArchetype,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // W-L-D
                    Text(
                        text = "${entry.matchesWon}-${entry.matchesLost}-${entry.matchesDrawn}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(44.dp)
                    )

                    // OMW%
                    Text(
                        text = "${String.format("%.1f", entry.omwPercent)}%",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.width(48.dp)
                    )

                    // Points
                    Text(
                        text = "${entry.matchPoints}",
                        color = DigiCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.width(36.dp)
                    )

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Detail Pemain",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Tiebreaker explanation note
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCardSurface)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = DigiCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sistem Tiebreaker Standar Bandai Digimon TCG:\n1. Match Points (Win = 3, Draw = 1, Loss = 0)\n2. OMW% (Opponents' Match Win %)\n3. GW% (Game Win %)\nKlik pemain untuk melihat analisa kelemahan & rekor mendalam.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
