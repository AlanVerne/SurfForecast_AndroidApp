package com.avaa.surfforecast.views.map;


import android.view.View;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class DirectionScroller extends FloatScroller {
    public DirectionScroller(View view) {
        super(view);
    }

    public DirectionScroller(View view, float value) {
        super(view, value);
    }


    @Override
    public boolean to(float to, boolean smooth) {
        if (scroller.isFinished() && value == to || !scroller.isFinished() && scroller.getFinalX() == (int) (to * 1000f)) {
            return false;
        } else {
            if (!scroller.isFinished()) scroller.abortAnimation();
            if (smooth) {
                if (to - value > Math.PI) value += Math.PI * 2;
                if (to - value < -Math.PI) value -= Math.PI * 2;
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
}
