package com.sek789102.jotwai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.ui.screens.EditorScreen
import com.sek789102.jotwai.ui.screens.HistoryScreen
import com.sek789102.jotwai.ui.screens.HomeScreen
import com.sek789102.jotwai.ui.screens.LabelsScreen
import com.sek789102.jotwai.ui.screens.SettingsScreen
import com.sek789102.jotwai.ui.theme.JotwaiTheme
import com.sek789102.jotwai.ui.theme.isDarkTheme
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import com.sek789102.jotwai.util.DeviceAuth
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Screens. Stored as strings so the back stack survives rotation and process death. */
sealed interface Route {
    data object Home : Route
    data class Editor(val id: Long, val focusBody: Boolean = false) : Route
    data class History(val id: Long) : Route
    data object Settings : Route
    data object Labels : Route

    fun encode(): String = when (this) {
        Home -> "home"
        is Editor -> "editor:$id:$focusBody"
        is History -> "history:$id"
        Settings -> "settings"
        Labels -> "labels"
    }

    companion object {
        fun decode(s: String): Route {
            val p = s.split(':')
            return when (p[0]) {
                "editor" -> Editor(p.getOrNull(1)?.toLongOrNull() ?: 0L, p.getOrNull(2) == "true")
                "history" -> History(p.getOrNull(1)?.toLongOrNull() ?: 0L)
                "settings" -> Settings
                "labels" -> Labels
                else -> Home
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    private val appVm: AppViewModel by viewModels()
    private val incoming = MutableStateFlow<Intent?>(null)
    private var stoppedAt = 0L
    private var legacyAuthCallback: ((Boolean) -> Unit)? = null
    private val legacyAuth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        legacyAuthCallback?.invoke(r.resultCode == RESULT_OK)
        legacyAuthCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) incoming.value = intent

        setContent {
            val prefs by appVm.prefs.collectAsStateWithLifecycle()
            val p = prefs ?: return@setContent // wait for settings to avoid a theme flash
            val dark = isDarkTheme(p.themeMode)
            JotwaiTheme(themeMode = p.themeMode, dynamicColor = p.dynamicColor, textScale = p.textSize.scale) {
                // Lets automated UI checks find elements by their test tags.
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.semantics { testTagsAsResourceId = true }
                ) { AppRoot(dark) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = intent
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Locked notes close again after the app has been in the background for a minute.
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS) appVm.lockAll()
        stoppedAt = 0L
    }

    private fun authenticate(onResult: (Boolean) -> Unit) {
        DeviceAuth.authenticate(
            activity = this,
            title = getString(R.string.unlock_title),
            legacyLauncher = { i ->
                legacyAuthCallback = onResult
                legacyAuth.launch(i)
            },
            onResult = onResult
        )
    }

    @Composable
    private fun AppRoot(dark: Boolean) {
        val prefs by appVm.prefs.collectAsStateWithLifecycle()
        val p = prefs ?: return
        val unlocked by appVm.unlocked.collectAsStateWithLifecycle()
        val allNotes by appVm.allNotes.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        val stack: SnapshotStateList<Route> = rememberSaveable(
            saver = listSaver<SnapshotStateList<Route>, String>(
                save = { list -> list.map { it.encode() } },
                restore = { saved -> mutableStateListOf(*saved.map { Route.decode(it) }.toTypedArray()) }
            )
        ) { mutableStateListOf(Route.Home) }
        val current = stack.last()

        fun push(r: Route) { stack.add(r) }
        fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

        fun openNote(id: Long, focusBody: Boolean = false, replace: Boolean = false) {
            val note = allNotes?.firstOrNull { it.note.id == id }?.note
            val go = {
                if (replace) pop()
                push(Route.Editor(id, focusBody))
            }
            when {
                note == null -> go()
                !note.isLocked || id in unlocked -> go()
                !DeviceAuth.isAvailable(this) -> {
                    appVm.markUnlocked(id)
                    go()
                }
                else -> authenticate { ok ->
                    if (ok) {
                        appVm.markUnlocked(id)
                        go()
                    }
                }
            }
        }

        fun newNote(type: NoteType, text: String = "", title: String = "", images: List<String> = emptyList()) {
            appVm.createNote(type, content = text, title = title, images = images) { id ->
                push(Route.Editor(id, focusBody = type == NoteType.TEXT && text.isEmpty()))
            }
        }

        // One place shows snackbar messages, whichever screen is open.
        LaunchedEffect(Unit) {
            appVm.messages.collect { m ->
                val r = snackbar.showSnackbar(m.text, actionLabel = m.undoLabel, withDismissAction = m.undoLabel == null)
                if (r == SnackbarResult.ActionPerformed) m.onUndo?.invoke()
            }
        }

        // Widget, notification, shortcut and "share to app" entry points.
        val pending by incoming.collectAsStateWithLifecycle()
        LaunchedEffect(pending, allNotes != null) {
            val i = pending ?: return@LaunchedEffect
            if (allNotes == null) return@LaunchedEffect
            incoming.value = null
            handleIntent(i, open = { id -> stack.removeAll { it != Route.Home }; openNote(id) }, create = ::newNote)
        }

        // A locked note that got re-locked (app was in the background) closes.
        LaunchedEffect(unlocked, current, allNotes) {
            val r = current
            val id = when (r) {
                is Route.Editor -> r.id
                is Route.History -> r.id
                else -> return@LaunchedEffect
            }
            val n = allNotes?.firstOrNull { it.note.id == id }?.note ?: return@LaunchedEffect
            if (n.isLocked && id !in unlocked && DeviceAuth.isAvailable(this@MainActivity)) {
                stack.removeAll { it != Route.Home }
            }
        }

        BackHandler(enabled = stack.size > 1 && current !is Route.Editor) { pop() }

        when (val r = current) {
            Route.Home -> HomeScreen(
                vm = appVm,
                prefs = p,
                darkTheme = dark,
                snackbar = snackbar,
                onOpenNote = { openNote(it) },
                onNewNote = { newNote(it) },
                onOpenSettings = { push(Route.Settings) },
                onManageLabels = { push(Route.Labels) }
            )
            is Route.Editor -> EditorScreen(
                noteId = r.id,
                appVm = appVm,
                prefs = p,
                darkTheme = dark,
                focusBodyOnOpen = r.focusBody,
                snackbar = snackbar,
                onBack = { pop() },
                onOpenHistory = { push(Route.History(r.id)) },
                onOpenNote = { id -> openNote(id, replace = true) }
            )
            is Route.History -> HistoryScreen(noteId = r.id, onBack = { pop() }, onRestored = { pop() })
            Route.Settings -> SettingsScreen(vm = appVm, prefs = p, snackbar = snackbar, onBack = { pop() })
            Route.Labels -> LabelsScreen(vm = appVm, onBack = { pop() })
        }
    }

    private fun handleIntent(
        i: Intent,
        open: (Long) -> Unit,
        create: (NoteType, String, String, List<String>) -> Unit
    ) {
        when (i.action) {
            ACTION_OPEN_NOTE -> i.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it > 0 }?.let(open)
            ACTION_NEW_NOTE -> create(NoteType.TEXT, "", "", emptyList())
            ACTION_NEW_CHECKLIST -> create(NoteType.CHECKLIST, "", "", emptyList())
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                val text = i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = i.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().take(200)
                val uris: List<Uri> = if (i.action == Intent.ACTION_SEND) {
                    listOfNotNull(streamExtra(i))
                } else {
                    streamListExtra(i)
                }
                lifecycleScope.launch {
                    val images = withContext(Dispatchers.IO) {
                        uris.take(10).mapNotNull { NoteImageStorage.saveCompressedImageFromUri(this@MainActivity, it) }
                    }
                    if (text.isNotBlank() || images.isNotEmpty()) create(NoteType.TEXT, text, subject, images)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun streamExtra(i: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else i.getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun streamListExtra(i: Intent): List<Uri> =
        (if (Build.VERSION.SDK_INT >= 33) i.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) else i.getParcelableArrayListExtra(Intent.EXTRA_STREAM))
            .orEmpty()

    companion object {
        const val ACTION_OPEN_NOTE = "com.sek789102.jotwai.OPEN_NOTE"
        const val ACTION_NEW_NOTE = "com.sek789102.jotwai.NEW_NOTE"
        const val ACTION_NEW_CHECKLIST = "com.sek789102.jotwai.NEW_CHECKLIST"
        const val EXTRA_NOTE_ID = "note_id"
        private const val RELOCK_AFTER_MS = 60_000L

        fun openNoteIntent(context: Context, noteId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_NOTE)
                .setData(Uri.parse("jotwai://note/$noteId")) // keeps each note's PendingIntent distinct
                .putExtra(EXTRA_NOTE_ID, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun newNoteIntent(context: Context, checklist: Boolean): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(if (checklist) ACTION_NEW_CHECKLIST else ACTION_NEW_NOTE)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun homeIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
