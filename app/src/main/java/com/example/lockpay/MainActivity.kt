package com.example.lockpay

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        if (Config.token(this).isEmpty()) {
            // One-time setup (done by the shop at hand-over).
            val server = EditText(this).apply { hint = "Server URL (http://IP:8000)"; setText(Config.server(this@MainActivity)) }
            val devId = EditText(this).apply { hint = "Device ID"; inputType = InputType.TYPE_CLASS_NUMBER }
            val token = EditText(this).apply { hint = "Device token" }
            root.addView(server); root.addView(devId); root.addView(token)
            root.addView(Button(this).apply {
                text = "Save and enroll"
                setOnClickListener {
                    val id = devId.text.toString().toLongOrNull()
                    if (server.text.isBlank() || id == null || token.text.isBlank()) {
                        Toast.makeText(this@MainActivity, "Fill in all three fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    Config.save(this@MainActivity, server.text.toString().trim(), id, token.text.toString().trim())
                    Enforcer.harden(this@MainActivity)
                    CheckInJob.schedule(this@MainActivity)
                    Thread { Checkin.run(applicationContext) }.start()
                    recreate()
                }
            })
        } else {
            root.addView(Button(this).apply {
                text = "Check in now"
                setOnClickListener {
                    Thread {
                        val ok = Checkin.run(applicationContext)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, if (ok) "Checked in" else "Could not reach server", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }.start()
                }
            })
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        if (Config.isLocked(this)) {            // while locked, only the lock screen is reachable
            Enforcer.showLock(this); finish(); return
        }
        refresh()
    }

    private fun refresh() {
        val owner = Enforcer.isOwner(this)
        status.text = buildString {
            append("Device owner: ${if (owner) "YES" else "NO"}\n")
            if (!owner) append("Locking will not work. Run:\nadb shell dpm set-device-owner $packageName/.AdminReceiver\n")
            append("Enrolled: ${if (Config.token(this@MainActivity).isEmpty()) "no" else "yes (device ${Config.deviceId(this@MainActivity)})"}\n")
            append("State: ${if (Config.isLocked(this@MainActivity)) "LOCKED" else "unlocked"}\n")
        }
    }
}
