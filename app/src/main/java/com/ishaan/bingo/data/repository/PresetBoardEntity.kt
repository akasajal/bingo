package com.ishaan.bingo.data.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preset_boards")
data class PresetBoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val numbers: String // comma-separated ints, e.g. "1,15,3,..."
)