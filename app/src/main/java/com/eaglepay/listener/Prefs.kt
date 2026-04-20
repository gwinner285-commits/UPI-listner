package com.eaglepay.listener

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "eagle_pay_prefs"
    private const val KEY_TOKEN = "device_token"
    private const val KEY_WEBHOOK = "webhook_url"
    private const val KEY_NAME = "device_name"
    private const val KEY_LAST = "last_event_at"
    private const val KEY_COUNT = "event_count"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun savePairing(ctx: Context, token: String, webhook: String, name: String) {
        sp(ctx).edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_WEBHOOK, webhook)
            putString(KEY_NAME, name)
        }.apply()
    }

    fun token(ctx: Context): String? = sp(ctx).getString(KEY_TOKEN, null)
    fun webhook(ctx: Context): String? = sp(ctx).getString(KEY_WEBHOOK, null)
    fun deviceName(ctx: Context): String? = sp(ctx).getString(KEY_NAME, null)

    fun isPaired(ctx: Context): Boolean = token(ctx) != null && webhook(ctx) != null

    // Bug 4 fix: event_count was an Int and would overflow after ~2.1B events.
    // Switched to Long; reads transparently migrate any pre-existing Int value.
    fun recordEvent(ctx: Context) {
        val s = sp(ctx)
        val current = readEventCount(s)
        s.edit()
            .putLong(KEY_LAST, System.currentTimeMillis())
            .putLong(KEY_COUNT, current + 1L)
            .apply()
    }

    fun lastEventAt(ctx: Context): Long = sp(ctx).getLong(KEY_LAST, 0L)
    fun eventCount(ctx: Context): Long = readEventCount(sp(ctx))

    /** Tolerates the legacy Int value persisted by older builds. */
    private fun readEventCount(s: SharedPreferences): Long = try {
        s.getLong(KEY_COUNT, 0L)
    } catch (_: ClassCastException) {
        val legacy = s.getInt(KEY_COUNT, 0).toLong()
        s.edit().putLong(KEY_COUNT, legacy).apply()
        legacy
    }

    fun clear(ctx: Context) { sp(ctx).edit().clear().apply() }
}
