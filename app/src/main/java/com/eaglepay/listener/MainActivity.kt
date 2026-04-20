package com.eaglepay.listener

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eaglepay.listener.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val gson = Gson()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) return@registerForActivityResult
        try {
            val payload = gson.fromJson(result.contents, PairingPayload::class.java)
            require(payload.token.isNotBlank() && payload.webhook.isNotBlank()) { "invalid QR" }
            Prefs.savePairing(this, payload.token, payload.webhook, payload.name ?: "Phone")
            Toast.makeText(this, "Paired!", Toast.LENGTH_SHORT).show()
            refreshUi()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid QR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    data class PairingPayload(val v: Int = 1, val token: String = "", val webhook: String = "", val name: String? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnScan.setOnClickListener {
            scanLauncher.launch(ScanOptions().apply {
                setOrientationLocked(true)
                setBeepEnabled(false)
                setPrompt("Scan the pairing QR from your Eagle Pay dashboard")
            })
        }
        b.btnEnableAccess.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        b.btnBattery.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        b.btnUnpair.setOnClickListener {
            Prefs.clear(this)
            refreshUi()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val paired = Prefs.isPaired(this)
        val accessGranted = NotificationCaptureService.isAccessGranted(this)
        val batteryIgnored = (getSystemService(POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)

        b.statusPaired.text = if (paired) "✅ Paired (${Prefs.deviceName(this)})" else "❌ Not paired"
        b.statusAccess.text = if (accessGranted) "✅ Notification access granted" else "❌ Notification access required"
        b.statusBattery.text = if (batteryIgnored) "✅ Battery optimisation off" else "⚠️ Battery optimisation on (may kill app)"

        val last = Prefs.lastEventAt(this)
        b.statusLast.text = if (last == 0L) "No events yet"
        else "Last event: ${SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(last))} • ${Prefs.eventCount(this)} total"

        b.btnEnableAccess.visibility = if (accessGranted) View.GONE else View.VISIBLE
        b.btnBattery.visibility = if (batteryIgnored) View.GONE else View.VISIBLE
        b.btnUnpair.visibility = if (paired) View.VISIBLE else View.GONE

        val ready = paired && accessGranted
        b.statusReady.text = if (ready) "🟢 Listener is RUNNING" else "🔴 Setup incomplete"
        b.statusReady.setTextColor(if (ready) 0xFF22C55E.toInt() else 0xFFEF4444.toInt())
    }
}
