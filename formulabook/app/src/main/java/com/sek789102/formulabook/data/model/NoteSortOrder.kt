package com.sek789102.formulabook.data.model

import androidx.annotation.StringRes
import com.sek789102.formulabook.R

enum class NoteSortOrder(@param:StringRes val labelRes: Int) {
    UPDATED_DESC(R.string.sort_updated_desc),
    CREATED_DESC(R.string.sort_created_desc),
    TITLE_ASC(R.string.sort_title_asc),
    SUBJECT_ASC(R.string.sort_subject_asc);

    companion object {
        fun fromName(name: String?): NoteSortOrder {
            return entries.firstOrNull { it.name == name } ?: UPDATED_DESC
        }
    }
}
