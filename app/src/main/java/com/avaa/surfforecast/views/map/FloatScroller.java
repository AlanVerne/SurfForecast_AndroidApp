package com.avaa.surfforecast.views.map;

import android.view.View;
import android.widget.Scroller;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class FloatScroller {
    protected final View view;
    protected final Scroller scroller;
    protected float value = 0f;


    public FloatScroller(View view) {
        this.view = view;
        scroller = new Scroller(view.getContext());
    }

    public FloatScroller(View view, float value) {
        this(view);
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
        if (scroller.isFinished() && value == to || !scroller.isFinished() && scroller.getFinalX() == (int) (to * 1000f)) {
            return false;
        } else {
            if (!scroller.isFinished()) scroller.abortAnimation();
            if (smooth) {
                int dx = (int) ((to - value) * 1000f);
                scroller.startScroll((int) (value * 1000f), 0, dx, 0, 1000);
                repaint();
                return false;
            } else {
                value = to;
                repaint();
                return true;
            }
        }
    }


    public float getDestination() {
        return !scroller.isFinished() ? scroller.getFinalX() / 1000.0f : value;
    }


    protected void repaint() {
        ((SurfSpotsMap) view).repaint();
    }
}
