package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import com.example.data.repository.AdvanceResult
import com.example.data.repository.TournamentRepository
import com.example.service.RoundTimerService
import com.example.service.TimerManager
import com.example.util.AnalyticsEngine
import com.example.util.CsvHelper
import com.example.util.IndividualPlayerAnalytics
import com.example.util.OverallTournamentAnalytics
import com.example.util.StandingEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val timerSeconds: StateFlow<Int> = TimerManager.timerSeconds
    val isTimerRunning: StateFlow<Boolean> = TimerManager.isTimerRunning
    private val _inAppNotification = MutableStateFlow<InAppAlert?>(null)
    private val _playerSearchQuery = MutableStateFlow("")

    // Analytics scope filter: "CURRENT" or "ALL_TOURNAMENTS"
    private val _analyticsScope = MutableStateFlow("CURRENT")
    val analyticsScope: StateFlow<String> = _analyticsScope.asStateFlow()

    // Store filter for organizers e.g. "ALL", "Toko A", "Toko B"
    private val _selectedStoreFilter = MutableStateFlow("ALL")
    val selectedStoreFilter: StateFlow<String> = _selectedStoreFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeTournament: StateFlow<TournamentEntity?> =
        repository.activeTournamentFlow
            .onEach { t ->
                if (t != null) {
                    _selectedRound.value = t.currentRound
                    TimerManager.setTimerSeconds(t.roundDurationMinutes * 60)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeTournamentFlow: StateFlow<TournamentEntity?> = activeTournament

    @OptIn(ExperimentalCoroutinesApi::class)
    val matchesFlow: StateFlow<List<MatchEntity>> = activeTournament
        .flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else repository.getMatchesForTournament(t.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val standingsFlow: StateFlow<List<StandingEntry>> = activeTournament
        .flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else repository.getStandingsFlow(t.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allMatchesFlow: StateFlow<List<MatchEntity>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTournamentsFlow: StateFlow<List<TournamentEntity>> = repository.allTournamentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPlayersFlow: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allUsersFlow: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedRound: StateFlow<Int> = _selectedRound.asStateFlow()
    val selectedPlayer: StateFlow<PlayerEntity?> = _selectedPlayer.asStateFlow()
    val inAppNotification: StateFlow<InAppAlert?> = _inAppNotification.asStateFlow()
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            val generatedAdminPassword = repository.checkAndSeedInitialData()
            if (generatedAdminPassword != null) {
                showInAppNotification(
                    "Akun Admin Dibuat",
                    "Username: masteradmin — Password: $generatedAdminPassword\n" +
                        "Catat sekarang dan segera ganti. Password ini tidak akan ditampilkan lagi."
                )
            }

            val savedUserId = authPrefs.getLong("session_user_id", -1L)
            if (savedUserId != -1L) {
                val user = repository.getUserById(savedUserId)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    authPrefs.edit().remove("session_user_id").apply()
                }
            }
            _isAuthChecking.value = false
        }
    }

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
            if (username.trim().length < 4) {
                onResult(false, "Username minimal 4 karakter.")
                return@launch
            }
            if (password.length < 8) {
                onResult(false, "Password minimal 8 karakter.")
                return@launch
            }
            if (fullName.isBlank()) {
                onResult(false, "Nama lengkap wajib diisi.")
                return@launch
            }

            repository.registerPlayerAccount(
                username = username,
                password = password,
                fullName = fullName,
                handle = handle,
                deckArchetype = deckArchetype,
                deckColors = deckColors
            ).onSuccess { (user, player) ->
                _currentUser.value = user
                authPrefs.edit().putLong("session_user_id", user.id).apply()
                showInAppNotification("Pendaftaran Berhasil", "Akun '${player.name}' berhasil dibuat.")
                onResult(true, null)
            }.onFailure {
                onResult(false, it.message ?: "Pendaftaran gagal.")
            }
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
        val currentRunning = TimerManager.isTimerRunning.value
        val context = getApplication<Application>()
        if (currentRunning) {
            val intent = Intent(context, RoundTimerService::class.java).apply {
                action = RoundTimerService.ACTION_STOP
            }
            context.startService(intent)
        } else {
            val intent = Intent(context, RoundTimerService::class.java).apply {
                putExtra(RoundTimerService.EXTRA_SECONDS, TimerManager.timerSeconds.value)
            }
            ContextCompat.startForegroundService(context, intent)
            showInAppNotification(
                "Timer Pertandingan Dimulai",
                "Waktu ronde sedang berjalan. Pemain memiliki 3 giliran tambahan jika waktu habis."
            )
        }
    }

    fun resetRoundTimer(minutes: Int = 45) {
        val context = getApplication<Application>()
        val intent = Intent(context, RoundTimerService::class.java).apply {
            action = RoundTimerService.ACTION_STOP
        }
        context.startService(intent)
        TimerManager.setTimerRunning(false)
        TimerManager.setTimerSeconds(minutes * 60)
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
            ).onSuccess {
                showInAppNotification("Skor Tersimpan", "Hasil $p1Score - $p2Score tersimpan. Klasemen diperbarui.")
            }.onFailure {
                showInAppNotification("Skor Ditolak", it.message ?: "Data pertandingan tidak valid.")
            }
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

    fun revertToPreviousRound() {
        val tourney = activeTournamentFlow.value ?: return
        if (tourney.currentRound <= 1) return
        viewModelScope.launch {
            repository.revertToPreviousRound(tourney.id)
            _selectedRound.value = tourney.currentRound - 1
            resetRoundTimer(tourney.roundDurationMinutes)
            showInAppNotification(
                "Kembali ke Ronde ${tourney.currentRound - 1}",
                "Pairing ronde berjalan telah dihapus."
            )
        }
    }

    fun advanceToNextRound() {
        val tourney = activeTournamentFlow.value ?: return
        viewModelScope.launch {
            when (val res = repository.advanceToNextRound(tourney.id, tourney.currentRound)) {
                is AdvanceResult.Success -> {
                    _selectedRound.value = res.round
                    resetRoundTimer(tourney.roundDurationMinutes)
                    val warn = if (res.forcedRematches > 0)
                        " Catatan: ${res.forcedRematches} meja terpaksa mengulang lawan lama karena kombinasi tersisa sudah habis."
                    else ""
                    showInAppNotification("Ronde ${res.round} Dimulai", "Pairing Swiss baru telah dibuat.$warn")
                }
                AdvanceResult.TournamentFinished ->
                    showInAppNotification("Turnamen Selesai", "Semua ronde telah dimainkan. Cek klasemen akhir.")
                is AdvanceResult.Blocked ->
                    showInAppNotification("Belum Bisa Lanjut", res.reason)
            }
        }
    }

    fun addNewPlayer(name: String, handle: String, bandaiUid: String) {
        viewModelScope.launch {
            val cleanHandle = if (handle.isBlank() || handle.trim() == "-") "-" else if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}"
            val cleanUid = CsvHelper.cleanBandaiUid(bandaiUid)

            val currentPlayers = repository.getAllPlayersList()
            if (cleanHandle != "-") {
                val cleanCheck = cleanHandle.removePrefix("@")
                if (currentPlayers.any { it.handle.removePrefix("@") != "-" && it.handle.removePrefix("@").equals(cleanCheck, ignoreCase = true) }) {
                    showInAppNotification(
                        "Pendaftaran Gagal",
                        "Username '$cleanHandle' sudah terdaftar pada pemain lain!"
                    )
                    return@launch
                }
            }

            if (cleanUid != "-") {
                if (currentPlayers.any { it.bandaiUid != "-" && it.bandaiUid.equals(cleanUid, ignoreCase = true) }) {
                    showInAppNotification(
                        "Pendaftaran Gagal",
                        "UID Bandai+ '$cleanUid' sudah terdaftar pada pemain lain!"
                    )
                    return@launch
                }
            }

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

    fun deletePlayer(playerId: Long) {
        viewModelScope.launch {
            val player = repository.getPlayerById(playerId)
            repository.deletePlayer(playerId)
            if (_selectedPlayer.value?.id == playerId) {
                _selectedPlayer.value = null
            }
            showInAppNotification(
                "Pemain Dihapus",
                "${player?.name ?: "Pemain"} berhasil dihapus dari database."
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

            val currentPlayers = repository.getAllPlayersList()
            if (cleanHandle != "-") {
                val cleanCheck = cleanHandle.removePrefix("@")
                if (currentPlayers.any { it.id != playerId && it.handle.removePrefix("@") != "-" && it.handle.removePrefix("@").equals(cleanCheck, ignoreCase = true) }) {
                    showInAppNotification(
                        "Penyimpanan Gagal",
                        "Username '$cleanHandle' sudah digunakan oleh pemain lain!"
                    )
                    return@launch
                }
            }

            if (cleanUid != "-") {
                if (currentPlayers.any { it.id != playerId && it.bandaiUid != "-" && it.bandaiUid.equals(cleanUid, ignoreCase = true) }) {
                    showInAppNotification(
                        "Penyimpanan Gagal",
                        "UID Bandai+ '$cleanUid' sudah terdaftar pada pemain lain!"
                    )
                    return@launch
                }
            }

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
            ).onSuccess {
                val formatLabel = if (matchFormat.equals("BO1", ignoreCase = true)) "Best of 1 (1 Game)" else "Best of 3"
                showInAppNotification(
                    "Turnamen Baru Dibuat",
                    "$name diselenggarakan oleh $organizerName format $formatLabel dengan $totalRounds ronde."
                )
            }.onFailure {
                showInAppNotification("Gagal Membuat Turnamen", it.message ?: "Terjadi kesalahan.")
            }
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

    fun importPlayers(players: List<PlayerEntity>) {
        viewModelScope.launch {
            if (players.isEmpty()) {
                showInAppNotification("Import Gagal", "Tidak ada data peserta yang valid dalam file CSV.")
                return@launch
            }
            val currentPlayers = repository.getAllPlayersList()
            var addedCount = 0
            var skippedCount = 0

            players.forEach { p ->
                val name = p.name.trim()
                if (name.isBlank()) {
                    skippedCount++
                    return@forEach
                }

                val cleanHandle = if (p.handle.isBlank() || p.handle.trim() == "-") "-" else if (p.handle.startsWith("@")) p.handle.trim() else "@${p.handle.trim()}"
                val cleanUid = CsvHelper.cleanBandaiUid(p.bandaiUid)

                val isDupHandle = cleanHandle != "-" && currentPlayers.any {
                    it.handle.removePrefix("@") != "-" && it.handle.removePrefix("@").equals(cleanHandle.removePrefix("@"), ignoreCase = true)
                }
                val isDupUid = cleanUid != "-" && currentPlayers.any {
                    it.bandaiUid != "-" && it.bandaiUid.equals(cleanUid, ignoreCase = true)
                }

                if (isDupHandle || isDupUid) {
                    skippedCount++
                } else {
                    val toAdd = p.copy(
                        name = name,
                        handle = cleanHandle,
                        bandaiUid = cleanUid,
                        deckArchetype = p.deckArchetype.trim().ifBlank { "Unknown" },
                        deckColor = p.deckColor.trim().ifBlank { "UNKNOWN" },
                        avatarId = (1..8).random()
                    )
                    repository.addPlayer(toAdd)
                    addedCount++
                }
            }

            val msg = if (skippedCount > 0) {
                "$addedCount peserta berhasil ditambahkan ($skippedCount dilewati karena UID/Handle duplikat)."
            } else {
                "$addedCount peserta berhasil ditambahkan ke turnamen."
            }
            showInAppNotification("Import Selesai", msg)
        }
    }

    fun importTournament(tournament: TournamentEntity, matches: List<MatchEntity>) {
        viewModelScope.launch {
            val newId = repository.insertImportedTournament(tournament)
            val matchesWithId = matches.map { it.copy(tournamentId = newId) }
            repository.insertMatches(matchesWithId)
            showInAppNotification("Import Turnamen Berhasil", "Turnamen ${tournament.name} telah diimpor.")
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
