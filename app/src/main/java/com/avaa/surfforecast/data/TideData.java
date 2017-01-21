package com.avaa.surfforecast.data;

import android.graphics.Path;
import android.util.Log;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;


/**
 * Created by Alan on 17 Oct 2016.
 */


public class TideData {
    private static final String TAG = "TideData";

    //public TimeZone timeZone = null;

    public long  fetched;
    public long  fetchedSuccessfully;

    public final String preciseStr;
    public final String extremumsStr;

    private final SortedMap<Long, int[]> precise = new TreeMap<>();
    private final SortedMap<Long, Integer> extremums = new TreeMap<>();

    private int hasDaysStartingFrom = -1;
    private int hasDaysN = 0;


    public boolean equals(TideData o) {
        return o!=null && precise.equals(o.precise) && extremums.equals(o.extremums);
    }


    public TideData(Long fetched) {
        this.fetched = fetched;
        this.fetchedSuccessfully = 0L;

        this.preciseStr = null;
        this.extremumsStr = null;
    }
    public TideData(String precise, String extremums) {
        this(precise, extremums, 0l, 0l);
    }
    public TideData(String preciseStr, String extremumsStr, Long fetched, Long fetchedSuccessfully) {
        this.fetched = fetched;
        this.fetchedSuccessfully = fetchedSuccessfully;

        this.preciseStr = preciseStr;
        this.extremumsStr = extremumsStr;

        if (preciseStr != null) {
            String[] daily = preciseStr.split("\n");

            if (daily.length % 2 == 1) return;

            for (int i = 0; i < daily.length; i += 2) {
                String[] strValues = daily[i + 1].split(" ");
                if (strValues.length < 24 * 6 + 1) continue;
                int[] values = new int[strValues.length];
                for (int j = 0; j < strValues.length; j++) {
                    values[j] = Integer.valueOf(strValues[j]);
                }
                this.precise.put(Long.valueOf(daily[i]), values);
            }
        }
        if (extremumsStr != null) {
            String[] split = extremumsStr.split("\n");

            if (split.length % 2 == 1) return;

            for (int i = 0; i < split.length; i += 2) {
                this.extremums.put(Long.valueOf(split[i]), Integer.valueOf(split[i+1]));
            }
        }
    }


    public boolean isEmpty() {
        return precise.isEmpty() || extremums.isEmpty();
    }


    public int hasDays() { // 1 - only today, 7 - 7 days
        Calendar calendar = Common.getCalendarToday(Common.TIME_ZONE);
        int day = calendar.get(Calendar.DAY_OF_YEAR);

        if (hasDaysStartingFrom == day) {
            //Log.i(TAG, "hasDays() | cached: startfrom = " + hasDaysStartingFrom + ", days = " + hasDaysN);
            return hasDaysN;
        }

        hasDaysStartingFrom = day;
        hasDaysN = 0;
        while (hasData(Common.getUnixTimeFromCalendar(calendar))) {
            calendar.add(Calendar.DATE, 1);
            hasDaysN++;
        }

        //Log.i(TAG, "hasDays() | calculated: startfrom = " + hasDaysStartingFrom + ", days = " + hasDaysN);

        return hasDaysN;
    }


    public boolean needUpdate() {
        return hasDays() < 7;
    }
    public boolean needAndCanUpdate() {
        long currentTimeMillis = System.currentTimeMillis();
        return (fetched == 0 || fetched + 60*1000 < currentTimeMillis) &&
               (fetchedSuccessfully == 0 || fetchedSuccessfully + 60*60*1000 < currentTimeMillis) &&
               needUpdate();
    }


    public Path getPath(long day, float w, float h, int min, int max) {
        return getPath(day, w, h, min, max, 0, 24);
    }
    public Path getPath(long day, float w, float h, int min, int max, int hStart, int hEnd) {
        int[] values = precise.get(day);
        if (values == null) return null;

        Path p = new Path();

        p.moveTo(0, h*2);

        hStart *= 6; hEnd *= 6;
        int hWidth = hEnd - hStart;
        int minMaxH = max - min;
        for (int i = hStart; i <= hEnd; i++) {
            p.lineTo(w * (i-hStart) / hWidth, h * (1f - ((float)values[i]/10.0f - min) / minMaxH));
        }

        p.lineTo(w, h*2);
        p.close();

        return p;
    }
    public Path getPath2(long day, float w, float h, int min, int max, int hStart, int hEnd) {
        int[] values  = precise.get(day);
        Calendar c = new GregorianCalendar(Common.TIME_ZONE);
        c.setTime(new Date(day*1000));
        c.add(Calendar.DATE, 1);
        int[] values2 = precise.get(c.getTime().getTime()/1000);
        if (values == null || values2 == null) return null;

        Path p = new Path();

        p.moveTo(0, h*2);

        hStart *= 6; hEnd *= 6;
        int hWidth = hEnd - hStart;
        int minMaxH = max - min;
        for (int i = hStart; i <= hEnd; i++) {
            if (i > 24*6) p.lineTo(w * (i-hStart) / hWidth, h * (1f - ((float)values2[i-24*6]/10.0f - min) / minMaxH));
            else if (i >= 0) p.lineTo(w * (i-hStart) / hWidth, h * (1f - ((float)values[i]/10.0f - min) / minMaxH));
        }

        p.lineTo(w, h*2);
        p.close();

        return p;
    }


