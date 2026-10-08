package com.agcoding.networkapp.backup.domain.model

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile

data class AppBackupData(
    val version: Int,
    val exportedAt: String,
    val profile: UserProfile?,
    val theme: AppTheme?,
    val language: AppLanguage?,
    val entries: List<NetWorthEntry>,
    /** Empty when the backup predates accounts: the current accounts are kept. */
    val accounts: List<Account> = emptyList(),
    /** Null when the backup predates fixed expenses: the current ones are kept. */
    val fixedExpenses: List<FixedExpense>? = null,
    /** Null when the backup predates expense analysis: the current choices are kept. */
    val categoryFeedback: List<CategoryFeedback>? = null,
)
