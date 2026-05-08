package com.agcoding.networkapp.backup.data

import com.agcoding.networkapp.backup.domain.model.AppBackupData
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSerializer @Inject constructor() {

    fun serialize(
        entries: List<NetWorthEntry>,
        profile: UserProfile,
        theme: AppTheme,
        language: AppLanguage
    ): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        val profileObj = JSONObject().apply {
            put("name", profile.name)
            put("email", profile.email)
            put("targetAmount", profile.targetAmount)
            put("createdAt", profile.createdAt?.toString() ?: JSONObject.NULL)
        }
        root.put("profile", profileObj)

        val settingsObj = JSONObject().apply {
            put("theme", theme.name)
            put("language", language.name)
        }
        root.put("settings", settingsObj)

        val entriesArray = JSONArray()
        entries.forEach { entry ->
            entriesArray.put(JSONObject().apply {
                put("id", entry.id)
                put("value", entry.value)
                put("date", entry.date.toString())
            })
        }
        root.put("entries", entriesArray)

        return root.toString(2)
    }

    fun isValid(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val version = root.getInt("version")
            if (version > BACKUP_VERSION || version < 1) return false
            val entries = root.getJSONArray("entries")
            // Validate at least one entry format if entries exist
            if (entries.length() > 0) {
                val first = entries.getJSONObject(0)
                first.getDouble("value")
                LocalDate.parse(first.getString("date"))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deserialize(json: String): AppBackupData {
        val root = JSONObject(json)
        val version = root.getInt("version")
        val exportedAt = root.optString("exportedAt", "")

        val profile = if (root.has("profile")) {
            val p = root.getJSONObject("profile")
            UserProfile(
                name = p.optString("name", ""),
                email = p.optString("email", ""),
                targetAmount = p.optDouble("targetAmount", 0.0),
                createdAt = p.optString("createdAt", null)
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            )
        } else null

        val theme = if (root.has("settings")) {
            runCatching {
                AppTheme.valueOf(root.getJSONObject("settings").getString("theme"))
            }.getOrNull()
        } else null

        val language = if (root.has("settings")) {
            runCatching {
                AppLanguage.valueOf(root.getJSONObject("settings").getString("language"))
            }.getOrNull()
        } else null

        val entriesArray = root.getJSONArray("entries")
        val entries = (0 until entriesArray.length()).mapNotNull { i ->
            runCatching {
                val e = entriesArray.getJSONObject(i)
                NetWorthEntry(
                    id = e.optLong("id", 0L),
                    value = e.getDouble("value"),
                    date = LocalDate.parse(e.getString("date"))
                )
            }.getOrNull()
        }

        return AppBackupData(
            version = version,
            exportedAt = exportedAt,
            profile = profile,
            theme = theme,
            language = language,
            entries = entries
        )
    }

    companion object {
        const val BACKUP_VERSION = 1
    }
}
