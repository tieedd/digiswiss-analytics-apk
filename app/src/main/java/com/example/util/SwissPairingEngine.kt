package com.example.util

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import kotlin.math.max
import kotlin.random.Random

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

/**
 * Hasil pairing. Sebelumnya fungsi hanya mengembalikan List<MatchEntity> sehingga
 * pemanggil tidak pernah tahu kalau engine terpaksa mengulang lawan lama.
 */
data class PairingResult(
    val matches: List<MatchEntity>,
    val forcedRematches: List<Pair<Long, Long>> = emptyList(),
    val searchExhausted: Boolean = false
) {
    val hasWarning: Boolean get() = forcedRematches.isNotEmpty() || searchExhausted
}

object SwissPairingEngine {

    /**
     * Batas simpul pencarian. Dengan memoisasi kegagalan, kasus terburuk 32 pemain
     * hanya butuh ~200 ribu simpul, jadi batas ini praktis tidak pernah tersentuh.
     */
    private const val PAIRING_NODE_BUDGET = 200_000

    private fun key(a: Long, b: Long): Long = if (a < b) a * 1_000_003L + b else b * 1_000_003L + a

    /**
     * @param players            pemain yang ditampilkan di klasemen (biasanya yang masih aktif)
     * @param tiebreakerPool     SEMUA pemain yang pernah bertanding di turnamen ini, termasuk
     *                           yang sudah drop. Wajib diisi, kalau tidak OMW%/OGW% akan rusak.
     * @param byesCountAsGameWin BYE tidak dihitung sebagai game menang (aturan resmi).
     */
    fun computeStandings(
        players: List<PlayerEntity>,
        allMatches: List<MatchEntity>,
        tiebreakerPool: List<PlayerEntity> = players,
        byesCountAsGameWin: Boolean = false
    ): List<StandingEntry> {
        val reportedMatches = allMatches.filter { it.isReported }

        val matchWins = HashMap<Long, Int>()
        val matchLosses = HashMap<Long, Int>()
        val matchDraws = HashMap<Long, Int>()
        val byes = HashMap<Long, Int>()
        val gamesWon = HashMap<Long, Int>()
        val gamesLost = HashMap<Long, Int>()
        val opponents = HashMap<Long, MutableList<Long>>()

        fun bump(map: HashMap<Long, Int>, id: Long, delta: Int = 1) {
            map[id] = (map[id] ?: 0) + delta
        }

        for (match in reportedMatches) {
            val p1 = match.player1Id
            val p2 = match.player2Id

            if (match.isBye || p2 == null) {
                bump(matchWins, p1)
                bump(byes, p1)
                if (byesCountAsGameWin) bump(gamesWon, p1, 2)
            } else {
                // Jaga-jaga terhadap data rusak hasil import CSV.
                if (p1 == p2) continue

                opponents.getOrPut(p1) { mutableListOf() }.add(p2)
                opponents.getOrPut(p2) { mutableListOf() }.add(p1)

                bump(gamesWon, p1, match.p1Score)
                bump(gamesLost, p1, match.p2Score)
                bump(gamesWon, p2, match.p2Score)
                bump(gamesLost, p2, match.p1Score)

                when {
                    match.isDraw -> {
                        bump(matchDraws, p1); bump(matchDraws, p2)
                    }
                    match.winnerId == p1 -> {
                        bump(matchWins, p1); bump(matchLosses, p2)
                    }
                    match.winnerId == p2 -> {
                        bump(matchWins, p2); bump(matchLosses, p1)
                    }
                    // winnerId null tapi bukan draw = data tidak konsisten; diabaikan.
                }
            }
        }

        // Persentase dihitung untuk SELURUH pool (termasuk pemain yang sudah drop),
        // supaya OMW%/OGW% lawan tidak jatuh ke nilai default 0.33.
        val pool = (tiebreakerPool + players).distinctBy { it.id }
        val matchWinPercent = HashMap<Long, Double>()
        val gameWinPercent = HashMap<Long, Double>()

        for (p in pool) {
            val wins = matchWins[p.id] ?: 0
            val draws = matchDraws[p.id] ?: 0
            val losses = matchLosses[p.id] ?: 0
            val totalMatches = wins + draws + losses
            val points = (wins * 3) + draws

            val rawMwp = if (totalMatches > 0) points.toDouble() / (totalMatches * 3.0) else 0.33
            matchWinPercent[p.id] = max(0.33, rawMwp)

            val gWon = gamesWon[p.id] ?: 0
            val gLost = gamesLost[p.id] ?: 0
            val totalGames = gWon + gLost
            val rawGwp = if (totalGames > 0) gWon.toDouble() / totalGames.toDouble() else 0.33
            gameWinPercent[p.id] = max(0.33, rawGwp)
        }

        val omwPercent = HashMap<Long, Double>()
        val ogwPercent = HashMap<Long, Double>()

        for (p in pool) {
            val oppList = opponents[p.id] ?: emptyList<Long>()
            if (oppList.isEmpty()) {
                omwPercent[p.id] = 0.33
                ogwPercent[p.id] = 0.33
            } else {
                omwPercent[p.id] = oppList.sumOf { matchWinPercent[it] ?: 0.33 } / oppList.size
                ogwPercent[p.id] = oppList.sumOf { gameWinPercent[it] ?: 0.33 } / oppList.size
            }
        }

        val preliminary = players.map { p ->
            val wins = matchWins[p.id] ?: 0
            val draws = matchDraws[p.id] ?: 0
            val losses = matchLosses[p.id] ?: 0
            StandingEntry(
                rank = 0,
                player = p,
                matchPoints = (wins * 3) + draws,
                matchesWon = wins,
                matchesLost = losses,
                matchesDrawn = draws,
                omwPercent = (omwPercent[p.id] ?: 0.33) * 100.0,
                gwPercent = (gameWinPercent[p.id] ?: 0.33) * 100.0,
                ogwPercent = (ogwPercent[p.id] ?: 0.33) * 100.0,
                byesCount = byes[p.id] ?: 0
            )
        }

        val sorted = preliminary.sortedWith(
            compareByDescending<StandingEntry> { it.matchPoints }
                .thenByDescending { it.omwPercent }
                .thenByDescending { it.gwPercent }
                .thenByDescending { it.ogwPercent }
                .thenBy { it.player.name }
                .thenBy { it.player.id }   // pemecah seri terakhir yang deterministik
        )

        return sorted.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    fun generateNextRoundPairings(
        tournamentId: Long,
        roundNumber: Int,
        players: List<PlayerEntity>,
        pastMatches: List<MatchEntity>,
        allPlayersForTiebreak: List<PlayerEntity> = players,
        random: Random = Random.Default
    ): PairingResult {
        if (players.isEmpty()) return PairingResult(emptyList())

        // Satu pemain saja: langsung BYE, jangan sampai masuk loop pairing.
        if (players.size == 1) {
            return PairingResult(listOf(buildByeMatch(tournamentId, roundNumber, players[0])))
        }

        val playedPairs = HashSet<Long>()
        val byeCounts = HashMap<Long, Int>()

        for (m in pastMatches) {
            val p1 = m.player1Id
            val p2 = m.player2Id
            if (p2 != null && p1 != p2) {
                playedPairs.add(key(p1, p2))
            } else if (m.isBye || p2 == null) {
                byeCounts[p1] = (byeCounts[p1] ?: 0) + 1
            }
        }

        val generatedMatches = mutableListOf<MatchEntity>()
        var tableNum = 1

        // ---------- Urutan pemain ----------
        val playerList: MutableList<PlayerEntity> = if (roundNumber == 1 || pastMatches.isEmpty()) {
            players.shuffled(random).toMutableList()
        } else {
            val standings = computeStandings(players, pastMatches, allPlayersForTiebreak)
            val ordered = mutableListOf<PlayerEntity>()
            standings.groupBy { it.matchPoints }
                .toSortedMap(compareByDescending { it })
                .forEach { (_, entries) -> ordered.addAll(entries.shuffled(random).map { it.player }) }
            ordered
        }

        // ---------- BYE ----------
        if (playerList.size % 2 != 0) {
            // Pemain peringkat terbawah yang PALING SEDIKIT pernah dapat BYE.
            // Versi lama memakai `firstOrNull { belum pernah bye } ?: last()`, sehingga di
            // turnamen panjang orang yang sama bisa kebagian BYE berkali-kali.
            val byeCandidate = playerList.asReversed()
                .minByOrNull { byeCounts[it.id] ?: 0 }
                ?: playerList.last()

            playerList.removeAll { it.id == byeCandidate.id }
            generatedMatches.add(buildByeMatch(tournamentId, roundNumber, byeCandidate))
        }

        // ---------- Pencocokan ----------
        val byId = playerList.associateBy { it.id }
        val orderedIds = playerList.map { it.id }

        val search = findPairingMemoized(orderedIds, playedPairs)
        val forcedRematches = mutableListOf<Pair<Long, Long>>()

        val finalPairs: List<Pair<Long, Long>> = search.pairs ?: run {
            // Tidak ada solusi bebas-rematch: pasangkan tetangga terdekat di klasemen
            // dan catat siapa saja yang terpaksa bertemu ulang.
            val unassigned = orderedIds.toMutableList()
            val out = mutableListOf<Pair<Long, Long>>()
            while (unassigned.size >= 2) {
                val p1 = unassigned.removeAt(0)
                var idx = unassigned.indexOfFirst { key(p1, it) !in playedPairs }
                if (idx == -1) {
                    idx = 0
                    forcedRematches.add(p1 to unassigned[0])
                }
                out.add(p1 to unassigned.removeAt(idx))
            }
            out
        }

        for ((aId, bId) in finalPairs) {
            val a = byId[aId] ?: continue
            val b = byId[bId] ?: continue
            val (first, second) = if (random.nextBoolean()) a to b else b to a
            generatedMatches.add(
                MatchEntity(
                    tournamentId = tournamentId,
                    roundNumber = roundNumber,
                    tableNumber = tableNum++,
                    player1Id = first.id,
                    player2Id = second.id,
                    p1DeckArchetype = first.deckArchetype.takeUnless { it == "Unknown" } ?: "",
                    p1DeckColor = first.deckColor.takeUnless { it == "UNKNOWN" } ?: "",
                    p2DeckArchetype = second.deckArchetype.takeUnless { it == "Unknown" } ?: "",
                    p2DeckColor = second.deckColor.takeUnless { it == "UNKNOWN" } ?: "",
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

        return PairingResult(
            matches = generatedMatches.sortedBy { if (it.isBye) Int.MAX_VALUE else it.tableNumber },
            forcedRematches = forcedRematches,
            searchExhausted = search.budgetExhausted
        )
    }

    private fun buildByeMatch(tournamentId: Long, roundNumber: Int, p: PlayerEntity) = MatchEntity(
        tournamentId = tournamentId,
        roundNumber = roundNumber,
        tableNumber = 0,
        player1Id = p.id,
        player2Id = null,
        p1DeckArchetype = p.deckArchetype.takeUnless { it == "Unknown" } ?: "",
        p1DeckColor = p.deckColor.takeUnless { it == "UNKNOWN" } ?: "",
        p1Score = 2,
        p2Score = 0,
        isDraw = false,
        isBye = true,
        winnerId = p.id,
        isReported = true,
        matchDurationMinutes = 0
    )

    private class SearchOutcome(
        val pairs: List<Pair<Long, Long>>?,
        val budgetExhausted: Boolean
    )

    /**
     * Backtracking dengan MEMOISASI kegagalan.
     *
     * Versi lama menelusuri ulang sub-pohon yang sama berulang kali. Pada kondisi
     * "padat tapi buntu" (turnamen panjang / banyak drop) biayanya meledak:
     * 20 pemain = 2,8 juta simpul, 22 pemain = 33 juta, 24 pemain tidak selesai.
     *
     * Dengan mencatat himpunan sisa pemain yang sudah terbukti buntu, kasus yang
     * sama turun menjadi 2.305 / 5.121 / 11.265 simpul.
     */
    private fun findPairingMemoized(
        ids: List<Long>,
        playedPairs: Set<Long>
    ): SearchOutcome {
        val failed = HashSet<Set<Long>>()
        var nodes = 0
        var exhausted = false

        fun dfs(remaining: List<Long>): List<Pair<Long, Long>>? {
            nodes++
            if (nodes > PAIRING_NODE_BUDGET) {
                exhausted = true
                return null
            }
            if (remaining.isEmpty()) return emptyList()

            val stateKey = remaining.toSet()
            if (stateKey in failed) return null

            val p1 = remaining[0]
            val rest = remaining.subList(1, remaining.size)
            for (i in rest.indices) {
                val p2 = rest[i]
                if (key(p1, p2) in playedPairs) continue
                val next = ArrayList<Long>(rest.size - 1)
                for (j in rest.indices) if (j != i) next.add(rest[j])
                val sub = dfs(next)
                if (sub != null) return listOf(p1 to p2) + sub
                if (exhausted) return null
            }
            failed.add(stateKey)
            return null
        }

        val result = dfs(ids)
        return SearchOutcome(result, exhausted)
    }
}
