package com.cleanyourtv.idlesleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After a reboot or an update the alarm is gone: start a fresh inactivity period. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Idle.poke(ctx);
            Screen.poke(ctx);
        }
    }
}
