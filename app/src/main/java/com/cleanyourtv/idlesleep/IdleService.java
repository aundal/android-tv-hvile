package com.cleanyourtv.idlesleep;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Resets the inactivity timer on every remote key (never consumed: the remote keeps working) and
 * puts the TV to sleep through the lock-screen global action, which an ordinary app cannot do
 * otherwise. Asks for no events and reads no windows.
 */
public class IdleService extends AccessibilityService {
    private static IdleService instance;

    static boolean running() {
        return instance != null;
    }

    static boolean sleep() {
        IdleService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        Idle.poke(this);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) Idle.poke(this);
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
