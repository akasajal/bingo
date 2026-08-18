package com.ishaan.bingo.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PresetBoardEntity::class], version = 1, exportSchema = false)
abstract class BingoDatabase : RoomDatabase() {
    abstract fun presetBoardDao(): PresetBoardDao

    companion object {
        @Volatile private var INSTANCE: BingoDatabase? = null

        fun getInstance(context: Context): BingoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BingoDatabase::class.java,
                    "bingo_database"
                ).build().also { INSTANCE = it }
            }
    }
}