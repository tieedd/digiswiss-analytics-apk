package com.example.util

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.DigimonColor

data class MatchupRecord(
    val opponentColor: DigimonColor,
    val opponentArchetype: String,
    val totalMatches: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double
)

data class ColorStat(
    val color: DigimonColor,
    val totalGames: Int,
    val wins: Int,
    val winRate: Double
)

data class MetaShare(
    val archetype: String,
    val color: DigimonColor,
    val count: Int,
    val sharePercent: Double
)

data class WeaknessDiagnostic(
    val title: String,
    val severity: DiagnosticSeverity,
    val description: String,
    val recommendation: String
)

enum class DiagnosticSeverity {
    HIGH, MEDIUM, POSITIVE
}

data class IndividualPlayerAnalytics(
    val player: PlayerEntity,
    val totalMatches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val matchWinRate: Double,
    val gameWinRate: Double,
    val avgDurationMinutes: Double,
    val firstTurnMatches: Int,
    val firstTurnWins: Int,
    val firstTurnWinRate: Double,
    val secondTurnMatches: Int,
    val secondTurnWins: Int,
    val secondTurnWinRate: Double,
    val blitzGamesCount: Int, // < 20 min
    val standardGamesCount: Int, // 20-35 min
    val grindGamesCount: Int, // > 35 min
    val matchups: List<MatchupRecord>,
    val weaknesses: List<WeaknessDiagnostic>
)

data class OverallTournamentAnalytics(
    val totalPlayers: Int,
    val totalMatches: Int,
    val reportedMatches: Int,
    val avgMatchDurationMinutes: Double,
    val metaDistribution: List<MetaShare>,
    val colorPerformance: List<ColorStat>,
    val blitzPacePercent: Double,
    val standardPacePercent: Double,
    val grindPacePercent: Double,
    val fastestMatchMinutes: Int,
    val longestMatchMinutes: Int,
    val topDeckArchetype: String,
    val mostDominantColor: DigimonColor
)

object AnalyticsEngine {

