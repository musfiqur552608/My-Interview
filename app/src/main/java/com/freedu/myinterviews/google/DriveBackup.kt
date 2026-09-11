package com.freedu.myinterviews.google

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google Drive **appDataFolder** backup (hidden from the user's Drive, private
 * to this app). Same AccountManager OAuth as Gmail — no SDKs, user-revocable.
 * Payload is the same CSV format as local export, so restores reuse CsvBackup.
 */
object DriveBackup {
    const val FILENAME = "interview-tracker-backup.csv"

    private fun conn(url: String, method: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 25_000
            readTimeout = 25_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun read(conn: HttpURLConnection): String {
        val code = conn.responseCode
        if (code == 401) throw TokenExpiredException()
        if (code !in 200..299) {
            val err = runCatching {
                conn.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            error("Drive error $code ${err?.take(200).orEmpty()}")
        }
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun existingFileId(token: String): String? {
        val q = URLEncoder.encode(
            "'appDataFolder' in parents and name='$FILENAME' and trashed=false", "UTF-8"
        )
        val json = JSONObject(
            read(conn(
                "https://www.googleapis.com/drive/v3/files" +
                    "?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime)" +
                    "&orderBy=modifiedTime desc&pageSize=1", "GET", token
            ))
        )
        val files = json.optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    /** Upload (create or replace). Returns a human summary. */
    suspend fun backup(token: String, csv: String): String = withContext(Dispatchers.IO) {
        val id = existingFileId(token)
        if (id == null) {
            val boundary = "itb${System.currentTimeMillis()}"
            val meta = JSONObject()
                .put("name", FILENAME)
                .put("parents", org.json.JSONArray().put("appDataFolder"))
                .toString()
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(meta).append("\r\n")
                append("--$boundary\r\n")
                append("Content-Type: text/csv\r\n\r\n")
                append(csv).append("\r\n")
                append("--$boundary--\r\n")
            }.toByteArray()
            val c = conn(
                "https://www.googleapis.com/upload/drive/v3/files" +
                    "?uploadType=multipart&fields=id", "POST", token
            )
            c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            c.doOutput = true
            c.outputStream.use { it.write(body) }
            read(c)
            "Backed up to Google Drive just now."
        } else {
            val c = conn(
                "https://www.googleapis.com/upload/drive/v3/files/$id" +
                    "?uploadType=media&fields=modifiedTime", "PATCH", token
            )
            c.setRequestProperty("Content-Type", "text/csv")
            c.doOutput = true
            c.outputStream.use { it.write(csv.toByteArray()) }
            val at = JSONObject(read(c)).optString("modifiedTime")
            "Backed up to Google Drive ($at)."
        }
    }

    /** Download latest backup CSV. Null when none exists yet. */
    suspend fun restore(token: String): String? = withContext(Dispatchers.IO) {
        val id = existingFileId(token) ?: return@withContext null
        read(conn("https://www.googleapis.com/drive/v3/files/$id?alt=media", "GET", token))
    }
}
