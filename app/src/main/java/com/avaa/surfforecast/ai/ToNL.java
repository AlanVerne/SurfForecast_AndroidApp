package com.avaa.surfforecast.ai;

import com.avaa.surfforecast.data.SurfConditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alan on 19 Jan 2017.
 */

public class ToNL {
    private static final Map<Integer, String> WIND_DA_TO_STRING = new HashMap<Integer, String>() {{
        put(0,      "on shore");
        put(90-45,  "cross-on shore");
        put(90,     "cross shore");
        put(90+45,  "cross-off shore");
        put(180,    "off shore");
        put(270-45, "cross-off shore");
        put(270,    "cross shore");
        put(270+45, "cross-on shore");
        put(360,    "on shore");
    }};
    private static final Map<Integer, String> WIND_DA_TO_NL = new HashMap<Integer, String>() {{
        put(0,      "on shore");
        put(90,     "cross shore");
        put(180,    "off shore");
        put(270,    "cross shore");
        put(360,    "on shore");
    }};


    public static String windToNL(int speed, String angleNL) {
        int speedBS = SurfConditions.windSpeedToBeaufort(speed);

        String speedNL;
        if (speedBS == 0) speedNL = "no wind";
        else if (speedBS == 1) speedNL = "light " + angleNL + " air";
        else if (speedBS == 2) speedNL = "light " + angleNL + " breeze";
        else if (speedBS == 3) speedNL = "gentle " + angleNL + " breeze";
        else if (speedBS == 4) speedNL = "moderate " + angleNL + " breeze";
        else speedNL = "fresh " + angleNL + " breeze";

        return speedNL;
    }


    public static String windRelativeToString(float a) {
        return WIND_DA_TO_STRING.get((int)(Math.round(a*4/Math.PI)*45));
    }
    public static String windRelativeToNL(float a) {
        return WIND_DA_TO_NL.get((int)(Math.round(a*2/Math.PI)*90));
    }


    public static String floatToNL(float f) {
        int i = Math.round(f);
        if (i == f || (f-0.15f < i && i < f+0.15f)) return String.valueOf(i);

        i = Math.round(f-0.5f);
        if (f-0.15f-0.5f < i && i < f+0.15f-0.5f) return i + " and a half";

        return String.valueOf(Math.round(f*10)/10f);
    }
}
