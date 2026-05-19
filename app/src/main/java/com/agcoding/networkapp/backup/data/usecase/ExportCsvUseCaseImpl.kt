package com.agcoding.networkapp.backup.data.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.backup.domain.usecase.ExportCsvUseCase
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportCsvUseCaseImpl @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
    private val accountRepository: AccountRepository,
) : ExportCsvUseCase {

    override suspend fun invoke(): String {
        val entries  = netWorthRepository.getEntries().first().getOrElse { emptyList() }
        val accounts = accountRepository.getAccounts().first().associateBy { it.id }
        val fmt      = DateTimeFormatter.ISO_LOCAL_DATE

        return buildString {
            appendLine("Date,Amount,Note,Account")
            entries.sortedBy { it.date }.forEach { entry ->
                val accountName = accounts[entry.accountId]?.name ?: "Main"
                val note        = entry.note.replace("\"", "\"\"")
                appendLine("${entry.date.format(fmt)},${entry.value},\"$note\",\"$accountName\"")
            }
        }
    }
}
