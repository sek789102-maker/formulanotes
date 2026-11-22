package com.sek789102.changkhamnuan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sek789102.changkhamnuan.domain.ItemKind
import kotlinx.coroutines.flow.Flow

/** A job / customer quotation. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val siteAddress: String = "",
    val note: String = "",
    val overheadPercent: Double = 0.0,
    val discount: Double = 0.0,
    val vat: Boolean = false,
    val withholding: Boolean = false,
    /** Running number shown on the quote, e.g. QT-2026-0007. */
    val quoteNo: Int = 0,
    val validDays: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val kind: ItemKind,
    val name: String,
    val qty: Double,
    val unit: String,
    val unitPrice: Double,
    /** Catalog key for materials from a calculator (used to fill in saved prices). */
    val priceKey: String? = null,
    /** e.g. "ก่ออิฐ · ผนังห้องนอน" — which calculation produced it. */
    val group: String = "",
    val sortOrder: Int = 0
)

/** A payment stage (งวดงาน). [percent] > 0 means a share of the job total; otherwise [amount] in baht. */
@Entity(
    tableName = "installments",
    foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val percent: Double = 0.0,
    val amount: Double = 0.0,
    /** 0 = not paid yet. */
    val paidAt: Long = 0L,
    val sortOrder: Int = 0
)

/** Real money spent on a job: daily wages (LABOR: qty = man-days) or site expenses (MATERIAL / OTHER). */
@Entity(
    tableName = "costs",
    foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class CostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val date: Long,
    val kind: ItemKind,
    val title: String,
    val qty: Double,
    val unitCost: Double,
    val note: String = ""
) {
    val amount: Double get() = qty * unitCost
}

@Entity(tableName = "prices")
data class PriceEntity(@PrimaryKey val key: String, val price: Double)

class DbConverters {
    @TypeConverter fun kindToString(k: ItemKind): String = k.name
    @TypeConverter fun stringToKind(s: String?): ItemKind = ItemKind.entries.firstOrNull { it.name == s } ?: ItemKind.OTHER
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Insert
    suspend fun insertProject(p: ProjectEntity): Long

    @Update
    suspend fun updateProject(p: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("SELECT COALESCE(MAX(quoteNo), 0) FROM projects")
    suspend fun maxQuoteNo(): Int

    @Query("SELECT * FROM items WHERE projectId = :projectId ORDER BY sortOrder, id")
    fun observeItems(projectId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE projectId = :projectId ORDER BY sortOrder, id")
    suspend fun getItems(projectId: Long): List<ItemEntity>

    @Query("SELECT * FROM items")
    suspend fun getAllItems(): List<ItemEntity>

    @Query("SELECT projectId AS projectId, COUNT(*) AS count, SUM(qty * unitPrice) AS total FROM items GROUP BY projectId")
    fun observeSummaries(): Flow<List<ProjectSummary>>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM items WHERE projectId = :projectId")
    suspend fun maxSort(projectId: Long): Int

    @Insert
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM prices")
    fun observePrices(): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices")
    suspend fun getPrices(): List<PriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrice(p: PriceEntity)

    @Query("DELETE FROM prices WHERE `key` = :key")
    suspend fun deletePrice(key: String)

    // ---- installments ----
    @Query("SELECT * FROM installments WHERE projectId = :projectId ORDER BY sortOrder, id")
    fun observeInstallments(projectId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE projectId = :projectId ORDER BY sortOrder, id")
    suspend fun getInstallments(projectId: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallments(): List<InstallmentEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM installments WHERE projectId = :projectId")
    suspend fun maxInstallmentSort(projectId: Long): Int

    @Insert
    suspend fun insertInstallments(list: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(i: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :id")
    suspend fun deleteInstallment(id: Long)

    // ---- costs ----
    @Query("SELECT * FROM costs WHERE projectId = :projectId ORDER BY date DESC, id DESC")
    fun observeCosts(projectId: Long): Flow<List<CostEntity>>

    @Query("SELECT * FROM costs")
    suspend fun getAllCosts(): List<CostEntity>

    @Insert
    suspend fun insertCosts(list: List<CostEntity>)

    @Update
    suspend fun updateCost(c: CostEntity)

    @Query("DELETE FROM costs WHERE id = :id")
    suspend fun deleteCost(id: Long)
}

data class ProjectSummary(val projectId: Long, val count: Int, val total: Double?)

@Database(
    entities = [ProjectEntity::class, ItemEntity::class, PriceEntity::class, InstallmentEntity::class, CostEntity::class],
    version = 2, exportSchema = true
)
@TypeConverters(DbConverters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ProjectDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "changkhamnuan.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }

        /** 1.3: payment stages and real job costs. Existing jobs, items and prices are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `installments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `percent` REAL NOT NULL, `amount` REAL NOT NULL, `paidAt` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_projectId` ON `installments` (`projectId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `costs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, `qty` REAL NOT NULL, `unitCost` REAL NOT NULL, `note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_costs_projectId` ON `costs` (`projectId`)")
            }
        }
    }
}
