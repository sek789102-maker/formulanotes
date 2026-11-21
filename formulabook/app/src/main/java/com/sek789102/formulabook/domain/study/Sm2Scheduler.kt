package com.sek789102.formulabook.domain.study

import com.sek789102.formulabook.data.model.FormulaBlock
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

enum class Sm2Rating {
    HARD,
    GOOD,
    EASY
}

object Sm2Scheduler {

    fun previewIntervalDays(block: FormulaBlock, rating: Sm2Rating): Int {
        val ef = block.easeFactor.coerceAtLeast(1.3)
        return when (rating) {
            Sm2Rating.HARD -> 1
            Sm2Rating.GOOD -> when (block.repetitions) {
                0 -> 1
                1 -> 6
                else -> max(1, (block.intervalDays.coerceAtLeast(1) * ef).roundToInt())
            }
            Sm2Rating.EASY -> when (block.repetitions) {
                0 -> 4
                1 -> 6
                else -> max(1, (block.intervalDays.coerceAtLeast(1) * ef * 1.3).roundToInt())
            }
        }
    }

    fun scheduleReview(
        block: FormulaBlock,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): FormulaBlock {
        val prevEf = block.easeFactor.coerceAtLeast(1.3)
        val intervalDays = previewIntervalDays(block, rating)

        val (nextRepetitions, nextEf, mastered) = when (rating) {
            Sm2Rating.HARD -> Triple(
                0,
                max(1.3, prevEf - 0.15),
                false
            )
            Sm2Rating.GOOD -> Triple(
                block.repetitions + 1,
                prevEf,
                true
            )
            Sm2Rating.EASY -> Triple(
                block.repetitions + 1,
                max(1.3, prevEf + 0.15),
                true
            )
        }

        val nextReviewAt = Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(nowMillis)
            add(Calendar.DAY_OF_YEAR, intervalDays)
        }.timeInMillis

        return block.copy(
            mastered = mastered,
            repetitions = nextRepetitions,
            intervalDays = intervalDays,
            easeFactor = nextEf,
            nextReviewAtMillis = nextReviewAt,
            lastReviewedAtMillis = nowMillis
        )
    }

    fun isDueForReview(
        block: FormulaBlock,
        endOfTodayMillis: Long
    ): Boolean {
        if (block.nextReviewAtMillis == 0L) {
            // Never scheduled via SM-2 yet: due unless previously marked mastered in legacy data
            return !block.mastered
        }
        return block.nextReviewAtMillis <= endOfTodayMillis
    }

    fun endOfDayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun startOfDayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
