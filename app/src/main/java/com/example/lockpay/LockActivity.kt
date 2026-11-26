package com.example.lockpay

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class LockActivity : Activity() {
    companion object { var instance: LockActivity? = null }

    private lateinit var body: TextView
    private lateinit var callBtn: Button

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        instance = this
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF111111.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Payment overdue"; textSize = 28f; setTextColor(Color.WHITE)
        })
        body = TextView(this).apply {
            textSize = 18f; setTextColor(Color.LTGRAY); setPadding(0, pad, 0, pad)
        }
        root.addView(body)

        callBtn = Button(this).apply {
            text = "Call support"
            setOnClickListener { dial(Config.support(this@LockActivity)) }
        }
        root.addView(callBtn)

        root.addView(Button(this).apply {
            text = "I've paid - check now"
            setOnClickListener {
                Thread {
                    val ok = Checkin.run(applicationContext)
                    runOnUiThread {
                        Toast.makeText(
                            this@LockActivity,
                            if (!ok) "No connection. Check your internet." else "Still locked. Contact support if you've paid.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.start()
            }
        })

        // Always available: emergency calls go through the dialer.
        root.addView(Button(this).apply {
            text = "Emergency call"
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL)) }
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (!Config.isLocked(this)) { finish(); return }
        render()
        val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
        if (dpm.isDeviceOwnerApp(packageName) && dpm.isLockTaskPermitted(packageName)) {
            val am = getSystemService(ActivityManager::class.java)
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) startLockTask()
        }
    }

    fun render() {
        val a = JSONObject(Config.account(this))
        val overdue = a.optLong("overdue", 0) / 100.0
        val balance = a.optLong("balance", 0) / 100.0
        val next = if (a.isNull("next_due")) "-" else a.optString("next_due", "-")
        body.text = "Your phone is locked because a payment is overdue.\n\n" +
            "Overdue: %.2f\nTotal balance: %.2f\nNext due: %s\n\n".format(overdue, balance, next) +
            "After you pay, tap \"I've paid\" or contact support."
        callBtn.visibility = if (Config.support(this).isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun dial(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
    }

    override fun onBackPressed() { /* blocked while locked */ }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
