package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity

import com.example.data.repository.TournamentRepository
import com.example.util.AnalyticsEngine
import com.example.util.IndividualPlayerAnalytics
import com.example.util.NotificationHelper
import com.example.util.OverallTournamentAnalytics
import com.example.util.StandingEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InAppAlert(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String
)


class TournamentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TournamentRepository(application.applicationContext)
    private val authPrefs = application.getSharedPreferences("digiswiss_session", Context.MODE_PRIVATE)

    // Current logged in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthChecking = MutableStateFlow(true)
    val isAuthChecking: StateFlow<Boolean> = _isAuthChecking.asStateFlow()

    private val _selectedRound = MutableStateFlow(1)
    private val _selectedPlayer = MutableStateFlow<PlayerEntity?>(null)
    private val _timerSeconds = MutableStateFlow(45 * 60)
    private val _isTimerRunning = MutableStateFlow(false)
    private val _inAppNotification = MutableStateFlow<InAppAlert?>(null)
    private val _playerSearchQuery = MutableStateFlow("")

    // Analytics scope filter: "CURRENT" or "ALL_TOURNAMENTS"
    private val _analyticsScope = MutableStateFlow("CURRENT")
    val analyticsScope: StateFlow<String> = _analyticsScope.asStateFlow()

    // Store filter for organizers e.g. "ALL", "Toko A", "Toko B"
    private val _selectedStoreFilter = MutableStateFlow("ALL")
    val selectedStoreFilter: StateFlow<String> = _selectedStoreFilter.asStateFlow()

    private val _matchesFlow = MutableStateFlow<List<MatchEntity>>(emptyList())
    val matchesFlow: StateFlow<List<MatchEntity>> = _matchesFlow.asStateFlow()

    private val _allMatchesFlow = MutableStateFlow<List<MatchEntity>>(emptyList())
    val allMatchesFlow: StateFlow<List<MatchEntity>> = _allMatchesFlow.asStateFlow()

    private val _standingsFlow = MutableStateFlow<List<StandingEntry>>(emptyList())
    val standingsFlow: StateFlow<List<StandingEntry>> = _standingsFlow.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            // Check session persistence
            val savedUserId = authPrefs.getLong("session_user_id", -1L)
            if (savedUserId != -1L) {
                val user = repository.getUserById(savedUserId)
                if (user != null) {
                    _currentUser.value = user
                }
            }
            _isAuthChecking.value = false
        }

        viewModelScope.launch {
            repository.activeTournamentFlow.collect { tourney ->
                if (tourney != null) {
                    _selectedRound.value = tourney.currentRound
                    _timerSeconds.value = tourney.roundDurationMinutes * 60

                    launch {
                        repository.getMatchesForTournament(tourney.id).collect { matches ->
                            _matchesFlow.value = matches
                        }
                    }
                    launch {
                        repository.getStandingsFlow(tourney.id).collect { standings ->
                            _standingsFlow.value = standings
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            // Collect all matches across database for overall store analytics
            while (true) {
                _allMatchesFlow.value = repository.getAllMatches()
                delay(3000)
            }
        }
    }

    val activeTournamentFlow: StateFlow<TournamentEntity?> = repository.activeTournamentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTournamentsFlow: StateFlow<List<TournamentEntity>> = repository.allTournamentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlayersFlow: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsersFlow: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedRound: StateFlow<Int> = _selectedRound.asStateFlow()
    val selectedPlayer: StateFlow<PlayerEntity?> = _selectedPlayer.asStateFlow()
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    val inAppNotification: StateFlow<InAppAlert?> = _inAppNotification.asStateFlow()
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    // --- Authentication ---
    fun login(username: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.login(username.trim(), password)
            if (user != null) {
                _currentUser.value = user
                authPrefs.edit().putLong("session_user_id", user.id).apply()
                showInAppNotification("Login Berhasil", "Selamat datang, ${user.fullName} (${user.role})")
                onResult(true, null)
            } else {
                onResult(false, "Username atau password salah")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        authPrefs.edit().remove("session_user_id").apply()
        showInAppNotification("Logout", "Anda telah keluar dari akun.")
    }

    fun switchUserQuick(username: String, password: String) {
        login(username, password) { _, _ -> }
    }

    fun registerPlayerAccount(
        username: String,
        password: String,
        fullName: String,
        handle: String,
        deckArchetype: String,
        deckColors: List<String>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (!repository.checkUsernameAvailable(username)) {
                onResult(false, "Username '$username' sudah terpakai.")
                return@launch
            }
            val (savedUser, savedPlayer) = repository.registerPlayerAccount(
                username = username,
                password = password,
                fullName = fullName,
                handle = handle,
                deckArchetype = deckArchetype,
                deckColors = deckColors
            )
            _currentUser.value = savedUser
            showInAppNotification(
                "Pendaftaran Berhasil",
                "Akun Pemain '${savedPlayer.name}' berhasil dibuat dengan deck $deckArchetype!"
            )
            onResult(true, null)
        }
    }

    fun createOrganizerAccount(
        username: String,
        password: String,
        fullName: String,
        affiliation: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null || !user.isAdmin) {
            onResult(false, "Hanya Admin yang berhak membuat akun Penyelenggara.")
            return
        }

        viewModelScope.launch {
            if (!repository.checkUsernameAvailable(username)) {
                onResult(false, "Username '$username' sudah terdaftar.")
                return@launch
            }
            val newOrganizer = repository.createOrganizerAccount(
                username = username,
                password = password,
                fullName = fullName,
                affiliation = affiliation
            )
            showInAppNotification(
                "Akun Penyelenggara Dibuat",
                "Akun ${newOrganizer.fullName} (${newOrganizer.username}) berhasil dibuat oleh Admin."
            )
            onResult(true, null)
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
            showInAppNotification("Akun Diperbarui", "Data user ${user.username} berhasil diubah.")
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            showInAppNotification("Akun Dihapus", "Akun berhasil dihapus dari sistem.")
        }
    }

    // --- Tournament Management ---
    fun setSelectedRound(round: Int) {
        _selectedRound.value = round
    }

    fun setPlayerSearchQuery(query: String) {
        _playerSearchQuery.value = query
    }

    fun selectPlayerForDetails(player: PlayerEntity?) {
        _selectedPlayer.value = player
    }

    fun setAnalyticsScope(scope: String) {
        _analyticsScope.value = scope
    }

    fun setSelectedStoreFilter(store: String) {
        _selectedStoreFilter.value = store
    }

    fun toggleRoundTimer() {
        val currentRunning = _isTimerRunning.value
        if (currentRunning) {
            timerJob?.cancel()
            _isTimerRunning.value = false
        } else {
            _isTimerRunning.value = true
            timerJob = viewModelScope.launch {
                showInAppNotification(
                    "Timer Pertandingan Dimulai",
                    "Waktu ronde sedang berjalan. Pemain memiliki 3 giliran tambahan jika waktu habis."
                )
                while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                    delay(1000)
                    _timerSeconds.value = _timerSeconds.value - 1
                }
                if (_timerSeconds.value <= 0) {
                    _isTimerRunning.value = false
                    showInAppNotification(
                        "WAKTU HABIS! Extra Turns",
                        "Waktu ronde telah habis! Jalankan aturan 3 giliran tambahan (Turn 0-3)."
                    )
                    NotificationHelper.sendTournamentNotification(
                        getApplication(),
                        7777,
                        "WAKTU RONDE HABIS!",
                        "Waktu ronde habis. Selesaikan 3 giliran tambahan (Turn 0-3)."
                    )
                }
            }
        }
    }

    fun resetRoundTimer(minutes: Int = 45) {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSeconds.value = minutes * 60
    }

    fun reportScore(
        matchId: Long,
        p1Score: Int,
        p2Score: Int,
        isDraw: Boolean,
        winnerId: Long?,
        durationMinutes: Int,
        firstTurnPlayerId: Long?
    ) {
        viewModelScope.launch {
            repository.reportMatchScore(
                matchId = matchId,
                p1Score = p1Score,
                p2Score = p2Score,
                isDraw = isDraw,
                winnerId = winnerId,
                durationMinutes = durationMinutes,
                firstTurnPlayerId = firstTurnPlayerId
            )
            showInAppNotification(
                "Skor Berhasil Disimpan",
                "Hasil $p1Score - $p2Score tersimpan. Peringkat klasemen diperbarui secara real-time!"
            )
        }
    }

    // Player or Organizer Concede action
    fun concedeMatch(matchId: Long, playerId: Long) {
        viewModelScope.launch {
            repository.concedeMatch(matchId, playerId)
            showInAppNotification(
                "Pemain Mengalah (Conceded)",
                "Pertandingan diselesaikan. Kemenangan otomatis diberikan kepada lawan."
            )
        }
    }

    // Drop player action (by Player or Organizer/Admin)
    fun dropPlayer(tournamentId: Long, playerId: Long) {
        viewModelScope.launch {
            repository.dropPlayerFromTournament(tournamentId, playerId)
            showInAppNotification(
                "Pemain Telah Drop",
                "Pemain telah didrop dari turnamen dan tidak akan dipasangkan pada ronde berikutnya."
            )
        }
    }

    fun setPlayerDeckForTournament(tournamentId: Long, playerId: Long, archetype: String, colors: String) {
        viewModelScope.launch {
            repository.setPlayerDeckForTournament(tournamentId, playerId, archetype, colors)
            showInAppNotification(
                "Deck Tersimpan",
                "Deck '$archetype' berhasil disimpan untuk statistik pemain di turnamen ini."
            )
        }
    }

    // Admit / Enroll player to tournament
    fun enrollPlayer(tournamentId: Long, playerId: Long) {
        viewModelScope.launch {
            repository.enrollPlayerToTournament(tournamentId, playerId)
            showInAppNotification("Pemain Diikutsertakan", "Pemain berhasil didaftarkan ke dalam turnamen ini.")
        }
    }

    fun advanceToNextRound() {
        val tourney = activeTournamentFlow.value ?: return
        viewModelScope.launch {
            repository.advanceToNextRound(tourney.id)
            _selectedRound.value = tourney.currentRound + 1
            resetRoundTimer(tourney.roundDurationMinutes)
            showInAppNotification(
                "Ronde ${tourney.currentRound + 1} Dimulai",
                "Pairing baru telah di-generate otomatis dengan sistem Swiss tanpa tanding ulang!"
            )
        }
    }

    fun addNewPlayer(name: String, handle: String, bandaiUid: String) {
        viewModelScope.launch {
            val cleanHandle = if (handle.isBlank() || handle.trim() == "-") "-" else if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}"
            val cleanUid = if (bandaiUid.isBlank() || bandaiUid.trim() == "-") "-" else bandaiUid.trim()
            val newPlayer = PlayerEntity(
                name = name.trim(),
                handle = cleanHandle,
                bandaiUid = cleanUid,
                deckArchetype = "Unknown",
                deckColor = "UNKNOWN",
                avatarId = (1..8).random()
            )
            repository.addPlayer(newPlayer)
            showInAppNotification(
                "Peserta Terdaftar",
                "${newPlayer.name} berhasil didaftarkan ke turnamen."
            )
        }
    }

    fun updatePlayer(
        playerId: Long,
        name: String,
        handle: String,
        bandaiUid: String,
        deckArchetype: String = "",
        deckColor: String = ""
    ) {
        viewModelScope.launch {
            val existing = repository.getPlayerById(playerId) ?: return@launch
            val cleanHandle = if (handle.isBlank() || handle.trim() == "-") "-" else if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}"
            val cleanUid = if (bandaiUid.isBlank() || bandaiUid.trim() == "-") "-" else bandaiUid.trim()
            val updated = existing.copy(
                name = name.trim(),
                handle = cleanHandle,
                bandaiUid = cleanUid,
                deckArchetype = if (deckArchetype.isNotBlank()) deckArchetype.trim() else existing.deckArchetype,
                deckColor = if (deckColor.isNotBlank()) deckColor.trim() else existing.deckColor
            )
            repository.updatePlayer(updated)
            if (_selectedPlayer.value?.id == playerId) {
                _selectedPlayer.value = updated
            }
            showInAppNotification("Data Diperbarui", "Data peserta ${updated.name} berhasil disimpan.")
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        updatePlayer(
            playerId = player.id,
            name = player.name,
            handle = player.handle,
            bandaiUid = player.bandaiUid,
            deckArchetype = player.deckArchetype,
            deckColor = player.deckColor
        )
    }

    fun createNewTournament(
        name: String,
        dateText: String,
        totalRounds: Int,
        durationMins: Int,
        location: String,
        matchFormat: String = "BO3",
        selectedPlayerIds: List<Long>? = null
    ) {
        val user = _currentUser.value
        val organizerId = if (user?.isOrganizer == true) user.id else null
        val organizerName = user?.affiliation?.ifEmpty { user.fullName } ?: "DigiSwiss Organizer"

        viewModelScope.launch {
            repository.createNewTournament(
                name = name,
                dateText = dateText,
                rounds = totalRounds,
                roundDuration = durationMins,
                location = location,
                matchFormat = matchFormat,
                organizerId = organizerId,
                organizerName = organizerName,
                selectedPlayerIds = selectedPlayerIds
            )
            val formatLabel = if (matchFormat.equals("BO1", ignoreCase = true)) "Best of 1 (1 Game)" else "Best of 3"
            showInAppNotification(
                "Turnamen Baru Dibuat",
                "$name diselenggarakan oleh $organizerName format $formatLabel dengan $totalRounds ronde."
            )
        }
    }

    fun setTournamentFormat(matchFormat: String) {
        val tourney = activeTournamentFlow.value ?: return
        viewModelScope.launch {
            repository.updateTournamentFormat(tourney.id, matchFormat)
            val newDuration = if (matchFormat.equals("BO1", ignoreCase = true)) 25 else 45
            resetRoundTimer(newDuration)
            val formatLabel = if (matchFormat.equals("BO1", ignoreCase = true)) "Best of 1" else "Best of 3"
            showInAppNotification(
                "Format Pertandingan Diubah",
                "Format turnamen diganti ke $formatLabel. Timer ronde disesuaikan ke $newDuration menit."
            )
        }
    }

    fun resetTournament() {
        val tourney = activeTournamentFlow.value ?: return
        viewModelScope.launch {
            repository.resetTournamentRounds(tourney.id)
            _selectedRound.value = 1
            resetRoundTimer(tourney.roundDurationMinutes)
            showInAppNotification(
                "Turnamen Direset",
                "Semua ronde telah direset kembali ke Ronde 1."
            )
        }
    }

    // Admin match modification
    fun adminModifyMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.updateMatch(match)
            showInAppNotification("Pertandingan Diubah", "Data pertandingan meja #${match.tableNumber} diperbarui oleh Admin.")
        }
    }

    // Admin player modification
    fun adminUpdatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
            showInAppNotification("Data Pemain Diubah", "Pemain ${player.name} berhasil diperbarui.")
        }
    }

    fun adminDeletePlayer(playerId: Long) {
        viewModelScope.launch {
            repository.deletePlayer(playerId)
            showInAppNotification("Pemain Dihapus", "Pemain telah dihapus dari sistem.")
        }
    }

    fun adminDeleteTournament(tournamentId: Long) {
        viewModelScope.launch {
            repository.deleteTournament(tournamentId)
            showInAppNotification("Turnamen Dihapus", "Turnamen dan semua pertandingannya berhasil dihapus.")
        }
    }

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    fun showInAppNotification(title: String, message: String) {
        _inAppNotification.value = InAppAlert(title = title, message = message)
    }

    fun getPlayerAnalytics(playerId: Long, players: List<PlayerEntity>, matches: List<MatchEntity>): IndividualPlayerAnalytics? {
        return AnalyticsEngine.analyzePlayer(playerId, players, matches)
    }

    fun getTournamentAnalytics(players: List<PlayerEntity>, matches: List<MatchEntity>): OverallTournamentAnalytics {
        return AnalyticsEngine.analyzeTournament(players, matches)
    }


}
