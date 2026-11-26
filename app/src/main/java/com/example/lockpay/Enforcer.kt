package com.example.lockpay

import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.telecom.TelecomManager

/** All Device Owner actions live here. Nothing is wiped; nothing is read from the phone. */
object Enforcer {
    private val LOCKED_RESTRICTIONS = listOf(
        UserManager.DISALLOW_FACTORY_RESET,
        UserManager.DISALLOW_SAFE_BOOT
    )

    private fun dpm(c: Context) = c.getSystemService(DevicePolicyManager::class.java)
    private fun admin(c: Context) = ComponentName(c, AdminReceiver::class.java)
    fun isOwner(c: Context) = dpm(c).isDeviceOwnerApp(c.packageName)

    /** Make the app hard to remove or stop while enrolled. Reversed by release(). */
    fun harden(c: Context) {
        if (!isOwner(c)) return
        try {
            val d = dpm(c); val a = admin(c)
            d.setUninstallBlocked(a, c.packageName, true)
            if (Build.VERSION.SDK_INT >= 30) d.setUserControlDisabledPackages(a, listOf(c.packageName))
        } catch (_: SecurityException) {}
    }

    private fun unharden(c: Context) {
        if (!isOwner(c)) return
        try {
            val d = dpm(c); val a = admin(c)
            d.setUninstallBlocked(a, c.packageName, false)
            if (Build.VERSION.SDK_INT >= 30) d.setUserControlDisabledPackages(a, emptyList())
        } catch (_: SecurityException) {}
    }

    fun lock(c: Context) {
        Config.setLocked(c, true)
        if (isOwner(c)) {
            try {
                val d = dpm(c); val a = admin(c)
                // Allow our screen plus the default dialer, so emergency calls always work.
                val pkgs = mutableSetOf(c.packageName)
                c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { pkgs.add(it) }
                d.setLockTaskPackages(a, pkgs.toTypedArray())
                if (Build.VERSION.SDK_INT >= 28) {
                    d.setLockTaskFeatures(a, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                }
                LOCKED_RESTRICTIONS.forEach { d.addUserRestriction(a, it) }
            } catch (_: SecurityException) {}
        }
        showLock(c)
    }

    fun showLock(c: Context) {
        val i = Intent(c, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val opts = if (isOwner(c) && dpm(c).isLockTaskPermitted(c.packageName))
            ActivityOptions.makeBasic().apply { setLockTaskEnabled(true) }.toBundle() else null
        c.startActivity(i, opts)
    }

    fun unlock(c: Context) {
        Config.setLocked(c, false)
        LockActivity.instance?.let { a ->
            a.runOnUiThread {
                try { a.stopLockTask() } catch (_: Exception) {}
                a.finish()
            }
        }
        if (isOwner(c)) {
            try {
                val d = dpm(c); val a = admin(c)
                LOCKED_RESTRICTIONS.forEach { d.clearUserRestriction(a, it) }
                d.setLockTaskPackages(a, arrayOf<String>())
            } catch (_: SecurityException) {}
        }
    }

    /** Customer finished paying: remove all control and give the phone back. */
    fun release(c: Context) {
        unlock(c)
        unharden(c)
        c.getSystemService(JobScheduler::class.java).cancelAll()
        if (isOwner(c)) {
            try { dpm(c).clearDeviceOwnerApp(c.packageName) } catch (_: SecurityException) {}
        }
        Config.clear(c)
    }
}
