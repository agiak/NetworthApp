package com.agcoding.networkapp.expenseanalysis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Category the user picked for a (normalised) expense name. */
@Entity(tableName = "expense_category_feedback")
data class CategoryFeedbackEntity(
    @PrimaryKey val nameKey: String,
    val category: String,
)
