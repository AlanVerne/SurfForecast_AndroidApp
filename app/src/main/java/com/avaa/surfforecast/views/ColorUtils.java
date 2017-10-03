package com.avaa.surfforecast.views;

import com.avaa.surfforecast.drawers.MetricsAndPaints;

/**
 * Created by Alan on 3 Oct 2017.
 */

public class ColorUtils {
    public static int alpha(float a, int color) {
        return (int) (a * 0xff) << 24 | 0x00ffffff & color;
    }
}
