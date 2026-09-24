package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.DigiSwissDatabase
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import com.example.util.NotificationHelper
import com.example.util.PasswordHasher
import com.example.util.StandingEntry
import com.example.util.SwissPairingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

sealed class AdvanceResult {
    data class Success(val round: Int, val forcedRematches: Int) : AdvanceResult()
    object TournamentFinished : AdvanceResult()
    data class Blocked(val reason: String) : AdvanceResult()
}

class TournamentRepository(private val context: Context) {
    private val db = DigiSwissDatabase.getDatabase(context)
    private val playerDao = db.playerDao()
    private val tournamentDao = db.tournamentDao()
    private val matchDao = db.matchDao()
    private val userDao = db.userDao()

    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allTournamentsFlow: Flow<List<TournamentEntity>> = tournamentDao.getAllTournaments()
    val activeTournamentFlow: Flow<TournamentEntity?> = tournamentDao.getActiveTournamentFlow()
    val allMatchesFlow: Flow<List<MatchEntity>> = matchDao.getAllMatchesFlow()

    suspend fun checkAndSeedInitialData(): String? = withContext(Dispatchers.IO) {
        var generatedAdminPassword: String? = null

        val admin = userDao.getUserByUsername("masteradmin")
        if (admin == null) {
            val pwd = "admin" + (1000..9999).random()
            userDao.insertUser(
                UserEntity(
                    username = "masteradmin",
                    password = PasswordHasher.hash(pwd),
                    role = "ADMIN",
                    fullName = "Master Administrator",
                    affiliation = "DigiSwiss Central Admin"
                )
            )
            generatedAdminPassword = pwd
        } else if (PasswordHasher.needsRehash(admin.password)) {
            userDao.updateUser(admin.copy(password = PasswordHasher.hash(admin.password)))
        }

        val ogre = userDao.getUserByUsername("ogremaster")
        if (ogre == null) {
            userDao.insertUser(
                UserEntity(
                    username = "ogremaster",
                    password = PasswordHasher.hash("ogremaster"),
                    role = "ORGANIZER",
                    fullName = "Ogre Master",
                    affiliation = "Midnight Ogre"
                )
            )
        } else if (PasswordHasher.needsRehash(ogre.password)) {
            userDao.updateUser(ogre.copy(password = PasswordHasher.hash(ogre.password)))
        }

        generatedAdminPassword
    }

    // --- Authentication & User Operations ---
    suspend fun login(username: String, password: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(username.trim()) ?: return@withContext null
        if (!PasswordHasher.verify(password, user.password)) return@withContext null

        // Upgrade otomatis: password lama berformat teks polos langsung di-hash
        // begitu pengguna berhasil login pertama kali.
        if (PasswordHasher.needsRehash(user.password)) {
            userDao.updateUser(user.copy(password = PasswordHasher.hash(password)))
        }
        user
    }

