package com.cleanyourtv.idlesleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

/**
 * Absolute screen-time limit: sleeps after N minutes of screen-on time no matter what plays or
 * which keys are pressed. The alarm uses elapsed time (never wakes a sleeping TV); an overdue
 * firing means the TV was just woken, so it restarts the period (= reset on every sleep).
 */
final class Screen {
    static final String ACTION_CHECK = "com.cleanyourtv.idlesleep.SCREEN_CHECK";
    private static final long OVERDUE_MS = 120_000;

    private Screen() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("screen", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context ctx) {
        return prefs(ctx).getBoolean("enabled", false);
    }

    static void setEnabled(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean("enabled", v).apply();
    }

    static int minutes(Context ctx) {
        return clamp(prefs(ctx).getInt("minutes", 60), 1, 300);
    }

    static void setMinutes(Context ctx, int m) {
        prefs(ctx).edit().putInt("minutes", clamp(m, 1, 300)).apply();
    }

    /** Warning countdown in seconds; 0 means sleep at once with no warning. */
    static int warnSeconds(Context ctx) {
        return clamp(prefs(ctx).getInt("warn_seconds", 60), 0, 300);
    }

    static void setWarnSeconds(Context ctx, int s) {
        prefs(ctx).edit().putInt("warn_seconds", clamp(s, 0, 300)).apply();
    }

    static long dueElapsed(Context ctx) {
        return prefs(ctx).getLong("due_elapsed", 0);
    }

    /** Fresh full period from now (wake, boot, Ja answer, manual sleep). */
    static void poke(Context ctx) {
        if (!enabled(ctx)) {
            cancel(ctx);
            return;
        }
        long at = SystemClock.elapsedRealtime() + (long) minutes(ctx) * 60_000L;
        prefs(ctx).edit().putLong("due_elapsed", at).apply();
        schedule(ctx, at);
    }

    static void cancel(Context ctx) {
        prefs(ctx).edit().remove("due_elapsed").apply();
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(pending(ctx));
    }

    static boolean overdue(long dueElapsed) {
        return dueElapsed > 0 && SystemClock.elapsedRealtime() - dueElapsed > OVERDUE_MS;
    }

    private static void schedule(Context ctx, long atElapsed) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pending(ctx);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, atElapsed, pi);
        } else {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, atElapsed, pi);
            } catch (SecurityException e) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, atElapsed, pi);
            }
        }
    }

    private static PendingIntent pending(Context ctx) {
        Intent intent = new Intent(ctx, IdleReceiver.class).setAction(ACTION_CHECK);
        return PendingIntent.getBroadcast(ctx, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
