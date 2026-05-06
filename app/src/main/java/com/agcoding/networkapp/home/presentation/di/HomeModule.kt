package com.agcoding.networkapp.home.presentation.di

import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCaseImpl
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCaseImpl
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
    abstract fun bindGetMonthlyNetWorthUseCase(impl: GetMonthlyNetWorthUseCaseImpl): GetMonthlyNetWorthUseCase

    @Binds
    abstract fun bindGetNetWorthEntriesUseCase(impl: GetNetWorthEntriesUseCaseImpl): GetNetWorthEntriesUseCase
}
