package com.cleanyourtv.idlesleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

/**
 * The inactivity deadline and its alarm. Every remote key pushes the deadline a full period out;
 * the alarm uses elapsed time (never wakes a sleeping TV) and an overdue firing means the TV was
 * just woken, so it restarts the period instead of sleeping.
 */
final class Idle {
    static final String ACTION_CHECK = "com.cleanyourtv.idlesleep.CHECK";
    /** An alarm this far past its deadline means the TV slept through it: fresh start, no sleep. */
    private static final long OVERDUE_MS = 120_000;

    private Idle() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("idle", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context ctx) {
        return prefs(ctx).getBoolean("enabled", true);
    }

    static void setEnabled(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean("enabled", v).apply();
    }

    static int minutes(Context ctx) {
        return clamp(prefs(ctx).getInt("minutes", 5), 1, 300);
    }

    static void setMinutes(Context ctx, int m) {
        prefs(ctx).edit().putInt("minutes", clamp(m, 1, 300)).apply();
    }

    static boolean overlay(Context ctx) {
        return warnSeconds(ctx) > 0;
    }

    static void setOverlay(Context ctx, boolean v) {
        if (!v) setWarnSeconds(ctx, 0);
        else if (warnSeconds(ctx) <= 0) setWarnSeconds(ctx, 60);
    }

    /** Warning countdown in seconds; 0 means sleep at once with no warning. */
    static int warnSeconds(Context ctx) {
        SharedPreferences p = prefs(ctx);
        if (p.contains("warn_seconds")) return clamp(p.getInt("warn_seconds", 60), 0, 300);
        // Migrate the first version: overlay on/off + 15-300 seconds.
        if (!p.getBoolean("overlay", true)) return 0;
        return clamp(p.getInt("overlay_seconds", 60), 0, 300);
    }

    static void setWarnSeconds(Context ctx, int s) {
        prefs(ctx).edit().putInt("warn_seconds", clamp(s, 0, 300)).apply();
    }

    static long dueElapsed(Context ctx) {
        return prefs(ctx).getLong("due_elapsed", 0);
    }

    /** A key was pressed (or media is playing): push the deadline a full period out. */
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
        return PendingIntent.getBroadcast(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
