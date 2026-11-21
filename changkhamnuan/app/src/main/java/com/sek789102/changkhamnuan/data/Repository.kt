package com.sek789102.changkhamnuan.data

import androidx.room.withTransaction
import com.sek789102.changkhamnuan.domain.CalcResult
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.MaterialLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

class InvalidBackupException : IllegalArgumentException("Not a ช่างคำนวณ backup")

class Repository(private val db: AppDb, private val settings: SettingsRepository) {
    private val dao = db.dao()

    val projects = dao.observeProjects()
    val summaries = dao.observeSummaries()
    val prices = dao.observePrices()
    fun observeProject(id: Long) = dao.observeProject(id)
    fun observeItems(projectId: Long) = dao.observeItems(projectId)
    suspend fun getProject(id: Long) = dao.getProject(id)
    suspend fun getItems(projectId: Long) = dao.getItems(projectId)

    // ---- projects ----
    suspend fun createProject(name: String, customer: String = ""): Long = db.withTransaction {
        val no = dao.maxQuoteNo() + 1
        dao.insertProject(ProjectEntity(name = name.trim().ifBlank { "งานใหม่" }.take(80), customerName = customer.trim().take(80), quoteNo = no))
    }

    suspend fun updateProject(p: ProjectEntity) = dao.updateProject(p.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteProject(id: Long) = dao.deleteProject(id)

    suspend fun duplicateProject(id: Long): Long? = db.withTransaction {
        val p = dao.getProject(id) ?: return@withTransaction null
        val no = dao.maxQuoteNo() + 1
        val now = System.currentTimeMillis()
        val newId = dao.insertProject(p.copy(id = 0, name = p.name + " (สำเนา)", quoteNo = no, createdAt = now, updatedAt = now))
        dao.insertItems(dao.getItems(id).map { it.copy(id = 0, projectId = newId) })
        newId
    }

    private suspend fun touch(projectId: Long) {
        dao.getProject(projectId)?.let { dao.updateProject(it.copy(updatedAt = System.currentTimeMillis())) }
    }

    // ---- items ----
    /**
     * Adds a calculation's purchase list (and optional labour) to a project, filling in saved prices.
     * Lines with nothing to buy are skipped.
     */
    suspend fun addCalcResult(
        projectId: Long,
        group: String,
        result: CalcResult,
        laborRate: Double
    ) = db.withTransaction {
        val prices = dao.getPrices().associate { it.key to it.price }
        var order = dao.maxSort(projectId)
        val items = buildList {
            result.materials.filter { it.buyQty > 0 }.forEach { m: MaterialLine ->
                add(ItemEntity(projectId = projectId, kind = ItemKind.MATERIAL, name = m.name, qty = m.buyQty, unit = m.buyUnit,
                    unitPrice = prices[m.priceKey] ?: 0.0, priceKey = m.priceKey, group = group, sortOrder = ++order))
            }
            if (laborRate > 0 && result.laborBase > 0) {
                add(ItemEntity(projectId = projectId, kind = ItemKind.LABOR, name = "ค่าแรง $group",
                    qty = Math.round(result.laborBase * 100) / 100.0, unit = result.laborUnit, unitPrice = laborRate, group = group, sortOrder = ++order))
            }
        }
        dao.insertItems(items)
        touch(projectId)
    }

    suspend fun addItem(projectId: Long, kind: ItemKind, name: String, qty: Double, unit: String, unitPrice: Double) {
        dao.insertItems(listOf(ItemEntity(projectId = projectId, kind = kind, name = name.trim().take(120), qty = qty, unit = unit.trim().take(20),
            unitPrice = unitPrice, sortOrder = dao.maxSort(projectId) + 1)))
        touch(projectId)
    }

    suspend fun updateItem(item: ItemEntity) {
        dao.updateItem(item)
        touch(item.projectId)
    }

    suspend fun deleteItem(item: ItemEntity) {
        dao.deleteItem(item.id)
        touch(item.projectId)
    }

    // ---- prices ----
    suspend fun setPrice(key: String, price: Double?) {
        if (price == null || price <= 0) dao.deletePrice(key) else dao.upsertPrice(PriceEntity(key, price))
    }

    /** Puts saved prices into a project's lines that still have no price. */
    suspend fun applySavedPrices(projectId: Long): Int = db.withTransaction {
        val prices = dao.getPrices().associate { it.key to it.price }
        var n = 0
        dao.getItems(projectId).forEach { item ->
            val p = item.priceKey?.let { prices[it] }
            if (p != null && item.unitPrice == 0.0) {
                dao.updateItem(item.copy(unitPrice = p))
                n++
            }
        }
        if (n > 0) touch(projectId)
        n
    }

    // ---- backup (single JSON file) ----
    suspend fun exportBackup(out: OutputStream, s: Settings) = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("app", "ChangKhamnuan").put("version", 1).put("exportedAt", System.currentTimeMillis())
            .put("profile", JSONObject(SettingsRepository.profileToJson(s.profile)))
            .put("rates", JSONObject(SettingsRepository.ratesToJson(s.rateOverrides)))
            .put("prices", JSONObject().apply { dao.getPrices().forEach { put(it.key, it.price) } })
        val items = dao.getAllItems().groupBy { it.projectId }
        root.put("projects", JSONArray().apply {
            dao.getProjects().forEach { p ->
                put(
                    JSONObject()
                        .put("name", p.name).put("customerName", p.customerName).put("customerPhone", p.customerPhone)
                        .put("siteAddress", p.siteAddress).put("note", p.note).put("overheadPercent", p.overheadPercent)
                        .put("discount", p.discount).put("vat", p.vat).put("withholding", p.withholding)
                        .put("quoteNo", p.quoteNo).put("validDays", p.validDays).put("createdAt", p.createdAt).put("updatedAt", p.updatedAt)
                        .put("items", JSONArray().apply {
                            items[p.id].orEmpty().forEach { i ->
                                put(
                                    JSONObject().put("kind", i.kind.name).put("name", i.name).put("qty", i.qty).put("unit", i.unit)
                                        .put("unitPrice", i.unitPrice).put("priceKey", i.priceKey ?: JSONObject.NULL)
                                        .put("group", i.group).put("sortOrder", i.sortOrder)
                                )
                            }
                        })
                )
            }
        })
        out.write(root.toString(1).toByteArray(Charsets.UTF_8))
        out.flush()
    }

