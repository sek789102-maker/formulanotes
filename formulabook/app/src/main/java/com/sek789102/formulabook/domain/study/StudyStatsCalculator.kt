package com.sek789102.formulabook.domain.study

import com.sek789102.formulabook.data.model.StudySessionEntity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DailySubjectMinutes(
    val subjectId: String,
    val minutes: Int
)

data class DailyStudyBar(
    val dayStartMillis: Long,
    val totalMinutes: Int,
    val breakdown: List<DailySubjectMinutes>
)

object StudyStatsCalculator {

    private const val FOURTEEN_DAYS_MILLIS = 14L * 24L * 60L * 60L * 1000L

    fun toLocalDayIndex(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Convert midnight local date into an integer day ordinal (YYYY * 400 + dayOfYear avoids DST shifts)
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        // Even cleaner: compute UTC epoch day of (year, month, day)
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(utc.timeInMillis)
    }

    fun computeReadingStreak(
        sessions: List<StudySessionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        if (sessions.isEmpty()) return 0
        val studiedDays = sessions
            .filter { it.durationMinutes > 0 }
            .map { toLocalDayIndex(it.completedAt, timeZone) }
            .toHashSet()
        if (studiedDays.isEmpty()) return 0

        val todayIdx = toLocalDayIndex(nowMillis, timeZone)
        var cursor = when {
            todayIdx in studiedDays -> todayIdx
            (todayIdx - 1L) in studiedDays -> todayIdx - 1L
            else -> return 0
        }

        var streak = 0
        while (cursor in studiedDays) {
            streak++
            cursor--
        }
        return streak
    }

    fun computeLast7DaysStats(
        sessions: List<StudySessionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DailyStudyBar> {
        val todayIdx = toLocalDayIndex(nowMillis, timeZone)
        val groupedByDay = sessions
            .filter { it.durationMinutes > 0 }
            .groupBy { toLocalDayIndex(it.completedAt, timeZone) }

        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return (6 downTo 0).map { daysAgo ->
            val targetIdx = todayIdx - daysAgo
            val dayCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val daySessions = groupedByDay[targetIdx].orEmpty()
            val bySubject = daySessions
                .groupBy { it.subject }
                .map { (subjectId, list) ->
                    DailySubjectMinutes(subjectId = subjectId, minutes = list.sumOf { it.durationMinutes })
                }
                .sortedByDescending { it.minutes }

            DailyStudyBar(
                dayStartMillis = dayCal.timeInMillis,
                totalMinutes = daySessions.sumOf { it.durationMinutes },
                breakdown = bySubject
            )
        }
    }

    fun shouldShowBackupReminder(
        notesCount: Int,
        oldestNoteCreatedAtMillis: Long?,
        lastBackupExportedAtMillis: Long,
        dismissedUntilMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (notesCount <= 0) return false
        if (nowMillis < dismissedUntilMillis) return false
        val baseline = if (lastBackupExportedAtMillis > 0L) {
            lastBackupExportedAtMillis
        } else {
            oldestNoteCreatedAtMillis ?: nowMillis
        }
        return (nowMillis - baseline) >= FOURTEEN_DAYS_MILLIS
    }
}
