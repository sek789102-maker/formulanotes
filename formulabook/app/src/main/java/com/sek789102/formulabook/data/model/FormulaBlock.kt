package com.sek789102.formulabook.data.model

import java.util.UUID

data class FormulaBlock(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val expression: String = "",
    val variablesNote: String = "",
    val mastered: Boolean = false,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val nextReviewAtMillis: Long = 0L,
    val lastReviewedAtMillis: Long = 0L,
    val blockType: String = BLOCK_TYPE_FORMULA
) {
    val isFlashcard: Boolean
        get() = blockType == BLOCK_TYPE_FLASHCARD

    companion object {
        const val BLOCK_TYPE_FORMULA = "FORMULA"
        const val BLOCK_TYPE_FLASHCARD = "FLASHCARD"
    }
}
