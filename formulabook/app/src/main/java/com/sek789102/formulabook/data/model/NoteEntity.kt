package com.sek789102.formulabook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val subject: String = SubjectCategory.MATH.name,
    val content: String = "",
    val formulas: List<FormulaBlock> = emptyList(),
    val imagePaths: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val needsReview: Boolean = false,
    val paperStyle: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
