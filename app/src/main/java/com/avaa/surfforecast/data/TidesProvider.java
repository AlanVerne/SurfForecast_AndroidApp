package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;

/**
 * Created by Alan on 2 Jul 2016.
 */

@Deprecated
public class TidesProvider {
    public static TidesProvider getInstance() {
        return instance;
    }
    public static TidesProvider getInstance(SharedPreferences sp, BusyStateListener bsl) {
        if (instance == null) instance = new TidesProvider(sp, bsl);
        return instance;
    }

    public SortedMap<Integer, Integer> get() { return get(0); }
    public SortedMap<Integer, Integer> get(int plusDays) {
        if (tides == null) {
            tryUpdate();
            return null;
        }

        Log.i(TAG, tides.size() + " " + tides.toString());

        Calendar calendarNow = GregorianCalendar.getInstance(TIME_ZONE);

        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        calendar.set(calendarNow.get(Calendar.YEAR), calendarNow.get(Calendar.MONTH), calendarNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, plusDays);

        long today = calendar.getTime().getTime();
        long start = today;
        calendar.add(Calendar.DATE, 1);
        long end   = calendar.getTime().getTime();

        SortedMap<Long, Integer> bef = tides.headMap(start);
        SortedMap<Long, Integer> aft = tides.tailMap(end);
        SortedMap<Long, Integer> tidesTodayRaw = tides.subMap(bef.isEmpty() ? 0L : bef.lastKey(), aft.isEmpty() ? Long.MAX_VALUE : aft.firstKey() + 1);
        SortedMap<Integer, Integer> tidesToday = new TreeMap<>();
        for (Map.Entry<Long, Integer> tide : tidesTodayRaw.entrySet()) {
            tidesToday.put((int)(tide.getKey() - today)/1000/60, tide.getValue());
        }

        Log.i(TAG, plusDays + " " + tidesToday.size() + " " + tidesToday.toString());

        return tidesToday;
    }
    public SortedMap<Integer, Integer> get(int plusDays, boolean extend) {
        if (tides == null) {
            tryUpdate();
            return null;
        }

        Calendar calendarNow = GregorianCalendar.getInstance(TIME_ZONE);

        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        calendar.set(calendarNow.get(Calendar.YEAR), calendarNow.get(Calendar.MONTH), calendarNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, plusDays);

        long today = calendar.getTime().getTime();
        long start = today;
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.HOUR, 18);
        long end   = calendar.getTime().getTime();

        SortedMap<Long, Integer> bef = tides.headMap(start);
        SortedMap<Long, Integer> tidesTodayRaw = tides.subMap(bef.isEmpty() ? 0L : bef.lastKey(), end);
        SortedMap<Integer, Integer> tidesToday = new TreeMap<>();
        for (Map.Entry<Long, Integer> tide : tidesTodayRaw.entrySet()) {
            tidesToday.put((int)(tide.getKey() - today)/1000/60, tide.getValue());
        }

