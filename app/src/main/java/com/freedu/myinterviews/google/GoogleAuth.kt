package com.freedu.myinterviews.google

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google OAuth without any SDK dependency: the on-device Google account
 * authenticator mints tokens via AccountManager (system consent UI on first
 * use). Works with any Google account on the device; no API keys, no client
 * ids, fully user-revocable at https://myaccount.google.com/permissions.
 */
object GoogleAuth {
    const val GMAIL_READONLY = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
    const val DRIVE_APPDATA = "oauth2:https://www.googleapis.com/auth/drive.appdata"

    fun pickAccountIntent(): Intent =
        AccountManager.newChooseAccountIntent(
            null, null, arrayOf("com.google"), null, null, null, null
        )

    sealed interface AuthResult {
        data class Token(val token: String) : AuthResult
        data class Consent(val intent: Intent) : AuthResult
        data class Error(val msg: String) : AuthResult
    }

    /** Mint (or reuse) an OAuth token; Consent carries the system approval intent to launch. */
    suspend fun getToken(activity: Activity, accountName: String, scope: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            val am = AccountManager.get(activity)
            try {
                am.getAuthToken(
                    Account(accountName, "com.google"), scope, null, activity,
                    { future ->
                        try {
                            val b = future.result
                            val token = b.getString(AccountManager.KEY_AUTHTOKEN)
                            @Suppress("DEPRECATION")
                            val consent = b.getParcelable(AccountManager.KEY_INTENT) as? Intent
                            when {
                                token != null -> cont.resume(AuthResult.Token(token))
                                consent != null -> cont.resume(AuthResult.Consent(consent))
                                else -> cont.resume(
                                    AuthResult.Error(
                                        b.getString(AccountManager.KEY_ERROR_MESSAGE) ?: "Sign-in failed"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            if (!cont.isCompleted) {
                                cont.resume(AuthResult.Error(e.message ?: "Sign-in failed"))
                            }
                        }
                    }, null
                )
            } catch (e: Exception) {
                if (!cont.isCompleted) cont.resume(AuthResult.Error(e.message ?: "Sign-in failed"))
            }
        }

    fun invalidate(ctx: android.content.Context, token: String) {
        runCatching {
            AccountManager.get(ctx).invalidateAuthToken("com.google", token)
        }
    }
}
