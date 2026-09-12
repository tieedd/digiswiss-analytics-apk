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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun MatchCard(
    match: MatchEntity,
    player1: PlayerEntity,
    player2: PlayerEntity?,
    onReportScoreClick: () -> Unit,
    onConcedeClick: ((playerId: Long) -> Unit)? = null,
    onDropPlayerClick: ((playerId: Long) -> Unit)? = null,
    isOrganizerOrAdmin: Boolean = true,
    currentLoggedInPlayerId: Long? = null,
    modifier: Modifier = Modifier
) {
    val isCurrentPlayerInMatch = currentLoggedInPlayerId != null &&
            (player1.id == currentLoggedInPlayerId || player2?.id == currentLoggedInPlayerId)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_card_table_${match.tableNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentPlayerInMatch) DigiCyan else if (match.isReported) DigiCyan.copy(alpha = 0.3f) else CyberCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Bar: Table Number, Round, Status, Conceded Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (match.isBye) DigiGold.copy(alpha = 0.2f) else DigiCyan.copy(alpha = 0.15f))
                            .border(1.dp, if (match.isBye) DigiGold else DigiCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (match.isBye) "BYE" else "MEJA ${match.tableNumber}",
                            color = if (match.isBye) DigiGold else DigiCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (isCurrentPlayerInMatch) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DigiCyan)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MATCH ANDA",
                                color = Color(0xFF0F172A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (match.isConceded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONCEDED",
                                color = Color(0xFFFCA5A5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (match.matchDurationMinutes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${match.matchDurationMinutes}m",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Match Status Indicator
                if (match.isReported) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DigiGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DigiGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SELESAI",
                            color = DigiGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DigiGold.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = DigiGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BERTANDING",
                            color = DigiGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player 1 Row
            val isP1Winner = match.isReported && (match.winnerId == player1.id || match.isBye)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isP1Winner) DigiCyan.copy(alpha = 0.08f) else CyberCardElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player1.name,
                            color = if (isP1Winner) DigiCyan else Color.White,
                            fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        DigimonColorBadge(colorName = player1.deckColor, showLabel = false)
                    }
                    Text(
                        text = player1.deckArchetype,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // P1 Score
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isP1Winner) DigiCyan else CyberCardBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (match.isReported) "${match.p1Score}" else "-",
                        color = if (isP1Winner) Color(0xFF0F172A) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player 2 Row or BYE
            if (player2 != null) {
                val isP2Winner = match.isReported && match.winnerId == player2.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isP2Winner) DigiCyan.copy(alpha = 0.08f) else CyberCardElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player2.name,
                                color = if (isP2Winner) DigiCyan else Color.White,
                                fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DigimonColorBadge(colorName = player2.deckColor, showLabel = false)
                        }
                        Text(
                            text = player2.deckArchetype,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // P2 Score
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isP2Winner) DigiCyan else CyberCardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (match.isReported) "${match.p2Score}" else "-",
                            color = if (isP2Winner) Color(0xFF0F172A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // BYE Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCardElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Mendapat BYE otomatis (3 Poin, 2-0)",
                        color = DigiGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interactive Actions
            if (!match.isBye && player2 != null) {
                Spacer(modifier = Modifier.height(10.dp))

                // Organizer or Admin can report / modify score
                if (isOrganizerOrAdmin) {
                    if (!match.isReported) {
                        Button(
                            onClick = onReportScoreClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_score_btn_table_${match.tableNumber}"),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Atur / Lapor Skor Pertandingan",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onReportScoreClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_score_btn_table_${match.tableNumber}"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = DigiCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Revoke & Ubah Skor",
                                color = DigiCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Player Direct Actions: "Pemain: saat permainan berlangsung, pemain dapat mengalah langsung atau drop dari permainan"
                if (isCurrentPlayerInMatch && !match.isReported) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Concede button for the player
                        Button(
                            onClick = {
                                if (currentLoggedInPlayerId != null && onConcedeClick != null) {
                                    onConcedeClick(currentLoggedInPlayerId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mengalah (Concede)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Drop button for the player
                        OutlinedButton(
                            onClick = {
                                if (currentLoggedInPlayerId != null && onDropPlayerClick != null) {
                                    onDropPlayerClick(currentLoggedInPlayerId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Drop Turnamen", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
