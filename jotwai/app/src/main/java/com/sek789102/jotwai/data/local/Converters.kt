package com.sek789102.jotwai.data.local

import androidx.room.TypeConverter
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.RepeatRule
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun stringsToJson(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun jsonToStrings(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(value)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun checklistToJson(value: List<ChecklistItem>): String = ChecklistJson.encode(value)

    @TypeConverter
    fun jsonToChecklist(value: String?): List<ChecklistItem> = ChecklistJson.decode(value)

    @TypeConverter
    fun noteTypeToString(value: NoteType): String = value.name

    @TypeConverter
    fun stringToNoteType(value: String?): NoteType =
        NoteType.entries.firstOrNull { it.name == value } ?: NoteType.TEXT

    @TypeConverter
    fun repeatToString(value: RepeatRule): String = value.name

    @TypeConverter
    fun stringToRepeat(value: String?): RepeatRule =
        RepeatRule.entries.firstOrNull { it.name == value } ?: RepeatRule.NONE
}

object ChecklistJson {
    fun toArray(items: List<ChecklistItem>): JSONArray = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked)) }
    }

    fun fromArray(arr: JSONArray?): List<ChecklistItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
                add(ChecklistItem(id = id, text = o.optString("text"), checked = o.optBoolean("checked")))
            }
        }
    }

    fun encode(items: List<ChecklistItem>): String = toArray(items).toString()

    fun decode(value: String?): List<ChecklistItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { fromArray(JSONArray(value)) }.getOrDefault(emptyList())
    }
}
