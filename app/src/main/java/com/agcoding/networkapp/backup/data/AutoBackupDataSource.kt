package com.agcoding.networkapp.backup.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.mapper.AccountEntityToDomainMapper
import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.mapper.FixedExpenseEntityToDomainMapper
import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.mapper.NetWorthEntityToDomainMapper
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoBackupDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NetWorthDao,
    private val mapper: NetWorthEntityToDomainMapper,
    private val accountDao: AccountDao,
    private val accountMapper: AccountEntityToDomainMapper,
    private val fixedExpenseDao: FixedExpenseDao,
    private val fixedExpenseMapper: FixedExpenseEntityToDomainMapper,
    private val serializer: BackupSerializer,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun trigger() = withContext(ioDispatcher) {
        try {
            val entries = dao.getAllEntriesOnce().map { mapper.map(it) }
            val accounts = accountDao.getAllAccountsOnce().map { accountMapper.map(it) }
            val fixedExpenses = fixedExpenseDao.getAllOnce().map { fixedExpenseMapper.toDomain(it) }
            val profile = settingsRepository.getUserProfile().first()
            val theme = settingsRepository.getAppTheme().first()
            val language = settingsRepository.getAppLanguage().first()
            val json = serializer.serialize(entries, profile, theme, language, accounts, fixedExpenses)
            writeToDownloads(json)
        } catch (e: Exception) {
            Timber.e(e, "Auto-backup failed")
        }
    }

    private fun writeToDownloads(json: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(json)
        } else {
            writeViaFileApi(json)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(json: String) {
        try {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val existingUri = resolver.query(collection, projection, selection, arrayOf(BACKUP_FILE_NAME), null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    android.content.ContentUris.withAppendedId(
                        collection,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    )
                } else null
            }
            val uri = existingUri ?: run {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, BACKUP_FILE_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                resolver.insert(collection, values)
            }
            uri?.let { u ->
                resolver.openOutputStream(u, "wt")?.use { it.write(json.toByteArray()) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Auto-backup MediaStore write failed")
        }
    }

    private fun writeViaFileApi(json: String) {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(dir, BACKUP_FILE_NAME).writeText(json)
        } catch (e: Exception) {
            Timber.e(e, "Auto-backup file write failed")
        }
    }

    companion object {
        private const val BACKUP_FILE_NAME = "networkapp_backup.json"
    }
}
