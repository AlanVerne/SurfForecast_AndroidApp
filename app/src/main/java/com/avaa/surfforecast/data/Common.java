package com.avaa.surfforecast.data;

import android.graphics.PointF;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Alan on 21 Sep 2016.
 */

public class Common {
    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT+8");
    public static final String BENOA_PORT_ID = "5382";

    public static final String strNOW   = "now";
    public static final String strFT    = "ft";
    public static final String strS     = "s";
    public static final String strM     = "m";
    public static final String strKMH   = "km/h";
    public static final String strWIND      = "wind";
    public static final String strSWELL     = "swell";
    public static final String strTIDE      = "tide";
    public static final String strWIND_U    = "WIND";
    public static final String strSWELL_U   = "SWELL";
    public static final String strTIDE_U    = "TIDE";
    public static final String STR_NO_WIND_DATA     = "No wind data";
    public static final String STR_NO_SWELL_DATA    = "No swell data";
    public static final String STR_NO_TIDE_DATA     = "No tide data";


    public static int strToInt(String s, int def) {
        try {
            while (s.startsWith("0") && s.length() > 1) s = s.substring(1);
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }


    public static long getToday(TimeZone timeZone) {
        return getDay(0, timeZone);
    }
    public static long getDay(int plusDays, TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, plusDays);
        return calendar.getTime().getTime() / 1000;
    }
    public static Calendar getCalendarToday(TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        //calendar.add(Calendar.DATE, plusDays);
        return calendar;
    }


    public static long getUnixTimeFromCalendar(Calendar calendar) {
        return calendar.getTime().getTime() / 1000;
    }
//    public static long getUnixTime(TimeZone timeZone) {
//        return System.currentTimeMillis()/1000; //new GregorianCalendar(Common.TIME_ZONE).getTime().getTime() / 1000;
//    }


    public static int getNowTimeInt(TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        return calendar.get(Calendar.HOUR_OF_DAY)*60 + calendar.get(Calendar.MINUTE);
    }
}

