package com.avaa.surfforecast.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * Created by Alan on 3 Jan 2018.
 */


public class DT {
    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT+8");


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


    public static Calendar getCalendarTodayStart(TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        //calendar.add(Calendar.DATE, plusDays);
        return calendar;
    }


    public static long getUnixTimeFromCalendar(Calendar calendar) {
        return calendar.getTime().getTime() / 1000;
    }


    public static int getNowTimeMinutes(TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    //    public static long getUnixTime(TimeZone timeZone) {
//        return System.currentTimeMillis()/1000; //new GregorianCalendar(Common.TIME_ZONE).getTime().getTime() / 1000;
//    }
}
