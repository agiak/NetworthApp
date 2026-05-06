package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<UserProfile> = repository.getUserProfile()
}

class SetUserProfileUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(profile: UserProfile) = repository.setUserProfile(profile)
}

class IsProfileCreatedUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isProfileCreated()
}

class SetProfileCreatedUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(created: Boolean) = repository.setProfileCreated(created)
}
