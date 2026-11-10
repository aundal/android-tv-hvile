package com.cleanyourtv.idlesleep;

import android.app.Activity;
import android.app.AlarmManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Two columns, remote-driven: Inaktivitet (sleep after quiet keys and quiet sound) and Skærmtid
 * (sleep after screen-on minutes no matter what). Up/Down walks a column and wraps across the
 * top; Left/Right adjusts; OK confirms.
 */
public class SettingsActivity extends Activity {
    private TextView statusService;
    private TextView statusOverlay;
    private final TextView[] left = new TextView[3];
    private final TextView[] right = new TextView[3];
    private final View.OnKeyListener[] leftAdjust = new View.OnKeyListener[3];
    private final View.OnKeyListener[] rightAdjust = new View.OnKeyListener[3];
    private TextView testRow;
    private TextView nowRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 40);
        list.setPadding(pad, pad, pad, pad);

        TextView title = Ui.text(this, 28, Color.WHITE);
        title.setText(R.string.settings_title);
        title.setGravity(Gravity.CENTER);
        list.addView(title);

        statusService = Ui.text(this, 16, Color.WHITE);
        statusService.setGravity(Gravity.CENTER);
        statusOverlay = Ui.text(this, 16, Color.WHITE);
        statusOverlay.setGravity(Gravity.CENTER);

        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        columns.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout leftCol = column(columns, getString(R.string.col_idle));
        LinearLayout rightCol = column(columns, getString(R.string.col_screen));
        list.addView(columns);

        left[0] = row(leftCol, 400, 18);
        left[0].setOnClickListener(v -> {
            Idle.setEnabled(this, !Idle.enabled(this));
            if (Idle.enabled(this)) Idle.poke(this); else Idle.cancel(this);
            refresh();
        });
        left[1] = meter(leftCol, 400, 18);
        leftAdjust[1] = adjust(l -> {
            Idle.setMinutes(this, Idle.minutes(this) + (l ? -1 : 1));
            Idle.poke(this);
        });
        left[1].setOnKeyListener(leftAdjust[1]);
        left[1].setOnClickListener(v -> {
            Idle.setMinutes(this, Idle.minutes(this) + 5);
            Idle.poke(this);
            refresh();
        });
        left[2] = meter(leftCol, 400, 18);
        leftAdjust[2] = adjust(l -> Idle.setWarnSeconds(this, Idle.warnSeconds(this) + (l ? -5 : 5)));
        left[2].setOnKeyListener(leftAdjust[2]);
        left[2].setOnClickListener(v -> {
            Idle.setWarnSeconds(this, Idle.warnSeconds(this) + 30);
            refresh();
        });

        right[0] = row(rightCol, 400, 18);
        right[0].setOnClickListener(v -> {
            Screen.setEnabled(this, !Screen.enabled(this));
            if (Screen.enabled(this)) Screen.poke(this); else Screen.cancel(this);
            refresh();
        });
        right[1] = meter(rightCol, 400, 18);
        rightAdjust[1] = adjust(l -> {
            Screen.setMinutes(this, Screen.minutes(this) + (l ? -1 : 1));
            Screen.poke(this);
        });
        right[1].setOnKeyListener(rightAdjust[1]);
        right[1].setOnClickListener(v -> {
            Screen.setMinutes(this, Screen.minutes(this) + 5);
            Screen.poke(this);
            refresh();
        });
        right[2] = meter(rightCol, 400, 18);
        rightAdjust[2] = adjust(l -> Screen.setWarnSeconds(this, Screen.warnSeconds(this) + (l ? -5 : 5)));
        right[2].setOnKeyListener(rightAdjust[2]);
        right[2].setOnClickListener(v -> {
            Screen.setWarnSeconds(this, Screen.warnSeconds(this) + 30);
            refresh();
        });

        wireColumns();

        testRow = Ui.row(list, 520, 18);
        testRow.setText(R.string.test_overlay);
        testRow.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, StillActivity.class)
                        .putExtra(StillActivity.EXTRA_TIMER, StillActivity.TIMER_IDLE)
                        .putExtra(StillActivity.EXTRA_SECONDS, Math.max(15, Idle.warnSeconds(this)))
                        .putExtra(StillActivity.EXTRA_MINUTES, Idle.minutes(this))));
        nowRow = Ui.row(list, 520, 18);
        nowRow.setText(R.string.sleep_now);
        nowRow.setOnClickListener(v -> {
            if (!IdleService.sleep()) refresh();
        });
        statusService.setPadding(0, Ui.dp(this, 16), 0, 0);
        list.addView(statusService);
        list.addView(statusOverlay);
        wireBottom();

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout root = new FrameLayout(this);
        root.addView(scroll, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        setContentView(root);
    }

    private LinearLayout column(LinearLayout parent, String header) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 12);
        col.setPadding(pad, 0, pad, 0);
        TextView h = Ui.text(this, 22, Color.WHITE);
        h.setText(header);
        h.setPadding(0, 0, 0, Ui.dp(this, 4));
        col.addView(h);
        parent.addView(col, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return col;
    }

    private TextView row(LinearLayout parent, int widthDp, int sp) {
        return Ui.row(parent, widthDp, sp);
    }

    private Ui.MeterRow meter(LinearLayout parent, int widthDp, int sp) {
        return Ui.meterRow(parent, widthDp, sp);
    }

    /** Left/Right adjusts; returns true when consumed. */
    private interface Step {
        void apply(boolean left);
    }

    private View.OnKeyListener adjust(Step step) {
        return (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                step.apply(true);
                refresh();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                step.apply(false);
                refresh();
                return true;
            }
            return false;
        };
    }

    /** Up/Down walks a column and wraps across the top row. */
    private void wireColumns() {
        for (int i = 0; i < 3; i++) {
            final int row = i;
            View.OnKeyListener nav = (v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                boolean leftSide = v == left[row];
                TextView[] here = leftSide ? left : right;
                TextView[] there = leftSide ? right : left;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    (row == 0 ? there[0] : here[row - 1]).requestFocus();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    (row == 2 ? testRow : here[row + 1]).requestFocus();
                    return true;
                }
                return false;
            };
            left[i].setOnKeyListener(combine(leftAdjust[i], nav));
            right[i].setOnKeyListener(combine(rightAdjust[i], nav));
        }
    }

    private void wireBottom() {
        testRow.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                left[2].requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                nowRow.requestFocus();
                return true;
            }
            return false;
        });
        nowRow.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                testRow.requestFocus();
                return true;
            }
            return false;
        });
    }

    private View.OnKeyListener combine(View.OnKeyListener first, View.OnKeyListener second) {
        if (first == null) return second;
        return (v, keyCode, event) -> first.onKey(v, keyCode, event) || second.onKey(v, keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        left[0].requestFocus();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The watch service may still be binding when the screen opens: re-read the status.
        if (hasFocus) refresh();
    }

    private void refresh() {
        boolean service = IdleService.running();
        statusService.setText(getString(service
                ? R.string.status_service_on : R.string.status_service_off));
        boolean overlayOk = Settings.canDrawOverlays(this);
        statusOverlay.setText(getString(overlayOk
                ? R.string.status_overlay_on : R.string.status_overlay_off));
        left[0].setText(getString(Idle.enabled(this) ? R.string.enabled_on : R.string.enabled_off));
        left[1].setText(getString(R.string.minutes_row, Idle.minutes(this)));
        ((Ui.MeterRow) left[1]).setFraction((Idle.minutes(this) - 1) / 299f);
        left[2].setText(warnText(Idle.warnSeconds(this)));
        ((Ui.MeterRow) left[2]).setFraction(Idle.warnSeconds(this) / 300f);
        right[0].setText(getString(Screen.enabled(this) ? R.string.enabled_on : R.string.enabled_off));
        right[1].setText(getString(R.string.minutes_row, Screen.minutes(this)));
        ((Ui.MeterRow) right[1]).setFraction((Screen.minutes(this) - 1) / 299f);
        right[2].setText(warnText(Screen.warnSeconds(this)));
        ((Ui.MeterRow) right[2]).setFraction(Screen.warnSeconds(this) / 300f);
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = getSystemService(AlarmManager.class);
            if (am != null && !am.canScheduleExactAlarms()) {
                statusOverlay.append(getString(R.string.status_alarm_off));
            }
        }
    }

    private String warnText(int seconds) {
        if (seconds <= 0) return getString(R.string.warn_off);
        return getString(R.string.warn_seconds_row, seconds);
    }
}
