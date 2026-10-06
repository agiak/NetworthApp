package com.agcoding.networkapp.backup.data

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.room.withTransaction
import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.backup.domain.model.AppBackupData
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.mapper.FixedExpenseEntityToDomainMapper
import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.widget.NetWorthDetailWidget
import com.agcoding.networkapp.widget.NetWorthWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replaces the database content with a backup in a single transaction, keeping the original
 * ids so entries and fixed expenses still point to the right accounts.
 * If anything fails, nothing is changed.
 */
@Singleton
class BackupRestoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: NetWorthDatabase,
    private val netWorthDao: NetWorthDao,
    private val accountDao: AccountDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val fixedExpenseMapper: FixedExpenseEntityToDomainMapper,
    private val autoBackup: AutoBackupDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun restore(backup: AppBackupData) = withContext(ioDispatcher) {
        db.withTransaction {
            if (backup.accounts.isNotEmpty()) {
                accountDao.deleteAll()
                accountDao.insertAll(
                    backup.accounts.map { account ->
                        AccountEntity(
                            id              = account.id,
                            name            = account.name,
                            startingBalance = account.startingBalance,
                            colorHex        = account.colorHex,
                            monthlySalary   = account.monthlySalary,
                        )
                    }
                )
            }

            netWorthDao.deleteAllEntries()
            netWorthDao.insertAll(
                backup.entries.map { entry ->
                    NetWorthEntity(
                        id           = entry.id,
                        value        = entry.value,
                        dateEpochDay = entry.date.toEpochDay(),
                        note         = entry.note,
                        accountId    = entry.accountId,
                    )
                }
            )

            backup.fixedExpenses?.let { expenses ->
                fixedExpenseDao.deleteAll()
                fixedExpenseDao.insertAll(expenses.map(fixedExpenseMapper::toEntity))
            }
        }
        refreshWidgets()
        autoBackup.trigger()
    }

    private suspend fun refreshWidgets() {
        try {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(NetWorthWidget::class.java)
                .forEach { id -> NetWorthWidget().update(context, id) }
            manager.getGlanceIds(NetWorthDetailWidget::class.java)
                .forEach { id -> NetWorthDetailWidget().update(context, id) }
        } catch (e: Exception) {
            Timber.e(e, "Widget refresh failed")
        }
    }
}
