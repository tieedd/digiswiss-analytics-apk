package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.ui.components.DigimonColorBadge
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.DigiPurple
import com.example.ui.theme.DigiRed
import com.example.data.local.TournamentEntity
import com.example.ui.theme.TextMuted
import com.example.util.AnalyticsEngine
import com.example.util.DiagnosticSeverity

@Composable
fun AnalyticsScreen(
    players: List<PlayerEntity>,
    matches: List<MatchEntity>,
    allMatches: List<MatchEntity> = matches,
    allTournaments: List<TournamentEntity> = emptyList(),
    activeTournament: TournamentEntity? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Individual, 1: Overall Meta
    var scopeFilter by remember { mutableStateOf("CURRENT") } // "CURRENT", "STORE", "ALL"
    var selectedPlayerId by remember(players) {
        mutableStateOf(players.firstOrNull()?.id ?: 0L)
    }

    val storeName = activeTournament?.organizerName ?: "Toko A - DigiLabs Jakarta"
    val filteredMatches = remember(scopeFilter, matches, allMatches, activeTournament, allTournaments) {
        when (scopeFilter) {
            "CURRENT" -> {
                if (activeTournament != null) {
                    val m = allMatches.filter { it.tournamentId == activeTournament.id }
                    if (m.isNotEmpty()) m else matches
                } else matches
            }
            "STORE" -> {
                val storeKeywords = listOf("Toko A", "DigiLabs", "Store")
                val storeTourneyIds = allTournaments.filter { t ->
                    storeKeywords.any { kw -> t.organizerName.contains(kw, ignoreCase = true) || t.location.contains(kw, ignoreCase = true) }
                }.map { it.id }.toSet()
                if (storeTourneyIds.isNotEmpty()) allMatches.filter { storeTourneyIds.contains(it.tournamentId) } else allMatches
            }
            else -> allMatches
        }
    }

    val individualAnalytics = remember(selectedPlayerId, players, filteredMatches) {
        if (selectedPlayerId != 0L) {
            AnalyticsEngine.analyzePlayer(selectedPlayerId, players, filteredMatches)
        } else null
    }

    val overallAnalytics = remember(players, filteredMatches) {
        AnalyticsEngine.analyzeTournament(players, filteredMatches)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Analisa Performa Berbasis Data",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "DigiLabs Analytics • Identifikasi Kelemahan & Metagame",
                    color = DigiCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scope Filter (Single Tournament vs Store Toko vs Overall)
                Text(
                    text = "Lingkup Analisa Statistik:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val scopes = listOf(
                        Triple("CURRENT", "Turnamen Ini", activeTournament?.name?.take(18) ?: "Aktif"),
                        Triple("STORE", "Toko Penyelenggara", "Toko A / DigiLabs"),
                        Triple("ALL", "Semua Turnamen", "Global Archive")
                    )
                    scopes.forEach { (key, title, subtitle) ->
                        val isSelected = scopeFilter == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                .border(1.dp, if (isSelected) DigiCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable { scopeFilter = key }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    color = if (isSelected) DigiCyan else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 8.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Toggle Tabs (Individual vs Overall Meta)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberCardSurface,
                    contentColor = DigiCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = DigiCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) DigiCyan else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Performa Per-Orangan",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) DigiCyan else TextMuted
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) DigiCyan else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Meta Keseluruhan",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) DigiCyan else TextMuted
                                )
                            }
                        }
                    )
                }
            }
        }

        // ================= INDIVIDUAL ANALYTICS TAB =================
        if (selectedTab == 0) {
            // Player Selector Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih Pemain untuk Dianalisa:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(players) { p ->
                            val isSelected = p.id == selectedPlayerId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) DigiCyan else CyberCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPlayerId = p.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DigimonColorBadge(colorName = p.deckColor, showLabel = false)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = p.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) DigiCyan else Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (individualAnalytics != null) {
                // Key Metrics Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Win Rate Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "MATCH WIN RATE",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", individualAnalytics.matchWinRate)}%",
                                        color = if (individualAnalytics.matchWinRate >= 50.0) DigiGreen else DigiRed,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${individualAnalytics.wins}M - ${individualAnalytics.losses}K - ${individualAnalytics.draws}S",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Average Duration Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "RATA-RATA DURASI",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", individualAnalytics.avgDurationMinutes)} m",
                                        color = DigiGold,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Tempo Pertandingan",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Turn Order Performance (1st vs 2nd Turn)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = DigiCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Performa Berdasarkan Urutan Giliran (Turn Order)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Giliran Pertama (Turn 1)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Jalan Pertama (Going 1st)", color = Color.White, fontSize = 12.sp)
                                    Text(
                                        "${String.format("%.1f", individualAnalytics.firstTurnWinRate)}% (${individualAnalytics.firstTurnWins}/${individualAnalytics.firstTurnMatches} W)",
                                        color = DigiCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (individualAnalytics.firstTurnWinRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = DigiCyan,
                                    trackColor = CyberCardBorder
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Giliran Kedua (Turn 2)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Jalan Kedua (Going 2nd)", color = Color.White, fontSize = 12.sp)
                                    Text(
                                        "${String.format("%.1f", individualAnalytics.secondTurnWinRate)}% (${individualAnalytics.secondTurnWins}/${individualAnalytics.secondTurnMatches} W)",
                                        color = DigiGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (individualAnalytics.secondTurnWinRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = DigiGold,
                                    trackColor = CyberCardBorder
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // In-Depth Weakness Diagnostics & Tactical Advice
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisa Game Mendalam & Identifikasi Kelemahan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (individualAnalytics.weaknesses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCardElevated)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Data pertandingan belum cukup untuk memetakan pola kelemahan spesifik. Selesaikan lebih banyak ronde!",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            individualAnalytics.weaknesses.forEach { diag ->
                                val borderColor = when (diag.severity) {
                                    DiagnosticSeverity.HIGH -> DigiRed
                                    DiagnosticSeverity.MEDIUM -> DigiGold
                                    DiagnosticSeverity.POSITIVE -> DigiGreen
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = diag.title,
                                                color = borderColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(borderColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (diag.severity == DiagnosticSeverity.HIGH) "PRIORITAS TINGGI" else if (diag.severity == DiagnosticSeverity.MEDIUM) "PERHATIAN" else "KEUNGGULAN",
                                                    color = borderColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = diag.description,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberCardSurface)
                                                .padding(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text(
                                                    text = "Saran Taktis: ",
                                                    color = DigiCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = diag.recommendation,
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Matchup Matrix vs Archetypes & Colors
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Matriks Matchup vs Archetype Lawan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (individualAnalytics.matchups.isEmpty()) {
                                Text(
                                    text = "Belum ada riwayat tanding untuk pemain ini.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            } else {
                                individualAnalytics.matchups.forEach { m ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                DigimonColorBadge(colorName = m.opponentColor.name, showLabel = false)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = m.opponentArchetype,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = "${m.wins}W - ${m.losses}L (${String.format("%.0f", m.winRate)}%)",
                                                color = if (m.winRate >= 50.0) DigiGreen else DigiRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (m.winRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = m.opponentColor.badgeColor,
                                            trackColor = CyberCardBorder
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= OVERALL META ANALYTICS TAB =================
        if (selectedTab == 1) {
            // Tournament Meta Share
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = DigiCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Distribusi Meta Deck (Meta Share)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        overallAnalytics.metaDistribution.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DigimonColorBadge(colorName = item.color.name, showLabel = false)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.archetype,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "${item.count} Pemain (${String.format("%.1f", item.sharePercent)}%)",
                                        color = DigiCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (item.sharePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = item.color.badgeColor,
                                    trackColor = CyberCardBorder
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Color Win Rates in Tournament
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Win Rate Berdasarkan Warna Digimon TCG",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        overallAnalytics.colorPerformance.forEach { stat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DigimonColorBadge(colorName = stat.color.name)
                                    Text(
                                        text = "${String.format("%.1f", stat.winRate)}% (${stat.wins}/${stat.totalGames} Games)",
                                        color = stat.color.badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (stat.winRate / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = stat.color.badgeColor,
                                    trackColor = CyberCardBorder
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Match Duration & Pace Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisa Tempo & Durasi Turnamen",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Rata-rata Durasi:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "${String.format("%.1f", overallAnalytics.avgMatchDurationMinutes)} Menit",
                                    color = DigiGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Match Tercepat:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "${overallAnalytics.fastestMatchMinutes} Menit",
                                    color = DigiGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Match Terlama:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "${overallAnalytics.longestMatchMinutes} Menit",
                                    color = DigiRed,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pace Breakdown bar
                        Text(
                            text = "Distribusi Kecepatan Game:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            if (overallAnalytics.blitzPacePercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((overallAnalytics.blitzPacePercent.toFloat()).coerceAtLeast(1f))
                                        .fillMaxSize()
                                        .background(DigiGreen)
                                )
                            }
                            if (overallAnalytics.standardPacePercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((overallAnalytics.standardPacePercent.toFloat()).coerceAtLeast(1f))
                                        .fillMaxSize()
                                        .background(DigiCyan)
                                )
                            }
                            if (overallAnalytics.grindPacePercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((overallAnalytics.grindPacePercent.toFloat()).coerceAtLeast(1f))
                                        .fillMaxSize()
                                        .background(DigiGold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "⚡ Blitz (<20m): ${String.format("%.0f", overallAnalytics.blitzPacePercent)}%",
                                color = DigiGreen,
                                fontSize = 10.sp
                            )
                            Text(
                                "⚔️ Standar (20-35m): ${String.format("%.0f", overallAnalytics.standardPacePercent)}%",
                                color = DigiCyan,
                                fontSize = 10.sp
                            )
                            Text(
                                "🛡️ Grind (>35m): ${String.format("%.0f", overallAnalytics.grindPacePercent)}%",
                                color = DigiGold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
