package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.avaa.surfforecast.MainModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;


/**
 * Created by Alan on 2 Jul 2016.
 */


public class SurfConditionsProvider {
    private static final String TAG = "SurfConditionsProvider";

    private static final String SPKEY_SF_SET = "SFSet";
    private static final String SPKEY_SF_LAST_UPDATE = "SFLastUpdate";

    public interface UpdateListener {
        void onUpdate(SurfConditionsProvider surfConditionsProvider);
    }

    private boolean loaded = false;

    public String url = "";
    public String urlfull = "";
    public String urlfullweek = "";

    public TreeMap<Long, SurfConditions> conditions = null;
    public HashMap<Integer, TreeMap<Long, SurfConditions>> allconditions = null;
    public long lastUpdate = 0;
    private List<UpdateListener> uls = new ArrayList<>();
    public BusyStateListener bsl = null;

    private SurfConditionsRetriever t0 = null;
    private SurfConditionsRetriever t1 = null;


    public SurfConditionsProvider(String url) {
        this.url = url;
        this.urlfull = "http://www.surf-forecast.com/breaks/" + url + "/forecasts/latest";
        this.urlfullweek = this.urlfull + "/six_day";
    }


    public boolean isNoData() {
        return conditions == null || get(0) == null && get(1) == null && get(2) == null && get(3) == null && get(4) == null && get(5) == null && get(6) == null;
    }


    public void setBsl(BusyStateListener bsl) {
        this.bsl = bsl;
    }


    public SurfConditions getNow() {
        SurfConditionsOneDay sc = get(0);
        return sc == null ? null : sc.getNow();
    }

    public SortedMap<Integer, SurfConditions> getFixed(int plusDays) {
        SurfConditionsOneDay sc = get(plusDays);
        return sc == null ? null : sc.getFixed();
    }


    public boolean isDetailed(int plusDays) {
        SurfConditionsOneDay conditions = get(plusDays);
        return conditions != null && conditions.isDetailed();
    }


    public SurfConditionsOneDay get() {
        return get(0);
    }

    public SurfConditionsOneDay get(int plusDays) {
        if (!loaded) load();

        if (conditions == null) {
            update();
            return null;
        }

        Calendar calendarNow = GregorianCalendar.getInstance(TIME_ZONE);

        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        calendar.set(calendarNow.get(Calendar.YEAR), calendarNow.get(Calendar.MONTH), calendarNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, plusDays);
        long start = calendar.getTime().getTime();
        calendar.add(Calendar.DATE, 1);
        long end = calendar.getTime().getTime();

        SortedMap<Long, SurfConditions> bef = conditions.headMap(start);
        SortedMap<Long, SurfConditions> aft = conditions.tailMap(end);
        SortedMap<Long, SurfConditions> conditionsTodayRaw = conditions.subMap(bef.isEmpty() ? 0L : bef.lastKey(), aft.isEmpty() ? Long.MAX_VALUE : aft.firstKey() + 1);
        //Log.i("SFProvider", bef.size()+" "+aft.size()+" "+conditionsTodayRaw.size()+" "+conditionsTodayRaw.toString()+" "+start+" "+end);
        SurfConditionsOneDay conditionsToday = new SurfConditionsOneDay();
        for (Map.Entry<Long, SurfConditions> tide : conditionsTodayRaw.entrySet()) {
            int h = (int) ((tide.getKey() - start) / 1000 / 60);
            if (h > -5 * 60) conditionsToday.put(h, tide.getValue());
        }

        //Log.i(TAG, "get(int plusDays) " + plusDays +" "+ conditionsToday.size());

        if (conditionsToday.isEmpty()) {
            update();
            return null;
        }

        return conditionsToday;
    }


    public int hoursFromLastUpdate() {
        return (int) ((new Date().getTime() - lastUpdate) / 1000 / 60 / 60);
    }

    public boolean needUpdate() {
        return hoursFromLastUpdate() > 3;
    }

    public boolean updateIfNeed() {
        if (needUpdate()) {
            update();
            return true;
        }
        return false;
    }


    public void update() {
        if (t0 != null && t0.getStatus() != AsyncTask.Status.FINISHED || t1 != null && t1.getStatus() != AsyncTask.Status.FINISHED) {
            //Log.i(TAG, "UPDATE IS ALREADY RUNNING" + url + " last update was " + hoursFromLastUpdate() + "h before");
        } else {
            //Log.i(TAG, "UPDATE " + url + " last update was " + hoursFromLastUpdate() + "h before");
            t0 = new SurfConditionsRetriever(this, false);
            t1 = new SurfConditionsRetriever(this, true);
            t0.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            t1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public void setConditions(TreeMap<Long, SurfConditions> newConditions) {
        if (newConditions != null) {
            if (conditions == null) conditions = newConditions;
            else conditions.putAll(newConditions);

            Calendar singaporeCalendar = new GregorianCalendar(TIME_ZONE);
            singaporeCalendar.add(Calendar.DATE, -3);
            long startFrom = singaporeCalendar.getTime().getTime();

            conditions = new TreeMap<>(conditions.subMap(startFrom, conditions.lastKey() + 1));
            lastUpdate = new Date().getTime();

            save();
            fireUpdated();
        }
    }


    public void addUpdateListener(UpdateListener ul) {
        uls.add(ul);
    }

    public String getURL() {
        return urlfull;
    }


    private boolean load() {
        SharedPreferences sp = MainModel.instance.sharedPreferences;

        lastUpdate = sp.getLong(SPKEY_SF_LAST_UPDATE + url, 0);

        Set<String> tideTimes = sp.getStringSet(SPKEY_SF_SET + url, null);

        if (tideTimes == null) return false;

        conditions = new TreeMap<>();
        for (String s : tideTimes) {
            String[] split = s.split("\n");
            conditions.put(Long.valueOf(split[0]), SurfConditions.fromString(split[1]));
        }

        loaded = true;

        return needUpdate();
    }

    public void save() {
        SharedPreferences sp = MainModel.instance.sharedPreferences;

        Set<String> tidesStringSet = new HashSet<>();
        for (Map.Entry<Long, SurfConditions> i : conditions.entrySet()) {
            tidesStringSet.add(i.getKey().toString() + "\n" + i.getValue().toString());
        }

        SharedPreferences.Editor edit = sp.edit();
        edit.putStringSet(SPKEY_SF_SET + url, tidesStringSet);
        edit.putLong(SPKEY_SF_LAST_UPDATE + url, lastUpdate);
        edit.apply();
    }


    public void fireUpdated() {
        for (UpdateListener ul : uls) ul.onUpdate(this);
    }
}
