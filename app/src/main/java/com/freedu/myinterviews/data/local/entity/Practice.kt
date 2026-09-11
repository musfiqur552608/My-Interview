package com.freedu.myinterviews.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Logged AI-coach / mock-interview practice session (feature: AI interview coach). */
@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val role: String = "",
    val questionsJson: String = "",
    val answersJson: String = "",
    val score: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
