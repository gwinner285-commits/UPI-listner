package com.eaglepay.listener

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object WebhookSender {
    private const val TAG = "EaglePayWebhook"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    data class Payload(
        val amount: Double,
        val utr: String?,
        val source: String,
        val payer_vpa: String?,
        val raw_text: String,
        val captured_at: Long = System.currentTimeMillis(), // epoch ms when notification was captured
    )

    fun send(ctx: Context, payload: Payload): Boolean {
        val url = Prefs.webhook(ctx) ?: return false
        val token = Prefs.token(ctx) ?: return false

        val req = Request.Builder()
            .url(url)
            .header("X-Device-Token", token)
            .header("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody(JSON))
            .build()

        return try {
            client.newCall(req).execute().use { res ->
                val ok = res.isSuccessful
                Log.d(TAG, "POST ${res.code} amount=${payload.amount} utr=${payload.utr} matched=$ok")
                if (ok && payload.source != "heartbeat") Prefs.recordEvent(ctx)
                ok
            }
        } catch (e: Exception) {
            Log.e(TAG, "send failed", e)
            false
        }
    }

    /** Heartbeat: an empty event with amount=0 to update last_seen_at. */
    fun heartbeat(ctx: Context) {
        send(ctx, Payload(amount = 0.0, utr = null, source = "heartbeat", payer_vpa = null, raw_text = ""))
    }
}
