package com.avaa.surfforecast.views.Map;

import android.content.Context;
import android.widget.Scroller;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class FloatScroller {
    protected final Scroller scroller;
    protected float value = 0f;


    public FloatScroller(Context context) {
        scroller = new Scroller(context);
    }

    public FloatScroller(Context context, float value) {
        scroller = new Scroller(context);
        this.value = value;
    }


    public float getValue() {
        return value;
    }


    public boolean compute() {
        if (scroller.computeScrollOffset()) {
            float newHV = scroller.getCurrX() / 1000f;
            if (value == newHV) return false;
            value = newHV;
            return true;
        }
        return false;
    }


    public boolean to(float to, boolean smooth) {
        if (scroller.isFinished() && value == to) {
            return false;
        } else {
            if (!scroller.isFinished()) scroller.abortAnimation();
            if (smooth) {
                int dx = (int) ((to - value) * 1000f);
                scroller.startScroll((int) (value * 1000f), 0, dx, 0, 1000);
                return false;
            } else {
                value = to;
                return true;
            }
        }
    }
}
