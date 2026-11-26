package com.example.lockpay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Config.token(c).isEmpty()) return
        CheckInJob.schedule(c)
        if (Config.isLocked(c)) Enforcer.showLock(c)   // kiosk mode ends on reboot, so re-enter it
    }
}
