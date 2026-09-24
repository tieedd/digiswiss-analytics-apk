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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.CsvHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerEntity
import com.example.data.local.MatchEntity
import com.example.ui.components.AddPlayerDialog
import com.example.ui.components.DigimonColorBadge
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.DigiRed
import com.example.ui.theme.TextMuted

import com.example.data.local.UserEntity

@Composable
fun PlayersScreen(
    players: List<PlayerEntity>,
    matches: List<MatchEntity>,
    currentUser: UserEntity?,
    onPlayerClick: (PlayerEntity) -> Unit,
    onAddNewPlayer: (name: String, handle: String, bandaiUid: String) -> Unit,
    onDeletePlayer: ((PlayerEntity) -> Unit)? = null,
    onImportPlayers: ((List<PlayerEntity>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val imported = CsvHelper.importPlayersFromCsv(context, it)
                onImportPlayers?.invoke(imported)
            }
        }
    )

    val filteredPlayers = remember(players, searchQuery) {
        if (searchQuery.isBlank()) {
            players
        } else {
            players.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.handle.contains(searchQuery, ignoreCase = true) ||
                        it.bandaiUid.contains(searchQuery, ignoreCase = true) ||
                        it.deckArchetype.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showAddDialog) {
        AddPlayerDialog(
            existingPlayers = players,
            onDismiss = { showAddDialog = false },
            onConfirmAdd = { name, handle, bandaiUid ->
                onAddNewPlayer(name, handle, bandaiUid)
                showAddDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .testTag("players_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
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
                        Column {
                            Text(
                                text = "Manajemen Peserta",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${players.size} Peserta Terdaftar di Database",
                                color = DigiCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (currentUser?.isAdmin == true || currentUser?.isOrganizer == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { 
                                        if (players.isEmpty()) {
                                            Toast.makeText(context, "Tidak ada data peserta untuk diekspor", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val csv = CsvHelper.exportPlayersToCsv(players)
                                            CsvHelper.shareCsv(context, csv, "Peserta_DigiSwiss")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DigiGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .testTag("export_players_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Export CSV Peserta", tint = DigiGold, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        importLauncher.launch(
                                            arrayOf(
                                                "text/*",
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/csv",
                                                "application/vnd.ms-excel",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DigiCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .testTag("import_players_button")
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = "Import CSV Peserta", tint = DigiCyan, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("add_player_top_button").height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tambah Pemain",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari peserta, ID, atau archetype...", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_search_bar"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberCardElevated,
                            unfocusedContainerColor = CyberCardElevated,
                            focusedBorderColor = DigiCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Player Items
            items(filteredPlayers) { player ->
                val playerMatches = matches.filter { it.player1Id == player.id || it.player2Id == player.id }
                val totalWins = player.totalWins + playerMatches.count { it.isReported && it.winnerId == player.id }
                val totalLosses = player.totalLosses + playerMatches.count { it.isReported && it.winnerId != null && it.winnerId != player.id && !it.isDraw }
                val totalDraws = player.totalDraws + playerMatches.count { it.isReported && it.isDraw }
                val totalMatchesPlayed = totalWins + totalLosses + totalDraws
                val winRate = if (totalMatchesPlayed > 0) (totalWins.toDouble() / totalMatchesPlayed.toDouble()) * 100.0 else 0.0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                        .clickable { onPlayerClick(player) }
                        .padding(12.dp)
                        .testTag("player_card_${player.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Icon
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CyberCardSurface)
                                    .border(1.dp, DigiCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.name.take(2).uppercase(),
                                    color = DigiCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = player.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    DigimonColorBadge(colorName = player.deckColor, showLabel = false)
                                }
                                Text(
                                    text = "${player.deckArchetype} • ${player.handle} • UID: ${if (player.bandaiUid.isNotBlank()) player.bandaiUid else "-"}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Player Record & WinRate & Delete Option
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format("%.0f", winRate)}% WR",
                                    color = if (winRate >= 50.0) DigiGreen else DigiGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${totalWins}W - ${totalLosses}L",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (onDeletePlayer != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { playerToDelete = player },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_player_btn_${player.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus Pemain ${player.name}",
                                        tint = DigiRed.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (playerToDelete != null) {
        val target = playerToDelete!!
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = {
                Text(
                    text = "Hapus Pemain?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus data pemain '${target.name}' (${target.handle}) dari database? Data pemain ini akan dihapus secara permanen.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = target
                        playerToDelete = null
                        onDeletePlayer?.invoke(p)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DigiRed)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = CyberCardSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }
}