    fun analyzePlayer(
        playerId: Long,
        players: List<PlayerEntity>,
        allMatches: List<MatchEntity>
    ): IndividualPlayerAnalytics? {
        val player = players.find { it.id == playerId } ?: return null
        val playerMap = players.associateBy { it.id }

        val playerMatches = allMatches.filter {
            it.isReported && (it.player1Id == playerId || it.player2Id == playerId)
        }

        var wins = 0
        var losses = 0
        var draws = 0
        var gamesWon = 0
        var gamesPlayed = 0
        var totalDuration = 0
        var durationCount = 0

        var firstTurnCount = 0
        var firstTurnWinCount = 0
        var secondTurnCount = 0
        var secondTurnWinCount = 0

        var blitzCount = 0
        var standardCount = 0
        var grindCount = 0

        val matchupMap = mutableMapOf<Pair<DigimonColor, String>, Pair<Int, Int>>() // (Color, Arch) -> (Wins, Losses)

        for (m in playerMatches) {
            val isP1 = m.player1Id == playerId
            val oppId = if (isP1) m.player2Id else m.player1Id
            val opp = oppId?.let { playerMap[it] }

            // Get deck info for this specific match
            val oppArchetype = if (isP1) m.p2DeckArchetype else m.p1DeckArchetype
            val oppColorStr = if (isP1) m.p2DeckColor else m.p1DeckColor

            val myGames = if (isP1) m.p1Score else m.p2Score
            val oppGames = if (isP1) m.p2Score else m.p1Score
            gamesWon += myGames
            gamesPlayed += (myGames + oppGames)

            if (m.matchDurationMinutes > 0) {
                totalDuration += m.matchDurationMinutes
                durationCount++
                when {
                    m.matchDurationMinutes < 20 -> blitzCount++
                    m.matchDurationMinutes <= 35 -> standardCount++
                    else -> grindCount++
                }
            }

            val isMyWin = m.winnerId == playerId
            if (m.isDraw) {
                draws++
            } else if (isMyWin) {
                wins++
            } else {
                losses++
            }

            // Turn 1 vs Turn 2
            if (m.firstTurnPlayerId != null) {
                val tookFirstTurn = m.firstTurnPlayerId == playerId
                if (tookFirstTurn) {
                    firstTurnCount++
                    if (isMyWin) firstTurnWinCount++
                } else {
                    secondTurnCount++
                    if (isMyWin) secondTurnWinCount++
                }
            }

            // Matchup record
            if (opp != null) {
                val primaryColor = oppColorStr.split(",").firstOrNull()?.trim() ?: "UNKNOWN"
                val oppColor = DigimonColor.fromString(primaryColor)
                val arch = oppArchetype.ifEmpty { "Unknown" }
                val key = Pair(oppColor, arch)
                val current = matchupMap[key] ?: Pair(0, 0)
                val newWins = if (isMyWin) current.first + 1 else current.first
                val newLosses = if (!isMyWin && !m.isDraw) current.second + 1 else current.second
                matchupMap[key] = Pair(newWins, newLosses)
            }
        }

        val totalMatches = wins + losses + draws
        val matchWinRate = if (totalMatches > 0) (wins.toDouble() / totalMatches.toDouble()) * 100.0 else 0.0
        val gameWinRate = if (gamesPlayed > 0) (gamesWon.toDouble() / gamesPlayed.toDouble()) * 100.0 else 0.0
        val avgDuration = if (durationCount > 0) totalDuration.toDouble() / durationCount.toDouble() else 0.0

        val firstTurnRate = if (firstTurnCount > 0) (firstTurnWinCount.toDouble() / firstTurnCount.toDouble()) * 100.0 else 0.0
        val secondTurnRate = if (secondTurnCount > 0) (secondTurnWinCount.toDouble() / secondTurnCount.toDouble()) * 100.0 else 0.0

        val matchupList = matchupMap.map { (key, record) ->
            val total = record.first + record.second
            val rate = if (total > 0) (record.first.toDouble() / total.toDouble()) * 100.0 else 0.0
            MatchupRecord(
                opponentColor = key.first,
                opponentArchetype = key.second,
                totalMatches = total,
                wins = record.first,
                losses = record.second,
                winRate = rate
            )
        }.sortedByDescending { it.totalMatches }

        // Generate weakness diagnostics
        val diagnostics = mutableListOf<WeaknessDiagnostic>()

        if (secondTurnCount > 0 && secondTurnRate < 45.0) {
            diagnostics.add(
                WeaknessDiagnostic(
                    title = "Kelemahan Giliran Kedua (Going 2nd Choke)",
                    severity = DiagnosticSeverity.HIGH,
                    description = "Winrate saat giliran kedua hanya ${String.format("%.1f", secondTurnRate)}% (berbanding ${String.format("%.1f", firstTurnRate)}% saat giliran pertama).",
                    recommendation = "Siapkan kartu rookie 1-cost atau kartu Option removal cepat (misal: Memory Boost / Training) untuk mengatasi memory pass rendah lawan di Turn 1."
                )
            )
        } else if (firstTurnRate >= 70.0) {
            diagnostics.add(
                WeaknessDiagnostic(
                    title = "Dominasi Giliran Pertama (High 1st Turn Tempo)",
                    severity = DiagnosticSeverity.POSITIVE,
                    description = "Memiliki winrate impresif ${String.format("%.1f", firstTurnRate)}% saat membuka permainan.",
                    recommendation = "Pertahankan konsistensi raising area dan breeding line tanpa terburu-buru keluar ke battle area."
                )
            )
        }

        if (grindCount > 0 && avgDuration > 32.0) {
            diagnostics.add(
                WeaknessDiagnostic(
                    title = "Kerentanan Pertandingan Lama (>32 Menit)",
                    severity = DiagnosticSeverity.MEDIUM,
                    description = "Rata-rata durasi match mencapai ${String.format("%.1f", avgDuration)} menit dengan beberapa game mendekati time limit.",
                    recommendation = "Tingkatkan kartu finisher (seperti ACE Digimon atau kartu hybrid) untuk menutup security lawan lebih awal sebelum lawan menstabilkan board."
                )
            )
        }

        val worstMatchup = matchupList.filter { it.totalMatches >= 1 && it.winRate < 50.0 }.minByOrNull { it.winRate }
        if (worstMatchup != null) {
            diagnostics.add(
                WeaknessDiagnostic(
                    title = "Matchup Buruk vs Deck ${worstMatchup.opponentColor.displayName}",
                    severity = DiagnosticSeverity.HIGH,
                    description = "Rekor ${worstMatchup.wins}W - ${worstMatchup.losses}L melawan ${worstMatchup.opponentArchetype} (${String.format("%.1f", worstMatchup.winRate)}% WR).",
                    recommendation = "Tambahkan kartu tech spesifik anti-meta untuk menangkal strategi ${worstMatchup.opponentArchetype} (misal: De-Digivolve, DP Reduction, atau Tamer blocker)."
                )
            )
        } else if (matchupList.isNotEmpty()) {
            val bestMatchup = matchupList.maxByOrNull { it.winRate }
            if (bestMatchup != null && bestMatchup.winRate >= 75.0) {
                diagnostics.add(
                    WeaknessDiagnostic(
                        title = "Keunggulan Mutlak vs ${bestMatchup.opponentColor.displayName}",
                        severity = DiagnosticSeverity.POSITIVE,
                        description = "Sangat unggul melawan ${bestMatchup.opponentArchetype} dengan winrate ${String.format("%.1f", bestMatchup.winRate)}%.",
                        recommendation = "Strategi dan alur tempo Anda sudah sangat optimal melawan gaya main archetype ini."
                    )
                )
            }
        }

        return IndividualPlayerAnalytics(
            player = player,
            totalMatches = totalMatches,
            wins = wins,
            losses = losses,
            draws = draws,
            matchWinRate = matchWinRate,
            gameWinRate = gameWinRate,
            avgDurationMinutes = avgDuration,
            firstTurnMatches = firstTurnCount,
            firstTurnWins = firstTurnWinCount,
            firstTurnWinRate = firstTurnRate,
            secondTurnMatches = secondTurnCount,
            secondTurnWins = secondTurnWinCount,
            secondTurnWinRate = secondTurnRate,
            blitzGamesCount = blitzCount,
            standardGamesCount = standardCount,
            grindGamesCount = grindCount,
            matchups = matchupList,
            weaknesses = diagnostics
        )
    }

