package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, PlayerEntity::class, TournamentEntity::class, MatchEntity::class],
    version = 6,
    exportSchema = false
)
abstract class DigiSwissDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playerDao(): PlayerDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile private var INSTANCE: DigiSwissDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bersihkan duplikat username dulu, kalau tidak index unik gagal dibuat.
                db.execSQL(
                    """
                    DELETE FROM users WHERE id NOT IN (
                        SELECT MIN(id) FROM users GROUP BY username
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users(username)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_matches_tournamentId_roundNumber ON matches(tournamentId, roundNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_matches_player1Id ON matches(player1Id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_matches_player2Id ON matches(player2Id)")
            }
        }

        fun getDatabase(context: Context): DigiSwissDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DigiSwissDatabase::class.java,
                    "digiswiss_tcg.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
