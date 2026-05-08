package com.agcoding.networkapp.backup.data.di

import com.agcoding.networkapp.backup.domain.usecase.ExportDataUseCase
import com.agcoding.networkapp.backup.domain.usecase.ExportDataUseCaseImpl
import com.agcoding.networkapp.backup.domain.usecase.ImportDataUseCase
import com.agcoding.networkapp.backup.domain.usecase.ImportDataUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindExportDataUseCase(impl: ExportDataUseCaseImpl): ExportDataUseCase

    @Binds
    @Singleton
    abstract fun bindImportDataUseCase(impl: ImportDataUseCaseImpl): ImportDataUseCase
}
