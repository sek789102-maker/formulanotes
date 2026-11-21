package com.sek789102.formulabook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subject: String = SubjectCategory.MATH.name,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis()
)
