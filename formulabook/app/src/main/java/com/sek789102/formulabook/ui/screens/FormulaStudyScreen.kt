package com.sek789102.formulabook.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.study.MatchCardEntry
import com.sek789102.formulabook.domain.study.MatchGameEngine
import com.sek789102.formulabook.domain.study.Sm2Rating
import com.sek789102.formulabook.domain.study.Sm2Scheduler
import com.sek789102.formulabook.ui.components.EmptyStateIllustration
import com.sek789102.formulabook.ui.components.EmptyStateView
import com.sek789102.formulabook.ui.components.FormulaEvaluatorDialog
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily
import com.sek789102.formulabook.ui.theme.SuccessBgLight
import com.sek789102.formulabook.ui.theme.SuccessFgLight
import com.sek789102.formulabook.ui.viewmodel.StudyFormulaItem
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class StudyMode {
    FLASHCARD,
    QUIZ,
    MATCH
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaStudyScreen(
    studyFormulas: List<StudyFormulaItem>,
    subjects: List<SubjectEntity>,
    onRateFormulaSm2: (Long, String, Sm2Rating) -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var studyModeName by rememberSaveable { mutableStateOf(StudyMode.FLASHCARD.name) }
    val studyMode = StudyMode.entries.firstOrNull { it.name == studyModeName } ?: StudyMode.FLASHCARD

    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyDueToday by rememberSaveable { mutableStateOf(false) }
    var onlyUnmastered by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var evalFormula by remember { mutableStateOf<FormulaBlock?>(null) }

    // Quiz state
    var quizRound by rememberSaveable { mutableIntStateOf(0) }
    var quizScore by rememberSaveable { mutableIntStateOf(0) }
    var quizStreak by rememberSaveable { mutableIntStateOf(0) }
    var selectedAnswerIndex by rememberSaveable { mutableIntStateOf(-1) }

    val endOfTodayMillis = remember(studyFormulas.size) { Sm2Scheduler.endOfDayMillis() }
    val dueTodayCount = remember(studyFormulas, endOfTodayMillis) {
        studyFormulas.count { Sm2Scheduler.isDueForReview(it.formula, endOfTodayMillis) }
    }

    val filtered = remember(studyFormulas, selectedSubjectId, onlyDueToday, onlyUnmastered, endOfTodayMillis) {
        studyFormulas.filter { item ->
            (selectedSubjectId == null || item.subjectId == selectedSubjectId) &&
                (!onlyDueToday || Sm2Scheduler.isDueForReview(item.formula, endOfTodayMillis)) &&
                (!onlyUnmastered || !item.formula.mastered)
        }
    }

    val safeIndex = if (filtered.isEmpty()) 0 else currentIndex.coerceIn(0, filtered.lastIndex)
    val masteredCount = studyFormulas.count { it.formula.mastered }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = Dimens.Space16, vertical = Dimens.Space16)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    StudentMascotAvatar(
                        sizeDp = 50,
                        drawableRes = R.drawable.img_mascot_study,
                        contentDescription = stringResource(R.string.mascot_study_desc)
                    )
                    Spacer(Modifier.width(Dimens.Space12))
                    Column {
                        Text(
                            text = stringResource(R.string.nav_study),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.study_mastered_progress, masteredCount, studyFormulas.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (studyFormulas.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = studyMode == StudyMode.FLASHCARD,
                            onClick = { studyModeName = StudyMode.FLASHCARD.name },
                            label = { Text(stringResource(R.string.study_mode_cards)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("study_mode_cards_chip")
                        )
                        FilterChip(
                            selected = studyMode == StudyMode.QUIZ,
                            onClick = {
                                studyModeName = StudyMode.QUIZ.name
                                selectedAnswerIndex = -1
                            },
                            label = { Text(stringResource(R.string.study_mode_quiz)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("study_mode_quiz_chip")
                        )
                        FilterChip(
                            selected = studyMode == StudyMode.MATCH,
                            onClick = { studyModeName = StudyMode.MATCH.name },
                            label = { Text(stringResource(R.string.study_mode_match)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("study_mode_match_chip")
                        )
                    }
                }
            }

            if (studyFormulas.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.Space8))
                LinearProgressIndicator(
                    progress = { masteredCount.toFloat() / studyFormulas.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Space8),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(Dimens.Space12))
                // SM-2 Today's Review Due Banner (Feature 8)
                val doneBannerBg = if (isDarkTheme) Color(0xFF123B2B) else SuccessBgLight
                val doneBannerFg = if (isDarkTheme) Color(0xFF6EE7B7) else SuccessFgLight
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = if (dueTodayCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        doneBannerBg
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
                    ) {
                        Icon(
                            imageVector = if (dueTodayCount > 0) Icons.Rounded.EventAvailable else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (dueTodayCount > 0) MaterialTheme.colorScheme.primary else doneBannerFg,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (dueTodayCount > 0) {
                                stringResource(R.string.study_due_today_banner, dueTodayCount)
                            } else {
                                stringResource(R.string.study_due_today_done)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dueTodayCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else doneBannerFg
                        )
                    }
                }
            }
        }

        if (studyFormulas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = stringResource(R.string.empty_study_title),
                    subtitle = stringResource(R.string.empty_study_subtitle),
                    illustration = EmptyStateIllustration.STUDY,
                    actionLabel = stringResource(R.string.action_new_note),
                    onAction = onCreateNote
                )
            }
        } else if (studyMode == StudyMode.MATCH) {
            SpeedMatchArena(
                studyFormulas = studyFormulas,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        } else if (studyMode == StudyMode.QUIZ) {
            val quizQuestionItem = studyFormulas[quizRound % studyFormulas.size]
            val quizSubject = subjects.resolveSubject(quizQuestionItem.subjectId)
            val formulaSignature = remember(studyFormulas) {
                studyFormulas.map { it.formula.id to it.formula.expression }
            }
            val choices = remember(quizRound, quizQuestionItem.formula.id, formulaSignature) {
                val correctExpr = quizQuestionItem.formula.expression
                val distractors = studyFormulas
                    .map { it.formula.expression }
                    .distinct()
                    .filter { it != correctExpr }
                    .shuffled()
                    .take(3)
                (listOf(correctExpr) + distractors).shuffled()
            }
            val correctChoiceIdx = choices.indexOf(quizQuestionItem.formula.expression)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.quiz_score, quizScore),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.quiz_streak, quizStreak),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    if (quizStreak >= 2) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudentMascotAvatar(
                                    sizeDp = 42,
                                    drawableRes = R.drawable.img_mascot_trophy,
                                    contentDescription = stringResource(R.string.mascot_trophy_desc)
                                )
                                Text(
                                    text = stringResource(R.string.quiz_streak_cheer, quizStreak),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = quizSubject.badgeBgColor()
                            ) {
                                Text(
                                    text = quizSubject.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = quizSubject.badgeFgColor(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.quiz_question_prompt),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = quizQuestionItem.formula.title.ifBlank {
                                    quizQuestionItem.noteTitle.ifBlank { stringResource(R.string.note_untitled_paren) }
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (quizQuestionItem.formula.variablesNote.isNotBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.quiz_hint_prefix,
                                        quizQuestionItem.formula.variablesNote
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (choices.size < 2) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.quiz_need_more_formulas),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    choices.forEachIndexed { idx, expr ->
                        val answered = selectedAnswerIndex != -1
                        val isCorrect = idx == correctChoiceIdx
                        val isSelected = idx == selectedAnswerIndex
                        val correctBg = if (isDarkTheme) Color(0xFF123B2B) else SuccessBgLight
                        val correctFg = if (isDarkTheme) Color(0xFF6EE7B7) else SuccessFgLight
                        val wrongBg = MaterialTheme.colorScheme.errorContainer
                        val wrongFg = MaterialTheme.colorScheme.onErrorContainer
                        val bg = when {
                            !answered -> MaterialTheme.colorScheme.surface
                            isCorrect -> correctBg
                            isSelected -> wrongBg
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        }
                        val borderColor = when {
                            !answered -> MaterialTheme.colorScheme.outlineVariant
                            isCorrect -> correctFg
                            isSelected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        val textColor = when {
                            !answered -> MaterialTheme.colorScheme.onSurface
                            isCorrect -> correctFg
                            isSelected -> wrongFg
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            onClick = {
                                if (!answered) {
                                    selectedAnswerIndex = idx
                                    if (isCorrect) {
                                        quizScore++
                                        quizStreak++
                                    } else {
                                        quizStreak = 0
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = bg,
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = expr,
                                fontFamily = if (quizQuestionItem.formula.isFlashcard) FontFamily.Default else JetBrainsMonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }

                    if (selectedAnswerIndex != -1) {
                        Button(
                            onClick = {
                                quizRound++
                                selectedAnswerIndex = -1
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.quiz_next_question))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            // Flashcard Mode with SM-2 Filters & Rating
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = onlyDueToday,
                    onClick = {
                        onlyDueToday = !onlyDueToday
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text(stringResource(R.string.study_filter_due_today, dueTodayCount)) }
                )
                FilterChip(
                    selected = onlyUnmastered,
                    onClick = {
                        onlyUnmastered = !onlyUnmastered
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text(stringResource(R.string.study_filter_unmastered)) }
                )
                subjects.forEach { cat ->
                    if (studyFormulas.any { it.subjectId == cat.id }) {
                        FilterChip(
                            selected = selectedSubjectId == cat.id,
                            onClick = {
                                selectedSubjectId = if (selectedSubjectId == cat.id) null else cat.id
                                currentIndex = 0
                                isFlipped = false
                            },
                            label = { Text(cat.displayName()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.accentStripeColor(),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.study_all_reviewed_title),
                        subtitle = stringResource(R.string.study_all_reviewed_subtitle),
                        illustration = EmptyStateIllustration.DONE,
                        actionLabel = stringResource(R.string.study_show_all_formulas),
                        onAction = {
                            onlyDueToday = false
                            onlyUnmastered = false
                            selectedSubjectId = null
                            currentIndex = 0
                            isFlipped = false
                        }
                    )
                }
            } else {
                val item = filtered[safeIndex]
                val itemSubject = subjects.resolveSubject(item.subjectId)
                val accent = itemSubject.accentStripeColor()
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 360),
                    label = "flashcard_flip"
                )

                val hardDays = Sm2Scheduler.previewIntervalDays(item.formula, Sm2Rating.HARD)
                val goodDays = Sm2Scheduler.previewIntervalDays(item.formula, Sm2Rating.GOOD)
                val easyDays = Sm2Scheduler.previewIntervalDays(item.formula, Sm2Rating.EASY)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = Dimens.SheetMaxWidth),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.study_card_counter, safeIndex + 1, filtered.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(Dimens.Space12))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 14f * density
                                }
                                .clickable { isFlipped = !isFlipped },
                            shape = RoundedCornerShape(Dimens.Radius24),
                            color = if (rotation <= 90f) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            border = BorderStroke(Dimens.BorderSelected, accent.copy(alpha = 0.55f)),
                            shadowElevation = Dimens.ElevationRaised
                        ) {
                            if (rotation <= 90f) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 240.dp)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = itemSubject.badgeBgColor()
                                        ) {
                                            Text(
                                                text = itemSubject.displayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = itemSubject.badgeFgColor(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        val (nowMs, todayStartMs) = remember(
                                            item.formula.id,
                                            item.formula.nextReviewAtMillis
                                        ) {
                                            System.currentTimeMillis() to Sm2Scheduler.startOfDayMillis()
                                        }
                                        if (item.formula.nextReviewAtMillis > nowMs) {
                                            val daysUntil = TimeUnit.MILLISECONDS.toDays(
                                                item.formula.nextReviewAtMillis - todayStartMs
                                            ).toInt().coerceAtLeast(1)
                                            Text(
                                                text = stringResource(R.string.study_sm2_next_review, daysUntil),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else if (item.formula.mastered) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = stringResource(R.string.formula_mastered),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.formula.title.ifBlank {
                                                stringResource(R.string.study_card_fallback_title, item.noteTitle)
                                            },
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.study_card_from_note, item.noteTitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Flip,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.study_tap_to_flip),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 240.dp)
                                        .graphicsLayer { rotationY = 180f }
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.formula.title.ifBlank { item.noteTitle },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.formula.expression,
                                            fontFamily = if (item.formula.isFlashcard) FontFamily.Default else JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (item.formula.isFlashcard) 20.sp else 23.sp,
                                            lineHeight = if (item.formula.isFlashcard) 28.sp else 31.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        if (item.formula.variablesNote.isNotBlank()) {
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = item.formula.variablesNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!item.formula.isFlashcard) {
                                            OutlinedButton(
                                                onClick = { evalFormula = item.formula },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(R.string.study_try_evaluate))
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onOpenNote(item.noteId) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(stringResource(R.string.study_open_note))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // SM-2 Rating Controls + Prev/Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    currentIndex = if (safeIndex > 0) safeIndex - 1 else filtered.lastIndex
                                    isFlipped = false
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.study_prev_card)
                                )
                            }

                            val hardBg = if (isDarkTheme) Color(0xFF4C1D24) else Color(0xFFFFE4E6)
                            val hardFg = if (isDarkTheme) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                            val goodBg = if (isDarkTheme) Color(0xFF1E3A6D) else Color(0xFFDBEAFE)
                            val goodFg = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
                            val easyBg = if (isDarkTheme) Color(0xFF123B2B) else Color(0xFFD1FAE5)
                            val easyFg = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)

                            Sm2RatingButton(
                                label = stringResource(R.string.study_sm2_hard),
                                days = hardDays,
                                containerColor = hardBg,
                                contentColor = hardFg,
                                onClick = {
                                    onRateFormulaSm2(item.noteId, item.formula.id, Sm2Rating.HARD)
                                    currentIndex = (safeIndex + 1) % filtered.size
                                    isFlipped = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sm2_hard_button")
                            )

                            Sm2RatingButton(
                                label = stringResource(R.string.study_sm2_good),
                                days = goodDays,
                                containerColor = goodBg,
                                contentColor = goodFg,
                                onClick = {
                                    onRateFormulaSm2(item.noteId, item.formula.id, Sm2Rating.GOOD)
                                    currentIndex = (safeIndex + 1) % filtered.size
                                    isFlipped = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sm2_good_button")
                            )

                            Sm2RatingButton(
                                label = stringResource(R.string.study_sm2_easy),
                                days = easyDays,
                                containerColor = easyBg,
                                contentColor = easyFg,
                                onClick = {
                                    onRateFormulaSm2(item.noteId, item.formula.id, Sm2Rating.EASY)
                                    currentIndex = (safeIndex + 1) % filtered.size
                                    isFlipped = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sm2_easy_button")
                            )

                            IconButton(
                                onClick = {
                                    currentIndex = (safeIndex + 1) % filtered.size
                                    isFlipped = false
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = stringResource(R.string.study_next_card)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    evalFormula?.let { f ->
        FormulaEvaluatorDialog(
            formula = f,
            onDismiss = { evalFormula = null }
        )
    }
}

@Composable
private fun Sm2RatingButton(
    label: String,
    days: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.study_sm2_interval_days, days),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedMatchArena(
    studyFormulas: List<StudyFormulaItem>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var roundSeed by rememberSaveable { mutableIntStateOf(0) }
    var bestTenths by rememberSaveable { mutableIntStateOf(0) }
    val signature = remember(studyFormulas) {
        studyFormulas.map { Triple(it.noteId, it.formula.id, it.formula.expression) }
    }
    var matchState by remember(roundSeed, signature) {
        mutableStateOf(MatchGameEngine.buildRound(studyFormulas))
    }
    var elapsedTenths by remember(roundSeed, signature) { mutableIntStateOf(0) }

    LaunchedEffect(roundSeed, signature, matchState.isCompleted, matchState.leftCards.isEmpty()) {
        if (matchState.leftCards.isEmpty() || matchState.isCompleted) return@LaunchedEffect
        while (true) {
            delay(100L)
            elapsedTenths++
        }
    }

    LaunchedEffect(matchState.mismatchPair) {
        if (matchState.mismatchPair != null) {
            delay(500L)
            matchState = MatchGameEngine.clearMismatch(matchState)
        }
    }

    LaunchedEffect(matchState.isCompleted) {
        if (matchState.isCompleted && elapsedTenths > 0) {
            bestTenths = if (bestTenths == 0) elapsedTenths else minOf(bestTenths, elapsedTenths)
        }
    }

    val formattedTime = String.format(Locale.US, "%.1fs", elapsedTenths / 10.0)
    val correctBg = if (isDarkTheme) Color(0xFF123B2B) else SuccessBgLight
    val correctFg = if (isDarkTheme) Color(0xFF6EE7B7) else SuccessFgLight

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
        ) {
            if (matchState.leftCards.size < 2) {
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius16),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.Space16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                    ) {
                        StudentMascotAvatar(
                            sizeDp = 46,
                            drawableRes = R.drawable.img_mascot_study,
                            contentDescription = stringResource(R.string.mascot_study_desc)
                        )
                        Text(
                            text = stringResource(R.string.match_need_more_items),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius16),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_pairs_progress,
                                matchState.matchedPairIds.size,
                                matchState.totalPairs
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                            Text(
                                text = stringResource(R.string.match_timer_label, formattedTime),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.match_mistakes_label, matchState.mistakes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (matchState.mistakes > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                if (matchState.isCompleted) {
                    val bestFormatted = String.format(Locale.US, "%.1fs", bestTenths / 10.0)
                    Surface(
                        shape = RoundedCornerShape(Dimens.Radius24),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(Dimens.BorderSelected, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        shadowElevation = Dimens.ElevationRaised,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_win_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.Space24),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                        ) {
                            StudentMascotAvatar(
                                sizeDp = 72,
                                drawableRes = R.drawable.img_mascot_trophy,
                                contentDescription = stringResource(R.string.mascot_trophy_desc)
                            )
                            Text(
                                text = stringResource(R.string.match_win_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(
                                    R.string.match_win_subtitle,
                                    formattedTime,
                                    matchState.accuracyPercent
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (bestTenths > 0) {
                                Surface(
                                    shape = RoundedCornerShape(Dimens.Radius12),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.match_best_time, bestFormatted),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8)
                                    )
                                }
                            }
                            Button(
                                onClick = { roundSeed++ },
                                shape = RoundedCornerShape(Dimens.Radius16),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("match_play_again_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Dimens.Space8))
                                Text(stringResource(R.string.match_play_again), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.match_instruction_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
                        ) {
                            matchState.leftCards.forEach { card ->
                                MatchTileCard(
                                    entry = card,
                                    isAnswerColumn = false,
                                    isMatched = card.pairId in matchState.matchedPairIds,
                                    isSelected = matchState.selectedLeftId == card.cardId,
                                    isMismatched = matchState.mismatchPair?.first == card.cardId,
                                    correctBg = correctBg,
                                    correctFg = correctFg,
                                    onClick = { matchState = MatchGameEngine.selectLeft(matchState, card.cardId) },
                                    modifier = Modifier.testTag("match_left_${card.cardId}")
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
                        ) {
                            matchState.rightCards.forEach { card ->
                                MatchTileCard(
                                    entry = card,
                                    isAnswerColumn = true,
                                    isMatched = card.pairId in matchState.matchedPairIds,
                                    isSelected = matchState.selectedRightId == card.cardId,
                                    isMismatched = matchState.mismatchPair?.second == card.cardId,
                                    correctBg = correctBg,
                                    correctFg = correctFg,
                                    onClick = { matchState = MatchGameEngine.selectRight(matchState, card.cardId) },
                                    modifier = Modifier.testTag("match_right_${card.cardId}")
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.Space24))
        }
    }
}

@Composable
private fun MatchTileCard(
    entry: MatchCardEntry,
    isAnswerColumn: Boolean,
    isMatched: Boolean,
    isSelected: Boolean,
    isMismatched: Boolean,
    correctBg: Color,
    correctFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isMatched -> correctBg
        isMismatched -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        isMatched -> correctFg.copy(alpha = 0.65f)
        isMismatched -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val fg = when {
        isMatched -> correctFg
        isMismatched -> MaterialTheme.colorScheme.onErrorContainer
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = !isMatched,
        shape = RoundedCornerShape(Dimens.Radius16),
        color = bg,
        border = BorderStroke(if (isSelected || isMismatched) Dimens.BorderSelected else Dimens.BorderThin, border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space12, vertical = Dimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
        ) {
            Text(
                text = entry.text,
                fontFamily = if (isAnswerColumn && !entry.isFlashcard) JetBrainsMonoFontFamily else FontFamily.Default,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isMatched) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isMatched) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = correctFg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
