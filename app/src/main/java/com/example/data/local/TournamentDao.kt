package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY id DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getActiveTournamentFlow(): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveTournament(): TournamentEntity?

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Long): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    @Query("SELECT COUNT(*) FROM tournaments")
    suspend fun getTournamentCount(): Int
}
