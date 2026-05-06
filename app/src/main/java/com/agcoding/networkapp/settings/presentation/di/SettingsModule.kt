package com.agcoding.networkapp.settings.presentation.di

import com.agcoding.networkapp.settings.domain.usecase.GenerateDummyDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GenerateDummyDataUseCaseImpl
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCaseImpl
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCaseImpl
import com.agcoding.networkapp.settings.domain.usecase.SetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppLanguageUseCaseImpl
import com.agcoding.networkapp.settings.domain.usecase.SetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppThemeUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindGenerateDummyDataUseCase(impl: GenerateDummyDataUseCaseImpl): GenerateDummyDataUseCase

    @Binds
    abstract fun bindGetAppLanguageUseCase(impl: GetAppLanguageUseCaseImpl): GetAppLanguageUseCase

    @Binds
    abstract fun bindGetAppThemeUseCase(impl: GetAppThemeUseCaseImpl): GetAppThemeUseCase

    @Binds
    abstract fun bindSetAppLanguageUseCase(impl: SetAppLanguageUseCaseImpl): SetAppLanguageUseCase

    @Binds
    abstract fun bindSetAppThemeUseCase(impl: SetAppThemeUseCaseImpl): SetAppThemeUseCase
}
