package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alan on 2 Jul 2016.
 */

public class METARProvider {
    public static METARProvider getInstance() {
        return instance;
    }
    public static METARProvider getInstance(SharedPreferences sp, BusyStateListener bsl) {
        if (instance == null) instance = new METARProvider(sp, bsl);
        return instance;
    }


    public METAR get(String name) {
        if (name == null) return null;
        METAR metar = metars.get(name);
        Log.i(TAG, "get() " + toString(name, metar));
        long currentTimeMillis = System.currentTimeMillis();
        if (metar == null || (metar.isMinutePassedFromLastFetch(currentTimeMillis) && metar.isOld(currentTimeMillis))) update(name);
        if (metar == null) return null;
        if (metar.isVeryOld(currentTimeMillis)) return null;
        return metar;
    }


    public void update(String name) {
        Log.i(TAG, "update() " + name);
        if (name == null) return;
        METARRetriever task = tasks.get(name);
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.i(TAG, "UPDATE IS ALREADY RUNNING");
            return;
        }
        task = new METARRetriever(this, name);
        tasks.put(name, task);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public interface UpdateListener {
        void onUpdate(String name, METAR metar);
    }
    public void addUpdateListener(UpdateListener ul) {
        uls.add(ul);
    }


    // --


    private static final String SPKEY_METARS = "METARs";
    private static final String TAG = "metarProvider";

    private static METARProvider instance = null;

    private final SharedPreferences sp;
    private final List<UpdateListener> uls = new ArrayList<>();

    public BusyStateListener bsl = null;

    private Map<String, METAR> metars = new HashMap<>();
    private Map<String, METARRetriever> tasks = new HashMap<>();


    private METARProvider(SharedPreferences sp, BusyStateListener bsl) {
        this.sp = sp;
        this.bsl = bsl;
        load();
        update(SurfSpots.WADD);
    }


    public void newMetar(String name, METAR metar) {
        metars.put(name, metar);
        if (!metar.isVeryOld()) {
            fireUpdated(name, metar);
        }
    }


    public void fireUpdated(String name, METAR metar) {
        for (UpdateListener ul : uls) ul.onUpdate(name, metar);
    }


    public static String toString(String name, METAR metar) {
        return "Metar for " + name + ": " + (metar == null ? "null" : metar.toString());
    }


    private void load() {
        String s = sp.getString(SPKEY_METARS, null);
        if (s == null) return;
        String[] split = s.split("\n");
        if (split.length % 2 == 1) return;
        for (int i = 0; i < split.length; i += 2) {
            METAR metar = METAR.fromSavableString(split[i + 1]);
            Log.i(TAG, "load() loaded for " + split[i] + " | " + metar.toString());
            if (!metar.isVeryOld()) metars.put(split[i], metar);
        }
    }
    public void save() {
        Log.i(TAG, "save()");
        String s = "";
        long currentTimeMillis = System.currentTimeMillis();
        for (Map.Entry<String, METAR> entry : metars.entrySet()) {
            if (!entry.getValue().isVeryOld(currentTimeMillis)) s += "\n" + entry.getKey() + "\n" + entry.getValue().toSavableString();
        }
        if (s.isEmpty()) s = null;
        else s = s.substring(1);
        sp.edit().putString(SPKEY_METARS, s).apply();
    }
}
