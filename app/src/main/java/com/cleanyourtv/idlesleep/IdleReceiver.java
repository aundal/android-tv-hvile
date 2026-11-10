package com.cleanyourtv.idlesleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;

/** The two timer alarms: quiet-time and screen-time. Both sleep through the same warning. */
public class IdleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Idle.ACTION_CHECK.equals(action)) {
            checkIdle(ctx);
        } else if (Screen.ACTION_CHECK.equals(action)) {
            checkScreen(ctx);
        }
    }

    private void checkIdle(Context ctx) {
        if (!Idle.enabled(ctx)) return;
        long due = Idle.dueElapsed(ctx);
        if (due <= 0 || SystemClock.elapsedRealtime() < due) {
            Idle.poke(ctx); // stray alarm: re-arm instead of acting
            return;
        }
        if (Idle.overdue(due)) {
            Idle.poke(ctx); // the TV slept through the deadline: just woken, fresh period
            return;
        }
        AudioManager am = ctx.getSystemService(AudioManager.class);
        if (am != null && am.isMusicActive()) {
            Idle.poke(ctx); // a film plays untouched: wait a full period more
            return;
        }
        fire(ctx, StillActivity.TIMER_IDLE, Idle.warnSeconds(ctx));
    }

    private void checkScreen(Context ctx) {
        if (!Screen.enabled(ctx)) return;
        long due = Screen.dueElapsed(ctx);
        if (due <= 0 || SystemClock.elapsedRealtime() < due) {
            Screen.poke(ctx); // stray alarm: re-arm instead of acting
            return;
        }
        if (Screen.overdue(due)) {
            Screen.poke(ctx); // just woken: fresh period
            return;
        }
        fire(ctx, StillActivity.TIMER_SCREEN, Screen.warnSeconds(ctx));
    }

    private void fire(Context ctx, String timer, int warnSeconds) {
        if (warnSeconds <= 0) {
            IdleService.sleep();
            Idle.poke(ctx);
            Screen.poke(ctx);
            return;
        }
        int minutes = StillActivity.TIMER_SCREEN.equals(timer) ? Screen.minutes(ctx) : Idle.minutes(ctx);
        Intent i = new Intent(ctx, StillActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(StillActivity.EXTRA_TIMER, timer)
                .putExtra(StillActivity.EXTRA_SECONDS, warnSeconds)
                .putExtra(StillActivity.EXTRA_MINUTES, minutes);
        ctx.startActivity(i);
    }
}
