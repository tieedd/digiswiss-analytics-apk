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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.DigimonColor
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitDeckDialog(
    player: PlayerEntity,
    matches: List<MatchEntity> = emptyList(),
    allMatches: List<MatchEntity> = emptyList(),
    allPlayers: List<PlayerEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSubmit: (archetype: String, colors: String) -> Unit
) {
    // Resolve current tournament deck if already assigned
    val currentTourneyMatch = remember(player, matches) {
        matches.firstOrNull {
            (it.player1Id == player.id && it.p1DeckArchetype.isNotBlank() && it.p1DeckArchetype != "Unknown") ||
            (it.player2Id == player.id && it.p2DeckArchetype.isNotBlank() && it.p2DeckArchetype != "Unknown")
        }
    }
    val tourneyArchetype = if (currentTourneyMatch?.player1Id == player.id) currentTourneyMatch.p1DeckArchetype else currentTourneyMatch?.p2DeckArchetype
    val tourneyColor = if (currentTourneyMatch?.player1Id == player.id) currentTourneyMatch.p1DeckColor else currentTourneyMatch?.p2DeckColor

    val initialArchetype = tourneyArchetype?.takeIf { it.isNotBlank() && it != "Unknown" }
        ?: player.deckArchetype.takeIf { it.isNotBlank() && it != "Unknown" }
        ?: ""

    val initialColorString = tourneyColor?.takeIf { it.isNotBlank() && it != "UNKNOWN" }
        ?: player.deckColor.takeIf { it.isNotBlank() && it != "UNKNOWN" }
        ?: ""

    val isEditing = initialArchetype.isNotBlank()

    var deckArchetype by remember { mutableStateOf(initialArchetype) }
    var selectedColors by remember {
        mutableStateOf<Set<DigimonColor>>(
            if (initialColorString.isNotBlank()) {
                initialColorString.split(",").mapNotNull { name ->
                    DigimonColor.entries.find { it.name.equals(name.trim(), ignoreCase = true) }
                }.toSet()
            } else emptySet()
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Extract historical decks strictly for this player only
    val historicalDecks = remember(player, matches, allMatches) {
        val playerPastDecks = mutableListOf<Pair<String, String>>()

        // 1. Current tournament matches for this player
        matches.forEach { m ->
            if (m.player1Id == player.id && m.p1DeckArchetype.isNotBlank() && m.p1DeckArchetype != "Unknown") {
                playerPastDecks.add(Pair(m.p1DeckArchetype.trim(), m.p1DeckColor.trim()))
            }
            if (m.player2Id == player.id && m.p2DeckArchetype.isNotBlank() && m.p2DeckArchetype != "Unknown") {
                playerPastDecks.add(Pair(m.p2DeckArchetype.trim(), m.p2DeckColor.trim()))
            }
        }

        // 2. Historical matches across all tournaments for this player
        allMatches.forEach { m ->
            if (m.player1Id == player.id && m.p1DeckArchetype.isNotBlank() && m.p1DeckArchetype != "Unknown") {
                playerPastDecks.add(Pair(m.p1DeckArchetype.trim(), m.p1DeckColor.trim()))
            }
            if (m.player2Id == player.id && m.p2DeckArchetype.isNotBlank() && m.p2DeckArchetype != "Unknown") {
                playerPastDecks.add(Pair(m.p2DeckArchetype.trim(), m.p2DeckColor.trim()))
            }
        }

        // 3. Current player's registered default deck
        if (player.deckArchetype.isNotBlank() && player.deckArchetype != "Unknown") {
            playerPastDecks.add(Pair(player.deckArchetype.trim(), player.deckColor.trim()))
        }

        // Distinct by archetype name case-insensitively
        val distinctDecks = mutableListOf<Pair<String, String>>()
        playerPastDecks.forEach { item ->
            if (distinctDecks.none { it.first.equals(item.first, ignoreCase = true) }) {
                distinctDecks.add(item)
            }
        }
        distinctDecks
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Save,
                            contentDescription = null,
                            tint = if (isEditing) DigiGold else DigiCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) "Edit Deck Pemain" else "Set Deck Pemain",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pemain: ",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // History / Quick Select Section
                if (historicalDecks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = DigiCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Histori Deck ${player.name}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historicalDecks) { (histArchetype, histColorStr) ->
                            val parsedColors = histColorStr.split(",").mapNotNull { name ->
                                DigimonColor.entries.find { it.name.equals(name.trim(), ignoreCase = true) }
                            }.toSet()
                            val isSelected = deckArchetype.equals(histArchetype, ignoreCase = true)
                            val accentColor = if (isSelected) DigiCyan else (parsedColors.firstOrNull()?.badgeColor ?: CyberCardBorder)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DigiCyan.copy(alpha = 0.16f) else CyberCardElevated)
                                    .border(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) DigiCyan else accentColor.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        deckArchetype = histArchetype
                                        selectedColors = parsedColors
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Mini color dots
                                    if (parsedColors.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            parsedColors.forEach { c ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(c.badgeColor)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Text(
                                        text = histArchetype,
                                        color = if (isSelected) DigiCyan else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DigiCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Archetype input
                OutlinedTextField(
                    value = deckArchetype,
                    onValueChange = { deckArchetype = it; errorMessage = null },
                    label = { Text("Nama Deck / Archetype (ex: Imperialdramon)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Warna Deck Utama (Bisa pilih > 1)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color selector
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DigimonColor.entries.forEach { dColor ->
                        val isSelected = selectedColors.contains(dColor)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) dColor.badgeColor.copy(alpha = 0.2f) else CyberCardElevated)
                                .border(1.dp, if (isSelected) dColor.badgeColor else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    errorMessage = null
                                    selectedColors = if (isSelected) {
                                        selectedColors - dColor
                                    } else {
                                        selectedColors + dColor
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(dColor.badgeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dColor.displayName,
                                    color = if (isSelected) dColor.badgeColor else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                    ) {
                        Text("Batal", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (deckArchetype.isBlank()) {
                                errorMessage = "Archetype deck wajib diisi."
                                return@Button
                            }
                            if (selectedColors.isEmpty()) {
                                errorMessage = "Pilih minimal 1 warna deck."
                                return@Button
                            }
                            
                            val colorString = selectedColors.joinToString(",") { it.name }
                            onConfirmSubmit(deckArchetype.trim(), colorString)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditing) DigiGold else DigiCyan
                        )
                    ) {
                        Text(
                            if (isEditing) "Perbarui Deck" else "Simpan Deck",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
