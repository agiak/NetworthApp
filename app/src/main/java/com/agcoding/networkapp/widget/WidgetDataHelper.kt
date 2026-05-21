package com.agcoding.networkapp.widget

import android.content.Context
import com.agcoding.networkapp.account.data.local.AccountEntity
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private const val WIDGET_DATA_TIMEOUT_MS = 5_000L

data class AccountWidgetData(
    val name: String,
    val colorHex: String,
    val balance: Double,
    val formattedBalance: String,
    val percentage: Float,
)

data class WidgetData(
    val totalFormatted: String,
    val change: Double?,
    val changeFormatted: String,
    val currency: AppCurrency,
    val accounts: List<AccountWidgetData>,
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        NetWorthWidgetEntryPoint::class.java,
    )
    val currency = withTimeoutOrNull(WIDGET_DATA_TIMEOUT_MS) {
        entryPoint.settingsRepository().getAppCurrency().first()
    } ?: AppCurrency.EUR
    val entries  = entryPoint.netWorthDao().getAllEntriesOnce()
    val accounts = entryPoint.accountDao().getAllAccountsOnce()

    val entriesByAccount = entries.groupBy { it.accountId }

    // Per-account latest and previous balance
    val accountData = accounts.map { account ->
        val sorted  = (entriesByAccount[account.id] ?: emptyList()).sortedByDescending { it.dateEpochDay }
        val latest  = sorted.getOrNull(0)?.value ?: account.startingBalance
        AccountWidgetEntry(account, latest, sorted.getOrNull(1)?.value ?: latest)
    }

    val total    = accountData.sumOf { it.current }
    val previous = accountData.sumOf { it.previous }
    val change   = if (entries.isNotEmpty()) total - previous else null

    val accountWidgets = accountData.map { entry ->
        AccountWidgetData(
            name             = entry.account.name,
            colorHex         = entry.account.colorHex,
            balance          = entry.current,
            formattedBalance = formatCurrency(entry.current, currency.symbol),
            percentage       = if (total > 0) (entry.current / total).toFloat().coerceIn(0f, 1f) else 0f,
        )
    }

    return WidgetData(
        totalFormatted  = formatCurrency(total, currency.symbol),
        change          = change,
        changeFormatted = if (change != null) {
            val prefix = if (change >= 0) "+" else "-"
            "$prefix${formatCurrency(abs(change), currency.symbol)}"
        } else "",
        currency        = currency,
        accounts        = accountWidgets,
    )
}

private data class AccountWidgetEntry(
    val account: AccountEntity,
    val current: Double,
    val previous: Double,
)

internal fun formatCurrency(value: Double, symbol: String) = "$symbol%,.0f".format(value)

internal fun widgetFallbackData() = WidgetData(
    totalFormatted = "—",
    change = null,
    changeFormatted = "",
    currency = AppCurrency.EUR,
    accounts = emptyList(),
)
