package com.agcoding.networkapp.shared.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThemeDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BiometricDataStore
