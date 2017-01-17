package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by Alan on 17 Jan 2017.
 */

public class CircleAnimatedFrameLayout extends FrameLayout {
    public CircleAnimatedFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == VISIBLE) state = 1;
        if (visibility == INVISIBLE) state = 2;
        prevTime = SystemClock.uptimeMillis();
        repaint();
        //super.setVisibility(visibility);
    }

    Paint bgShadow = new Paint(){{setAntiAlias(false);setColor(0xffffffff);}};
    Paint bgMain   = new Paint(){{setAntiAlias(true); setColor(0xffffffff);}};

    long prevTime = -1;
    int state = 0;
    float i = 0;
    @Override
    public void draw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        long now = SystemClock.uptimeMillis();
        i = state == 1 ? i + (now - prevTime) / 200f : i - (now - prevTime) / 200f;
        prevTime = now;

        if (i <= 0) i = 0;
        else if (i >= 1) i = 1;
        else repaint();

        float i = state == 1 ? (float)Math.pow(this.i, 0.2) : (float)Math.pow(this.i, 2);

        float r = (height - getPaddingTop()) * i;

        Path p = new Path();
        p.addCircle(state == 2 ? width *(1 - i*2/3) : width *i/3, height, r, Path.Direction.CCW);

        bgShadow.setColor((int)(0xaa*i) * 0x1000000 | 0xaaaaaa);
        canvas.drawRect(0,0, width, height, bgShadow);

        if (this.i == 1) {
            canvas.drawPath(p, bgMain);
            super.draw(canvas);
        }
        else {
            canvas.save();
            canvas.clipPath(p);
            canvas.drawRect(0, height-r, width, height, bgMain);
            super.draw(canvas);
            canvas.restore();
        }
    }

    private void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        }
        else {
            postInvalidate();
        }
    }
}
