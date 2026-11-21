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
}

data class ProjectSummary(val projectId: Long, val count: Int, val total: Double?)

@Database(entities = [ProjectEntity::class, ItemEntity::class, PriceEntity::class], version = 1, exportSchema = true)
@TypeConverters(DbConverters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ProjectDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "changkhamnuan.db")
                .build().also { instance = it }
        }
    }
}
