package com.agcoding.networkapp.cloud.domain.usecase

import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.usecase.SetProfileCreatedUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetUserProfileUseCase
import javax.inject.Inject

/**
 * Signs out of the cloud and resets the phone to the sign in / register page: the household's
 * data and the profile that came with the account are removed (they're in the cloud).
 */
class SignOutUseCase @Inject constructor(
    private val repository: CloudRepository,
    private val setUserProfileUseCase: SetUserProfileUseCase,
    private val setProfileCreatedUseCase: SetProfileCreatedUseCase,
) {
    /** @param force sign out even if some changes couldn't be uploaded first */
    suspend operator fun invoke(force: Boolean = false): Result<Unit> =
        repository.signOut(force).onSuccess {
            setUserProfileUseCase(UserProfile())
            // Back to the welcome page (see NetWorthApp)
            setProfileCreatedUseCase(false)
        }
}
