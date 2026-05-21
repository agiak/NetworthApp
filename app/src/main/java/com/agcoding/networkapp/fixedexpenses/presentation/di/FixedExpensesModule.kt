package com.agcoding.networkapp.fixedexpenses.presentation.di

import com.agcoding.networkapp.fixedexpenses.domain.usecase.AddFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.AddFixedExpenseUseCaseImpl
import com.agcoding.networkapp.fixedexpenses.domain.usecase.DeleteFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.DeleteFixedExpenseUseCaseImpl
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesUseCaseImpl
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesYearlySummaryUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesYearlySummaryUseCaseImpl
import com.agcoding.networkapp.fixedexpenses.domain.usecase.UpdateFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.UpdateFixedExpenseUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FixedExpensesModule {

    @Binds abstract fun bindGetFixedExpenses(impl: GetFixedExpensesUseCaseImpl): GetFixedExpensesUseCase
    @Binds abstract fun bindGetFixedExpensesYearlySummary(impl: GetFixedExpensesYearlySummaryUseCaseImpl): GetFixedExpensesYearlySummaryUseCase
    @Binds abstract fun bindAddFixedExpense(impl: AddFixedExpenseUseCaseImpl): AddFixedExpenseUseCase
    @Binds abstract fun bindUpdateFixedExpense(impl: UpdateFixedExpenseUseCaseImpl): UpdateFixedExpenseUseCase
    @Binds abstract fun bindDeleteFixedExpense(impl: DeleteFixedExpenseUseCaseImpl): DeleteFixedExpenseUseCase
}
