package com.agcoding.networkapp.cloud.data.sync

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.room.withTransaction
import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.backup.data.AutoBackupDataSource
import com.agcoding.networkapp.cloud.data.local.SyncLinkDao
import com.agcoding.networkapp.cloud.data.local.SyncLinkEntity
import com.agcoding.networkapp.cloud.data.network.NetworkMonitor
import com.agcoding.networkapp.cloud.data.remote.RemoteRows
import com.agcoding.networkapp.cloud.data.remote.RemoteTable
import com.agcoding.networkapp.cloud.data.remote.SupabaseClient
import com.agcoding.networkapp.cloud.data.remote.SupabaseException
import com.agcoding.networkapp.cloud.data.session.CloudSessionStore
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.model.CloudSession
import com.agcoding.networkapp.cloud.domain.model.SyncProgress
import com.agcoding.networkapp.cloud.domain.model.SyncStage
import com.agcoding.networkapp.cloud.domain.model.SyncStatus
import com.agcoding.networkapp.cloud.domain.model.SyncSummary
import com.agcoding.networkapp.cloud.domain.sync.LocalItem
import com.agcoding.networkapp.cloud.domain.sync.RemoteItem
import com.agcoding.networkapp.cloud.domain.sync.SyncFields
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Account
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Entry
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Expense
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Feedback
import com.agcoding.networkapp.cloud.domain.sync.SyncLink
import com.agcoding.networkapp.cloud.domain.sync.hashOf
import com.agcoding.networkapp.cloud.domain.sync.planSync
import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackDao
import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackEntity
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseEntity
import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.widget.NetWorthDetailWidget
import com.agcoding.networkapp.widget.NetWorthWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way sync between the local database and the household's data on Supabase.
 *
 * Every sync pulls the household's rows, merges each table with [planSync] against the links
 * saved last time, pushes what changed here and applies what changed elsewhere. Tables go in
 * dependency order (accounts first) so entries and expenses can translate account ids.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val store: CloudSessionStore,
    private val db: NetWorthDatabase,
    private val accountDao: AccountDao,
    private val netWorthDao: NetWorthDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val categoryFeedbackDao: CategoryFeedbackDao,
    private val linkDao: SyncLinkDao,
    private val autoBackup: AutoBackupDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private var pending: Job? = null

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Progress of the running (or last) sync. */
    private val _progress = MutableStateFlow<SyncProgress?>(null)
    val progress: StateFlow<SyncProgress?> = _progress.asStateFlow()

    /** Local changes not uploaded yet (e.g. made while offline); survives restarts. */
    private val _hasPendingChanges = MutableStateFlow(false)
    val hasPendingChanges: StateFlow<Boolean> = _hasPendingChanges.asStateFlow()

    /** Bumped on every local change, so a sync only clears the changes it actually uploaded. */
    private val changeVersion = AtomicLong(0)

    init {
        scope.launch { _hasPendingChanges.value = store.hasPendingChanges() || _hasPendingChanges.value }
        // Back online: upload what was changed meanwhile and fetch the other phones' changes
        scope.launch {
            networkMonitor.isOnline.drop(1).filter { it }.collect { requestSync(delayMillis = 0) }
        }
    }

    /**
     * Syncs shortly after the latest request; does nothing while not connected to a household
     * or without internet (the change stays pending and goes up once back online).
     *
     * @param localChange the request comes from a change made on this phone
     */
    fun requestSync(delayMillis: Long = DEBOUNCE_MILLIS, localChange: Boolean = false) {
        if (localChange) {
            changeVersion.incrementAndGet()
            if (!_hasPendingChanges.value) {
                _hasPendingChanges.value = true
                scope.launch { store.savePendingChanges(true) }
            }
        }
        pending?.cancel()
        pending = scope.launch {
            delay(delayMillis)
            if (store.current().isSyncing && networkMonitor.isOnline.value) syncNow()
        }
    }

    /**
     * @param replaceLocal drop this device's data and take the household's (when joining)
     */
    suspend fun syncNow(replaceLocal: Boolean = false): Result<SyncSummary> = withContext(ioDispatcher) {
        mutex.withLock {
            _status.value = SyncStatus.Syncing
            val version = changeVersion.get()
            runCatching { sync(replaceLocal) }
                .onSuccess {
                    _status.value = SyncStatus.Idle
                    // Changes made while syncing stay pending for the next round
                    if (changeVersion.get() == version) {
                        _hasPendingChanges.value = false
                        store.savePendingChanges(false)
                    } else {
                        requestSync()
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Sync failed")
                    _status.value = SyncStatus.Failed(error.message ?: error.javaClass.simpleName)
                }
                .recoverCatching { throw toCloudException(it) }
        }
    }

    /** A session that is valid for at least another minute; null when signed out. */
    suspend fun validSession(): CloudSession? {
        val session = store.current().session ?: return null
        if (!session.expiresSoon(Instant.now().epochSecond)) return session
        return try {
            client.refresh(session.refreshToken).also { store.saveSession(it) }
        } catch (e: SupabaseException) {
            // The refresh token was revoked or expired: sign in again
            if (e.status in 400..499) {
                store.clearSession()
                throw CloudException(CloudError.SESSION_EXPIRED, e)
            }
            throw e
        }
    }

    /** Before syncing with [householdId]: links to another household's rows mean nothing there. */
    suspend fun prepareFor(householdId: String) {
        if (store.linksHouseholdId() != householdId) {
            linkDao.clear()
            store.saveLinksHouseholdId(householdId)
        }
    }

    // ── Sync ────────────────────────────────────────────────────────────────

    private suspend fun sync(replaceLocal: Boolean): SyncSummary {
        report(SyncStage.CONNECTING, 0.02f)
        val session = validSession() ?: return SyncSummary()
        val household = store.current().household ?: return SyncSummary()
        val hid = household.id
        prepareFor(hid)

        // Pull first: if the network fails nothing local has changed
        report(SyncStage.CHECKING, 0.08f)
        val remote = RemoteTable.entries.associateWith { table ->
            client.selectAll(session, table.tableName, hid, orderBy = table.keyColumn)
                .map { RemoteRows.parse(table, it) }
        }

        if (replaceLocal) {
            db.withTransaction {
                netWorthDao.deleteAllEntries()
                fixedExpenseDao.deleteAll()
                categoryFeedbackDao.deleteAll()
                accountDao.deleteAll()
                linkDao.clear()
            }
        }

        // Accounts
        val accounts = syncTable(
            table = RemoteTable.ACCOUNTS, session = session, householdId = hid,
            remote = remote.getValue(RemoteTable.ACCOUNTS),
            local = accountDao.getAllAccountsOnce().map { it.toLocalItem() },
            newRemoteId = { UUID.randomUUID().toString() },
            insert = { r -> accountDao.insertAccount(r.toAccount(id = 0)).toString() },
            update = { key, r -> accountDao.updateAccount(r.toAccount(id = key.toLong())); true },
            delete = { key -> accountDao.deleteAccount(key.toLong()) },
            stage = SyncStage.ACCOUNTS, from = 0.15f, to = 0.30f,
        )

        val accountLinks = linkDao.getAll(RemoteTable.ACCOUNTS.name)
        val localToRemoteAccount = accountLinks.associate { it.localKey.toLong() to it.remoteId }
        val remoteToLocalAccount = accountLinks.associate { it.remoteId to it.localKey.toLong() }

        // Net worth entries (only those whose account is synced)
        val entries = syncTable(
            table = RemoteTable.ENTRIES, session = session, householdId = hid,
            remote = remote.getValue(RemoteTable.ENTRIES),
            local = netWorthDao.getAllEntriesOnce().mapNotNull { it.toLocalItem(localToRemoteAccount) },
            newRemoteId = { UUID.randomUUID().toString() },
            insert = { r -> r.toEntry(id = 0, remoteToLocalAccount)?.let { netWorthDao.insertReturningId(it).toString() } },
            update = { key, r ->
                val entry = r.toEntry(id = key.toLong(), remoteToLocalAccount)
                entry?.let { netWorthDao.updateEntry(it) }
                entry != null
            },
            delete = { key -> netWorthDao.deleteEntry(key.toLong()) },
            stage = SyncStage.ENTRIES, from = 0.30f, to = 0.75f,
        )

        // Fixed expenses
        val expenses = syncTable(
            table = RemoteTable.EXPENSES, session = session, householdId = hid,
            remote = remote.getValue(RemoteTable.EXPENSES),
            local = fixedExpenseDao.getAllOnce().map { it.toLocalItem(localToRemoteAccount) },
            newRemoteId = { UUID.randomUUID().toString() },
            insert = { r -> fixedExpenseDao.insertReturningId(r.toExpense(id = 0, remoteToLocalAccount)).toString() },
            update = { key, r -> fixedExpenseDao.update(r.toExpense(id = key.toLong(), remoteToLocalAccount)); true },
            delete = { key -> fixedExpenseDao.deleteById(key.toLong()) },
            stage = SyncStage.EXPENSES, from = 0.75f, to = 0.88f,
        )

        // Categories chosen in Expense Analysis (keyed by name on both sides)
        val categories = syncTable(
            table = RemoteTable.FEEDBACK, session = session, householdId = hid,
            remote = remote.getValue(RemoteTable.FEEDBACK),
            local = categoryFeedbackDao.getAllOnce().map { LocalItem(it.nameKey, mapOf(Feedback.CATEGORY to it.category)) },
            newRemoteId = { it.localKey },
            insert = { r -> r.toFeedback()?.let { categoryFeedbackDao.upsert(it); it.nameKey } },
            update = { _, r -> r.toFeedback()?.let { categoryFeedbackDao.upsert(it) } != null },
            delete = { key -> categoryFeedbackDao.delete(key) },
            stage = SyncStage.CATEGORIES, from = 0.88f, to = 0.95f,
        )

        report(SyncStage.FINISHING, 0.97f)
        val received = accounts.received + entries.received + expenses.received + categories.received
        // Keep the backup file and the home screen widgets up to date with the other phones' changes
        if (received > 0 || replaceLocal) {
            autoBackup.trigger(requestSync = false)
            refreshWidgets()
        }
        store.saveLastSync(System.currentTimeMillis())
        report(SyncStage.DONE, 1f)

        return SyncSummary(
            accountsUploaded   = accounts.pushed,
            entriesUploaded    = entries.pushed,
            expensesUploaded   = expenses.pushed,
            categoriesUploaded = categories.pushed,
            received           = received,
        )
    }

    private class TableResult(val pushed: Int, val received: Int)

    private fun report(stage: SyncStage, fraction: Float) {
        _progress.value = SyncProgress(stage, fraction.coerceIn(0f, 1f))
    }

    /** Merges one table, reporting progress between [from] and [to]. */
    private suspend fun syncTable(
        table: RemoteTable,
        session: CloudSession,
        householdId: String,
        remote: List<RemoteItem>,
        local: List<LocalItem>,
        newRemoteId: (LocalItem) -> String,
        insert: suspend (RemoteItem) -> String?,
        update: suspend (String, RemoteItem) -> Boolean,
        delete: suspend (String) -> Unit,
        stage: SyncStage,
        from: Float,
        to: Float,
    ): TableResult {
        report(stage, from)
        val oldLinks = linkDao.getAll(table.name).map { SyncLink(it.localKey, it.remoteId, it.hash) }
        val plan = planSync(local, remote, oldLinks, newRemoteId)

        // Remember the ids of new rows before pushing them, so a retry after a crash reuses them
        val oldKeys = oldLinks.map { it.localKey }.toSet()
        val newRows = plan.links.filter { it.localKey !in oldKeys }
        if (newRows.isNotEmpty()) linkDao.upsertAll(newRows.map { it.toEntity(table, hash = "") })

        // Uploading takes most of the time: it moves the bar up to 90% of this table's share
        val uploadSpan = (to - from) * 0.9f
        if (plan.push.isNotEmpty()) {
            client.upsert(
                session, table.tableName,
                plan.push.map { (remoteId, fields) -> RemoteRows.toJson(table, householdId, remoteId, fields) },
                table.conflictColumns,
                onProgress = { done, total -> report(stage, from + uploadSpan * done / total) },
            )
        }
        if (plan.softDelete.isNotEmpty()) {
            client.softDelete(session, table.tableName, householdId, table.keyColumn, plan.softDelete, Instant.now().toString())
        }

        val finalLinks = plan.links.associateBy { it.localKey }.toMutableMap()
        db.withTransaction {
            plan.deleteLocal.forEach { delete(it) }
            plan.updateLocal.forEach { (key, row) ->
                // Couldn't apply (e.g. its account isn't here): keep the old link so it's retried
                if (!update(key, row)) oldLinks.find { it.localKey == key }?.let { finalLinks[key] = it }
            }
            plan.insertLocal.forEach { row ->
                insert(row)?.let { key -> finalLinks[key] = SyncLink(key, row.remoteId, hashOf(row.fields)) }
            }
            linkDao.deleteAll(table.name)
            linkDao.upsertAll(finalLinks.values.map { it.toEntity(table, it.hash) })
        }
        report(stage, to)
        return TableResult(
            pushed   = plan.push.size,
            received = plan.deleteLocal.size + plan.updateLocal.size + plan.insertLocal.size,
        )
    }

    private suspend fun refreshWidgets() {
        try {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(NetWorthWidget::class.java).forEach { NetWorthWidget().update(context, it) }
            manager.getGlanceIds(NetWorthDetailWidget::class.java).forEach { NetWorthDetailWidget().update(context, it) }
        } catch (e: Exception) {
            Timber.e(e, "Widget refresh failed")
        }
    }

    // ── Local rows ↔ sync fields ────────────────────────────────────────────

    private fun AccountEntity.toLocalItem() = LocalItem(
        localKey = id.toString(),
        fields = mapOf(
            Account.NAME             to name.trim().ifBlank { DEFAULT_ACCOUNT_NAME }.take(100),
            Account.STARTING_BALANCE to SyncFields.money(startingBalance),
            Account.COLOR_HEX        to (colorHex.takeIf { HEX_COLOR.matches(it) } ?: DEFAULT_COLOR),
            Account.MONTHLY_SALARY   to SyncFields.money(monthlySalary.coerceAtLeast(0.0)),
        ),
    )

    private fun RemoteItem.toAccount(id: Long) = AccountEntity(
        id              = id,
        name            = fields[Account.NAME].orEmpty(),
        startingBalance = fields[Account.STARTING_BALANCE].toDoubleOrZero(),
        colorHex        = fields[Account.COLOR_HEX] ?: DEFAULT_COLOR,
        monthlySalary   = fields[Account.MONTHLY_SALARY].toDoubleOrZero(),
    )

    private fun NetWorthEntity.toLocalItem(accounts: Map<Long, String>): LocalItem? {
        val accountRemoteId = accounts[accountId] ?: return null
        return LocalItem(
            localKey = id.toString(),
            fields = mapOf(
                Entry.ACCOUNT_ID to accountRemoteId,
                Entry.VALUE      to SyncFields.money(value),
                Entry.DATE       to LocalDate.ofEpochDay(dateEpochDay).toString(),
                Entry.NOTE       to note.take(MAX_NOTE),
            ),
        )
    }

    private fun RemoteItem.toEntry(id: Long, accounts: Map<String, Long>): NetWorthEntity? {
        val accountId = accounts[fields[Entry.ACCOUNT_ID]] ?: return null
        val date = fields[Entry.DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return NetWorthEntity(
            id           = id,
            value        = fields[Entry.VALUE].toDoubleOrZero(),
            dateEpochDay = date.toEpochDay(),
            note         = fields[Entry.NOTE].orEmpty(),
            accountId    = accountId,
        )
    }

    private fun FixedExpenseEntity.toLocalItem(accounts: Map<Long, String>) = LocalItem(
        localKey = id.toString(),
        fields = mapOf(
            Expense.TITLE       to title.trim().ifBlank { DEFAULT_EXPENSE_TITLE }.take(200),
            Expense.NOTE        to note.take(MAX_NOTE),
            Expense.COST        to SyncFields.money(cost.coerceAtLeast(0.0)),
            Expense.DATE        to dateEpochDay?.let { LocalDate.ofEpochDay(it).toString() },
            Expense.RECURRENCE  to if (recurrence == "ANNUAL") "ANNUAL" else "MONTHLY",
            Expense.ACCOUNT_IDS to SyncFields.idList(
                accountIds.split(",").mapNotNull { it.trim().toLongOrNull()?.let(accounts::get) }
            ),
        ),
    )

    private fun RemoteItem.toExpense(id: Long, accounts: Map<String, Long>) = FixedExpenseEntity(
        id           = id,
        title        = fields[Expense.TITLE].orEmpty(),
        note         = fields[Expense.NOTE].orEmpty(),
        cost         = fields[Expense.COST].toDoubleOrZero(),
        dateEpochDay = fields[Expense.DATE]?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() },
        recurrence   = fields[Expense.RECURRENCE] ?: "MONTHLY",
        accountIds   = fields[Expense.ACCOUNT_IDS].orEmpty().split(",")
            .mapNotNull { accounts[it] }
            .joinToString(","),
    )

    private fun RemoteItem.toFeedback(): CategoryFeedbackEntity? {
        val category = fields[Feedback.CATEGORY]?.takeIf { ExpenseCategory.fromName(it) != null } ?: return null
        return CategoryFeedbackEntity(nameKey = remoteId, category = category)
    }

    private fun SyncLink.toEntity(table: RemoteTable, hash: String) = SyncLinkEntity(table.name, localKey, remoteId, hash)

    private fun String?.toDoubleOrZero(): Double = this?.let { runCatching { BigDecimal(it).toDouble() }.getOrNull() } ?: 0.0

    companion object {
        private const val DEBOUNCE_MILLIS = 1_500L
        private const val MAX_NOTE = 1000
        private const val DEFAULT_ACCOUNT_NAME = "Account"
        private const val DEFAULT_EXPENSE_TITLE = "Expense"
        private const val DEFAULT_COLOR = "#76C893"
        private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")

        fun toCloudException(error: Throwable): CloudException = when (error) {
            is CloudException     -> error
            is SupabaseException  -> CloudException(
                when {
                    error.code == "invalid_credentials" || error.code == "invalid_grant" -> CloudError.INVALID_CREDENTIALS
                    error.code == "email_not_confirmed"                                 -> CloudError.EMAIL_NOT_CONFIRMED
                    error.code == "user_already_exists" || error.code == "email_exists" -> CloudError.USER_EXISTS
                    error.code == "weak_password"                                       -> CloudError.WEAK_PASSWORD
                    error.code == "email_address_invalid" || error.code == "validation_failed" -> CloudError.INVALID_EMAIL
                    error.code?.startsWith("over_") == true || error.status == 429      -> CloudError.RATE_LIMITED
                    error.code == "P0002"                                               -> CloudError.INVALID_INVITE
                    error.status == 401                                                 -> CloudError.SESSION_EXPIRED
                    else                                                                -> CloudError.UNKNOWN
                },
                error,
            )
            is IOException        -> CloudException(CloudError.NO_CONNECTION, error)
            else                  -> CloudException(CloudError.UNKNOWN, error)
        }
    }
}
