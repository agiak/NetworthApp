package com.agcoding.networkapp.expenseanalysis.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.expenseanalysis.domain.categorizer.ExpenseCategorizer
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.repository.CategoryFeedbackRepository
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ExpenseAnalysisSource(
    val accounts: List<Account>,
    val expenses: List<FixedExpense>,
    val feedback: List<CategoryFeedback>,
) {
    /** Rebuilt whenever the user's choices change, so it learns from them straight away. */
    val categorizer: ExpenseCategorizer by lazy { ExpenseCategorizer(feedback) }
}

class ObserveExpenseAnalysisSourceUseCase @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getFixedExpensesUseCase: GetFixedExpensesUseCase,
    private val feedbackRepository: CategoryFeedbackRepository,
) {
    operator fun invoke(): Flow<Result<ExpenseAnalysisSource>> =
        combine(getAccountsUseCase(), getFixedExpensesUseCase(), feedbackRepository.getAll()) { accounts, expensesResult, feedback ->
            expensesResult.map { expenses -> ExpenseAnalysisSource(accounts, expenses, feedback) }
        }
}
