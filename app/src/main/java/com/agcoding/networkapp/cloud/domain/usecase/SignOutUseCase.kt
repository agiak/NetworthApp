package com.agcoding.networkapp.cloud.domain.usecase

import com.agcoding.networkapp.backup.data.AutoBackupDataSource
import com.agcoding.networkapp.biometric.domain.usecase.DisableSecurityUseCase
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.settings.domain.usecase.ClearUserProfileUseCase
import javax.inject.Inject

/**
 * Signs out and leaves the phone as if nobody had used it: the household's data, the profile,
 * the app lock and the backup file go (the data is in the cloud). The device preferences
 * (language, theme, currency) stay. Opening the app then shows the sign in / register page.
 */
class SignOutUseCase @Inject constructor(
    private val repository: CloudRepository,
    private val clearUserProfileUseCase: ClearUserProfileUseCase,
    private val disableSecurityUseCase: DisableSecurityUseCase,
    private val autoBackup: AutoBackupDataSource,
) {
    /** @param force sign out even if some changes couldn't be uploaded first */
    suspend operator fun invoke(force: Boolean = false): Result<Unit> =
        repository.signOut(force).onSuccess {
            autoBackup.deleteBackupFile()
            disableSecurityUseCase()
            // Last: this takes the app back to the welcome page (see NetWorthApp)
            clearUserProfileUseCase()
        }
}
