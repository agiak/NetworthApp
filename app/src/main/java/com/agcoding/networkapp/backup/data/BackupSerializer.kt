package com.agcoding.networkapp.backup.data

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.backup.domain.model.AppBackupData
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
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
        language: AppLanguage,
        accounts: List<Account> = emptyList(),
        fixedExpenses: List<FixedExpense> = emptyList(),
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

        val accountsArray = JSONArray()
        accounts.forEach { account ->
            accountsArray.put(JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("startingBalance", account.startingBalance)
                put("colorHex", account.colorHex)
            })
        }
        root.put("accounts", accountsArray)

        val expensesArray = JSONArray()
        fixedExpenses.forEach { expense ->
            expensesArray.put(JSONObject().apply {
                put("id", expense.id)
                put("title", expense.title)
                put("note", expense.note)
                put("cost", expense.cost)
                put("date", expense.date?.toString() ?: JSONObject.NULL)
                put("recurrence", expense.recurrence.name)
                val ids = JSONArray()
                expense.accountIds.forEach { ids.put(it) }
                put("accountIds", ids)
            })
        }
        root.put("fixedExpenses", expensesArray)

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

        val accounts = if (root.has("accounts")) {
            val arr = root.getJSONArray("accounts")
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val a = arr.getJSONObject(i)
                    Account(
                        id = a.optLong("id", 0L),
                        name = a.getString("name"),
                        startingBalance = a.optDouble("startingBalance", 0.0),
                        colorHex = a.optString("colorHex", "#76C893"),
                    )
                }.getOrNull()
            }
        } else emptyList()

        val fixedExpenses = if (root.has("fixedExpenses")) {
            val arr = root.getJSONArray("fixedExpenses")
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val e = arr.getJSONObject(i)
                    val accountIds = e.getJSONArray("accountIds").let { ids ->
                        (0 until ids.length()).map { ids.getLong(it) }
                    }
                    FixedExpense(
                        id = e.optLong("id", 0L),
                        title = e.getString("title"),
                        note = e.optString("note", ""),
                        cost = e.getDouble("cost"),
                        date = e.optString("date", null)
                            ?.takeIf { it.isNotBlank() && it != "null" }
                            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        recurrence = runCatching { RecurrenceType.valueOf(e.getString("recurrence")) }
                            .getOrDefault(RecurrenceType.MONTHLY),
                        accountIds = accountIds,
                    )
                }.getOrNull()
            }
        } else emptyList()

        return AppBackupData(
            version = version,
            exportedAt = exportedAt,
            profile = profile,
            theme = theme,
            language = language,
            entries = entries,
            accounts = accounts,
            fixedExpenses = fixedExpenses,
        )
    }

    companion object {
        const val BACKUP_VERSION = 2
    }
}