    fun analyzeTournament(
        players: List<PlayerEntity>,
        matches: List<MatchEntity>
    ): OverallTournamentAnalytics {
        val totalPlayers = players.size
        val reported = matches.filter { it.isReported }
        val playerMap = players.associateBy { it.id }

        // Duration analysis
        val durations = reported.filter { it.matchDurationMinutes > 0 }.map { it.matchDurationMinutes }
        val avgDuration = if (durations.isNotEmpty()) durations.average() else 0.0
        val fastest = durations.minOrNull() ?: 0
        val longest = durations.maxOrNull() ?: 0

        var blitz = 0
        var standard = 0
        var grind = 0
        for (d in durations) {
            when {
                d < 20 -> blitz++
                d <= 35 -> standard++
                else -> grind++
            }
        }
        val totalDurCount = durations.size.toDouble().coerceAtLeast(1.0)
        val blitzPct = (blitz / totalDurCount) * 100.0
        val stdPct = (standard / totalDurCount) * 100.0
        val grindPct = (grind / totalDurCount) * 100.0

        // Meta deck distribution
        val metaCountMap = mutableMapOf<Pair<String, DigimonColor>, Int>()
        
        // We'll calculate meta share based on the latest player entity since this is for tournament-level summary
        // For accurate per-match meta share, it would require aggregating distinct (archetype, color) from matches
        for (p in players) {
            val primaryColor = p.deckColor.split(",").firstOrNull()?.trim() ?: "UNKNOWN"
            val color = DigimonColor.fromString(primaryColor)
            val key = Pair(p.deckArchetype, color)
            metaCountMap[key] = (metaCountMap[key] ?: 0) + 1
        }
        val metaList = metaCountMap.map { (key, count) ->
            MetaShare(
                archetype = key.first,
                color = key.second,
                count = count,
                sharePercent = if (totalPlayers > 0) (count.toDouble() / totalPlayers.toDouble()) * 100.0 else 0.0
            )
        }.sortedByDescending { it.count }

        // Color Win Rates across reported matches
        val colorTotalGames = mutableMapOf<DigimonColor, Int>().withDefault { 0 }
        val colorWonGames = mutableMapOf<DigimonColor, Int>().withDefault { 0 }

        for (m in reported) {
            val p1ColorStr = m.p1DeckColor.ifEmpty { playerMap[m.player1Id]?.deckColor ?: "UNKNOWN" }
            val p2ColorStr = m.p2DeckColor.ifEmpty { m.player2Id?.let { playerMap[it]?.deckColor } ?: "UNKNOWN" }

            val primaryC1 = p1ColorStr.split(",").firstOrNull()?.trim() ?: "UNKNOWN"
            val c1 = DigimonColor.fromString(primaryC1)
            colorTotalGames[c1] = colorTotalGames.getValue(c1) + m.p1Score + m.p2Score
            colorWonGames[c1] = colorWonGames.getValue(c1) + m.p1Score

            if (m.player2Id != null) {
                val primaryC2 = p2ColorStr.split(",").firstOrNull()?.trim() ?: "UNKNOWN"
                val c2 = DigimonColor.fromString(primaryC2)
                colorTotalGames[c2] = colorTotalGames.getValue(c2) + m.p1Score + m.p2Score
                colorWonGames[c2] = colorWonGames.getValue(c2) + m.p2Score
            }
        }

        val colorStats = DigimonColor.entries.map { color ->
            val total = colorTotalGames.getValue(color)
            val wins = colorWonGames.getValue(color)
            val wr = if (total > 0) (wins.toDouble() / total.toDouble()) * 100.0 else 0.0
            ColorStat(color = color, totalGames = total, wins = wins, winRate = wr)
        }.filter { it.totalGames > 0 }.sortedByDescending { it.winRate }

        val dominantColor = colorStats.firstOrNull()?.color ?: DigimonColor.RED
        val topArchetype = metaList.firstOrNull()?.archetype ?: "Fenriloogamon OTK"

        return OverallTournamentAnalytics(
            totalPlayers = totalPlayers,
            totalMatches = matches.size,
            reportedMatches = reported.size,
            avgMatchDurationMinutes = avgDuration,
            metaDistribution = metaList,
            colorPerformance = colorStats,
            blitzPacePercent = blitzPct,
            standardPacePercent = stdPct,
            grindPacePercent = grindPct,
            fastestMatchMinutes = fastest,
            longestMatchMinutes = longest,
            topDeckArchetype = topArchetype,
            mostDominantColor = dominantColor
        )
    }
}
