package com.avaa.surfforecast.data;

import android.graphics.PointF;


/**
 * Created by Alan on 3 Oct 2017.
 */


public class SpotsArea {
    public final String name;
    public final PointF pointOnSVG;

    public SpotsArea(String name, PointF pointOnSVG) {
        this.name = name;
        this.pointOnSVG = pointOnSVG;
    }
}
