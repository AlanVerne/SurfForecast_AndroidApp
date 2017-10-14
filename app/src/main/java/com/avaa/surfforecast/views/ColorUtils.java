package com.avaa.surfforecast.views;


/**
 * Created by Alan on 3 Oct 2017.
 */


public class ColorUtils {
    public static int alpha(float a, int color) {
        return (int) (a * 0xff) << 24 | 0x00ffffff & color;
    }

    public static int alpha(int a, int color) {
        return a << 24 | 0x00ffffff & color;
    }
}
