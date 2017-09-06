package com.avaa.surfforecast.data;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Alan on 11 Oct 2016.
 */

public class METAR {
    public long timeFetched;
    public long time;
    public float windAngle; // -1 means VRB
    public int windSpeed;


    public boolean isOld() {
        return time + 40 * 60 * 1000 < System.currentTimeMillis();
    }

    public boolean isOld(long currentTimeMillis) {
        return time + 40 * 60 * 1000 < currentTimeMillis;
    }

    public boolean isVeryOld() {
        return time + 90 * 60 * 1000 < System.currentTimeMillis();
    }

    public boolean isVeryOld(long currentTimeMillis) {
        return time + 90 * 60 * 1000 < currentTimeMillis;
    }

    public boolean isMinutePassedFromLastFetch() { // not fetched recently
        return timeFetched + 1 * 60 * 1000 < System.currentTimeMillis();
    }

    public boolean isMinutePassedFromLastFetch(long currentTimeMillis) {
        return timeFetched + 1 * 60 * 1000 < currentTimeMillis;
    }


    @Override
    public String toString() {
        long currentTimeMillis = System.currentTimeMillis();
        return "wind = " + windAngle + "deg " + windSpeed + "km/h" + ", time = " + time + ", time fetched = " + timeFetched +
                ";   isMinutePassedFromLastFetch = " + isMinutePassedFromLastFetch(currentTimeMillis) + ", isOld = " + isOld(currentTimeMillis) + ", isVeryOld = " + isVeryOld(currentTimeMillis);
    }

    public String toSavableString() {
        return String.valueOf(timeFetched) + "\t" + String.valueOf(time) + "\t" +
                String.valueOf(windAngle) + "\t" + String.valueOf(windSpeed);
    }

    public static METAR fromSavableString(String s) {
        METAR metar = new METAR();

        String[] split = s.split("\t");

        metar.timeFetched = Long.valueOf(split[0]);
        metar.time = Long.valueOf(split[1]);
        metar.windAngle = Float.valueOf(split[2]);
        metar.windSpeed = Integer.valueOf(split[3]);

        return metar;
    }

    public static METAR fromMETARString(String s, long timeFetched) {
        METAR metar = new METAR();

        metar.timeFetched = timeFetched;

        String[] split = s.split(" ");
        for (String si : split) {
            if (si.endsWith("Z")) {
                int t = Common.strToInt(si.substring(0, 6), 0);
                if (t == 0) return null;
                Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                c.set(Calendar.DATE, t / 10000);
                c.set(Calendar.HOUR, t / 100 % 100);
                c.set(Calendar.MINUTE, t % 100);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                metar.time = c.getTime().getTime();
            } else if (si.endsWith("KT")) {
                metar.windAngle = Common.strToInt(si.substring(0, 3), -1);
                if (metar.windAngle != -1) {
                    metar.windAngle = (float) (((360 + 90 - metar.windAngle) % 360) * Math.PI / 180f);
                }
                metar.windSpeed = (int) (Common.strToInt(si.substring(3, 5), 0) * 1.852);
            }
        }

        return metar;
    }
}
