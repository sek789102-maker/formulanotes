package com.sek789102.changkhamnuan.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sek789102.changkhamnuan.App
import com.sek789102.changkhamnuan.data.BusinessProfile
import com.sek789102.changkhamnuan.data.InvalidBackupException
import com.sek789102.changkhamnuan.data.ItemEntity
import com.sek789102.changkhamnuan.data.ProjectEntity
import com.sek789102.changkhamnuan.data.ProjectSummary
import com.sek789102.changkhamnuan.data.Settings
import com.sek789102.changkhamnuan.data.ThemeMode
import com.sek789102.changkhamnuan.domain.CalcResult
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.Rates
import com.sek789102.changkhamnuan.util.QuoteRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val repo = app.repo
    private val settingsRepo = app.settingsRepo

    val settings: StateFlow<Settings?> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val rates: StateFlow<Rates> = settingsRepo.settings.map { Rates(it.rateOverrides) }.stateIn(viewModelScope, SharingStarted.Eagerly, Rates())
    val projects: StateFlow<List<ProjectEntity>?> = repo.projects.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val summaries: StateFlow<Map<Long, ProjectSummary>> =
        repo.summaries.map { l -> l.associateBy { it.projectId } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val prices: StateFlow<Map<String, Double>> =
        repo.prices.map { l -> l.associate { it.key to it.price } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    fun say(text: String) { _messages.tryEmit(text) }

    fun observeProject(id: Long) = repo.observeProject(id)
    fun observeItems(id: Long) = repo.observeItems(id)

    // ---- projects ----
    fun createProject(name: String, customer: String = "", then: (Long) -> Unit = {}) = viewModelScope.launch {
        then(repo.createProject(name, customer))
    }

    fun updateProject(p: ProjectEntity) = viewModelScope.launch { repo.updateProject(p) }
    fun deleteProject(id: Long) = viewModelScope.launch { repo.deleteProject(id); say("ลบงานแล้ว") }
    fun duplicateProject(id: Long, then: (Long) -> Unit) = viewModelScope.launch {
        repo.duplicateProject(id)?.let { say("ทำสำเนาแล้ว"); then(it) }
    }

    fun addCalcResult(projectId: Long, group: String, result: CalcResult, laborRate: Double, projectName: String) = viewModelScope.launch {
        repo.addCalcResult(projectId, group, result, laborRate)
        say("เพิ่มลงงาน “$projectName” แล้ว")
    }

    fun addItem(projectId: Long, kind: ItemKind, name: String, qty: Double, unit: String, price: Double) =
        viewModelScope.launch { repo.addItem(projectId, kind, name, qty, unit, price) }

    fun updateItem(item: ItemEntity) = viewModelScope.launch { repo.updateItem(item) }
    fun deleteItem(item: ItemEntity) = viewModelScope.launch { repo.deleteItem(item) }

    fun applySavedPrices(projectId: Long) = viewModelScope.launch {
        val n = repo.applySavedPrices(projectId)
        say(if (n > 0) "ใส่ราคาแล้ว $n รายการ" else "ไม่มีรายการที่ต้องใส่ราคา (ตั้งราคาได้ที่แท็บ “ราคาวัสดุ”)")
    }

    // ---- prices & settings ----
    fun setPrice(key: String, price: Double?) = viewModelScope.launch { repo.setPrice(key, price) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(t) }
    fun setProfile(p: BusinessProfile) = viewModelScope.launch { settingsRepo.setProfile(p); say("บันทึกข้อมูลร้านแล้ว") }
    fun setRate(key: String, value: Double?) = viewModelScope.launch {
        val cur = settings.value?.rateOverrides.orEmpty().toMutableMap()
        if (value == null) cur.remove(key) else cur[key] = value
        settingsRepo.setRates(cur)
    }
    fun resetRates() = viewModelScope.launch { settingsRepo.setRates(emptyMap()); say("คืนค่ามาตรฐานทั้งหมดแล้ว") }
    fun setOnboarded() = viewModelScope.launch { settingsRepo.setOnboarded() }

    // ---- quote sharing ----
    /** Builds the quote file in the cache folder and returns a share intent for it. */
    suspend fun quoteShareIntent(projectId: Long, asImage: Boolean): Intent? = withContext(Dispatchers.IO) {
        runCatching {
            val p = repo.getProject(projectId) ?: return@runCatching null
            val items = repo.getItems(projectId)
            val profile = settingsRepo.settings.first().profile
            val renderer = QuoteRenderer(app)
            val dir = File(app.cacheDir, "quotes").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val base = renderer.quoteNumber(p)
            val file = File(dir, if (asImage) "$base.png" else "$base.pdf")
            if (asImage) renderer.writePng(p, items, profile, file) else renderer.writePdf(p, items, profile, file)
            val uri: Uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND)
                .setType(if (asImage) "image/png" else "application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, "ใบเสนอราคา ${p.name}")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { say("สร้างใบเสนอราคาไม่สำเร็จ") }.getOrNull()
    }

    // ---- backup ----
    suspend fun exportBackup(out: OutputStream) {
        runCatching {
            repo.exportBackup(out, settingsRepo.settings.first())
            settingsRepo.setLastBackup(System.currentTimeMillis())
        }.onSuccess { say("สำรองข้อมูลแล้ว") }.onFailure { say("สำรองข้อมูลไม่สำเร็จ") }
    }

    suspend fun importBackup(input: InputStream) {
        runCatching { repo.importBackup(input) }
            .onSuccess { say("กู้คืนแล้ว $it งาน") }
            .onFailure { say(if (it is InvalidBackupException) "ไฟล์นี้ไม่ใช่ไฟล์สำรองของแอป" else "กู้คืนไม่สำเร็จ") }
    }
}
