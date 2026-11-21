package com.sek789102.changkhamnuan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sek789102.changkhamnuan.data.AppDb
import com.sek789102.changkhamnuan.data.Repository
import com.sek789102.changkhamnuan.data.Settings
import com.sek789102.changkhamnuan.data.SettingsRepository
import com.sek789102.changkhamnuan.domain.Calculators
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.Rates
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDb
    private lateinit var repo: Repository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).allowMainThreadQueries().build()
        repo = Repository(db, SettingsRepository(ctx))
    }

    @After fun tearDown() = db.close()

    @Test fun `calculation goes into a project with saved prices and labour`() = runBlocking {
        repo.setPrice("brick_mon", 1.2)
        repo.setPrice("cement_bag", 150.0)
        val id = repo.createProject("ต่อเติมครัว", "คุณสมชาย")
        val res = Calculators.bricks.run(mapOf("type" to "mon_half", "length" to "4", "height" to "3"), Rates())
        repo.addCalcResult(id, "ผนังครัว", res, laborRate = 180.0)
        val items = repo.getItems(id)
        val brick = items.first { it.priceKey == "brick_mon" }
        assertEquals(1656.0, brick.qty, 0.0)
        assertEquals(1.2, brick.unitPrice, 0.0)
        assertEquals(0.0, items.first { it.priceKey == "sand" }.unitPrice, 0.0)
        val labor = items.single { it.kind == ItemKind.LABOR }
        assertEquals(12.0, labor.qty, 0.0)
        assertEquals(180.0, labor.unitPrice, 0.0)

        // A price set later fills the empty line.
        repo.setPrice("sand", 600.0)
        assertEquals(1, repo.applySavedPrices(id))
        assertEquals(600.0, repo.getItems(id).first { it.priceKey == "sand" }.unitPrice, 0.0)
    }

    @Test fun `quote numbers increase and duplicates copy items`() = runBlocking {
        val a = repo.createProject("A")
        val b = repo.createProject("B")
        assertEquals(repo.getProject(a)!!.quoteNo + 1, repo.getProject(b)!!.quoteNo)
        repo.addItem(a, ItemKind.OTHER, "ค่าขนส่ง", 1.0, "เที่ยว", 500.0)
        val c = repo.duplicateProject(a)!!
        assertEquals(1, repo.getItems(c).size)
        assertTrue(repo.getProject(c)!!.name.contains("สำเนา"))
        repo.deleteProject(a)
        assertEquals(0, repo.getItems(a).size)
    }

    @Test fun `backup round trip and no duplicates on second restore`() = runBlocking {
        val id = repo.createProject("บ้านคุณแดง", "แดง")
        repo.addItem(id, ItemKind.MATERIAL, "อิฐมอญ", 1000.0, "ก้อน", 1.2)
        repo.setPrice("sand", 650.0)
        val out = ByteArrayOutputStream()
        repo.exportBackup(out, Settings())

        val db2 = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).allowMainThreadQueries().build()
        try {
            val repo2 = Repository(db2, SettingsRepository(ctx))
            assertEquals(1, repo2.importBackup(ByteArrayInputStream(out.toByteArray())))
            val p = db2.dao().getProjects().single()
            assertEquals("แดง", p.customerName)
            assertEquals(1000.0, repo2.getItems(p.id).single().qty, 0.0)
            assertEquals(650.0, db2.dao().getPrices().single { it.key == "sand" }.price, 0.0)
            assertEquals(0, repo2.importBackup(ByteArrayInputStream(out.toByteArray())))
        } finally {
            db2.close()
        }
    }
}
