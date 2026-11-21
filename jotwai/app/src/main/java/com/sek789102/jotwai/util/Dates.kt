package com.sek789102.jotwai.util

import android.content.Context
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.RepeatRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Dates {
    private val thai = Locale.forLanguageTag("th-TH")

    private fun dayDiff(a: Long, b: Long): Int {
        val ca = Calendar.getInstance().apply { timeInMillis = a; set(Calendar.HOUR_OF_DAY, 12) }
        val cb = Calendar.getInstance().apply { timeInMillis = b; set(Calendar.HOUR_OF_DAY, 12) }
        return Math.round((cb.timeInMillis - ca.timeInMillis) / 86_400_000.0).toInt()
    }

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", thai).format(Date(millis))

    /** "วันนี้ 14:30", "พรุ่งนี้ 08:00", "12 ม.ค. 09:00", "12 ม.ค. 2569 09:00". */
    fun dayAndTime(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
        val t = time(millis)
        return when (dayDiff(now, millis)) {
            0 -> context.getString(R.string.date_today, t)
            1 -> context.getString(R.string.date_tomorrow, t)
            -1 -> context.getString(R.string.date_yesterday, t)
            else -> {
                val sameYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR) ==
                    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
                SimpleDateFormat(if (sameYear) "d MMM HH:mm" else "d MMM yyyy HH:mm", thai).format(Date(millis))
            }
        }
    }

    /** "แก้ไขเมื่อ 14:30" / "แก้ไขเมื่อ 3 มี.ค." */
    fun edited(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
        val label = when (dayDiff(now, millis)) {
            0 -> time(millis)
            -1 -> context.getString(R.string.date_yesterday, time(millis))
            else -> SimpleDateFormat("d MMM yyyy", thai).format(Date(millis))
        }
        return context.getString(R.string.edited_at, label)
    }

    fun full(millis: Long): String = SimpleDateFormat("d MMM yyyy HH:mm", thai).format(Date(millis))

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
