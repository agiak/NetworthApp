package com.agcoding.networkapp.savings.domain.usecase

import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesUseCase
import com.agcoding.networkapp.savings.domain.model.SavingsPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSavingsPlanUseCase @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getFixedExpensesUseCase: GetFixedExpensesUseCase,
) {
    operator fun invoke(): Flow<Result<SavingsPlan>> =
        combine(getAccountsUseCase(), getFixedExpensesUseCase()) { accounts, expensesResult ->
            expensesResult.map { expenses -> computeSavingsPlan(accounts, expenses) }
        }
}
