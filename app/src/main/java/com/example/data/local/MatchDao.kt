package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, tableNumber ASC")
    fun getMatchesForTournamentFlow(tournamentId: Long): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber ASC, tableNumber ASC")
    suspend fun getMatchesForTournament(tournamentId: Long): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId AND roundNumber = :roundNumber ORDER BY tableNumber ASC")
    fun getMatchesForRoundFlow(tournamentId: Long, roundNumber: Int): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId AND roundNumber = :roundNumber ORDER BY tableNumber ASC")
    suspend fun getMatchesForRound(tournamentId: Long, roundNumber: Int): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE player1Id = :playerId OR player2Id = :playerId ORDER BY id DESC")
    fun getMatchesForPlayerFlow(playerId: Long): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE player1Id = :playerId OR player2Id = :playerId ORDER BY id DESC")
    suspend fun getMatchesForPlayer(playerId: Long): List<MatchEntity>

    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatchesFlow(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY id DESC")
    suspend fun getAllMatchesList(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesForTournament(tournamentId: Long)
}
