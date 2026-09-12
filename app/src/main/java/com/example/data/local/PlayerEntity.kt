package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DigimonColor

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val handle: String,
    val deckArchetype: String,
    val deckColor: String = "PURPLE", // Can be single or comma-separated e.g. "RED,PURPLE"
    val totalTournaments: Int = 1,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalDraws: Int = 0,
    val trophies: Int = 0,
    val isRegistered: Boolean = true,
    val avatarId: Int = 1,
    val isDropped: Boolean = false,
    val userId: Long? = null
) {
    val winRatePercent: Double
        get() {
            val total = totalWins + totalLosses + totalDraws
            return if (total > 0) (totalWins.toDouble() / total.toDouble()) * 100.0 else 0.0
        }

    val colorsList: List<String>
        get() = deckColor.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }

    val colorObjects: List<DigimonColor>
        get() = colorsList.map { DigimonColor.fromString(it) }
}
