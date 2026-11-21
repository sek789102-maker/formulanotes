package com.sek789102.formulabook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_countdowns")
data class ExamCountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val subject: String = SubjectCategory.MATH.name,
    val examDateMillis: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
