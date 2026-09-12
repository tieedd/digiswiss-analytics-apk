package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateText: String,
    val totalRounds: Int = 3,
    val currentRound: Int = 1,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val roundDurationMinutes: Int = 45,
    val location: String = "DigiLabs Community Center",
    val matchFormat: String = "BO3", // "BO1" or "BO3"
    val organizerId: Long? = null,
    val organizerName: String = "Toko A - DigiLabs Jakarta",
    val enrolledPlayerIds: String = "", // Comma-separated list of admitted player IDs (if empty, all active players)
    val droppedPlayerIds: String = "" // Comma-separated list of dropped player IDs
) {
    val isBo1: Boolean
        get() = matchFormat.equals("BO1", ignoreCase = true)

    val formatDisplayName: String
        get() = if (isBo1) "Best of 1" else "Best of 3"

    val enrolledList: List<Long>
        get() = enrolledPlayerIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

    val droppedList: List<Long>
        get() = droppedPlayerIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

    fun isPlayerDropped(playerId: Long): Boolean {
        return droppedList.contains(playerId)
    }

    fun isPlayerEnrolled(playerId: Long): Boolean {
        return enrolledList.isEmpty() || enrolledList.contains(playerId)
    }
}
