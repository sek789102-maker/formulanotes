package com.sek789102.formulabook.domain.study

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GpaCourseEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val credits: Double,
    val gradePoint: Double
)

data class GpaSummary(
    val gpa: Double,
    val totalCredits: Double,
    val courseCount: Int
)

object GpaCalculator {

    val thaiGradeScale: List<Pair<Double, String>> = listOf(
        4.0 to "4.0 (A)",
        3.5 to "3.5 (B+)",
        3.0 to "3.0 (B)",
        2.5 to "2.5 (C+)",
        2.0 to "2.0 (C)",
        1.5 to "1.5 (D+)",
        1.0 to "1.0 (D)",
        0.0 to "0.0 (F)"
    )

    val creditPresets: List<Double> = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0)

    fun computeGpa(courses: List<GpaCourseEntry>): GpaSummary {
        val valid = courses.filter { it.credits > 0.0 && it.gradePoint >= 0.0 }
        val totalCredits = valid.sumOf { it.credits }
        if (totalCredits <= 0.0) {
            return GpaSummary(gpa = 0.0, totalCredits = 0.0, courseCount = valid.size)
        }
        val weightedSum = valid.sumOf { it.credits * it.gradePoint }
        return GpaSummary(
            gpa = weightedSum / totalCredits,
            totalCredits = totalCredits,
            courseCount = valid.size
        )
    }

    fun toJson(courses: List<GpaCourseEntry>): String {
        val array = JSONArray()
        courses.forEach { c ->
            array.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("credits", c.credits)
                    put("gradePoint", c.gradePoint)
                }
            )
        }
        return array.toString()
    }

    fun fromJson(raw: String?): List<GpaCourseEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name")
                    val credits = obj.optDouble("credits", 1.0)
                    val gradePoint = obj.optDouble("gradePoint", 4.0)
                    if (credits > 0.0) {
                        add(
                            GpaCourseEntry(
                                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                name = name,
                                credits = credits,
                                gradePoint = gradePoint.coerceIn(0.0, 4.0)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
