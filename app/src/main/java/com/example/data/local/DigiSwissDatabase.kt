package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PlayerEntity::class,
        TournamentEntity::class,
        MatchEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DigiSwissDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playerDao(): PlayerDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: DigiSwissDatabase? = null

        fun getDatabase(context: Context): DigiSwissDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DigiSwissDatabase::class.java,
                    "digiswiss_tcg.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
