package com.sek789102.formulabook

import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteEntity
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.StudySessionEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.ThemeMode
import com.sek789102.formulabook.domain.markdown.MarkdownBlock
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import com.sek789102.formulabook.domain.study.MatchGameEngine
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.QuickReferenceCatalog
import com.sek789102.formulabook.domain.study.Sm2Rating
import com.sek789102.formulabook.domain.study.Sm2Scheduler
import com.sek789102.formulabook.domain.study.StudyStatsCalculator
import com.sek789102.formulabook.domain.study.TimetableEntry
import com.sek789102.formulabook.ui.theme.BlueWhiteDarkColorScheme
import com.sek789102.formulabook.ui.theme.BlueWhiteLightColorScheme
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.PaperLineLight
import com.sek789102.formulabook.ui.viewmodel.StudyFormulaItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DomainLogicUnitTest {

    @Test
    fun `sm2 scheduler increases intervals on good and easy and resets on hard`() {
        val base = FormulaBlock(id = "f1", title = "F = ma", expression = "F = m * a")
        val day0 = 1_700_000_000_000L

        // First GOOD -> 1 day, repetitions = 1
        val afterFirstGood = Sm2Scheduler.scheduleReview(base, Sm2Rating.GOOD, day0)
        assertEquals(1, afterFirstGood.repetitions)
        assertEquals(1, afterFirstGood.intervalDays)
        assertTrue(afterFirstGood.mastered)

        // Second GOOD -> 6 days, repetitions = 2
        val afterSecondGood = Sm2Scheduler.scheduleReview(afterFirstGood, Sm2Rating.GOOD, day0 + TimeUnit.DAYS.toMillis(1))
        assertEquals(2, afterSecondGood.repetitions)
        assertEquals(6, afterSecondGood.intervalDays)

        // EASY -> longer interval & higher EF
        val afterEasy = Sm2Scheduler.scheduleReview(afterSecondGood, Sm2Rating.EASY, day0 + TimeUnit.DAYS.toMillis(7))
        assertTrue(afterEasy.intervalDays > 6)
        assertTrue(afterEasy.easeFactor > 2.5)

        // HARD -> resets repetitions to 0 and interval back to 1
        val afterHard = Sm2Scheduler.scheduleReview(afterEasy, Sm2Rating.HARD, day0 + TimeUnit.DAYS.toMillis(20))
        assertEquals(0, afterHard.repetitions)
        assertEquals(1, afterHard.intervalDays)
        assertFalse(afterHard.mastered)
    }

    @Test
    fun `sm2 due check recognizes unreviewed, due, and future cards`() {
        val now = 1_700_000_000_000L
        val endToday = Sm2Scheduler.endOfDayMillis(now)

        val fresh = FormulaBlock(title = "New", expression = "a + b")
        assertTrue(Sm2Scheduler.isDueForReview(fresh, endToday))

        val scheduledToday = Sm2Scheduler.scheduleReview(fresh, Sm2Rating.GOOD, now)
        assertFalse(Sm2Scheduler.isDueForReview(scheduledToday, endToday))

        val endTomorrow = Sm2Scheduler.endOfDayMillis(now + TimeUnit.DAYS.toMillis(1))
        assertTrue(Sm2Scheduler.isDueForReview(scheduledToday, endTomorrow))
    }

    @Test
    fun `study stats calculator computes consecutive streak and breaks on missed day`() {
        val utc = TimeZone.getTimeZone("UTC")
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val todayNoon = 1_700_050_000_000L // fixed reference UTC timestamp

        val consecutiveSessions = listOf(
            StudySessionEntity(subject = "MATH", durationMinutes = 25, completedAt = todayNoon),
            StudySessionEntity(subject = "PHYSICS", durationMinutes = 15, completedAt = todayNoon - dayMs),
            StudySessionEntity(subject = "MATH", durationMinutes = 20, completedAt = todayNoon - 2 * dayMs)
        )
        assertEquals(3, StudyStatsCalculator.computeReadingStreak(consecutiveSessions, todayNoon, utc))

        // Gap yesterday, studied 2 days ago -> streak breaks to 0 (if today also empty) or 1 (if today studied)
        val brokenSessions = listOf(
            StudySessionEntity(subject = "MATH", durationMinutes = 25, completedAt = todayNoon),
            StudySessionEntity(subject = "MATH", durationMinutes = 25, completedAt = todayNoon - 2 * dayMs)
        )
        assertEquals(1, StudyStatsCalculator.computeReadingStreak(brokenSessions, todayNoon, utc))

        val bars = StudyStatsCalculator.computeLast7DaysStats(consecutiveSessions, todayNoon, utc)
        assertEquals(7, bars.size)
        assertEquals(25, bars.last().totalMinutes)
    }

    @Test
    fun `backup reminder triggers after 14 days unless recently exported or snoozed`() {
        val now = 1_700_000_000_000L
        val fifteenDaysAgo = now - TimeUnit.DAYS.toMillis(15)
        val fiveDaysAgo = now - TimeUnit.DAYS.toMillis(5)
        val futureSnooze = now + TimeUnit.DAYS.toMillis(2)

        assertTrue(StudyStatsCalculator.shouldShowBackupReminder(1, fifteenDaysAgo, 0L, 0L, now))
        assertFalse(StudyStatsCalculator.shouldShowBackupReminder(0, fifteenDaysAgo, 0L, 0L, now))
        assertFalse(StudyStatsCalculator.shouldShowBackupReminder(3, fifteenDaysAgo, fiveDaysAgo, 0L, now))
        assertFalse(StudyStatsCalculator.shouldShowBackupReminder(3, fifteenDaysAgo, 0L, futureSnooze, now))
    }

    @Test
    fun `simple markdown parses blocks and toggles checklist lines accurately`() {
        val raw = "# หัวข้อใหญ่\n- ข้อแรก\n[ ] ทบทวน ==สูตรสำคัญ== **ด่วน**\n[x] เสร็จแล้ว"
        val blocks = SimpleMarkdown.parseBlocks(raw)
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertTrue(blocks[1] is MarkdownBlock.Bullet)
        assertTrue(blocks[2] is MarkdownBlock.Checklist)
        assertFalse((blocks[2] as MarkdownBlock.Checklist).checked)
        assertTrue((blocks[3] as MarkdownBlock.Checklist).checked)

        val toggled = SimpleMarkdown.toggleChecklistLine(raw, 2)
        val updatedBlocks = SimpleMarkdown.parseBlocks(toggled)
        assertTrue((updatedBlocks[2] as MarkdownBlock.Checklist).checked)
    }

    @Test
    fun `theme mode parser handles valid values and falls back to SYSTEM`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SYSTEM"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("INVALID_VALUE"))
    }

    @Test
    fun `blue white light and dark color schemes and dimens match design tokens`() {
        assertEquals(Color(0xFF2F80ED), BlueWhiteLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), BlueWhiteLightColorScheme.onPrimary)
        assertEquals(Color(0xFFDCEBFF), BlueWhiteLightColorScheme.primaryContainer)
        assertEquals(Color(0xFF0B3A75), BlueWhiteLightColorScheme.onPrimaryContainer)
        assertEquals(Color(0xFF56CCF2), BlueWhiteLightColorScheme.secondary)
        assertEquals(Color(0xFFE3F6FD), BlueWhiteLightColorScheme.secondaryContainer)
        assertEquals(Color(0xFF7C9CFF), BlueWhiteLightColorScheme.tertiary)
        assertEquals(Color(0xFFF7FAFF), BlueWhiteLightColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), BlueWhiteLightColorScheme.surface)
        assertEquals(Color(0xFFEEF3FB), BlueWhiteLightColorScheme.surfaceVariant)
        assertEquals(Color(0xFFC9D6EA), BlueWhiteLightColorScheme.outline)
        assertEquals(Color(0xFF1A2433), BlueWhiteLightColorScheme.onBackground)
        assertEquals(Color(0xFF1A2433), BlueWhiteLightColorScheme.onSurface)
        assertEquals(Color(0xFF5B6B82), BlueWhiteLightColorScheme.onSurfaceVariant)
        assertEquals(Color(0xFFE5484D), BlueWhiteLightColorScheme.error)
        assertEquals(Color(0xFFD6E6FA), PaperLineLight)

        assertEquals(Color(0xFF8AB8FF), BlueWhiteDarkColorScheme.primary)
        assertEquals(Color(0xFF0A2A55), BlueWhiteDarkColorScheme.onPrimary)
        assertEquals(Color(0xFF1E4A8A), BlueWhiteDarkColorScheme.primaryContainer)
        assertEquals(Color(0xFFDCEBFF), BlueWhiteDarkColorScheme.onPrimaryContainer)
        assertEquals(Color(0xFF7FD8F7), BlueWhiteDarkColorScheme.secondary)
        assertEquals(Color(0xFF0E1420), BlueWhiteDarkColorScheme.background)
        assertEquals(Color(0xFF151C2B), BlueWhiteDarkColorScheme.surface)
        assertEquals(Color(0xFF1E2738), BlueWhiteDarkColorScheme.surfaceVariant)
        assertEquals(Color(0xFFE6ECF5), BlueWhiteDarkColorScheme.onBackground)
        assertEquals(Color(0xFFE6ECF5), BlueWhiteDarkColorScheme.onSurface)
        assertEquals(Color(0xFFA3B1C6), BlueWhiteDarkColorScheme.onSurfaceVariant)
        assertEquals(Color(0xFF33405A), BlueWhiteDarkColorScheme.outline)

        assertEquals(4.dp, Dimens.Space4)
        assertEquals(8.dp, Dimens.Space8)
        assertEquals(12.dp, Dimens.Space12)
        assertEquals(16.dp, Dimens.Space16)
        assertEquals(24.dp, Dimens.Space24)
        assertEquals(32.dp, Dimens.Space32)
        assertEquals(8.dp, Dimens.Radius8)
        assertEquals(12.dp, Dimens.Radius12)
        assertEquals(16.dp, Dimens.Radius16)
        assertEquals(24.dp, Dimens.Radius24)
        assertEquals(48.dp, Dimens.MinTouchTarget)
    }

    @Test
    fun `match game engine handles pairing, mismatch tracking, and round completion`() {
        val items = listOf(
            StudyFormulaItem(1L, "ฟิสิกส์", "PHYSICS", FormulaBlock(id = "f1", title = "แรง", expression = "F = m * a")),
            StudyFormulaItem(2L, "คณิต", "MATH", FormulaBlock(id = "f2", title = "พีทาโกรัส", expression = "c² = a² + b²")),
            StudyFormulaItem(3L, "อังกฤษ", "ENGLISH", FormulaBlock(id = "f3", title = "Resilient", expression = "ฟื้นตัวเร็ว", blockType = FormulaBlock.BLOCK_TYPE_FLASHCARD))
        )

        var state = MatchGameEngine.buildRound(items, random = kotlin.random.Random(42))
        assertEquals(3, state.totalPairs)
        assertFalse(state.isCompleted)

        val firstLeft = state.leftCards.first()
        val wrongRight = state.rightCards.first { it.pairId != firstLeft.pairId }
        state = MatchGameEngine.selectLeft(state, firstLeft.cardId)
        state = MatchGameEngine.selectRight(state, wrongRight.cardId)
        assertEquals(1, state.mistakes)
        assertEquals(1, state.attempts)
        assertEquals(firstLeft.cardId to wrongRight.cardId, state.mismatchPair)

        state = MatchGameEngine.clearMismatch(state)
        assertEquals(null, state.mismatchPair)

        state.leftCards.forEach { leftCard ->
            val rightCard = state.rightCards.first { it.pairId == leftCard.pairId }
            state = MatchGameEngine.selectLeft(state, leftCard.cardId)
            state = MatchGameEngine.selectRight(state, rightCard.cardId)
        }
        assertTrue(state.isCompleted)
        assertEquals(3, state.matchedPairIds.size)
        assertEquals(75, state.accuracyPercent)
    }

    @Test
    fun `planner helper sorts daily timetable, serializes JSON, and extracts unchecked checklist tasks`() {
        val entries = listOf(
            TimetableEntry(id = "2", dayOfWeek = 1, startTime = "13:00", endTime = "14:00", subjectId = "PHYSICS", room = "301"),
            TimetableEntry(id = "1", dayOfWeek = 1, startTime = "08:30", endTime = "09:30", subjectId = "MATH", room = "102"),
            TimetableEntry(id = "3", dayOfWeek = 2, startTime = "09:00", endTime = "10:00", subjectId = "THAI", room = "204")
        )
        val monday = PlannerHelper.entriesForDay(entries, 1)
        assertEquals(2, monday.size)
        assertEquals("MATH", monday[0].subjectId)
        assertEquals("PHYSICS", monday[1].subjectId)

        val notes = listOf(
            NoteWithTags(
                note = NoteEntity(
                    id = 5L,
                    title = "การบ้านคณิต",
                    subject = "MATH",
                    content = "# งาน\n[ ] ทำแบบฝึกหัดหน้า 12\n[x] อ่านบทที่ 1\n- [ ] ทบทวนสูตร"
                ),
                tags = emptyList()
            )
        )
        val tasks = PlannerHelper.extractPendingTasks(notes)
        assertEquals(2, tasks.size)
        assertEquals("ทำแบบฝึกหัดหน้า 12", tasks[0].text)
        assertEquals(1, tasks[0].lineIndex)
        assertEquals("ทบทวนสูตร", tasks[1].text)
        assertEquals(3, tasks[1].lineIndex)
    }

    @Test
    fun `quick reference catalog filters subjects, summarizes notes, and converts items to notes`() {
        val subjects = QuickReferenceCatalog.availableSubjectIds()
        assertTrue(subjects.size >= 8)
        assertTrue("MATH" in subjects)
        assertTrue("ENGLISH" in subjects)
        assertTrue("THAI" in subjects)
        assertTrue("BIOLOGY" in subjects)

        val engItems = QuickReferenceCatalog.filterItems(SubjectCategory.ENGLISH.name, "Tense")
        assertTrue(engItems.isNotEmpty())
        assertTrue(engItems.all { it.subjectId == SubjectCategory.ENGLISH.name })

        val formulaItem = QuickReferenceCatalog.items.first { it.isFormula }
        val (formulaContent, formulaBlock) = QuickReferenceCatalog.toNoteDraftContent(formulaItem)
        assertTrue(formulaContent.contains(formulaItem.title))
        assertEquals(FormulaBlock.BLOCK_TYPE_FORMULA, formulaBlock.blockType)

        val summaryItem = QuickReferenceCatalog.items.first { !it.isFormula }
        val (_, flashcardBlock) = QuickReferenceCatalog.toNoteDraftContent(summaryItem)
        assertEquals(FormulaBlock.BLOCK_TYPE_FLASHCARD, flashcardBlock.blockType)

        val subjectEntities = SubjectCategory.entries.map { it.toEntity() }
        val notes = listOf(
            NoteWithTags(
                note = NoteEntity(
                    id = 1L,
                    title = "คำศัพท์",
                    subject = SubjectCategory.ENGLISH.name,
                    content = "test",
                    formulas = listOf(
                        FormulaBlock(id = "1", title = "a", expression = "b", mastered = true),
                        FormulaBlock(id = "2", title = "c", expression = "d", mastered = false)
                    )
                ),
                tags = emptyList()
            )
        )
        val summaries = QuickReferenceCatalog.summarizeSubjects(subjectEntities, notes)
        assertEquals(SubjectCategory.ENGLISH.name, summaries.first().subject.id)
        assertEquals(1, summaries.first().noteCount)
        assertEquals(2, summaries.first().cardCount)
        assertEquals(1, summaries.first().masteredCount)
        assertEquals(0.5f, summaries.first().masteryProgress, 0.001f)
    }

    @Test
    fun `planner helper validates HH mm times and maps UTC picker dates to local midnight across timezones`() {
        assertTrue(PlannerHelper.isValidTimeString("08:30"))
        assertTrue(PlannerHelper.isValidTimeString("8:05"))
        assertTrue(PlannerHelper.isValidTimeString("23:59"))
        assertFalse(PlannerHelper.isValidTimeString("24:00"))
        assertFalse(PlannerHelper.isValidTimeString("12:60"))
        assertFalse(PlannerHelper.isValidTimeString("abc"))
        assertEquals("08:05", PlannerHelper.normalizeTimeString("8:05"))

        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
        val newYork = TimeZone.getTimeZone("America/New_York")

        val utcCal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, java.util.Calendar.MARCH, 8, 0, 0, 0)
        }
        val utcMidnight = utcCal.timeInMillis

        val nyLocalMidnight = PlannerHelper.utcDatePickerMillisToLocalMidnight(utcMidnight, newYork)
        val nyCheck = java.util.Calendar.getInstance(newYork).apply { timeInMillis = nyLocalMidnight }
        assertEquals(2026, nyCheck.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.MARCH, nyCheck.get(java.util.Calendar.MONTH))
        assertEquals(8, nyCheck.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(0, nyCheck.get(java.util.Calendar.HOUR_OF_DAY))

        // March 8, 2026 is a 23-hour DST spring-forward day in America/New_York
        val march9Utc = utcCal.apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val nyNextMidnight = PlannerHelper.utcDatePickerMillisToLocalMidnight(march9Utc, newYork)
        assertEquals(1, PlannerHelper.daysBetweenLocalDates(nyNextMidnight, nyLocalMidnight, newYork))

        val bkkMidnight = PlannerHelper.utcDatePickerMillisToLocalMidnight(utcMidnight, bangkok)
        assertEquals(utcMidnight, PlannerHelper.localMillisToUtcMidnight(bkkMidnight, bangkok))
    }
}
