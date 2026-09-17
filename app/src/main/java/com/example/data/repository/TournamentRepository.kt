package com.example.data.repository

import android.content.Context
import com.example.data.local.DigiSwissDatabase
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import com.example.util.NotificationHelper
import com.example.util.StandingEntry
import com.example.util.SwissPairingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

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

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        // 1. Seed users if they don't exist
        val adminUser = userDao.getUserByUsername("masteradmin")
        if (adminUser == null) {
            userDao.insertUser(
                UserEntity(
                    username = "masteradmin",
                    password = "masteradmin",
                    role = "ADMIN",
                    fullName = "Master Administrator",
                    affiliation = "DigiSwiss Central Admin"
                )
            )
        }
        
        val ogreUser = userDao.getUserByUsername("ogremaster")
        if (ogreUser == null) {
            userDao.insertUser(
                UserEntity(
                    username = "ogremaster",
                    password = "ogremaster",
                    role = "ORGANIZER",
                    fullName = "Ogre Master",
                    affiliation = "Midnight Ogre"
                )
            )
        }

        // 2. No more dummy players
        // 3. No more dummy tournament
    }

    // --- Authentication & User Operations ---
    suspend fun login(username: String, password: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(username.trim())
        if (user != null && user.password == password) {
            user
        } else {
            null
        }
    }

    suspend fun registerPlayerAccount(
        username: String,
        password: String,
        fullName: String,
        handle: String,
        deckArchetype: String,
        deckColors: List<String>
    ): Pair<UserEntity, PlayerEntity> = withContext(Dispatchers.IO) {
        val colorsStr = if (deckColors.isEmpty()) "PURPLE" else deckColors.joinToString(",")
        val cleanHandle = if (handle.startsWith("@")) handle else "@$handle"

        val newPlayer = PlayerEntity(
            name = fullName.trim(),
            handle = cleanHandle.trim(),
            deckArchetype = deckArchetype.trim(),
            deckColor = colorsStr,
            totalTournaments = 1,
            totalWins = 0,
            totalLosses = 0,
            totalDraws = 0,
            trophies = 0,
            isRegistered = true,
            avatarId = ((1..8).random())
        )
        val playerId = playerDao.insertPlayer(newPlayer)
        val savedPlayer = newPlayer.copy(id = playerId)

        val newUser = UserEntity(
            username = username.trim(),
            password = password,
            role = "PLAYER",
            fullName = fullName.trim(),
            affiliation = "Pemain Independen",
            associatedPlayerId = playerId
        )
        val userId = userDao.insertUser(newUser)
        val savedUser = newUser.copy(id = userId)

        // Update player with userId
        playerDao.updatePlayer(savedPlayer.copy(userId = userId))

        // Also enroll to active tournament if exists
        val active = tournamentDao.getActiveTournament()
        if (active != null && active.status == "ACTIVE") {
            val enrolled = active.enrolledList.toMutableList()
            if (!enrolled.contains(playerId)) {
                enrolled.add(playerId)
                tournamentDao.updateTournament(
                    active.copy(enrolledPlayerIds = enrolled.joinToString(","))
                )
            }
        }

        Pair(savedUser, savedPlayer)
    }

    suspend fun createOrganizerAccount(
        username: String,
        password: String,
        fullName: String,
        affiliation: String
    ): UserEntity = withContext(Dispatchers.IO) {
        val newUser = UserEntity(
            username = username.trim(),
            password = password,
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
    fun getStandingsForTournament(tournamentId: Long): Flow<List<StandingEntry>> {
        return combine(
            playerDao.getAllPlayers(),
            matchDao.getMatchesForTournamentFlow(tournamentId),
            tournamentDao.getAllTournaments()
        ) { players, matches, tournaments ->
            val tourney = tournaments.find { it.id == tournamentId }
            val participatingPlayers = if (tourney != null && tourney.enrolledList.isNotEmpty()) {
                players.filter { tourney.isPlayerEnrolled(it.id) }
            } else {
                players.filter { it.isRegistered }
            }
            SwissPairingEngine.computeStandings(participatingPlayers, matches)
        }
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
    ) = withContext(Dispatchers.IO) {
        val existing = matchDao.getMatchById(matchId) ?: return@withContext
        val updated = existing.copy(
            p1Score = p1Score,
            p2Score = p2Score,
            isDraw = isDraw,
            winnerId = winnerId,
            isReported = true,
            matchDurationMinutes = durationMinutes,
            firstTurnPlayerId = firstTurnPlayerId
        )
        matchDao.updateMatch(updated)

        val p1 = playerDao.getPlayerById(existing.player1Id)
        val p2 = existing.player2Id?.let { playerDao.getPlayerById(it) }
        val resultText = if (isDraw) "Draw $p1Score - $p2Score" else if (winnerId == p1?.id) "${p1?.name} Menang $p1Score-$p2Score" else "${p2?.name} Menang $p2Score-$p1Score"
        NotificationHelper.sendTournamentNotification(
            context,
            matchId.toInt(),
            "Hasil Pertandingan Meja ${existing.tableNumber}",
            "${p1?.name ?: "P1"} vs ${p2?.name ?: "P2"}: $resultText"
        )
    }

    suspend fun revertToPreviousRound(tournamentId: Long) = withContext(Dispatchers.IO) {
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        if (tournament.currentRound <= 1) return@withContext

        // Delete matches for the current round
        val currentRoundMatches = matchDao.getMatchesForTournament(tournamentId).filter { it.roundNumber == tournament.currentRound }
        for (match in currentRoundMatches) {
            matchDao.deleteMatch(match)
        }

        // Decrement round
        val prevRound = tournament.currentRound - 1
        tournamentDao.updateTournament(tournament.copy(currentRound = prevRound, status = "ACTIVE"))
    }

    suspend fun advanceToNextRound(tournamentId: Long) = withContext(Dispatchers.IO) {
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        val allPastMatches = matchDao.getMatchesForTournament(tournamentId)

        val nextRoundNum = tournament.currentRound + 1
        if (nextRoundNum > tournament.totalRounds) {
            tournamentDao.updateTournament(tournament.copy(status = "COMPLETED"))
            NotificationHelper.sendTournamentNotification(
                context,
                9999,
                "Turnamen Selesai!",
                "${tournament.name} telah selesai. Cek klasemen akhir sekarang!"
            )
            return@withContext
        }

        val allPlayersList = playerDao.getRegisteredPlayersList()
        val eligiblePlayers = allPlayersList.filter { p ->
            tournament.isPlayerEnrolled(p.id) && !tournament.isPlayerDropped(p.id)
        }

        val newMatches = SwissPairingEngine.generateNextRoundPairings(
            tournamentId = tournamentId,
            roundNumber = nextRoundNum,
            players = eligiblePlayers,
            pastMatches = allPastMatches
        )

        matchDao.insertMatches(newMatches)
        tournamentDao.updateTournament(tournament.copy(currentRound = nextRoundNum))

        NotificationHelper.sendTournamentNotification(
            context,
            nextRoundNum * 100,
            "Pairing Ronde $nextRoundNum Tersedia!",
            "Pairing ronde $nextRoundNum telah dirilis. Silakan menuju meja pertandingan masing-masing."
        )
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
    ): Long = withContext(Dispatchers.IO) {
        val active = tournamentDao.getActiveTournament()
        if (active != null) {
            tournamentDao.updateTournament(active.copy(status = "COMPLETED"))
        }

        val enrolledStr = selectedPlayerIds?.joinToString(",") ?: ""

        val newTourney = TournamentEntity(
            name = name,
            dateText = dateText,
            totalRounds = rounds,
            currentRound = 1,
            status = "ACTIVE",
            roundDurationMinutes = roundDuration,
            location = location,
            matchFormat = matchFormat,
            organizerId = organizerId,
            organizerName = organizerName,
            enrolledPlayerIds = enrolledStr
        )
        val newId = tournamentDao.insertTournament(newTourney)

        val allPlayersList = playerDao.getRegisteredPlayersList()
        val participants = if (selectedPlayerIds != null && selectedPlayerIds.isNotEmpty()) {
            allPlayersList.filter { selectedPlayerIds.contains(it.id) }
        } else {
            allPlayersList
        }

        val r1Matches = SwissPairingEngine.generateNextRoundPairings(
            tournamentId = newId,
            roundNumber = 1,
            players = participants.shuffled(),
            pastMatches = emptyList()
        )
        matchDao.insertMatches(r1Matches)

        NotificationHelper.sendTournamentNotification(
            context,
            1001,
            "Turnamen Baru Dimulai!",
            "$name ($matchFormat) oleh $organizerName dibuka! Ronde 1 siap dimainkan."
        )

        newId
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
        val tournament = tournamentDao.getTournamentById(tournamentId) ?: return@withContext
        matchDao.deleteMatchesForTournament(tournamentId)

        val allPlayersList = playerDao.getRegisteredPlayersList()
        val participants = allPlayersList.filter { p ->
            tournament.isPlayerEnrolled(p.id) && !tournament.isPlayerDropped(p.id)
        }

        val r1Matches = SwissPairingEngine.generateNextRoundPairings(
            tournamentId = tournamentId,
            roundNumber = 1,
            players = participants.shuffled(),
            pastMatches = emptyList()
        )
        matchDao.insertMatches(r1Matches)
        tournamentDao.updateTournament(tournament.copy(currentRound = 1, status = "ACTIVE"))
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

    suspend fun setPlayerDeckForTournament(tournamentId: Long, playerId: Long, archetype: String, colors: String) = withContext(Dispatchers.IO) {
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

        // Also update their main PlayerEntity as their latest deck
        val player = playerDao.getPlayerById(playerId)
        if (player != null) {
            playerDao.updatePlayer(player.copy(deckArchetype = archetype, deckColor = colors))
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
