package com.example.lockpay

import android.content.Context

object Checkin {
    /** One check-in round trip. Returns true if the server was reached. Call off the main thread. */
    fun run(c: Context): Boolean {
        if (Config.token(c).isEmpty()) return false
        val res = Api.post(c, "/device/checkin") ?: return false

        Config.setAccount(
            c,
            res.optJSONObject("account")?.toString() ?: "{}",
            res.optString("support_phone", "")
        )

        val cmds = res.optJSONArray("commands")
        if (cmds != null) {
            for (i in 0 until cmds.length()) {
                val cmd = cmds.getJSONObject(i)
                val p = cmd.getJSONObject("payload")
                // Ignore anything not signed by our server key or meant for another device.
                if (!Verifier.verify(p, cmd.getString("signature"))) continue
                if (p.getLong("device_id") != Config.deviceId(c)) continue

                val id = p.getLong("id")
                val action = p.getString("action")
                if (id > Config.lastCmdId(c)) {          // replay protection
                    when (action) {
                        "LOCK" -> Enforcer.lock(c)
                        "UNLOCK" -> Enforcer.unlock(c)
                    }
                    Config.setLastCmdId(c, id)
                }
                Api.post(c, "/device/commands/$id/ack")
                if (action == "RELEASE") { Enforcer.release(c); return true }
            }
        }

        if (Config.isLocked(c)) {
            LockActivity.instance?.let { a -> a.runOnUiThread { a.render() } }
        }
        return true
    }
}