    /** Adds the backup's projects (skipping ones already present) and restores prices, rates and profile. */
    suspend fun importBackup(input: InputStream): Int = withContext(Dispatchers.IO) {
        val text = InputStreamReader(input, Charsets.UTF_8).readText()
        if (text.length > 20_000_000) throw InvalidBackupException()
        val root = runCatching { JSONObject(text) }.getOrNull() ?: throw InvalidBackupException()
        if (root.optString("app") != "ChangKhamnuan") throw InvalidBackupException()
        val existing = dao.getProjects().map { it.name to it.createdAt }.toSet()
        var added = 0
        db.withTransaction {
            root.optJSONObject("prices")?.let { o -> o.keys().forEach { k -> dao.upsertPrice(PriceEntity(k, o.optDouble(k))) } }
            val arr = root.optJSONArray("projects") ?: JSONArray()
            for (idx in 0 until arr.length()) {
                val o = arr.optJSONObject(idx) ?: continue
                val name = o.optString("name").take(80)
                val created = o.optLong("createdAt", System.currentTimeMillis())
                if ((name to created) in existing) continue
                val id = dao.insertProject(
                    ProjectEntity(
                        name = name, customerName = o.optString("customerName"), customerPhone = o.optString("customerPhone"),
                        siteAddress = o.optString("siteAddress"), note = o.optString("note"),
                        overheadPercent = o.optDouble("overheadPercent", 0.0), discount = o.optDouble("discount", 0.0),
                        vat = o.optBoolean("vat"), withholding = o.optBoolean("withholding"),
                        quoteNo = o.optInt("quoteNo", dao.maxQuoteNo() + 1), validDays = o.optInt("validDays", 30),
                        createdAt = created, updatedAt = o.optLong("updatedAt", created)
                    )
                )
                val its = o.optJSONArray("items") ?: JSONArray()
                dao.insertItems((0 until its.length()).mapNotNull { j ->
                    val io = its.optJSONObject(j) ?: return@mapNotNull null
                    ItemEntity(
                        projectId = id, kind = ItemKind.entries.firstOrNull { k -> k.name == io.optString("kind") } ?: ItemKind.OTHER,
                        name = io.optString("name"), qty = io.optDouble("qty", 0.0), unit = io.optString("unit"),
                        unitPrice = io.optDouble("unitPrice", 0.0),
                        priceKey = if (io.isNull("priceKey")) null else io.optString("priceKey"),
                        group = io.optString("group"), sortOrder = io.optInt("sortOrder", j)
                    )
                })
                added++
            }
        }
        root.optJSONObject("profile")?.let { settings.setProfile(SettingsRepository.profileFromJson(it.toString())) }
        root.optJSONObject("rates")?.let { settings.setRates(SettingsRepository.ratesFromJson(it.toString())) }
        added
    }
}
