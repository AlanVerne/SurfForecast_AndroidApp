package com.avaa.surfforecast.data;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Alan on 9 Aug 2016.
 */

public class DateTimeHelper {
    public static Calendar getTodaysStartTime() {
        Calendar calendarNow = GregorianCalendar.getInstance(Common.TIME_ZONE);
        Calendar calendar = GregorianCalendar.getInstance(Common.TIME_ZONE);
        calendar.set(calendarNow.get(Calendar.YEAR), calendarNow.get(Calendar.MONTH), calendarNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
//    public static long getNow() {
//        return System.currentTimeMillis();
//    }
}
