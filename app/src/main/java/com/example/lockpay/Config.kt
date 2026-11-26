package com.example.lockpay

import android.content.Context

object Config {
    private fun sp(c: Context) = c.getSharedPreferences("lockpay", Context.MODE_PRIVATE)

    fun server(c: Context): String = sp(c).getString("server", "")!!.ifEmpty { BuildConfig.SERVER_URL }
    fun token(c: Context): String = sp(c).getString("token", "")!!
    fun deviceId(c: Context): Long = sp(c).getLong("device_id", -1)

    fun save(c: Context, server: String, deviceId: Long, token: String) {
        sp(c).edit().putString("server", server).putLong("device_id", deviceId)
            .putString("token", token).commit()
    }

    fun isLocked(c: Context) = sp(c).getBoolean("locked", false)
    fun setLocked(c: Context, v: Boolean) { sp(c).edit().putBoolean("locked", v).commit() }

    // Highest command id already applied. Anything <= this is a replay and is ignored.
    fun lastCmdId(c: Context) = sp(c).getLong("last_cmd", 0)
    fun setLastCmdId(c: Context, id: Long) { sp(c).edit().putLong("last_cmd", id).commit() }

    fun account(c: Context): String = sp(c).getString("account", "{}")!!
    fun support(c: Context): String = sp(c).getString("support", "")!!
    fun setAccount(c: Context, account: String, support: String) {
        sp(c).edit().putString("account", account).putString("support", support).apply()
    }

    fun clear(c: Context) { sp(c).edit().clear().commit() }
}
