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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.local.PlayerEntity
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.TextMuted

@Composable
fun NewTournamentDialog(
    allPlayers: List<PlayerEntity> = emptyList(),
    defaultOrganizer: String = "DigiSwiss Organizer",
    onDismiss: () -> Unit,
    onAddNewPlayerClick: () -> Unit = {},
    onConfirmCreate: (
        name: String,
        dateText: String,
        totalRounds: Int,
        durationMins: Int,
        location: String,
        matchFormat: String,
        selectedPlayerIds: List<Long>?
    ) -> Unit
) {
    var name by remember { mutableStateOf("DigiFest Championship #14") }
    var location by remember { mutableStateOf(defaultOrganizer) }
    var matchFormat by remember { mutableStateOf("BO3") }
    var totalRounds by remember { mutableIntStateOf(3) }
    var durationMins by remember { mutableIntStateOf(45) }
    
    // Add Date fields
    val currentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    var tournamentDate by remember { mutableStateOf(currentDate) }

    // Selection of admitted players
    val selectedPlayerIds = remember {
        mutableStateListOf<Long>().apply {
            addAll(allPlayers.map { it.id })
        }
    }

    val isBo1 = matchFormat == "BO1"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("new_tournament_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buat Turnamen Baru",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tournament Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Turnamen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tournament_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tournament Date
                item {
                    OutlinedTextField(
                        value = tournamentDate,
                        onValueChange = { tournamentDate = it },
                        label = { Text("Tanggal Turnamen") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Location / Store Name
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lokasi / Nama Penyelenggara (Toko)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Match Format Selector
                item {
                    Text(
                        text = "Format Pertandingan Digimon TCG:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bo1 Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBo1) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                .border(1.dp, if (isBo1) DigiCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    matchFormat = "BO1"
                                    durationMins = 25
                                }
                                .padding(10.dp)
                                .testTag("select_format_bo1")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SportsScore,
                                        contentDescription = null,
                                        tint = if (isBo1) DigiCyan else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Best of 1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isBo1) DigiCyan else Color.White
                                    )
                                }
                                Text(
                                    text = "1 Game • Cepat (25 Menit)",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Bo3 Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isBo1) DigiGold.copy(alpha = 0.2f) else CyberCardElevated)
                                .border(1.dp, if (!isBo1) DigiGold else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    matchFormat = "BO3"
                                    durationMins = 45
                                }
                                .padding(10.dp)
                                .testTag("select_format_bo3")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SportsScore,
                                        contentDescription = null,
                                        tint = if (!isBo1) DigiGold else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Best of 3",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isBo1) DigiGold else Color.White
                                    )
                                }
                                Text(
                                    text = "First to 2 • Standar (45 Menit)",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Total Rounds
                item {
                    Text(
                        text = "Jumlah Ronde Swiss: $totalRounds Ronde",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2, 3, 4, 5).forEach { r ->
                            val isSelected = totalRounds == r
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DigiCyan.copy(alpha = 0.25f) else CyberCardElevated)
                                    .border(1.dp, if (isSelected) DigiCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { totalRounds = r }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$r",
                                    color = if (isSelected) DigiCyan else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Round Duration
                item {
                    Text(
                        text = "Durasi Timer Ronde: $durationMins Menit",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val durationOptions = if (isBo1) listOf(20, 25, 30, 35) else listOf(35, 45, 50, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durationOptions.forEach { d ->
                            val isSelected = durationMins == d
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DigiGold.copy(alpha = 0.25f) else CyberCardElevated)
                                    .border(1.dp, if (isSelected) DigiGold else CyberCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { durationMins = d }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${d}m",
                                    color = if (isSelected) DigiGold else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Player Admission Selection (Memilih pemain yang diikutsertakan)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = DigiCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pemain Diikutsertakan",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Counter Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DigiCyan.copy(alpha = 0.15f))
                                    .border(1.dp, DigiCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${selectedPlayerIds.size}/${allPlayers.size} Dipilih",
                                    color = DigiCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions Row: Pemain Baru & Pilih/Lepas Semua
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "+ Pemain Baru" button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DigiGold.copy(alpha = 0.15f))
                                    .border(1.dp, DigiGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onAddNewPlayerClick() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "+ Pemain Baru",
                                    color = DigiGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // "Pilih Semua" / "Lepas Semua" button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberCardElevated)
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (selectedPlayerIds.size == allPlayers.size) {
                                            selectedPlayerIds.clear()
                                        } else {
                                            selectedPlayerIds.clear()
                                            selectedPlayerIds.addAll(allPlayers.map { it.id })
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (selectedPlayerIds.size == allPlayers.size) "Lepas Semua" else "Pilih Semua",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // List of selectable players
                items(allPlayers) { player ->
                    val isChecked = selectedPlayerIds.contains(player.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) CyberCardElevated else Color.Transparent)
                            .clickable {
                                if (isChecked) selectedPlayerIds.remove(player.id)
                                else selectedPlayerIds.add(player.id)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                if (check) selectedPlayerIds.add(player.id)
                                else selectedPlayerIds.remove(player.id)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DigiCyan,
                                uncheckedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = player.deckArchetype,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        DigimonColorBadge(colorName = player.deckColor, showLabel = false)
                    }
                }

                // Action Buttons
                item {
                    Spacer(modifier = Modifier.height(18.dp))
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
                                if (name.isNotBlank()) {
                                    onConfirmCreate(
                                        name,
                                        tournamentDate,
                                        totalRounds,
                                        durationMins,
                                        location,
                                        matchFormat,
                                        selectedPlayerIds.toList()
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("confirm_create_tournament_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                            enabled = name.isNotBlank() && selectedPlayerIds.size >= 2
                        ) {
                            Text(
                                "Mulai Turnamen",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
