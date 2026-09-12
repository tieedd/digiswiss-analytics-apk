package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    ADMIN("Admin (Master)"),
    ORGANIZER("Penyelenggara (Toko/Store)"),
    PLAYER("Pemain (Player)")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String,
    val role: String, // "ADMIN", "ORGANIZER", "PLAYER"
    val fullName: String,
    val affiliation: String = "", // e.g. "Toko A - DigiLabs Jakarta"
    val associatedPlayerId: Long? = null
) {
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)

    val isOrganizer: Boolean
        get() = role.equals("ORGANIZER", ignoreCase = true)

    val isPlayer: Boolean
        get() = role.equals("PLAYER", ignoreCase = true)
}
