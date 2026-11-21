package com.sek789102.formulabook.domain.study

import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.domain.markdown.MarkdownBlock
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class TimetableEntry(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTime: String, // "08:30"
    val endTime: String,   // "09:30"
    val subjectId: String,
    val room: String = ""
)

data class PendingNoteTask(
    val noteId: Long,
    val noteTitle: String,
    val subjectId: String,
    val lineIndex: Int,
    val text: String
)

object PlannerHelper {

    private val TIME_REGEX = Regex("""^\s*([01]?\d|2[0-3]):([0-5]\d)\s*$""")

    fun isValidTimeString(time: String): Boolean = TIME_REGEX.matches(time)

    fun normalizeTimeString(time: String): String {
        val match = TIME_REGEX.matchEntire(time) ?: return time.trim()
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun timeMinutes(time: String): Int? {
        val match = TIME_REGEX.matchEntire(time) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return hour * 60 + minute
    }

    fun utcDatePickerMillisToLocalMidnight(
        utcMillis: Long,
        localTimeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance(localTimeZone).apply {
            clear()
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    fun localMillisToUtcMidnight(
        localMillis: Long,
        localTimeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val localCal = Calendar.getInstance(localTimeZone).apply {
            timeInMillis = localMillis
        }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis
    }

    fun daysBetweenLocalDates(
        targetMillis: Long,
        fromMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val diff = StudyStatsCalculator.toLocalDayIndex(targetMillis, timeZone) -
            StudyStatsCalculator.toLocalDayIndex(fromMillis, timeZone)
        return diff.toInt()
    }

    fun todayDayOfWeek(calendar: Calendar = Calendar.getInstance()): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun entriesForDay(entries: List<TimetableEntry>, dayOfWeek: Int): List<TimetableEntry> {
        return entries
            .filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { normalizeTimeSortKey(it.startTime) }
    }

    private fun normalizeTimeSortKey(time: String): Int {
        val parts = time.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 99
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 99
        return h * 60 + m
    }

    fun extractPendingTasks(notes: List<NoteWithTags>): List<PendingNoteTask> {
        val tasks = mutableListOf<PendingNoteTask>()
        for (item in notes) {
            val blocks = SimpleMarkdown.parseBlocks(item.note.content)
            for (block in blocks) {
                if (block is MarkdownBlock.Checklist && !block.checked && block.text.isNotBlank()) {
                    tasks.add(
                        PendingNoteTask(
                            noteId = item.note.id,
                            noteTitle = item.note.title,
                            subjectId = item.note.subject,
                            lineIndex = block.lineIndex,
                            text = block.text
                        )
                    )
                }
            }
        }
        return tasks
    }

    fun toJson(entries: List<TimetableEntry>): String {
        val arr = JSONArray()
        entries.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("dayOfWeek", entry.dayOfWeek)
                    put("startTime", entry.startTime)
                    put("endTime", entry.endTime)
                    put("subjectId", entry.subjectId)
                    put("room", entry.room)
                }
            )
        }
        return arr.toString()
    }

    fun fromJson(raw: String?): List<TimetableEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val subjectId = obj.optString("subjectId").trim()
                    if (subjectId.isEmpty()) continue
                    val day = obj.optInt("dayOfWeek", 1).coerceIn(1, 7)
                    add(
                        TimetableEntry(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            dayOfWeek = day,
                            startTime = obj.optString("startTime", "08:30"),
                            endTime = obj.optString("endTime", "09:30"),
                            subjectId = subjectId,
                            room = obj.optString("room", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
