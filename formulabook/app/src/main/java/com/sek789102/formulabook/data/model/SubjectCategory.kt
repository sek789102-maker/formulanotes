package com.sek789102.formulabook.data.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.sek789102.formulabook.R

enum class SubjectCategory(
    @param:StringRes val labelRes: Int,
    val shortCode: String,
    val defaultThaiName: String,
    val colorHex: Long,
    val badgeBg: Color,
    val badgeFg: Color,
    val accentStripe: Color,
    @param:DrawableRes val mascotDrawableRes: Int
) {
    MATH(
        R.string.subject_math,
        "MATH",
        "คณิตศาสตร์",
        0xFF2563EB,
        Color(0xFFDBEAFE),
        Color(0xFF1D4ED8),
        Color(0xFF2563EB),
        R.drawable.img_mascot_math
    ),
    PHYSICS(
        R.string.subject_physics,
        "PHYS",
        "ฟิสิกส์",
        0xFF7C3AED,
        Color(0xFFEDE9FE),
        Color(0xFF6D28D9),
        Color(0xFF7C3AED),
        R.drawable.img_mascot_science
    ),
    CHEMISTRY(
        R.string.subject_chemistry,
        "CHEM",
        "เคมี",
        0xFF10B981,
        Color(0xFFD1FAE5),
        Color(0xFF047857),
        Color(0xFF10B981),
        R.drawable.img_mascot_science
    ),
    BIOLOGY(
        R.string.subject_biology,
        "BIO",
        "ชีววิทยา",
        0xFF65A30D,
        Color(0xFFECFCCB),
        Color(0xFF3F6212),
        Color(0xFF65A30D),
        R.drawable.img_mascot_science
    ),
    THAI(
        R.string.subject_thai,
        "THAI",
        "ภาษาไทย",
        0xFFEC4899,
        Color(0xFFFCE7F3),
        Color(0xFFBE185D),
        Color(0xFFEC4899),
        R.drawable.img_mascot_humanities
    ),
    ENGLISH(
        R.string.subject_english,
        "ENG",
        "ภาษาอังกฤษ",
        0xFFF43F5E,
        Color(0xFFFFE4E6),
        Color(0xFFBE123C),
        Color(0xFFF43F5E),
        R.drawable.img_mascot_english
    ),
    SOCIAL(
        R.string.subject_social,
        "SOC",
        "สังคมศึกษา",
        0xFF0EA5E9,
        Color(0xFFE0F2FE),
        Color(0xFF0369A1),
        Color(0xFF0EA5E9),
        R.drawable.img_mascot_humanities
    ),
    HISTORY(
        R.string.subject_history,
        "HIST",
        "ประวัติศาสตร์",
        0xFFEA580C,
        Color(0xFFFFEDD5),
        Color(0xFFC2410C),
        Color(0xFFEA580C),
        R.drawable.img_mascot_humanities
    ),
    COMPUTER(
        R.string.subject_computer,
        "CS",
        "คอมพิวเตอร์",
        0xFF06B6D4,
        Color(0xFFCFFAFE),
        Color(0xFF0E7490),
        Color(0xFF06B6D4),
        R.drawable.img_mascot_math
    ),
    ART(
        R.string.subject_art,
        "ART",
        "ศิลปะและดนตรี",
        0xFF8B5CF6,
        Color(0xFFF3E8FF),
        Color(0xFF7E22CE),
        Color(0xFF8B5CF6),
        R.drawable.img_mascot_english
    ),
    GENERAL(
        R.string.subject_general,
        "GEN",
        "ทั่วไป / อื่นๆ",
        0xFFF59E0B,
        Color(0xFFFEF3C7),
        Color(0xFFB45309),
        Color(0xFFF59E0B),
        R.drawable.img_student_mascot
    );

    fun toEntity(): SubjectEntity = SubjectEntity(
        id = name,
        name = defaultThaiName,
        colorHex = colorHex,
        isDefault = true,
        sortOrder = ordinal,
        createdAt = 0L
    )

    companion object {
        fun fromName(name: String?): SubjectCategory {
            return entries.firstOrNull { it.name == name } ?: GENERAL
        }

        fun findExact(id: String?): SubjectCategory? {
            return entries.firstOrNull { it.name == id }
        }

        fun isStemSubject(subjectId: String?): Boolean {
            return subjectId == MATH.name ||
                subjectId == PHYSICS.name ||
                subjectId == CHEMISTRY.name ||
                subjectId == COMPUTER.name
        }
    }
}

