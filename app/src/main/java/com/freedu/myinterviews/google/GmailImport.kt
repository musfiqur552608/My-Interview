package com.freedu.myinterviews.google

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GmailMessage(
    val id: String,
    val subject: String,
    val from: String,
    val date: String,
    val snippet: String
)

class TokenExpiredException : Exception("Session expired — please retry")

/**
 * Minimal Gmail REST client over HttpURLConnection (no Google SDKs).
 * Read-only: lists interview-related threads and decodes one message body
 * for the offline EmailParser.
 */
object GmailImport {

    private fun get(url: String, token: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = conn.responseCode
        if (code == 401) throw TokenExpiredException()
        if (code !in 200..299) error("Gmail error $code")
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    suspend fun listInterviewMail(token: String, max: Int = 15): List<GmailMessage> =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(
                "interview OR recruiter OR \"phone screen\" OR onsite OR offer newer_than:180d",
                "UTF-8"
            )
            val json = JSONObject(
                get(
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages" +
                        "?maxResults=$max&q=$q&fields=messages/id", token
                )
            )
            val ids = json.optJSONArray("messages") ?: return@withContext emptyList()
            (0 until ids.length()).mapNotNull { i ->
                runCatching { fetchMeta(token, ids.getJSONObject(i).getString("id")) }.getOrNull()
            }
        }

    private fun fetchMeta(token: String, id: String): GmailMessage {
        val json = JSONObject(
            get(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id" +
                    "?format=metadata&metadataHeaders=Subject&metadataHeaders=From" +
                    "&metadataHeaders=Date&fields=id,snippet,payload/headers", token
            )
        )
        var subject = ""; var from = ""; var date = ""
        val headers = json.optJSONObject("payload")?.optJSONArray("headers")
        if (headers != null) {
            for (i in 0 until headers.length()) {
                val h = headers.getJSONObject(i)
                when (h.optString("name").lowercase()) {
                    "subject" -> subject = h.optString("value")
                    "from" -> from = h.optString("value")
                    "date" -> date = h.optString("value")
                }
            }
        }
        return GmailMessage(id, subject, from, date, json.optString("snippet"))
    }

    /** Full plain-text body (prefers text/plain part, falls back to stripped HTML). */
    suspend fun fetchBody(token: String, id: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject(
            get(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id" +
                    "?format=full&fields=payload", token
            )
        )
        val payload = json.getJSONObject("payload")
        decodePart(payload) ?: ""
    }

    private fun decodePart(part: JSONObject): String? {
        val mime = part.optString("mimeType")
        val data = part.optJSONObject("body")?.optString("data")
        if (part.optJSONArray("parts") == null) {
            if (data.isNullOrBlank()) return null
            val text = String(
                android.util.Base64.decode(data, android.util.Base64.URL_SAFE),
                Charsets.UTF_8
            )
            return if (mime == "text/html") stripHtml(text) else text
        }
        val parts = part.getJSONArray("parts")
        // Prefer plain text anywhere in the tree.
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (p.optString("mimeType") == "text/plain") {
                decodePart(p)?.let { return it }
            }
        }
        for (i in 0 until parts.length()) {
            decodePart(parts.getJSONObject(i))?.let { return it }
        }
        return null
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<(script|style)[^>]*>.*?</\\1>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace(Regex("[ \t]+"), " ").replace(Regex("\n{3,}"), "\n\n")
            .trim()

    @Suppress("unused")
    fun invalidateOnAuthFailure(ctx: Context, token: String) =
        GoogleAuth.invalidate(ctx, token)
}
