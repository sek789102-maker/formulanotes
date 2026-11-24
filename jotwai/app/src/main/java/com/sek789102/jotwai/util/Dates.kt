package com.sek789102.jotwai.util

import android.content.Context
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.RepeatRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Dates {
    private val thMonths = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
    private val enMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private fun dayDiff(a: Long, b: Long): Int {
        val ca = Calendar.getInstance().apply { timeInMillis = a; set(Calendar.HOUR_OF_DAY, 12) }
        val cb = Calendar.getInstance().apply { timeInMillis = b; set(Calendar.HOUR_OF_DAY, 12) }
        return Math.round((cb.timeInMillis - ca.timeInMillis) / 86_400_000.0).toInt()
    }

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

    /** "12 ม.ค." / "12 Jan", with the year ("12 ม.ค. 2569" in the Buddhist era / "12 Jan 2026") when asked. */
    fun date(millis: Long, withYear: Boolean = true): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val en = Lang.isEnglish
        val dm = "${c.get(Calendar.DAY_OF_MONTH)} ${(if (en) enMonths else thMonths)[c.get(Calendar.MONTH)]}"
        if (!withYear) return dm
        return "$dm ${c.get(Calendar.YEAR) + if (en) 0 else 543}"
    }

    private fun sameYear(a: Long, b: Long) =
        Calendar.getInstance().apply { timeInMillis = a }.get(Calendar.YEAR) == Calendar.getInstance().apply { timeInMillis = b }.get(Calendar.YEAR)

    /** "วันนี้ 14:30", "พรุ่งนี้ 08:00", "12 ม.ค. 09:00", "12 ม.ค. 2569 09:00". */
    fun dayAndTime(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
        val t = time(millis)
        return when (dayDiff(now, millis)) {
            0 -> context.getString(R.string.date_today, t)
            1 -> context.getString(R.string.date_tomorrow, t)
            -1 -> context.getString(R.string.date_yesterday, t)
            else -> "${date(millis, withYear = !sameYear(now, millis))} $t"
        }
    }

    /** "แก้ไข 14:30" / "แก้ไข 3 มี.ค. 2569" */
    fun edited(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
        val label = when (dayDiff(now, millis)) {
            0 -> time(millis)
            -1 -> context.getString(R.string.date_yesterday, time(millis))
            else -> date(millis)
        }
        return context.getString(R.string.edited_at, label)
    }

    fun full(millis: Long): String = "${date(millis)} ${time(millis)}"

    /** "วันนี้", "เมื่อวานนี้", "3 วันที่แล้ว", or a date. */
    fun relativeDay(millis: Long, now: Long = System.currentTimeMillis()): String = when (val d = -dayDiff(now, millis)) {
        0 -> t("วันนี้", "Today")
        1 -> t("เมื่อวานนี้", "Yesterday")
        in 2..6 -> t("$d วันที่แล้ว", "$d days ago")
        else -> date(millis)
    }

    /** "เมื่อวานนี้ • 21:42" / "22 พ.ย. 2568 • 19:20". */
    fun dayLine(millis: Long, now: Long = System.currentTimeMillis()): String {
        val d = -dayDiff(now, millis)
        val day = if (d == 0 || d == 1) relativeDay(millis, now) else date(millis)
        return "$day • ${time(millis)}"
    }

    fun repeatLabel(context: Context, r: RepeatRule): String = context.getString(
        when (r) {
            RepeatRule.NONE -> R.string.repeat_none
            RepeatRule.DAILY -> R.string.repeat_daily
            RepeatRule.WEEKLY -> R.string.repeat_weekly
            RepeatRule.MONTHLY -> R.string.repeat_monthly
            RepeatRule.YEARLY -> R.string.repeat_yearly
        }
    )

    /** Today/tomorrow at [hour]:00 — used by the quick reminder choices. */
    fun at(dayOffset: Int, hour: Int, now: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
