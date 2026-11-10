package com.cleanyourtv.idlesleep;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Ser du stadig TV?" over whatever is on screen. Ja re-arms the timer that fired, Nej sleeps
 * at once, silence at zero sleeps too. Any non-navigation key counts as Ja.
 */
public class StillActivity extends Activity {
    static final String EXTRA_TIMER = "timer";
    static final String EXTRA_SECONDS = "seconds";
    static final String EXTRA_MINUTES = "minutes";
    static final String TIMER_IDLE = "idle";
    static final String TIMER_SCREEN = "screen";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView countdown;
    private long end;
    private boolean done;
    private String timer = TIMER_IDLE;
    private int reasonMinutes;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long left = (end - System.currentTimeMillis() + 999) / 1000;
            if (left <= 0) {
                answer(false);
                return;
            }
            countdown.setText(getString(R.string.still_countdown, left / 60, left % 60));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_TIMER)) timer = getIntent().getStringExtra(EXTRA_TIMER);
            int seconds = getIntent().getIntExtra(EXTRA_SECONDS, 60);
            int minutes = getIntent().getIntExtra(EXTRA_MINUTES, 0);
            end = System.currentTimeMillis() + (long) Math.max(1, seconds) * 1000L;
            reasonMinutes = minutes;
        } else {
            end = System.currentTimeMillis() + 60_000L;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(Ui.pill(this, 0xF2161826));
        int pad = Ui.dp(this, 36);
        box.setPadding(pad, pad, pad, pad);

        TextView title = Ui.text(this, 40, Color.WHITE);
        title.setText(TIMER_SCREEN.equals(timer) ? R.string.still_title_screen : R.string.still_title);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        TextView reason = Ui.text(this, 20, 0xFFB9B2FF);
        reason.setGravity(Gravity.CENTER);
        reason.setPadding(0, Ui.dp(this, 6), 0, 0);
        reason.setText(getString(TIMER_SCREEN.equals(timer)
                ? R.string.still_reason_screen : R.string.still_reason_idle, reasonMinutes));
        box.addView(reason);
        countdown = Ui.text(this, 34, Color.WHITE);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 14));
        box.addView(countdown);

        TextView yes = Ui.row(box, 460, 24);
        yes.setText(R.string.still_yes);
        yes.setOnClickListener(v -> answer(true));
        TextView no = Ui.row(box, 460, 24);
        no.setText(R.string.still_no);
        no.setOnClickListener(v -> answer(false));

        FrameLayout root = new FrameLayout(this);
        root.addView(box, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
        yes.requestFocus();
    }

    private void answer(boolean watching) {
        if (done) return;
        done = true;
        handler.removeCallbacks(tick);
        if (watching) {
            rearm();
        } else {
            IdleService.sleep();
            Idle.poke(this);
            Screen.poke(this);
        }
        finish();
    }

    private void rearm() {
        if (TIMER_SCREEN.equals(timer)) Screen.poke(this);
        else Idle.poke(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
                return super.onKeyDown(keyCode, event);
            default:
                answer(true); // volume and friends: someone is there
                return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        // Back or Home leaves the warning, not the decision: the next alarm still fires.
        handler.removeCallbacks(tick);
        super.onPause();
    }
}
