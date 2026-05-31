package com.agcoding.networkapp.backup.domain.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.backup.data.BackupSerializer
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportDataUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository,
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val fixedExpensesRepository: FixedExpensesRepository,
    private val serializer: BackupSerializer
) : ExportDataUseCase {

    override suspend fun invoke(): String {
        val entries = repository.getEntries().first().getOrElse { emptyList() }
        val accounts = accountRepository.getAccounts().first()
        val fixedExpenses = fixedExpensesRepository.getAll().first().getOrElse { emptyList() }
        val profile = settingsRepository.getUserProfile().first()
        val theme = settingsRepository.getAppTheme().first()
        val language = settingsRepository.getAppLanguage().first()
        return serializer.serialize(entries, profile, theme, language, accounts, fixedExpenses)
    }
}