    public SortedMap<Integer, Integer> getHourly(long day) {
        return getHourly(day, 0, 24);
    }
    public SortedMap<Integer, Integer> getHourly(long day, int start, int end) {
        int[] values = precise.get(day);
        if (values == null) return null;

        SortedMap<Integer, Integer> r = new TreeMap<>();
        for (int h = start; h <= end; h++) {
            r.put(h, Math.round((float)values[h*6] / 10.0f));
        }
        return r;
    }


    public static int tideToHML(int tide) {
        int t = 2;
        if (tide < 0.7) t = 1;
        if (tide > 1.4) t = 4;
        return t;
    }


    public Integer getTide(int plusDays, int time) {
        return getTide(Common.getDay(plusDays, Common.TIME_ZONE) + time*60);
    }
    public Integer getTide(long time) {
        float now = time / 60;

        int[] values = null;
        for (Map.Entry<Long, int[]> entry : precise.entrySet()) {
            if (time >= entry.getKey()) {
                values = entry.getValue();
                now = (time - entry.getKey()) / 60f;
            }
        }

        if (values == null) return null;
        if (now > 24*60) return null;

        now /= 10;

        int i1 = (int)Math.floor(now);
        int i2 = (int)Math.ceil(now);
        //Log.i(TAG, i1 + " " + i2 + " | " + values.length);
//        if (true) return null;
        if (i2 >= values.length) return values[i1];
        int h1 = values[i1];
        int h2 = values[i2];

        int h = Math.round((h1 + (h2-h1) * (now-i1)) / 10.0f);

        return h;
    }


    public Integer getNow() {
        int[] values = precise.get(Common.getToday(Common.TIME_ZONE));
        if (values == null) return null;

        Calendar calendar = new GregorianCalendar(Common.TIME_ZONE);
        float now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + calendar.get(Calendar.SECOND) / 60.0f;
        now /= 10;

        int i1 = (int)Math.floor(now);
        int i2 = (int)Math.ceil(now);
        int h1 = values[i1];
        int h2 = values[i2];

        int h = Math.round((h1 + (h2-h1) * (now-i1)) / 10.0f);

        return h;
    }


    public String getStateNow() {
        return getState(Common.getToday(TIME_ZONE), System.currentTimeMillis() / 1000);
    }
    public String getState(long day, int t) {
        ClosestExtremums ce = getClosestExtremums(day, day + t*60);
        return ce.getNowDirString();
    }
    public String getState(long day, long l) {
        ClosestExtremums ce = getClosestExtremums(day, l);
        return ce.getNowDirString();
    }


    public static String intToString(int h) {
        return String.valueOf(Math.round(h/10f)/10f);
    }


    private static class ClosestExtremums {
        int now;

        int t1, h1, t2, h2;

        boolean rising;
        boolean nowinside;
        int nowdir;

        private static final Map<Integer, String> dirToString = new HashMap<Integer, String>(){{
            put(0, "mid to low");
            put(1, "low");
            put(2, "low to mid");
            put(3, "raising mid"); //growing");
            put(4, "mid to high");
            put(5, "high");
            put(6, "high to mid");
            put(7, "lowering mid"); //descending
        }};

        public void set(int t1, int h1, int t2, int h2) {
            this.t1 = t1;
            this.h1 = h1;
            this.t2 = t2;
            this.h2 = h2;

            rising = h2 > h1;
            nowinside = t1 < now;

            if (true           && now < t1 - 60) nowdir = 0;
            if (t1 - 60 <= now && now < t1)        nowdir = 1;
            if (t1 + 00 <= now && now < (t1+t2)/2 - 60) nowdir = 2;
            if ((t1+t2)/2 - 60 <= now && true)      nowdir = 3;

            if (!rising) nowdir += 4;
        }

        public String getNowDirString() {
            return dirToString.get(nowdir);
        }
    }


    public ClosestExtremums getClosestExtremums(long day, long time) {
        SortedMap<Long, Integer> subMap = extremums.subMap(time - 60 * 60 * 3L, time + 60 * 60 * 24L);
        Iterator<Map.Entry<Long, Integer>> iterator = subMap.entrySet().iterator();
        Map.Entry<Long, Integer> e1 = iterator.next();
        Map.Entry<Long, Integer> e2 = iterator.next();

        ClosestExtremums ce = new ClosestExtremums();
        ce.now = (int)((time - day) / 60); //Common.getNowTimeInt(TIME_ZONE);
        ce.set((int)((e1.getKey()-day) / 60), e1.getValue() / 10,
               (int)((e2.getKey()-day) / 60), e2.getValue() / 10);
        return ce;
    }


    public Map<Integer, Integer> getExtremums(long day) {
        Map<Integer, Integer> result = new TreeMap<>();
        for (Map.Entry<Long, Integer> entry : extremums.subMap(day, day + 60L * 60 * 24).entrySet()) {
            result.put((int)((entry.getKey()-day) / 60), entry.getValue() / 10);
        }
        return result;
    }


    public boolean hasData(long day) {
        return precise.containsKey(day);
    }


    public int[] getPrecise(long day) {
        return precise.get(day);
    }


    @Override
    public String toString() {
        return precise.firstKey() + "-" + precise.lastKey() + "\n" + preciseStr;
    }
}
