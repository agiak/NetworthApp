package com.agcoding.networkapp.home.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackDao
import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackEntity
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseEntity

@Database(
    entities = [NetWorthEntity::class, AccountEntity::class, FixedExpenseEntity::class, CategoryFeedbackEntity::class],
    version = 9,
    exportSchema = false,
)
abstract class NetWorthDatabase : RoomDatabase() {
    abstract fun netWorthDao(): NetWorthDao
    abstract fun accountDao(): AccountDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun categoryFeedbackDao(): CategoryFeedbackDao
}
