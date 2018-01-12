package com.avaa.surfforecast.data;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Alan on 21 Sep 2016.
 */

public class Common {
    public static final String BENOA_PORT_ID = "5382";
    public static final String SANUR_PORT_ID = "5381";

    public static final String STR_NOW = "now";

    public static final String STR_KMH = "km/h";
    public static final String STR_FT = "ft";
    public static final String STR_S = "s";
    public static final String STR_KJ = "kJ";
    public static final String STR_M = "m";

    public static final String STR_WIND = "Wind";
    public static final String STR_SWELL = "Swell";
    public static final String STR_TIDE = "Tide";

    public static final String STR_WIND_U = "Wind";
    public static final String STR_SWELL_U = "Swell";
    public static final String STR_ENERGY_U = "Energy";
    public static final String STR_TIDE_U = "Tide";

    public static final String STR_NO_WIND_DATA = "No wind data";
    public static final String STR_NO_SWELL_DATA = "No swell data";
    public static final String STR_NO_TIDE_DATA = "No tide data";

    public static final String STR_DASH = "-";


    public static int strToInt(String s, int def) {
        try {
            while (s.startsWith("0") && s.length() > 1) s = s.substring(1);
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

