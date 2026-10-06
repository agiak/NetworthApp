package com.agcoding.networkapp.backup.domain.usecase

import com.agcoding.networkapp.backup.data.BackupRestoreDataSource
import com.agcoding.networkapp.backup.data.BackupSerializer
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class ImportDataUseCaseImpl @Inject constructor(
    private val restoreDataSource: BackupRestoreDataSource,
    private val settingsRepository: SettingsRepository,
    private val serializer: BackupSerializer
) : ImportDataUseCase {

    override fun validate(json: String): Boolean = serializer.isValid(json)

    override suspend fun invoke(json: String): Result<Unit> = runCatching {
        val backup = serializer.deserialize(json)

        // Accounts, entries and fixed expenses are replaced atomically
        restoreDataSource.restore(backup)

        // Restore profile if present
        backup.profile?.let { settingsRepository.setUserProfile(it) }

        // Restore theme if present (no app restart needed)
        backup.theme?.let { settingsRepository.setAppTheme(it) }

        // Language is intentionally not restored here to prevent a mid-import
        // activity restart. Users can change language manually in Settings.
    }
}
