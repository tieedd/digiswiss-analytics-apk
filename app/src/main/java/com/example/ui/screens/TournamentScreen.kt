package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import com.example.ui.components.DigimonColorBadge
import com.example.ui.components.MatchCard
import com.example.ui.components.RoundTimerCard
import com.example.ui.components.ScoreReportDialog
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.DigiRed
import com.example.ui.theme.TextMuted

import com.example.ui.viewmodel.TournamentViewModel

@Composable
fun TournamentScreen(
    viewModel: TournamentViewModel,
    tournament: TournamentEntity?,
    players: List<PlayerEntity>,
    matches: List<MatchEntity>,
    selectedRound: Int,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    currentUser: UserEntity?,
    onOpenAuthDialog: () -> Unit,
    onOpenAdminDialog: () -> Unit,
    onNavigateToStandings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playerToSubmitDeck by remember { mutableStateOf<PlayerEntity?>(null) }
    var matchToReport by remember { mutableStateOf<MatchEntity?>(null) }
    val playerMap = remember(players) { players.associateBy { it.id } }

    val totalRounds = tournament?.totalRounds ?: 3
    val currentRoundMatches = remember(matches, selectedRound) {
        matches.filter { it.roundNumber == selectedRound }
    }

    val isAllCurrentRoundReported = remember(currentRoundMatches) {
        currentRoundMatches.isNotEmpty() && currentRoundMatches.all { it.isReported }
    }

    // Current player object if logged in as PLAYER
    val loggedInPlayer = remember(currentUser, players) {
        if (currentUser?.isPlayer == true && currentUser.associatedPlayerId != null) {
            players.find { it.id == currentUser.associatedPlayerId }
        } else null
    }

    val isPlayerEnrolled = remember(tournament, loggedInPlayer) {
        if (tournament != null && loggedInPlayer != null) {
            tournament.isPlayerEnrolled(loggedInPlayer.id)
        } else false
    }

    val isPlayerDropped = remember(tournament, loggedInPlayer) {
        if (tournament != null && loggedInPlayer != null) {
            tournament.isPlayerDropped(loggedInPlayer.id)
        } else false
    }

    // Current player's match in this round
    val playerCurrentMatch = remember(currentRoundMatches, loggedInPlayer) {
        if (loggedInPlayer != null) {
            currentRoundMatches.find { it.player1Id == loggedInPlayer.id || it.player2Id == loggedInPlayer.id }
        } else null
    }

    // Score Report Dialog
    if (matchToReport != null) {
        val m = matchToReport!!
        val p1 = playerMap[m.player1Id]
        val p2 = m.player2Id?.let { playerMap[it] }
        if (p1 != null && p2 != null) {
            ScoreReportDialog(
                match = m,
                player1 = p1,
                player2 = p2,
                isBo1 = tournament?.isBo1 ?: false,
                onDismiss = { matchToReport = null },
                onConcedePlayer = { concededId ->
                    viewModel.concedeMatch(m.id, concededId)
                    matchToReport = null
                },
                onDropPlayer = { dropId ->
                    if (tournament != null) {
                        viewModel.dropPlayer(tournament.id, dropId)
                    }
                    matchToReport = null
                },
                onConfirmReport = { p1Score, p2Score, isDraw, winnerId, duration, turn1Id ->
                    viewModel.reportScore(
                        matchId = m.id,
                        p1Score = p1Score,
                        p2Score = p2Score,
                        isDraw = isDraw,
                        winnerId = winnerId,
                        durationMinutes = duration,
                        firstTurnPlayerId = turn1Id
                    )
                    matchToReport = null
                }
            )
        }
    }

    if (playerToSubmitDeck != null && tournament != null) {
        com.example.ui.components.SubmitDeckDialog(
            player = playerToSubmitDeck!!,
            matches = matches,
            onDismiss = { playerToSubmitDeck = null },
            onConfirmSubmit = { archetype, colors ->
                viewModel.setPlayerDeckForTournament(tournament.id, playerToSubmitDeck!!.id, archetype, colors)
                playerToSubmitDeck = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .testTag("tournament_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // User Role & Account Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (badgeColor, roleLabel, roleIcon) = when {
                            currentUser?.isAdmin == true -> Triple(DigiGold, "ADMIN (Master)", Icons.Default.AdminPanelSettings)
                            currentUser?.isOrganizer == true -> Triple(DigiCyan, "PENYELENGGARA", Icons.Default.Storefront)
                            currentUser?.isPlayer == true -> Triple(Color(0xFFA855F7), "PEMAIN", Icons.Default.Person)
                            else -> Triple(Color.Gray, "TAMU", Icons.Default.Person)
                        }

                        Icon(roleIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser?.fullName ?: "Belum Masuk",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = roleLabel,
                                        fontSize = 8.sp,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (currentUser?.isAdmin == true) "masteradmin • Akses Penuh Sistem"
                                else if (currentUser?.isOrganizer == true) "Penyelenggara: ${currentUser.affiliation.ifBlank { "DigiSwiss Organizer" }}"
                                else if (loggedInPlayer != null) "Deck: ${loggedInPlayer.deckArchetype}"
                                else "Login untuk akses fitur lengkap",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (currentUser?.isAdmin == true) {
                            OutlinedButton(
                                onClick = onOpenAdminDialog,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DigiGold),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DigiGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kelola", color = DigiGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onOpenAuthDialog,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCardElevated),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_switch_or_login")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = DigiCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentUser != null) "Ganti Akun" else "Masuk / Daftar",
                                color = DigiCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DigiCyan.copy(alpha = 0.2f))
                                .border(1.dp, DigiCyan, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SWISS TOURNAMENT",
                                color = DigiCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Penyelenggara: ${tournament?.organizerName ?: "DigiSwiss Organizer"}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tournament?.name ?: "DigiFest Store Championship 2026",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Dedicated Player Action Card (If logged in as Player)
        if (currentUser?.isPlayer == true && loggedInPlayer != null && tournament != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlayerDropped) Color(0xFFEF4444) else DigiCyan)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = DigiCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status Anda: ${loggedInPlayer.name}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DigimonColorBadge(colorName = loggedInPlayer.deckColor, showLabel = true)
                        }

                        Text(
                            text = "Deck: ${loggedInPlayer.deckArchetype}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isPlayerDropped) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Anda telah DROPPED dari turnamen ini. Anda tidak akan dipasangkan di ronde berikutnya.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (!isPlayerEnrolled) {
                            // Player is not yet enrolled
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Anda belum terdaftar di turnamen aktif ini.",
                                    color = DigiGold,
                                    fontSize = 11.sp
                                )
                                Button(
                                    onClick = { viewModel.enrollPlayer(tournament.id, loggedInPlayer.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.HowToReg, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Daftar Sekarang", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Player is enrolled!
                            if (playerCurrentMatch != null) {
                                val match = playerCurrentMatch
                                val opponentId = if (match.player1Id == loggedInPlayer.id) match.player2Id else match.player1Id
                                val opponent = opponentId?.let { playerMap[it] }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberCardElevated)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Meja #${match.tableNumber}: Lawan ${opponent?.name ?: "BYE"}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (match.isReported) "Status: Selesai (${match.p1Score} - ${match.p2Score})"
                                            else "Pertandingan sedang berlangsung",
                                            color = if (match.isReported) DigiGreen else DigiGold,
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (!match.isReported && opponent != null) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Concede action
                                            Button(
                                                onClick = { viewModel.concedeMatch(match.id, loggedInPlayer.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Mengalah", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Drop action
                                            OutlinedButton(
                                                onClick = { viewModel.dropPlayer(tournament.id, loggedInPlayer.id) },
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Drop", color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

        // Timer Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RoundTimerCard(
                    roundNumber = selectedRound,
                    totalSeconds = timerSeconds,
                    isRunning = isTimerRunning,
                    onToggleTimer = { viewModel.toggleRoundTimer() },
                    onResetTimer = { viewModel.resetRoundTimer() }
                )
            }
        }

        // Round Selector Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = (selectedRound - 1).coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                containerColor = CyberNavyBg,
                contentColor = DigiCyan,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedRound - 1 < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedRound - 1]),
                            color = DigiCyan
                        )
                    }
                }
            ) {
                for (r in 1..totalRounds) {
                    val isCurrentActive = tournament?.currentRound == r
                    Tab(
                        selected = selectedRound == r,
                        onClick = { viewModel.setSelectedRound(r) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ronde $r",
                                    fontWeight = if (selectedRound == r) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedRound == r) DigiCyan else Color.Gray
                                )
                                if (isCurrentActive) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(DigiGold)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Round Status Bar
        item {
            val reportedCount = currentRoundMatches.count { it.isReported }
            val totalMatchCount = currentRoundMatches.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Matchup Ronde $selectedRound",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Progres: $reportedCount / $totalMatchCount Meja Selesai",
                        color = if (isAllCurrentRoundReported) DigiGreen else TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Quick Link to Standings
                OutlinedButton(
                    onClick = onNavigateToStandings,
                    modifier = Modifier.testTag("view_standings_shortcut_btn"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = DigiGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Klasemen",
                        color = DigiGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Advance to Next Round Button (if current round matches all reported)
        if (isAllCurrentRoundReported && tournament != null && selectedRound == tournament.currentRound && tournament.currentRound < tournament.totalRounds) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DigiGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DigiGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Semua Meja Ronde $selectedRound Selesai!",
                                color = DigiGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Klasemen telah diperbarui. Lanjut ke pairing Swiss otomatis Ronde ${selectedRound + 1}.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.advanceToNextRound() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advance_round_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Lanjut ke Ronde ${selectedRound + 1} (Swiss Pairing)",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Tournament Completed View
        if (tournament?.status == "COMPLETED" || (isAllCurrentRoundReported && tournament != null && selectedRound == tournament.totalRounds)) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DigiGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turnamen Selesai!",
                                color = DigiGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Semua ronde telah selesai. Silakan update deck pemain jika pemain menggunakan deck yang berbeda di turnamen ini (untuk histori).",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // List enrolled players to update deck
                        if (currentUser?.isAdmin == true || currentUser?.isOrganizer == true || currentUser?.isPlayer == true) {
                            val playersToShow = if (currentUser?.isPlayer == true) {
                                listOfNotNull(loggedInPlayer)
                            } else {
                                tournament.enrolledList.mapNotNull { playerMap[it] }
                            }
                            
                            playersToShow.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.name,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    OutlinedButton(
                                        onClick = { playerToSubmitDeck = p },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Set Deck", fontSize = 11.sp, color = DigiCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Match Cards List
        if (currentRoundMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Belum ada pairing untuk Ronde $selectedRound",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Selesaikan ronde sebelumnya untuk generate ronde ini.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(currentRoundMatches) { match ->
                val p1 = playerMap[match.player1Id]
                val p2 = match.player2Id?.let { playerMap[it] }

                if (p1 != null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        MatchCard(
                            match = match,
                            player1 = p1,
                            player2 = p2,
                            onReportScoreClick = { matchToReport = match },
                            onConcedeClick = { playerId ->
                                viewModel.concedeMatch(match.id, playerId)
                            },
                            onDropPlayerClick = { playerId ->
                                if (tournament != null) {
                                    viewModel.dropPlayer(tournament.id, playerId)
                                }
                            },
                            isOrganizerOrAdmin = currentUser?.isAdmin == true || currentUser?.isOrganizer == true,
                            currentLoggedInPlayerId = loggedInPlayer?.id
                        )
                    }
                }
            }
        }
    }
}
