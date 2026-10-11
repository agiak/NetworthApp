package com.agcoding.networkapp.cloud.data.remote

import com.agcoding.networkapp.cloud.domain.model.CloudSession
import com.agcoding.networkapp.cloud.domain.model.Household
import com.agcoding.networkapp.cloud.domain.model.HouseholdMember
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** An error answered by Supabase (auth or database). */
class SupabaseException(
    val status: Int,
    message: String,
    /** e.g. "invalid_credentials", "email_not_confirmed", or a Postgres code like "P0002" */
    val code: String? = null,
) : IOException(message)

/**
 * Small client for the Supabase REST APIs the app needs: Auth (email + password) and
 * PostgREST (tables and RPC functions). Calls block, so run them on an IO dispatcher.
 */
@Singleton
class SupabaseClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Auth ────────────────────────────────────────────────────────────────

    /**
     * Creates the account through the `signup` edge function (no email verification, no email
     * sent), then signs in with it.
     */
    fun signUp(email: String, password: String, displayName: String): CloudSession {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("display_name", displayName)
        call("POST", "/functions/v1/signup", body.toString())
        return signIn(email, password)
    }

    fun signIn(email: String, password: String): CloudSession {
        val body = JSONObject().put("email", email).put("password", password)
        return parseSession(JSONObject(call("POST", "/auth/v1/token?grant_type=password", body.toString())))
    }

    fun refresh(refreshToken: String): CloudSession {
        val body = JSONObject().put("refresh_token", refreshToken)
        return parseSession(JSONObject(call("POST", "/auth/v1/token?grant_type=refresh_token", body.toString())))
    }

    /** Best effort: the local session is dropped either way. */
    fun signOut(accessToken: String) {
        runCatching { call("POST", "/auth/v1/logout", "{}", accessToken) }
    }

    // ── Households ──────────────────────────────────────────────────────────

    /** The household the user belongs to (the first one joined), or null. */
    fun findMyHousehold(session: CloudSession): Household? {
        val rows = JSONArray(call(
            "GET",
            "/rest/v1/household_members?user_id=eq.${session.userId}" +
                "&select=household_id,role,households(name)&order=joined_at.asc&limit=1",
            token = session.accessToken,
        ))
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return Household(
            id      = row.getString("household_id"),
            name    = row.optJSONObject("households")?.optString("name").orEmpty(),
            isOwner = row.optString("role") == "owner",
        )
    }

    fun createHousehold(session: CloudSession, name: String): Household {
        val id = UUID.randomUUID().toString()
        val body = JSONObject().put("id", id).put("name", name)
        call("POST", "/rest/v1/households", body.toString(), session.accessToken, prefer = "return=minimal")
        return Household(id, name, isOwner = true)
    }

    /** Joins with an invite code; throws [SupabaseException] for an invalid or used code. */
    fun joinHousehold(session: CloudSession, code: String): Household {
        call("POST", "/rest/v1/rpc/join_household", JSONObject().put("p_code", code).toString(), session.accessToken)
        return findMyHousehold(session) ?: throw SupabaseException(404, "Household not found after joining")
    }

    fun createInvite(session: CloudSession, householdId: String): String {
        val result = call(
            "POST", "/rest/v1/rpc/create_household_invite",
            JSONObject().put("p_household_id", householdId).toString(), session.accessToken,
        )
        return JSONTokener(result).nextValue().toString()
    }

    fun members(session: CloudSession, householdId: String): List<HouseholdMember> {
        val rows = JSONArray(call(
            "GET", "/rest/v1/household_members?household_id=eq.$householdId&select=user_id,role&order=joined_at.asc",
            token = session.accessToken,
        ))
        val members = (0 until rows.length()).map { rows.getJSONObject(it) }
        if (members.isEmpty()) return emptyList()
        val ids = members.joinToString(",") { it.getString("user_id") }
        val profiles = JSONArray(call(
            "GET", "/rest/v1/profiles?user_id=in.($ids)&select=user_id,display_name",
            token = session.accessToken,
        ))
        val names = (0 until profiles.length()).associate {
            val p = profiles.getJSONObject(it)
            p.getString("user_id") to p.optString("display_name")
        }
        return members.map { m ->
            val userId = m.getString("user_id")
            HouseholdMember(
                userId      = userId,
                displayName = names[userId].orEmpty(),
                isOwner     = m.optString("role") == "owner",
                isMe        = userId == session.userId,
            )
        }
    }

    fun leaveHousehold(session: CloudSession, householdId: String) {
        call(
            "DELETE", "/rest/v1/household_members?household_id=eq.$householdId&user_id=eq.${session.userId}",
            token = session.accessToken, prefer = "return=minimal",
        )
    }

    // ── Tables ──────────────────────────────────────────────────────────────

    /** Every row of [table] in the household, deleted ones included, page by page. */
    fun selectAll(session: CloudSession, table: String, householdId: String, orderBy: String): List<JSONObject> {
        val rows = mutableListOf<JSONObject>()
        var offset = 0
        while (true) {
            val page = JSONArray(call(
                "GET",
                "/rest/v1/$table?household_id=eq.$householdId&select=*&order=$orderBy&limit=$PAGE_SIZE&offset=$offset",
                token = session.accessToken,
            ))
            for (i in 0 until page.length()) rows += page.getJSONObject(i)
            if (page.length() < PAGE_SIZE) return rows
            offset += PAGE_SIZE
        }
    }

    /** Inserts or updates [rows] (all with the same keys), matching on [onConflict]. */
    fun upsert(
        session: CloudSession,
        table: String,
        rows: List<JSONObject>,
        onConflict: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        var done = 0
        rows.chunked(UPSERT_BATCH).forEach { batch ->
            call(
                "POST", "/rest/v1/$table?on_conflict=$onConflict", JSONArray(batch).toString(), session.accessToken,
                prefer = "resolution=merge-duplicates,return=minimal",
            )
            done += batch.size
            onProgress(done, rows.size)
        }
    }

    /** Marks rows as deleted; [column] is matched against [values]. */
    fun softDelete(session: CloudSession, table: String, householdId: String, column: String, values: List<String>, deletedAt: String) {
        values.chunked(FILTER_BATCH).forEach { batch ->
            val list = batch.joinToString(",") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
            call(
                "PATCH",
                "/rest/v1/$table?household_id=eq.$householdId&$column=in.(${encode(list)})",
                JSONObject().put("deleted_at", deletedAt).toString(), session.accessToken,
                prefer = "return=minimal",
            )
        }
    }

    // ── HTTP ────────────────────────────────────────────────────────────────

    private fun call(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        prefer: String? = null,
    ): String {
        val requestBody = (body ?: if (method == "GET" || method == "DELETE") null else "")
            ?.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(SupabaseConfig.URL + path)
            .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            .apply {
                if (token != null) header("Authorization", "Bearer $token")
                if (prefer != null) header("Prefer", prefer)
            }
            .method(method, requestBody)
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseError(response.code, text)
            return text
        }
    }

    private fun parseSession(json: JSONObject): CloudSession {
        val user = json.getJSONObject("user")
        val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
            ?: (System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600L))
        return CloudSession(
            accessToken  = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAt    = expiresAt,
            userId       = user.getString("id"),
            email        = user.optString("email"),
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val PAGE_SIZE = 1000
        private const val UPSERT_BATCH = 200
        private const val FILTER_BATCH = 100

        /** Auth answers {msg, error_code} or {error, error_description}; the database {message, code}. */
        internal fun parseError(status: Int, body: String): SupabaseException {
            val json = runCatching { JSONObject(body) }.getOrNull()
                ?: return SupabaseException(status, body.ifBlank { "HTTP $status" })
            val message = listOf("msg", "error_description", "message", "error")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                ?: "HTTP $status"
            val code = listOf("error_code", "code", "error")
                .firstNotNullOfOrNull { key -> json.opt(key)?.toString()?.takeIf { it.isNotBlank() && it != "null" } }
            return SupabaseException(status, message, code)
        }

        private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