    suspend fun registerPlayerAccount(
        username: String,
        password: String,
        fullName: String,
        handle: String,
        deckArchetype: String,
        deckColors: List<String>
    ): Result<Pair<UserEntity, PlayerEntity>> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanHandle = if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}"

        // atomic: hindari race condition dua orang daftar bersamaan dengan username sama
        db.withTransaction {
            if (userDao.getUserByUsername(cleanUser) != null) {
                return@withTransaction Result.failure(
                    IllegalArgumentException("Username '$cleanUser' sudah terpakai.")
                )
            }

            val currentPlayers = playerDao.getRegisteredPlayersList()
            if (cleanHandle != "@-") {
                val cleanCheck = cleanHandle.removePrefix("@")
                if (currentPlayers.any {
                        it.handle.removePrefix("@") != "-" &&
                            it.handle.removePrefix("@").equals(cleanCheck, ignoreCase = true)
                    }) {
                    return@withTransaction Result.failure(
                        IllegalArgumentException("Handle '$cleanHandle' sudah dipakai peserta lain.")
                    )
                }
            }

            val colorsStr = if (deckColors.isEmpty()) "PURPLE" else deckColors.joinToString(",")
            val newPlayer = PlayerEntity(
                name = fullName.trim(),
                handle = cleanHandle,
                deckArchetype = deckArchetype.trim().ifBlank { "Unknown" },
                deckColor = colorsStr,
                totalTournaments = 1,
                totalWins = 0,
                totalLosses = 0,
                totalDraws = 0,
                trophies = 0,
                isRegistered = true,
                avatarId = (1..8).random()
            )
            val playerId = playerDao.insertPlayer(newPlayer)

            val newUser = UserEntity(
                username = cleanUser,
                password = PasswordHasher.hash(password),
                role = "PLAYER",
                fullName = fullName.trim(),
                affiliation = "Independent Player",
                associatedPlayerId = playerId
            )
            val userId = userDao.insertUser(newUser)

            Result.success(newUser.copy(id = userId) to newPlayer.copy(id = playerId))
        }
    }

    suspend fun createOrganizerAccount(
        username: String,
        password: String,
        fullName: String,
        affiliation: String
    ): UserEntity = withContext(Dispatchers.IO) {
        val newUser = UserEntity(
            username = username.trim(),
            password = PasswordHasher.hash(password),
            role = "ORGANIZER",
            fullName = fullName.trim(),
            affiliation = affiliation.trim()
        )
        val id = userDao.insertUser(newUser)
        newUser.copy(id = id)
    }

    suspend fun getUserById(userId: Long): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun getPlayerById(playerId: Long): PlayerEntity? = withContext(Dispatchers.IO) {
        playerDao.getPlayerById(playerId)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Long) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId)
        if (user != null) {
            user.associatedPlayerId?.let { pId ->
                playerDao.deletePlayerById(pId)
            }
            userDao.deleteUser(user)
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim()) == null
    }

    // --- Tournament Enrollment & Drop ---
    suspend fun enrollPlayerToTournament(tournamentId: Long, playerId: Long) = withContext(Dispatchers.IO) {
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        val list = tournament.enrolledList.toMutableList()
        if (!list.contains(playerId)) {
            list.add(playerId)
            tournamentDao.updateTournament(tournament.copy(enrolledPlayerIds = list.joinToString(",")))
        }
    }

    suspend fun dropPlayerFromTournament(tournamentId: Long, playerId: Long) = withContext(Dispatchers.IO) {
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        val dropped = tournament.droppedList.toMutableList()
        if (!dropped.contains(playerId)) {
            dropped.add(playerId)
            tournamentDao.updateTournament(tournament.copy(droppedPlayerIds = dropped.joinToString(",")))
        }

        // Check if player has an active unreported match in the current round
        val currentMatches = matchDao.getMatchesForRound(tournamentId, tournament.currentRound)
        for (match in currentMatches) {
            if (!match.isReported && (match.player1Id == playerId || match.player2Id == playerId)) {
                val isBo1 = tournament.isBo1
                val maxScore = if (isBo1) 1 else 2
                val updatedMatch = if (match.player1Id == playerId) {
                    // P1 dropped, P2 wins
                    match.copy(
                        p1Score = 0,
                        p2Score = maxScore,
                        winnerId = match.player2Id,
                        isReported = true,
                        isConceded = true,
                        concededByPlayerId = playerId,
                        matchDurationMinutes = 5
                    )
                } else {
                    // P2 dropped, P1 wins
                    match.copy(
                        p1Score = maxScore,
                        p2Score = 0,
                        winnerId = match.player1Id,
                        isReported = true,
                        isConceded = true,
                        concededByPlayerId = playerId,
                        matchDurationMinutes = 5
                    )
                }
                matchDao.updateMatch(updatedMatch)
            }
        }
    }

    suspend fun concedeMatch(matchId: Long, concededPlayerId: Long) = withContext(Dispatchers.IO) {
        val match = matchDao.getMatchById(matchId) ?: return@withContext
        val tournament = tournamentDao.getTournamentById(match.tournamentId)
        val maxScore = if (tournament?.isBo1 == true) 1 else 2

        val updatedMatch = if (match.player1Id == concededPlayerId) {
            match.copy(
                p1Score = 0,
                p2Score = maxScore,
                winnerId = match.player2Id,
                isReported = true,
                isConceded = true,
                concededByPlayerId = concededPlayerId,
                matchDurationMinutes = if (match.matchDurationMinutes > 0) match.matchDurationMinutes else 10
            )
        } else {
            match.copy(
                p1Score = maxScore,
                p2Score = 0,
                winnerId = match.player1Id,
                isReported = true,
                isConceded = true,
                concededByPlayerId = concededPlayerId,
                matchDurationMinutes = if (match.matchDurationMinutes > 0) match.matchDurationMinutes else 10
            )
        }
        matchDao.updateMatch(updatedMatch)
    }

    // --- Standings & Matches ---
    fun getStandingsForTournament(tournamentId: Long): Flow<List<StandingEntry>> =
        combine(
            playerDao.getAllPlayers(),
            matchDao.getMatchesForTournamentFlow(tournamentId),
            tournamentDao.getAllTournaments()
        ) { players, matches, tournaments ->
            val tourney = tournaments.find { it.id == tournamentId } ?: return@combine emptyList()

            // Seluruh peserta yang pernah terdaftar (termasuk yang sudah drop)
            // dijadikan pool penghitungan tiebreaker, agar riwayat lawan mereka
            // tetap valid dan nilai OMW%/OGW% tidak anjlok ke 33%.
            val allEnrolled = if (tourney.enrolledList.isNotEmpty()) {
                players.filter { tourney.isPlayerEnrolled(it.id) }
            } else {
                players.filter { it.isRegistered }
            }

            // Peserta aktif yang ditampilkan di klasemen (yang sudah drop disembunyikan
            // dari peringkat aktif, tapi skor mereka tetap hidup di histori).
            val activePlayers = allEnrolled.filterNot { tourney.isPlayerDropped(it.id) }

            SwissPairingEngine.computeStandings(
                players = activePlayers,
                allMatches = matches,
                tiebreakerPool = allEnrolled
            )
        }

    fun getStandingsFlow(tournamentId: Long): Flow<List<StandingEntry>> = getStandingsForTournament(tournamentId)

    fun getMatchesForTournament(tournamentId: Long): Flow<List<MatchEntity>> = matchDao.getMatchesForTournamentFlow(tournamentId)

    suspend fun addPlayer(player: PlayerEntity): Long = playerDao.insertPlayer(player)

    suspend fun getAllPlayersList(): List<PlayerEntity> = withContext(Dispatchers.IO) {
        playerDao.getRegisteredPlayersList()
    }

    fun getMatchesForCurrentRound(tournamentId: Long, roundNumber: Int): Flow<List<MatchEntity>> {
        return matchDao.getMatchesForRoundFlow(tournamentId, roundNumber)
    }

    suspend fun reportMatchScore(
        matchId: Long,
        p1Score: Int,
        p2Score: Int,
        isDraw: Boolean,
        winnerId: Long?,
        durationMinutes: Int,
        firstTurnPlayerId: Long?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Validasi input: skor negatif atau di luar format langsung ditolak
        if (p1Score < 0 || p2Score < 0 || p1Score > 2 || p2Score > 2) {
            return@withContext Result.failure(IllegalArgumentException("Skor harus antara 0 dan 2."))
        }
        if (isDraw && winnerId != null) {
            return@withContext Result.failure(IllegalArgumentException("Pertandingan seri tidak boleh punya pemenang."))
        }
        if (!isDraw && winnerId == null && (p1Score > 0 || p2Score > 0)) {
            return@withContext Result.failure(IllegalArgumentException("Pemenang pertandingan wajib ditentukan."))
        }

        db.withTransaction {
            val existing = matchDao.getMatchById(matchId)
                ?: return@withTransaction Result.failure(IllegalStateException("Pertandingan tidak ditemukan."))

            val updated = existing.copy(
                p1Score = p1Score,
                p2Score = p2Score,
                isDraw = isDraw,
                winnerId = winnerId,
                isReported = true,
                matchDurationMinutes = durationMinutes.coerceIn(0, 600),
                firstTurnPlayerId = firstTurnPlayerId
            )
            matchDao.updateMatch(updated)

            val p1 = playerDao.getPlayerById(existing.player1Id)
            val p2 = existing.player2Id?.let { playerDao.getPlayerById(it) }
            val resultText = when {
                isDraw -> "Draw $p1Score - $p2Score"
                winnerId == p1?.id -> "${p1?.name ?: "P1"} Menang $p1Score-$p2Score"
                else -> "${p2?.name ?: "P2"} Menang $p2Score-$p1Score"
            }
            NotificationHelper.sendTournamentNotification(
                context,
                (matchId % 10000).toInt(),
                "Hasil Meja ${existing.tableNumber}",
                "${p1?.name ?: "P1"} vs ${p2?.name ?: "P2"}: $resultText"
            )
            Result.success(Unit)
        }
    }

    suspend fun revertToPreviousRound(tournamentId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withTransaction
            if (tournament.currentRound <= 1) return@withTransaction

            val currentRoundMatches = matchDao.getMatchesForTournament(tournamentId)
                .filter { it.roundNumber == tournament.currentRound }
            for (match in currentRoundMatches) {
                matchDao.deleteMatch(match)
            }
            tournamentDao.updateTournament(
                tournament.copy(currentRound = tournament.currentRound - 1, status = "ACTIVE")
            )
        }
    }

    suspend fun advanceToNextRound(
        tournamentId: Long,
        expectedCurrentRound: Int? = null
    ): AdvanceResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            val tournament = tournamentDao.getTournamentById(tournamentId)
                ?: return@withTransaction AdvanceResult.Blocked("Turnamen tidak ditemukan.")

            // Proteksi klik ganda: kalau pemanggil mengira ronde saat ini X,
            // tapi di database sudah X+1, batalkan eksekusi kedua.
            if (expectedCurrentRound != null && tournament.currentRound != expectedCurrentRound) {
                return@withTransaction AdvanceResult.Blocked("Ronde sudah dimajukan oleh proses lain.")
            }

            val currentMatches = matchDao.getMatchesForRound(tournamentId, tournament.currentRound)
            val unreported = currentMatches.filter { !it.isReported }
            if (unreported.isNotEmpty()) {
                val tables = unreported.joinToString(", ") { "Meja ${it.tableNumber}" }
                return@withTransaction AdvanceResult.Blocked(
                    "Masih ada ${unreported.size} pertandingan belum selesai: $tables."
                )
            }

            val nextRoundNum = tournament.currentRound + 1
            if (nextRoundNum > tournament.totalRounds) {
                tournamentDao.updateTournament(tournament.copy(status = "COMPLETED"))
                NotificationHelper.sendTournamentNotification(
                    context, 9999, "Turnamen Selesai!",
                    "${tournament.name} telah selesai. Cek klasemen akhir sekarang!"
                )
                return@withTransaction AdvanceResult.TournamentFinished
            }

            val allPlayers = playerDao.getRegisteredPlayersList()
            val eligible = allPlayers.filter { p ->
                tournament.isPlayerEnrolled(p.id) && !tournament.isPlayerDropped(p.id)
            }
            if (eligible.size < 2) {
                return@withTransaction AdvanceResult.Blocked("Peserta aktif kurang dari 2 orang.")
            }

            val allPastMatches = matchDao.getMatchesForTournament(tournamentId)
            val pairing = SwissPairingEngine.generateNextRoundPairings(
                tournamentId = tournamentId,
                roundNumber = nextRoundNum,
                players = eligible,
                pastMatches = allPastMatches,
                allPlayersForTiebreak = allPlayers.filter { tournament.isPlayerEnrolled(it.id) }
            )

            matchDao.insertMatches(pairing.matches)
            tournamentDao.updateTournament(tournament.copy(currentRound = nextRoundNum))

            NotificationHelper.sendTournamentNotification(
                context,
                nextRoundNum * 100,
                "Pairing Ronde $nextRoundNum Tersedia!",
                "Pairing ronde $nextRoundNum telah dirilis."
            )

            AdvanceResult.Success(nextRoundNum, pairing.forcedRematches.size)
        }
    }

    suspend fun insertImportedTournament(tournament: TournamentEntity): Long {
        return tournamentDao.insertTournament(tournament)
    }

    suspend fun insertMatches(matches: List<MatchEntity>) {
        matchDao.insertMatches(matches)
    }

    suspend fun createNewTournament(
        name: String,
        dateText: String,
        rounds: Int,
        roundDuration: Int,
        location: String,
        matchFormat: String = "BO3",
        organizerId: Long? = null,
        organizerName: String = "DigiSwiss Organizer",
        selectedPlayerIds: List<Long>? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        db.withTransaction {
            val allPlayers = playerDao.getRegisteredPlayersList()
            val participants = if (!selectedPlayerIds.isNullOrEmpty()) {
                allPlayers.filter { selectedPlayerIds.contains(it.id) }
            } else {
                allPlayers
            }
            if (participants.size < 2) {
                return@withTransaction Result.failure(
                    IllegalArgumentException("Turnamen butuh minimal 2 peserta terdaftar.")
                )
            }

            val active = tournamentDao.getActiveTournament()
            if (active != null) {
                tournamentDao.updateTournament(active.copy(status = "COMPLETED"))
            }

            val newTourney = TournamentEntity(
                name = name.trim().ifBlank { "Turnamen DigiSwiss" },
                dateText = dateText,
                totalRounds = rounds.coerceIn(1, 10),
                currentRound = 1,
                status = "ACTIVE",
                roundDurationMinutes = roundDuration.coerceIn(10, 120),
                location = location,
                matchFormat = matchFormat,
                organizerId = organizerId,
                organizerName = organizerName,
                enrolledPlayerIds = participants.joinToString(",") { it.id.toString() }
            )
            val newId = tournamentDao.insertTournament(newTourney)

            val r1 = SwissPairingEngine.generateNextRoundPairings(
                tournamentId = newId,
                roundNumber = 1,
                players = participants,
                pastMatches = emptyList()
            )
            matchDao.insertMatches(r1.matches)

            NotificationHelper.sendTournamentNotification(
                context, 1001, "Turnamen Baru Dimulai!",
                "$name ($matchFormat) dibuka. Ronde 1 siap dimainkan."
            )
            Result.success(newId)
        }
    }

    suspend fun updateTournamentFormat(tournamentId: Long, matchFormat: String) = withContext(Dispatchers.IO) {
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        val newDuration = if (matchFormat.equals("BO1", ignoreCase = true)) 25 else 45
        tournamentDao.updateTournament(
            tournament.copy(
                matchFormat = matchFormat,
                roundDurationMinutes = newDuration
            )
        )
    }

    suspend fun resetTournamentRounds(tournamentId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withTransaction
            matchDao.deleteMatchesForTournament(tournamentId)

            val allPlayers = playerDao.getRegisteredPlayersList()
            val participants = allPlayers.filter { p ->
                tournament.isPlayerEnrolled(p.id) && !tournament.isPlayerDropped(p.id)
            }

            val r1 = SwissPairingEngine.generateNextRoundPairings(
                tournamentId = tournamentId,
                roundNumber = 1,
                players = participants,
                pastMatches = emptyList()
            )
            matchDao.insertMatches(r1.matches)
            tournamentDao.updateTournament(tournament.copy(currentRound = 1, status = "ACTIVE"))
        }
    }

    suspend fun updatePlayer(player: PlayerEntity) = withContext(Dispatchers.IO) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(playerId: Long) = withContext(Dispatchers.IO) {
        playerDao.deletePlayerById(playerId)
    }

    suspend fun updateMatch(match: MatchEntity) = withContext(Dispatchers.IO) {
        matchDao.updateMatch(match)
    }

    suspend fun setPlayerDeckForTournament(
        tournamentId: Long,
        playerId: Long,
        archetype: String,
        colors: String
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val matches = matchDao.getMatchesForTournament(tournamentId)
            matches.forEach { match ->
                var updated = match
                if (match.player1Id == playerId) {
                    updated = updated.copy(p1DeckArchetype = archetype, p1DeckColor = colors)
                }
                if (match.player2Id == playerId) {
                    updated = updated.copy(p2DeckArchetype = archetype, p2DeckColor = colors)
                }
                if (updated != match) {
                    matchDao.updateMatch(updated)
                }
            }

            val player = playerDao.getPlayerById(playerId)
            if (player != null) {
                playerDao.updatePlayer(player.copy(deckArchetype = archetype, deckColor = colors))
            }
        }
    }

    suspend fun deleteTournament(tournamentId: Long) = withContext(Dispatchers.IO) {
        matchDao.deleteMatchesForTournament(tournamentId)
        val tourney = tournamentDao.getTournamentById(tournamentId)
        if (tourney != null) {
            tournamentDao.deleteTournament(tourney)
        }
    }

    suspend fun getAllMatchesForTournament(tournamentId: Long): List<MatchEntity> = withContext(Dispatchers.IO) {
        matchDao.getMatchesForTournament(tournamentId)
    }

    suspend fun getAllMatches(): List<MatchEntity> = withContext(Dispatchers.IO) {
        matchDao.getAllMatchesList()
    }
}
