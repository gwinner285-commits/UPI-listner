package com.eaglepay.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** No-op: NotificationListenerService is auto-started by the system once enabled.
 *  This receiver exists so the OS treats the app as "boot-aware" and is less
 *  aggressive about killing it. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { /* nothing */ }
}
