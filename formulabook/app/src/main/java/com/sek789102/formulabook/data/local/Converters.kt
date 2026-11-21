package com.sek789102.formulabook.data.local

import androidx.room.TypeConverter
import com.sek789102.formulabook.data.model.FormulaBlock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val array = JSONArray()
        for (item in list) {
            array.put(item)
        }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFormulaBlockList(list: List<FormulaBlock>): String {
        val array = JSONArray()
        for (block in list) {
            val obj = JSONObject().apply {
                put("id", block.id)
                put("title", block.title)
                put("expression", block.expression)
                put("variablesNote", block.variablesNote)
                put("mastered", block.mastered)
                put("repetitions", block.repetitions)
                put("intervalDays", block.intervalDays)
                put("easeFactor", block.easeFactor)
                put("nextReviewAtMillis", block.nextReviewAtMillis)
                put("lastReviewedAtMillis", block.lastReviewedAtMillis)
                put("blockType", block.blockType)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toFormulaBlockList(json: String?): List<FormulaBlock> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val mastered = obj.optBoolean("mastered", false)
                    val rawType = obj.optString("blockType")
                    val resolvedType = if (rawType == FormulaBlock.BLOCK_TYPE_FLASHCARD) {
                        FormulaBlock.BLOCK_TYPE_FLASHCARD
                    } else {
                        FormulaBlock.BLOCK_TYPE_FORMULA
                    }
                    add(
                        FormulaBlock(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = obj.optString("title"),
                            expression = obj.optString("expression"),
                            variablesNote = obj.optString("variablesNote"),
                            mastered = mastered,
                            repetitions = obj.optInt("repetitions", if (mastered) 1 else 0),
                            intervalDays = obj.optInt("intervalDays", if (mastered) 6 else 0),
                            easeFactor = obj.optDouble("easeFactor", 2.5),
                            nextReviewAtMillis = obj.optLong("nextReviewAtMillis", 0L),
                            lastReviewedAtMillis = obj.optLong("lastReviewedAtMillis", 0L),
                            blockType = resolvedType
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