        return tidesToday;
    }


    int now = -1;
    int nowh = -1;
    public Map.Entry<Integer, Integer> getTide() {
        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (now != this.now) {
            SortedMap<Integer, Integer> t = get();
            if (t == null) return null;
            this.now = now;
            nowh = interpolateTides(t, null, now).getValue();
        }
        return new AbstractMap.SimpleEntry<Integer, Integer>(now, nowh);
    }
    public int getTide(long time) {
        if (tides == null) return -100;

        Map.Entry<Long, Integer> prevTide = null;
        for (Map.Entry<Long, Integer> tide : tides.entrySet()) {
            if (tide.getKey() >= time) {
                if (tide.getKey() == time) return tide.getValue();
                else return prevTide==null ? -100 : interpolate(time, prevTide.getKey(), prevTide.getValue(), tide.getKey(), tide.getValue());
            }
            prevTide = tide;
        }

        return -100;
    }

    public static Map.Entry<Integer, Integer> interpolateTides(Map<Integer, Integer> tides, SortedMap<Integer, Integer> hourlyTides) {
        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return interpolateTides(tides, hourlyTides, now);
    }
    public static Map.Entry<Integer, Integer> interpolateTides(Map<Integer, Integer> tides, SortedMap<Integer, Integer> hourlyTides, int time) {
        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int nowTide = -100;

        if (hourlyTides != null) {
            int currentHour = 5 * 60;

            Map.Entry<Integer, Integer> prevTide = null;

            for (Map.Entry<Integer, Integer> tide : tides.entrySet()) {
                //if (prevTide == null && tide.getKey() > 0) prevTide = new AbstractMap.SimpleEntry<Integer, Integer>(0, tide.getValue());
                if (prevTide == null) {
                    if (tide.getKey() > currentHour) currentHour = (int)Math.ceil(tide.getKey()/60.0) * 60;
                }
                if (prevTide != null) {
                    Point p1 = new Point(prevTide.getKey(), prevTide.getValue());
                    Point p4 = new Point(tide.getKey(), tide.getValue());
                    float x23 = (p1.x + p4.x) / 2;

                    for (float i = 0f; i < 1f; i += 1f / 128f) {
                        float ii = 1f - i;
                        double h = Math.pow(ii, 3) * p1.x + 3 * Math.pow(ii, 2) * i * x23 + 3 * Math.pow(i, 2) * ii * x23 + Math.pow(i, 3) * p4.x;
                        if (currentHour <= 19 * 60 && h >= currentHour) {
                            double t = Math.pow(ii, 3) * p1.y + 3 * Math.pow(ii, 2) * i * p1.y + 3 * Math.pow(i, 2) * ii * p4.y + Math.pow(i, 3) * p4.y;
                            hourlyTides.put(currentHour, (int)t);
                            currentHour += 60;
                            if (currentHour > 24 * 60) break;
                        }
                        if (nowTide == -100 && h >= now) nowTide = (int) (Math.pow(ii, 3) * p1.y + 3 * Math.pow(ii, 2) * i * p1.y + 3 * Math.pow(i, 2) * ii * p4.y + Math.pow(i, 3) * p4.y);
                    }
                }
                if (currentHour > 24 * 60) break;
                prevTide = tide;
            }
        }
        else {
            Map.Entry<Integer, Integer> prevTide = null;

            for (Map.Entry<Integer, Integer> tide : tides.entrySet()) {
                if (prevTide != null && tide.getKey() > now) {
                    Point p1 = new Point(prevTide.getKey(), prevTide.getValue());
                    Point p4 = new Point(tide.getKey(), tide.getValue());
                    float x23 = (p1.x + p4.x) / 2;

                    for (float i = 0f; i < 1f; i += 1f / 128f) {
                        float ii = 1f - i;
                        double h = Math.pow(ii, 3) * p1.x + 3 * Math.pow(ii, 2) * i * x23 + 3 * Math.pow(i, 2) * ii * x23 + Math.pow(i, 3) * p4.x;
                        if (nowTide == -100 && h >= now) {
                            double t = Math.pow(ii, 3) * p1.y + 3 * Math.pow(ii, 2) * i * p1.y + 3 * Math.pow(i, 2) * ii * p4.y + Math.pow(i, 3) * p4.y;
                            nowTide = (int)t;
                            break;
                        }
                    }
                }
                prevTide = tide;
            }
        }

        return new AbstractMap.SimpleEntry<Integer, Integer>(now, nowTide);
    }
    public static int interpolate(long x, long x1, int y1, long x4, int y4) {
        double x23 = (x1 + x4) / 2;

        for (float i = 0f; i <= 1f; i += 1f / 128f) {
            float ii = 1f - i;
            double ix = Math.pow(ii, 3) * x1 + 3 * Math.pow(ii, 2) * i * x23 + 3 * Math.pow(i, 2) * ii * x23 + Math.pow(i, 3) * x4;
            if (ix >= x) {
                double t = Math.pow(ii, 3) * y1 + 3 * Math.pow(ii, 2) * i * y1 + 3 * Math.pow(i, 2) * ii * y4 + Math.pow(i, 3) * y4;
                return (int)t;
            }
        }

        return -100;
    }


    RetrieveTidesTask task = null;
    public void tryUpdate() {
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.i(TAG, "UPDATING IS ALREADY RUNNING");
            return;
        }
        task = new RetrieveTidesTask(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public interface UpdateListener {
        void onUpdate();
    }
    public void addUpdateListener(UpdateListener ul) {
        uls.add(ul);
    }


    // --


    private static final String SPKEY_TIDES_SET = "TidesSet";
    private static final String SPKEY_TIDES_LAST_UPDATE = "TidesLastUpdate";
    private static final Long   SEVEN_DAYS = 1000*60*60*24*1L;
    private static final String TAG = "TidesProvider";

    private static TidesProvider instance = null;

    private final SharedPreferences sp;
    private final List<UpdateListener> uls = new ArrayList<>();

    private SortedMap<Long, Integer> tides = null;
    private long lastUpdate = 0;
    private BusyStateListener bsl = null;


    private TidesProvider(SharedPreferences sp, BusyStateListener bsl) {
        this.sp = sp;
        this.bsl = bsl;
        if (!load()) tryUpdate();
    }


    private boolean load() {
        lastUpdate = sp.getLong(SPKEY_TIDES_LAST_UPDATE, 0);

        Set<String> tideTimes = sp.getStringSet(SPKEY_TIDES_SET, null);

        if (tideTimes == null) return false;

        tides = new TreeMap<>();
        for (String s : tideTimes) {
            String[] split = s.split("\t");
            tides.put(Long.valueOf(split[0]), Integer.valueOf(split[1]));
        }

        return new Date().getTime() - lastUpdate < SEVEN_DAYS;
    }
    private void save() {
        Set<String> tidesStringSet = new HashSet<>();
        for (Map.Entry<Long, Integer> i : tides.entrySet()) {
            tidesStringSet.add(i.getKey() + "\t" + i.getValue());
        }

        SharedPreferences.Editor edit = sp.edit();
        edit.putStringSet(SPKEY_TIDES_SET, tidesStringSet);
        edit.putLong(SPKEY_TIDES_LAST_UPDATE, lastUpdate);
        edit.apply();
    }


    private void fireUpdated() {
        for (UpdateListener ul : uls) ul.onUpdate();
    }


    // --


    private static class RetrieveTidesTask extends AsyncTask<String, Void, SortedMap<Long, Integer>> {
        private static final SimpleDateFormat FORMAT_D_MMMMM = new SimpleDateFormat("d MMMMM", Locale.ENGLISH);
        private static final SimpleDateFormat FORMAT_H_MM_A  = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        static {
            FORMAT_D_MMMMM.setTimeZone(TIME_ZONE);
            FORMAT_H_MM_A.setTimeZone(TIME_ZONE);
        }
        private static final String URL = "http://www.tide-forecast.com/locations/Denpasar/tides/latest";
        private static final String dateMarker = "<td class=\"date\" rowspan=\"";
        private static final String timeTideMarker = "<td class=\"time tide\">";
        private static final String levelMetricMarker = "<td class=\"level metric\">";


        private final TidesProvider tidesProvider;


        public RetrieveTidesTask(TidesProvider tidesProvider) {
            this.tidesProvider = tidesProvider;
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "onPreExecute()");
            tidesProvider.bsl.busyStateChanged(true);
        }

        protected SortedMap<Long, Integer> doInBackground(String... addr) {
            SortedMap<Long, Integer> newTides = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(URL);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(8 * 1000);
                connection.connect();

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                newTides = readPage(reader);
            } catch (Exception e) {
                Log.i(TAG, "Tides update failed");
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        Log.i(TAG, "Tides update failed");
                        ioe.printStackTrace();
                    }
                }
            }

            return newTides;
        }


        protected void onPostExecute(SortedMap<Long, Integer> newTides) {
            if (newTides != null) {
                Log.i(TAG, "onPostExecute " + newTides.toString());
                
                if (tidesProvider.tides == null) tidesProvider.tides = newTides;
                else {
                    Calendar calendar = new GregorianCalendar(TIME_ZONE);
                    calendar.add(Calendar.DATE, -3);
                    long startFrom = calendar.getTime().getTime();

                    tidesProvider.tides = new TreeMap<>(tidesProvider.tides.subMap(startFrom, newTides.firstKey()));
                    tidesProvider.tides.putAll(newTides);
                }
                tidesProvider.lastUpdate = new Date().getTime();
                tidesProvider.save();
                tidesProvider.fireUpdated();
            }
            tidesProvider.bsl.busyStateChanged(false);
        }


        private String getTagContent(String tag) {
            tag = tag.substring(tag.indexOf(">")+1);
            tag = tag.substring(0, tag.indexOf("<"));
            return tag;
        }


        private SortedMap<Long, Integer> readPage(BufferedReader reader) throws IOException {
            Calendar calendarCurrent = new GregorianCalendar(TIME_ZONE);
            Calendar c = new GregorianCalendar(TIME_ZONE);

            SortedMap<Long, Integer> tides = new TreeMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(dateMarker)) {
                    line = getTagContent(line);
                    line = line.substring(line.indexOf(' ') + 1);

                    try {
                        c.setTime(FORMAT_D_MMMMM.parse(line));

                        calendarCurrent.set(2016, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                        calendarCurrent.set(Calendar.MILLISECOND, 0);

                        Log.i(TAG, line +" "+ c.get(Calendar.MONTH) +" "+ c.get(Calendar.DAY_OF_MONTH) +" "+ calendarCurrent.getTime().getTime());
                    }
                    catch (Exception ignored) { }
                }
                else if (line.startsWith(timeTideMarker)) {
                    line = getTagContent(line);

                    try {
                        c.setTime(FORMAT_H_MM_A.parse(line));

                        calendarCurrent.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY));
                        calendarCurrent.set(Calendar.MINUTE, c.get(Calendar.MINUTE));

                        Log.i(TAG, "format test " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + " - " + calendarCurrent.getTime().getTime() + "   " + line);
                    }
                    catch (Exception ignored) { }

                    while ((line = reader.readLine()) != null && !line.startsWith(levelMetricMarker)) { }

                    line = getTagContent(line);
                    line = line.substring(0, line.indexOf(' '));
                    int intLevel = (int)(Double.valueOf(line) * 100);

                    Log.i(TAG, calendarCurrent.get(Calendar.DATE) + " date " + intLevel);

                    long time = calendarCurrent.getTime().getTime();
                    if (!tides.containsKey(time)) tides.put(time, intLevel);
                }
            }

            return tides;
        }
    }
}
