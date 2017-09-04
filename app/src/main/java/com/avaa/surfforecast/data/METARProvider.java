package com.avaa.surfforecast.data;

import android.content.SharedPreferences;

import com.avaa.surfforecast.AppContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Alan on 2 Jul 2016.
 */


public class METARProvider {
    public METAR get(String id) {
//        Log.i(TAG, "get() " + toString(id, metar));

        if (id == null) return null;

        METAR metar = metars.get(id);

        if (metar == null) {
            update(id);
            return null;
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            if (metar.isMinutePassedFromLastFetch(currentTimeMillis) && metar.isOld(currentTimeMillis)) {
                update(id);
            }
            if (metar.isVeryOld(currentTimeMillis)) {
                metars.remove(id);
                return null;
            } else {
                return metar;
            }
        }
    }


    public void update(String id) {
//        Log.i(TAG, "update(" + id + ")");

        if (id == null) return;

        if (DataRetrieversPool.getTask(id, METARRetriever.class) == null) {
//            Log.i(TAG, "UPDATE IS ALREADY RUNNING");
            return;
        }
        DataRetrieversPool.addTask(id, new METARRetriever(this, id));
    }


    public interface UpdateListener {
        void onUpdate(String id, METAR metar);
    }

    public void addUpdateListener(UpdateListener ul) {
        uls.add(ul);
    }


    // --


    private static final String SPKEY_METARS = "METARs";
    private static final String TAG = "metarProvider";

    private final List<UpdateListener> uls = new ArrayList<>();

    public BusyStateListener bsl = null;

    private Map<String, METAR> metars = new HashMap<>();


    public METARProvider(BusyStateListener bsl) {
        this.bsl = bsl;
    }


    public void init() {
        load();
        update(SurfSpots.WADD);
    }


    public void newMetar(String id, METAR metar) {
        metars.put(id, metar);
        if (!metar.isVeryOld()) {
            fireUpdated(id, metar);
        }
    }


    public void fireUpdated(String id, METAR metar) {
        for (UpdateListener ul : uls) ul.onUpdate(id, metar);
    }


    public static String toString(String id, METAR metar) {
        return "Metar for " + id + ": " + (metar == null ? "null" : metar.toString());
    }


    // --


    private void load() {
        SharedPreferences sp = AppContext.instance.sharedPreferences;

        String s = sp.getString(SPKEY_METARS, null);
        if (s == null) return;
        String[] split = s.split("\n");
        if (split.length % 2 == 1) return;
        for (int i = 0; i < split.length; i += 2) {
            METAR metar = METAR.fromSavableString(split[i + 1]);
//            Log.i(TAG, "load() | loaded for " + split[i] + " | " + metar.toString());
            if (!metar.isVeryOld()) metars.put(split[i], metar);
        }
    }

    public void save() {
//        Log.i(TAG, "save()");
        StringBuilder sb = new StringBuilder(100);
        long currentTimeMillis = System.currentTimeMillis();
        for (Map.Entry<String, METAR> entry : metars.entrySet()) {
            if (!entry.getValue().isVeryOld(currentTimeMillis)) {
                sb.append('\n');
                sb.append(entry.getKey());
                sb.append('\n');
                sb.append(entry.getValue().toSavableString());
            }
        }

        String s = null;
        if (sb.length() > 0) s = sb.substring(1);

        SharedPreferences sp = AppContext.instance.sharedPreferences;
        sp.edit().putString(SPKEY_METARS, s).apply();
    }
}
