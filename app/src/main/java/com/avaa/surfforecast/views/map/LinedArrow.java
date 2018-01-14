package com.avaa.surfforecast.views.map;

import android.graphics.Path;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class LinedArrow {
    private static final Path pathLinedArrow = new Path();

    public static Path get(float bx, float by, float angle, float size) {
        float cx = bx - (float) Math.cos(angle - Math.PI * 3 / 4) * size;
        float cy = by + (float) Math.sin(angle - Math.PI * 3 / 4) * size;
        float dx = bx - (float) Math.sin(angle - Math.PI * 3 / 4) * size;
        float dy = by - (float) Math.cos(angle - Math.PI * 3 / 4) * size;

        pathLinedArrow.reset();
        pathLinedArrow.moveTo(cx, cy);
        pathLinedArrow.lineTo(bx, by);
        pathLinedArrow.lineTo(dx, dy);

        return pathLinedArrow;
    }
}