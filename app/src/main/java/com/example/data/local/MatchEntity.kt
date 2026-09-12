package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tournamentId: Long,
    val roundNumber: Int,
    val tableNumber: Int,
    val player1Id: Long,
    val player2Id: Long?, // null for BYE
    val p1Score: Int = 0, // games won in match (0-2)
    val p2Score: Int = 0, // games won in match (0-2)
    val isDraw: Boolean = false,
    val isBye: Boolean = false,
    val winnerId: Long? = null,
    val isReported: Boolean = false,
    val matchDurationMinutes: Int = 0,
    val firstTurnPlayerId: Long? = null, // which player took turn 1
    val isConceded: Boolean = false,
    val concededByPlayerId: Long? = null
)
