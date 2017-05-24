package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Alan on 17 Jan 2017.
 */

public class CircleVoiceIndicator extends View {
    public CircleVoiceIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

//    public static final int COLOR_ACTIVE   = 0x66f50057;
    public static final int COLOR_ACTIVE   = 0x66f50057;
    public static final int COLOR_INACTIVE = 0x33000000;

    Paint pMain = new Paint() {{ setAntiAlias(true); setStyle(Style.FILL); setColor(COLOR_INACTIVE); }};

    //long prevTime = -1;

    float r = 0;
    float rv = 0;

    public float x, y;

    boolean awakened = false;


    public void newRms(float rms) {
        rv += rms * 1.5f;
        repaint();
    }


    public boolean isAwakened() {
        return awakened;
    }
    public void setAwakened(boolean b) {
        if (awakened == b) return;
        awakened = b;
        pMain.setColor(awakened ? COLOR_ACTIVE : COLOR_INACTIVE);
        repaint();
    }


    @Override
    public void draw(Canvas canvas) {
        r += rv;
        rv *= 0.65f;

        r += (awakened ? getResources().getDisplayMetrics().density*60 - r : getResources().getDisplayMetrics().density*20 - r) * 0.1f;

        if (!awakened) pMain.setAlpha((int)(pMain.getAlpha()*0.9));

        if (awakened || r > 25 || pMain.getAlpha() > 10) repaint();

        canvas.drawCircle(x, getHeight() - y, r, pMain);
    }


    private void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }
}
