package com.sek789102.changkhamnuan

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sek789102.changkhamnuan.data.AppDb
import com.sek789102.changkhamnuan.data.CostEntity
import com.sek789102.changkhamnuan.data.InstallmentEntity
import com.sek789102.changkhamnuan.domain.ItemKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A 1.0–1.2 database (schema v1, with data) must open in 1.3 with its jobs intact. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test fun `version 1 database migrates to version 2`() = runBlocking {
        val name = "migration-test.db"
        ctx.deleteDatabase(name)
        val file = ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            // Exactly the v1 schema exported by Room (schemas/…/1.json).
            db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `customerName` TEXT NOT NULL, `customerPhone` TEXT NOT NULL, `siteAddress` TEXT NOT NULL, `note` TEXT NOT NULL, `overheadPercent` REAL NOT NULL, `discount` REAL NOT NULL, `vat` INTEGER NOT NULL, `withholding` INTEGER NOT NULL, `quoteNo` INTEGER NOT NULL, `validDays` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `kind` TEXT NOT NULL, `name` TEXT NOT NULL, `qty` REAL NOT NULL, `unit` TEXT NOT NULL, `unitPrice` REAL NOT NULL, `priceKey` TEXT, `group` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_projectId` ON `items` (`projectId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `prices` (`key` TEXT NOT NULL, `price` REAL NOT NULL, PRIMARY KEY(`key`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b96e9e39690a940fd51447cfdc003454')")
            db.execSQL("INSERT INTO projects VALUES (1, 'ต่อเติมครัว', 'คุณสมชาย', '', '', '', 0, 0, 0, 0, 1, 30, 1, 1)")
            db.execSQL("INSERT INTO items VALUES (1, 1, 'MATERIAL', 'อิฐมอญ', 1656, 'ก้อน', 1.2, 'brick_mon', 'ผนัง', 1)")
            db.execSQL("INSERT INTO prices VALUES ('cement_bag', 150)")
            db.version = 1
        }

        val room = Room.databaseBuilder(ctx, AppDb::class.java, name).addMigrations(AppDb.MIGRATION_1_2).allowMainThreadQueries().build()
        val dao = room.dao()
        assertEquals("ต่อเติมครัว", dao.getProject(1)!!.name)
        assertEquals(1656.0, dao.getItems(1).single().qty, 0.0)
        assertEquals(150.0, dao.getPrices().single().price, 0.0)
        dao.insertInstallments(listOf(InstallmentEntity(projectId = 1, title = "มัดจำ", percent = 50.0)))
        dao.insertCosts(listOf(CostEntity(projectId = 1, date = 1, kind = ItemKind.LABOR, title = "ช่างปูน", qty = 2.0, unitCost = 400.0)))
        assertEquals(1, dao.getInstallments(1).size)
        assertEquals(800.0, dao.getAllCosts().single().amount, 0.0)
        // Deleting a job removes its stages and costs too.
        dao.deleteProject(1)
        assertEquals(0, dao.getAllInstallments().size)
        assertEquals(0, dao.getAllCosts().size)
        room.close()
    }
}
