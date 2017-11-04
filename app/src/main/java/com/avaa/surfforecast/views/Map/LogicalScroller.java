package com.avaa.surfforecast.views.Map;


import android.content.Context;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class LogicalScroller extends FloatScroller {
    public LogicalScroller(Context context) {
        super(context);
    }

    public LogicalScroller(Context context, float value) {
        super(context, value);
    }


    @Override
    public boolean to(float to, boolean smooth) {
        if (scroller.isFinished() && value == to || scroller.getFinalX() == (int) (to * 1000f)) {
            return false;
        } else {
            if (!scroller.isFinished()) scroller.abortAnimation();
            if (smooth) {
                int dx = (int) ((to - value) * 1000f);
                scroller.startScroll((int) (value * 1000f), 0, dx, 0, to == 1 ? 500 : 1000);
                return false;
            } else {
                value = to;
                return true;
            }
        }
    }
}
