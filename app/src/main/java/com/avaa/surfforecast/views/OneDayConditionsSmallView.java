package com.avaa.surfforecast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avaa.surfforecast.data.SurfConditions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Alan on 14 Jul 2016.
 */

public class OneDayConditionsSmallView extends LinearLayout {
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEE");

    private static final int LONG_DELAY  = 500;
    private static final int SHORT_DELAY = 50;


    private final Paint p = new Paint();

    private TextView  tvDate;
    private TextView  tvDayOfWeek;
    private ImageView ivValue;

    private int colorText = 0x000000;

    private int d;
    private int s;
    private int bh;
    private Bitmap b;
    private Canvas c;

//    private SortedMap<Integer, SurfConditions> conditions;

    private int[] h = new int[3];
    private int[] dh = new int[3];

    private volatile Timer timerHAnimataion = null;


    public OneDayConditionsSmallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public int getColorText() {
        return colorText;
    }
    public void setColorText(int colorText) {
        this.colorText = colorText;
        p.setColor(0xbb000000 | colorText);
    }


    public void setTextSize(int size) {
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tvDayOfWeek.setTextSize(TypedValue.COMPLEX_UNIT_PX, size/1.33f);
    }


    public void setConditions(SortedMap<Integer, SurfConditions> conditions) {
        if (timerHAnimataion != null) {
            timerHAnimataion.cancel();
            timerHAnimataion.purge();
        }

        if (conditions != null) {
            for (int i = 0; i < 3; i++) {
                dh[i] = 0;

                SurfConditions ic = conditions.get((i == 0 ? 5 : i == 1 ? 11 : 17) * 60);
                if (ic == null && i == 0) ic = conditions.get(2 * 60);
                if (ic != null) {
                    int n = (int)((ic.getWaveHeightInFt() + 0.5) / 2);
                    n *= 2;
                    if ((ic.getWaveHeightInFt() + 0.5) % 2 > 1) n++;

                    dh[i] = n;
                }
            }
        }
        else {
            for (int i = 0; i < 3; i++) dh[i] = 0;
            if (!isVisible() && equalizeH()) repaintBitmap();
        }

        if (needToAnimateH()) {
            timerHAnimataion = new Timer();
            timerHAnimataion.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (animateH()) repaintBitmap();
                    else if (timerHAnimataion != null) {
                        timerHAnimataion.cancel();
                        timerHAnimataion.purge();
                        timerHAnimataion = null;
                    }
                }
            }, isVisible() ? 100 : LONG_DELAY, SHORT_DELAY);
        }
    }


    private boolean isVisible() {
        return ((RelativeLayout)(this.getParent().getParent())).getAlpha() >= 0.75;
    }


    private boolean needToAnimateH() {
        for (int i = 0; i < 3; i++) if (h[i] != dh[i]) return true;
        return false;
    }
    private boolean equalizeH() {
        boolean changed = false;

        for (int i = 0; i < 3; i++) {
            if (h[i] != dh[i]) {
                changed = true;
                h[i] = dh[i];
            }
        }

        return changed;
    }
    private boolean animateH() {
        boolean changed = false;

        for (int i = 0; i < 3; i++) {
            if (h[i] != dh[i]) {
                changed = true;
                if (h[i] > dh[i]) h[i]--;
                else h[i]++;
            }
        }

        return changed;
    }


    private void repaintBitmap() {
        b.eraseColor(android.graphics.Color.TRANSPARENT);

        for (int i = 0; i < 3; i++) {
            int n = h[i];

            int y = bh;

            for (int j = 0; j < n/2; j++) {
                c.drawRect(i * (d + s), y - d, i * (d + s) + d, y, p);
                y -= d + s;
            }
            if (n % 2 == 1) c.drawRect(i * (d + s), y - d / 2, i * (d + s) + d, y, p);
        }

        ivValue.post(() -> {
            ivValue.setImageBitmap(b);
            //invalidate();
        });
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        ivValue.setVisibility(ivValue.getHeight() < bh ? INVISIBLE : VISIBLE);
        //Log.i("ODCSV", " " + ivValue.getHeight() + " " + bh);
    }


    public void setDate(Calendar c) {
        tvDate.setText(String.valueOf(c.get(Calendar.DATE)));

        tvDayOfWeek.setText(DAY_FORMAT.format(c.getTime()).toUpperCase());

        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            tvDate.setTextColor(0xdd000000 | colorText);
            tvDayOfWeek.setTextColor(0xdd000000 | colorText);
            tvDayOfWeek.setTypeface(null, Typeface.BOLD);
        }
        else {
            tvDate.setTextColor(0xdd000000 | colorText);
            tvDayOfWeek.setTextColor(0x88000000 | colorText);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }


    private void init(Context context) {
        setOrientation(VERTICAL);

        float density = getResources().getDisplayMetrics().density;

        tvDate = new TextView(context);
        tvDate.setGravity(Gravity.CENTER);
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, density*14);
        tvDayOfWeek = new TextView(context);
        tvDayOfWeek.setGravity(Gravity.CENTER);
        tvDayOfWeek.setTextSize(TypedValue.COMPLEX_UNIT_PX, density*10);
        ivValue = new ImageView(context);

        View space = new View(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, 0);
        layoutParams.weight = 2;
        space.setLayoutParams(layoutParams);

        addView(tvDate);
        addView(tvDayOfWeek);
        addView(space);
        addView(ivValue);

        d  = (int)(3.5 * density);
        s  = (int)(1.5 * density);
        bh = ((d+s)*6+d);

        b = Bitmap.createBitmap(d*3+s*2, bh, Bitmap.Config.ARGB_8888);
        c = new Canvas(b);

//        setBackgroundColor(0x33000000);
    }
}