data class SubjectColorOption(
    val colorHex: Long,
    val badgeBg: Color,
    val badgeFg: Color,
    val accentStripe: Color
)

val CustomSubjectColorPalette: List<SubjectColorOption> = listOf(
    SubjectColorOption(0xFF2563EB, Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF2563EB)),
    SubjectColorOption(0xFF7C3AED, Color(0xFFEDE9FE), Color(0xFF6D28D9), Color(0xFF7C3AED)),
    SubjectColorOption(0xFF059669, Color(0xFFD1FAE5), Color(0xFF047857), Color(0xFF059669)),
    SubjectColorOption(0xFF0284C7, Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFF0284C7)),
    SubjectColorOption(0xFFDB2777, Color(0xFFFCE7F3), Color(0xFFBE185D), Color(0xFFDB2777)),
    SubjectColorOption(0xFFD97706, Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFD97706)),
    SubjectColorOption(0xFF0D9488, Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF0D9488)),
    SubjectColorOption(0xFF4F46E5, Color(0xFFE0E7FF), Color(0xFF4338CA), Color(0xFF4F46E5))
)

fun SubjectEntity?.accentStripeColor(): Color {
    if (this == null) return SubjectCategory.GENERAL.accentStripe
    val builtIn = SubjectCategory.findExact(id)
    if (builtIn != null && colorHex == builtIn.colorHex) return builtIn.accentStripe
    return Color(colorHex)
}

fun SubjectEntity?.badgeBgColor(): Color {
    if (this == null) return SubjectCategory.GENERAL.badgeBg
    val builtIn = SubjectCategory.findExact(id)
    if (builtIn != null && colorHex == builtIn.colorHex) return builtIn.badgeBg
    val preset = CustomSubjectColorPalette.firstOrNull { it.colorHex == colorHex }
    return preset?.badgeBg ?: Color(colorHex).copy(alpha = 0.15f)
}

fun SubjectEntity?.badgeFgColor(): Color {
    if (this == null) return SubjectCategory.GENERAL.badgeFg
    val builtIn = SubjectCategory.findExact(id)
    if (builtIn != null && colorHex == builtIn.colorHex) return builtIn.badgeFg
    val preset = CustomSubjectColorPalette.firstOrNull { it.colorHex == colorHex }
    return preset?.badgeFg ?: Color(colorHex)
}

@DrawableRes
fun SubjectEntity?.mascotDrawableRes(): Int {
    if (this == null) return SubjectCategory.GENERAL.mascotDrawableRes
    return SubjectCategory.findExact(id)?.mascotDrawableRes ?: R.drawable.img_mascot_tags
}

fun SubjectEntity?.resolveDisplayName(context: Context): String {
    if (this == null) return context.getString(SubjectCategory.GENERAL.labelRes)
    val builtIn = if (isDefault) SubjectCategory.findExact(id) else null
    return if (builtIn != null) context.getString(builtIn.labelRes) else name
}

@Composable
fun SubjectEntity?.displayName(): String {
    if (this == null) return stringResource(SubjectCategory.GENERAL.labelRes)
    val builtIn = if (isDefault) SubjectCategory.findExact(id) else null
    return if (builtIn != null) stringResource(builtIn.labelRes) else name
}

fun List<SubjectEntity>.resolveSubject(subjectId: String): SubjectEntity {
    return firstOrNull { it.id == subjectId }
        ?: SubjectCategory.findExact(subjectId)?.toEntity()
        ?: SubjectCategory.GENERAL.toEntity()
}
