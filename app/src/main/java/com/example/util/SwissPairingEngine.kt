package com.example.util

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import kotlin.math.max

data class StandingEntry(
    val rank: Int,
    val player: PlayerEntity,
    val matchPoints: Int,
    val matchesWon: Int,
    val matchesLost: Int,
    val matchesDrawn: Int,
    val omwPercent: Double,
    val gwPercent: Double,
    val ogwPercent: Double,
    val byesCount: Int
)

data class MatchWithPlayers(
    val match: MatchEntity,
    val player1: PlayerEntity,
    val player2: PlayerEntity?
)

object SwissPairingEngine {

    /**
     * Compute current tournament standings based on official Bandai / TCG Swiss tiebreakers.
     */
    fun computeStandings(
        players: List<PlayerEntity>,
        allMatches: List<MatchEntity>
    ): List<StandingEntry> {
        val playerMap = players.associateBy { it.id }
        val reportedMatches = allMatches.filter { it.isReported }

        // Aggregate player stats
        val matchWins = mutableMapOf<Long, Int>().withDefault { 0 }
        val matchLosses = mutableMapOf<Long, Int>().withDefault { 0 }
        val matchDraws = mutableMapOf<Long, Int>().withDefault { 0 }
        val byes = mutableMapOf<Long, Int>().withDefault { 0 }
        val gamesWon = mutableMapOf<Long, Int>().withDefault { 0 }
        val gamesLost = mutableMapOf<Long, Int>().withDefault { 0 }
        val opponents = mutableMapOf<Long, MutableList<Long>>().withDefault { mutableListOf() }

        for (match in reportedMatches) {
            val p1 = match.player1Id
            val p2 = match.player2Id

            if (match.isBye || p2 == null) {
                matchWins[p1] = matchWins.getValue(p1) + 1
                byes[p1] = byes.getValue(p1) + 1
                gamesWon[p1] = gamesWon.getValue(p1) + 2
            } else {
                opponents.getOrPut(p1) { mutableListOf() }.add(p2)
                opponents.getOrPut(p2) { mutableListOf() }.add(p1)

                gamesWon[p1] = gamesWon.getValue(p1) + match.p1Score
                gamesLost[p1] = gamesLost.getValue(p1) + match.p2Score

                gamesWon[p2] = gamesWon.getValue(p2) + match.p2Score
                gamesLost[p2] = gamesLost.getValue(p2) + match.p1Score

                if (match.isDraw) {
                    matchDraws[p1] = matchDraws.getValue(p1) + 1
                    matchDraws[p2] = matchDraws.getValue(p2) + 1
                } else if (match.winnerId == p1) {
                    matchWins[p1] = matchWins.getValue(p1) + 1
                    matchLosses[p2] = matchLosses.getValue(p2) + 1
                } else if (match.winnerId == p2) {
                    matchWins[p2] = matchWins.getValue(p2) + 1
                    matchLosses[p1] = matchLosses.getValue(p1) + 1
                }
            }
        }

        // Match Win Percentage for each player (min 33% floor)
        val matchWinPercent = mutableMapOf<Long, Double>()
        val gameWinPercent = mutableMapOf<Long, Double>()

        for (p in players) {
            val wins = matchWins.getValue(p.id)
            val draws = matchDraws.getValue(p.id)
            val losses = matchLosses.getValue(p.id)
            val totalMatches = wins + draws + losses
            val points = (wins * 3) + (draws * 1)

            val rawMwp = if (totalMatches > 0) points.toDouble() / (totalMatches * 3.0) else 0.33
            matchWinPercent[p.id] = max(0.33, rawMwp)

            val gWon = gamesWon.getValue(p.id)
            val gLost = gamesLost.getValue(p.id)
            val totalGames = gWon + gLost
            val rawGwp = if (totalGames > 0) gWon.toDouble() / totalGames.toDouble() else 0.33
            gameWinPercent[p.id] = max(0.33, rawGwp)
        }

        // OMW% and OGW%
        val omwPercent = mutableMapOf<Long, Double>()
        val ogwPercent = mutableMapOf<Long, Double>()

        for (p in players) {
            val oppList = opponents[p.id] ?: emptyList()
            if (oppList.isEmpty()) {
                omwPercent[p.id] = 0.33
                ogwPercent[p.id] = 0.33
            } else {
                val sumOmw = oppList.sumOf { matchWinPercent[it] ?: 0.33 }
                omwPercent[p.id] = sumOmw / oppList.size.toDouble()

                val sumOgw = oppList.sumOf { gameWinPercent[it] ?: 0.33 }
                ogwPercent[p.id] = sumOgw / oppList.size.toDouble()
            }
        }

        // Build list and sort
        val preliminary = players.map { p ->
            val wins = matchWins.getValue(p.id)
            val draws = matchDraws.getValue(p.id)
            val losses = matchLosses.getValue(p.id)
            val pts = (wins * 3) + (draws * 1)
            val omw = omwPercent[p.id] ?: 0.33
            val gw = gameWinPercent[p.id] ?: 0.33
            val ogw = ogwPercent[p.id] ?: 0.33

            StandingEntry(
                rank = 0,
                player = p,
                matchPoints = pts,
                matchesWon = wins,
                matchesLost = losses,
                matchesDrawn = draws,
                omwPercent = omw * 100.0,
                gwPercent = gw * 100.0,
                ogwPercent = ogw * 100.0,
                byesCount = byes.getValue(p.id)
            )
        }

        val sorted = preliminary.sortedWith(
            compareByDescending<StandingEntry> { it.matchPoints }
                .thenByDescending { it.omwPercent }
                .thenByDescending { it.gwPercent }
                .thenByDescending { it.ogwPercent }
                .thenBy { it.player.name }
        )

        return sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    /**
     * Generate Swiss pairings for the next round.
     * Prevents previous rematches and handles byes gracefully.
     */
    fun generateNextRoundPairings(
        tournamentId: Long,
        roundNumber: Int,
        players: List<PlayerEntity>,
        pastMatches: List<MatchEntity>
    ): List<MatchEntity> {
        if (players.isEmpty()) return emptyList()

        // Track past pairings
        val playedPairs = mutableSetOf<Pair<Long, Long>>()
        val playersWithByes = mutableSetOf<Long>()

        for (m in pastMatches) {
            val p1 = m.player1Id
            val p2 = m.player2Id
            if (p2 != null) {
                playedPairs.add(Pair(p1, p2))
                playedPairs.add(Pair(p2, p1))
            } else if (m.isBye) {
                playersWithByes.add(p1)
            }
        }

        // ROUND 1: Strictly random pairings for tournament start
        if (roundNumber == 1 || pastMatches.isEmpty()) {
            val shuffledPool = players.shuffled().toMutableList()
            val generatedMatches = mutableListOf<MatchEntity>()
            var tableNum = 1

            // Handle BYE if odd number of players - pick randomly
            if (shuffledPool.size % 2 != 0) {
                val byeCandidate = shuffledPool.removeAt(shuffledPool.indices.random())
                generatedMatches.add(
                    MatchEntity(
                        tournamentId = tournamentId,
                        roundNumber = roundNumber,
                        tableNumber = 0, // table 0 is BYE
                        player1Id = byeCandidate.id,
                        player2Id = null,
                        p1DeckArchetype = if (byeCandidate.deckArchetype != "Unknown") byeCandidate.deckArchetype else "",
                        p1DeckColor = if (byeCandidate.deckColor != "UNKNOWN") byeCandidate.deckColor else "",
                        p1Score = 2,
                        p2Score = 0,
                        isDraw = false,
                        isBye = true,
                        winnerId = byeCandidate.id,
                        isReported = true,
                        matchDurationMinutes = 0
                    )
                )
            }

            // Pair remaining players 2 by 2 purely at random
            while (shuffledPool.size >= 2) {
                val p1 = shuffledPool.removeAt(0)
                val p2 = shuffledPool.removeAt(0)
                val (first, second) = if (kotlin.random.Random.nextBoolean()) Pair(p1, p2) else Pair(p2, p1)
                generatedMatches.add(
                    MatchEntity(
                        tournamentId = tournamentId,
                        roundNumber = roundNumber,
                        tableNumber = tableNum++,
                        player1Id = first.id,
                        player2Id = second.id,
                        p1DeckArchetype = if (first.deckArchetype != "Unknown") first.deckArchetype else "",
                        p1DeckColor = if (first.deckColor != "UNKNOWN") first.deckColor else "",
                        p2DeckArchetype = if (second.deckArchetype != "Unknown") second.deckArchetype else "",
                        p2DeckColor = if (second.deckColor != "UNKNOWN") second.deckColor else "",
                        p1Score = 0,
                        p2Score = 0,
                        isDraw = false,
                        isBye = false,
                        winnerId = null,
                        isReported = false,
                        matchDurationMinutes = 0
                    )
                )
            }

            return generatedMatches.sortedBy { if (it.isBye) 9999 else it.tableNumber }
        }

        // ROUND 2+: Swiss system with randomized matching within identical score brackets
        val standings = computeStandings(players, pastMatches)

        // Group players by match points, sorted descending (highest points first)
        val brackets = standings.groupBy { it.matchPoints }
            .toSortedMap(compareByDescending { it })

        // Build playerList by taking each bracket and shuffling it internally
        val playerList = mutableListOf<PlayerEntity>()
        for ((_, entries) in brackets) {
            playerList.addAll(entries.shuffled().map { it.player })
        }

        val generatedMatches = mutableListOf<MatchEntity>()
        var tableNum = 1

        // Handle BYE if odd number of players
        if (playerList.size % 2 != 0) {
            // Pick lowest ranked player without a BYE, randomize among eligible candidates with the lowest points
            val byeCandidate = playerList.reversed().firstOrNull { it.id !in playersWithByes }
                ?: playerList.last()

            playerList.remove(byeCandidate)
            generatedMatches.add(
                MatchEntity(
                    tournamentId = tournamentId,
                    roundNumber = roundNumber,
                    tableNumber = 0, // table 0 is BYE
                    player1Id = byeCandidate.id,
                    player2Id = null,
                    p1DeckArchetype = if (byeCandidate.deckArchetype != "Unknown") byeCandidate.deckArchetype else "",
                    p1DeckColor = if (byeCandidate.deckColor != "UNKNOWN") byeCandidate.deckColor else "",
                    p1Score = 2,
                    p2Score = 0,
                    isDraw = false,
                    isBye = true,
                    winnerId = byeCandidate.id,
                    isReported = true,
                    matchDurationMinutes = 0
                )
            )
        }

        // Swiss Matching with rematch avoidance using backtracking
        var finalPairs = mutableListOf<Pair<PlayerEntity, PlayerEntity>>()
        
        fun findValidPairing(
            remaining: List<PlayerEntity>,
            currentPairings: List<Pair<PlayerEntity, PlayerEntity>>
        ): List<Pair<PlayerEntity, PlayerEntity>>? {
            if (remaining.isEmpty()) return currentPairings

            val p1 = remaining[0]
            val candidates = remaining.drop(1)

            for (i in candidates.indices) {
                val p2 = candidates[i]
                if (Pair(p1.id, p2.id) !in playedPairs) {
                    val nextRemaining = candidates.toMutableList().apply { removeAt(i) }
                    val nextPairings = currentPairings + Pair(p1, p2)
                    val result = findValidPairing(nextRemaining, nextPairings)
                    if (result != null) return result
                }
            }
            return null
        }

        val backtrackResult = findValidPairing(playerList, emptyList())
        if (backtrackResult != null) {
            finalPairs.addAll(backtrackResult)
        } else {
            // Fallback greedy if absolutely impossible to prevent rematch
            val unassigned = playerList.toMutableList()
            while (unassigned.isNotEmpty()) {
                val p1 = unassigned.removeAt(0)
                var partnerIndex = -1

                for (i in unassigned.indices) {
                    val candidate = unassigned[i]
                    if (Pair(p1.id, candidate.id) !in playedPairs) {
                        partnerIndex = i
                        break
                    }
                }

                if (partnerIndex == -1 && unassigned.isNotEmpty()) {
                    partnerIndex = 0
                }

                if (partnerIndex != -1) {
                    val p2 = unassigned.removeAt(partnerIndex)
                    finalPairs.add(Pair(p1, p2))
                }
            }
        }

        for (pair in finalPairs) {
            val p1 = pair.first
            val p2 = pair.second
            val (first, second) = if (kotlin.random.Random.nextBoolean()) Pair(p1, p2) else Pair(p2, p1)
            generatedMatches.add(
                MatchEntity(
                    tournamentId = tournamentId,
                    roundNumber = roundNumber,
                    tableNumber = tableNum++,
                    player1Id = first.id,
                    player2Id = second.id,
                    p1DeckArchetype = if (first.deckArchetype != "Unknown") first.deckArchetype else "",
                    p1DeckColor = if (first.deckColor != "UNKNOWN") first.deckColor else "",
                    p2DeckArchetype = if (second.deckArchetype != "Unknown") second.deckArchetype else "",
                    p2DeckColor = if (second.deckColor != "UNKNOWN") second.deckColor else "",
                    p1Score = 0,
                    p2Score = 0,
                    isDraw = false,
                    isBye = false,
                    winnerId = null,
                    isReported = false,
                    matchDurationMinutes = 0
                )
            )
        }

        return generatedMatches.sortedBy { if (it.isBye) 9999 else it.tableNumber }
    }
}
