package com.agcoding.networkapp.home.presentation.di

import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.DeleteNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.DeleteNetWorthEntryUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntryByIdUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntryByIdUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.UpdateNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.UpdateNetWorthEntryUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class HomeModule {

    @Binds
    abstract fun bindAddNetWorthEntryUseCase(impl: AddNetWorthEntryUseCaseImpl): AddNetWorthEntryUseCase

    @Binds
    abstract fun bindDeleteNetWorthEntryUseCase(impl: DeleteNetWorthEntryUseCaseImpl): DeleteNetWorthEntryUseCase

    @Binds
    abstract fun bindGetMonthlyNetWorthUseCase(impl: GetMonthlyNetWorthUseCaseImpl): GetMonthlyNetWorthUseCase

    @Binds
    abstract fun bindGetNetWorthEntriesUseCase(impl: GetNetWorthEntriesUseCaseImpl): GetNetWorthEntriesUseCase

    @Binds
    abstract fun bindGetNetWorthEntryByIdUseCase(impl: GetNetWorthEntryByIdUseCaseImpl): GetNetWorthEntryByIdUseCase

    @Binds
    abstract fun bindUpdateNetWorthEntryUseCase(impl: UpdateNetWorthEntryUseCaseImpl): UpdateNetWorthEntryUseCase
}
