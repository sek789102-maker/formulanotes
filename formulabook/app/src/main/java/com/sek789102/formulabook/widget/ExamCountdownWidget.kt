package com.sek789102.formulabook.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sek789102.formulabook.MainActivity
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.model.resolveDisplayName
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.Sm2Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class WidgetExamUiState(
    val title: String,
    val subjectName: String,
    val dateLabel: String,
    val daysRemaining: Int
)

class ExamCountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nearestExam = withContext(Dispatchers.IO) {
            loadNearestUpcomingExam(context)
        }

        provideContent {
            GlanceTheme {
                WidgetCardContent(context = context, exam = nearestExam)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetCardContent(context: Context, exam: WidgetExamUiState?) {
        val containerBg = ColorProvider(Color(0xFFF7FAFF))
        val headerBg = ColorProvider(Color(0xFF2F80ED))
        val badgeBg = ColorProvider(Color(0xFFDCEBFF))
        val primaryText = ColorProvider(Color(0xFF1A2433))
        val secondaryText = ColorProvider(Color(0xFF5B6B82))
        val accentText = ColorProvider(Color(0xFF0B3A75))
        val whiteText = ColorProvider(Color.White)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(containerBg)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(headerBg)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = context.getString(R.string.widget_exam_header),
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(GlanceModifier.height(10.dp))

            if (exam == null) {
                Text(
                    text = context.getString(R.string.widget_exam_empty_title),
                    style = TextStyle(
                        color = primaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_exam_empty_subtitle),
                    style = TextStyle(
                        color = secondaryText,
                        fontSize = 12.sp
                    ),
                    maxLines = 2
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = exam.subjectName,
                            style = TextStyle(
                                color = accentText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = exam.title,
                            style = TextStyle(
                                color = primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = exam.dateLabel,
                            style = TextStyle(
                                color = secondaryText,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(GlanceModifier.width(10.dp))

                    Column(
                        modifier = GlanceModifier
                            .background(badgeBg)
                            .cornerRadius(14.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val countdownLabel = when {
                            exam.daysRemaining <= 0 -> context.getString(R.string.exam_days_today)
                            else -> context.getString(R.string.widget_exam_days_value, exam.daysRemaining)
                        }
                        Text(
                            text = countdownLabel,
                            style = TextStyle(
                                color = accentText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (exam.daysRemaining > 0) {
                            Text(
                                text = context.getString(R.string.widget_exam_days_unit),
                                style = TextStyle(
                                    color = accentText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadNearestUpcomingExam(context: Context): WidgetExamUiState? {
        val dao = AppDatabase.getInstance(context).noteDao()
        val todayStart = Sm2Scheduler.startOfDayMillis()
        val exams = dao.getAllExamsSnapshot()
            .filter { Sm2Scheduler.startOfDayMillis(it.examDateMillis) >= todayStart }
            .sortedBy { it.examDateMillis }
        val next = exams.firstOrNull() ?: return null

        val days = PlannerHelper.daysBetweenLocalDates(next.examDateMillis).coerceAtLeast(0)
        val subjectEntity = dao.getSubjectById(next.subject)
        val dateText = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("th-TH")).format(Date(next.examDateMillis))

        return WidgetExamUiState(
            title = next.title,
            subjectName = subjectEntity.resolveDisplayName(context),
            dateLabel = dateText,
            daysRemaining = days
        )
    }
}

class ExamCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExamCountdownWidget()
}

object ExamCountdownWidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching {
            ExamCountdownWidget().updateAll(context)
        }
    }
}
