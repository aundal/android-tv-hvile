package com.cleanyourtv.idlesleep;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Rounded rows that light up when focused; adjustable ones fill with the value, meter-style. */
final class Ui {
    private Ui() { }

    static TextView text(Context ctx, int sp, int color) {
        TextView view = new TextView(ctx);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return view;
    }

    static TextView row(LinearLayout parent, int widthDp) {
        return row(parent, widthDp, 20);
    }

    static TextView row(LinearLayout parent, int widthDp, int sp) {
        TextView row = text(parent.getContext(), sp, Color.WHITE);
        styleRow(parent.getContext(), row, widthDp);
        parent.addView(row, params(parent.getContext(), widthDp));
        return row;
    }

    /** An adjustable row whose background fills with the value between min and max. */
    static MeterRow meterRow(LinearLayout parent, int widthDp, int sp) {
        MeterRow row = new MeterRow(parent.getContext());
        row.setTextColor(Color.WHITE);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        styleRow(parent.getContext(), row, widthDp);
        row.setBackground(null); // the meter paints its own track, fill and focus accent
        parent.addView(row, params(parent.getContext(), widthDp));
        return row;
    }

    private static void styleRow(Context ctx, TextView row, int widthDp) {
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 20), dp(ctx, 14), dp(ctx, 20), dp(ctx, 14));
        row.setMinHeight(dp(ctx, 64));
        row.setFocusable(true);
        row.setClickable(true);
        row.setSingleLine(true);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, pill(ctx, 0x40FFFFFF));
        bg.addState(new int[]{}, pill(ctx, 0x14FFFFFF));
        row.setBackground(bg);
    }

    private static LinearLayout.LayoutParams params(Context ctx, int widthDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, widthDp),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 10);
        return lp;
    }

    static GradientDrawable pill(Context ctx, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(ctx, 28));
        return d;
    }

    static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** A row with a meter fill behind the text: fraction 0-1 of the width, accent while focused. */
    static class MeterRow extends TextView {
        private float fraction;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float radius;
        private float stroke;

        public MeterRow(Context ctx) {
            super(ctx);
            init(ctx, null);
        }

        public MeterRow(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
            init(ctx, attrs);
        }

        private void init(Context ctx, AttributeSet attrs) {
            radius = dp(ctx, 28);
            stroke = dp(ctx, 4);
            track.setColor(0x14FFFFFF);
            fill.setColor(0xFF3FA7D6);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(stroke);
            ring.setColor(Color.WHITE);
        }

        void setFraction(float f) {
            fraction = Math.max(0f, Math.min(1f, f));
            if (isFocused()) fill.setColor(0xFF7AC4E4);
            else fill.setColor(0xFF3FA7D6);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                // Full-bleed like the plain rows: padding is for the text only.
                rect.set(0, 0, w, h);
                canvas.drawRoundRect(rect, radius, radius, track);
                float fw = w * fraction;
                if (fw > 0) {
                    int save = canvas.save();
                    canvas.clipRect(0, 0, fw, h);
                    canvas.drawRoundRect(rect, radius, radius, fill);
                    canvas.restoreToCount(save);
                }
                if (isFocused()) {
                    float half = stroke / 2;
                    rect.set(half, half, w - half, h - half);
                    canvas.drawRoundRect(rect, radius, radius, ring);
                }
            }
            super.onDraw(canvas);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            setFraction(fraction); // refresh the accent when focus changes
        }
    }
}
