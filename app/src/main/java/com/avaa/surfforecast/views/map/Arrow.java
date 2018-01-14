package com.avaa.surfforecast.views.map;


/**
 * Created by Alan on 21 Oct 2017.
 */


import android.graphics.Path;
import android.graphics.RectF;


public class Arrow {
    private final static float SQRT_2 = (float) Math.sqrt(2);
    private final static Path pathArrow = new Path();

    // TODO cache results
    public static Path createArrow(float x, float y, float a, float arrowSize) {
        pathArrow.reset();
        pathArrow.moveTo(x - (float) Math.cos(a) * arrowSize * SQRT_2, y + (float) Math.sin(a) * arrowSize * SQRT_2);
        pathArrow.arcTo(new RectF(x - arrowSize, y - arrowSize, x + arrowSize, y + arrowSize), -a * 180 / (float) Math.PI + 45 + 180, 360 - 2 * 45, false);
        pathArrow.close();
        return pathArrow;
    }
}
