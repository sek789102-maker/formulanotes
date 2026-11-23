package com.sek789102.jotwai.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sek789102.jotwai.JotwaiApp
import com.sek789102.jotwai.MainActivity
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.ui.theme.NoteColors

private data class WidgetNote(val id: Long, val title: String, val preview: String, val color: Int)

/** Home-screen widget: pinned notes first, then the most recently edited, plus a "new note" button. */
class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as JotwaiApp
        val notes = runCatching {
            app.repository.getAllNotes()
                .map { it.note }
                .filter { !it.isTrashed && !it.isArchived && !it.isLocked }
                .sortedWith(compareByDescending<com.sek789102.jotwai.data.model.NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                .take(12)
                .map { n ->
                    val preview = if (n.type == NoteType.CHECKLIST) {
                        n.checklist.filter { !it.checked }.take(3).joinToString("\n") { "☐ ${it.text}" }
                    } else {
                        com.sek789102.jotwai.domain.TextEditing.toPlainText(n.content.take(400)).take(140)
                    }
                    WidgetNote(n.id, n.title, preview, n.color)
                }
        }.getOrDefault(emptyList())

        provideContent {
            GlanceTheme { Content(context, notes) }
        }
    }

    @Composable
    private fun Content(context: Context, notes: List<WidgetNote>) {
        val bg = ColorProvider(Color(0xFFFFFBF3))
        val ink = ColorProvider(Color(0xFF2B2118))
        val muted = ColorProvider(Color(0xFF6E6257))
        Column(
            modifier = GlanceModifier.fillMaxSize().background(bg).cornerRadius(20.dp).padding(10.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(MainActivity.homeIntent(context)))
                )
                Box(
                    modifier = GlanceModifier.size(36.dp).background(ColorProvider(Color(0xFFE8871E))).cornerRadius(18.dp)
                        .clickable(actionStartActivity(MainActivity.newNoteIntent(context, checklist = false))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (notes.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_empty),
                    style = TextStyle(color = muted, fontSize = 13.sp)
                )
            } else {
                LazyColumn {
                    items(notes, itemId = { it.id }) { n ->
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Column(
                                modifier = GlanceModifier.fillMaxWidth()
                                    .background(ColorProvider(NoteColors.light(n.color)))
                                    .cornerRadius(12.dp)
                                    .padding(10.dp)
                                    .clickable(actionStartActivity(MainActivity.openNoteIntent(context, n.id)))
                            ) {
                                if (n.title.isNotBlank()) {
                                    Text(n.title, maxLines = 1, style = TextStyle(color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                                }
                                if (n.preview.isNotBlank()) {
                                    Text(n.preview, maxLines = 3, style = TextStyle(color = muted, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { NotesWidget().updateAll(context) }
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
