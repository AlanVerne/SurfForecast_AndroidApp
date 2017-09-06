package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.avaa.surfforecast.MainModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alan on 20 May 2016.
 */

public class TideDataProvider {
    private static final String TAG = "TideDataProv";
    private static final String SPKEY_SAVED_PORT_IDS = "SavedPortIDs";
    private static final String SPKEY_TIDE_DATA_PRECISE = "TideDataPrecise";
    private static final String SPKEY_TIDE_DATA_EXTREMUMS = "TideDataExtremums";

    public static final Long ONE_DAY = 1000 * 60 * 60 * 24 * 1L;

    protected final SortedMap<String, TideData> portIDToTideData = new TreeMap<>();

    public interface TideDataProviderListener {
        void updated(String portID);

        void loadingStateChanged(String portID, boolean loading);
    }

    private final List<TideDataProviderListener> listeners = new ArrayList<>();

    public void addListener(TideDataProviderListener l) {
        listeners.add(l);
    }


    public void newDataFetched(String portID, TideData tideData) {
        if (tideData.equals(portIDToTideData.get(portID))) return;

        portIDToTideData.put(portID, tideData);
        save(portID);

        fireUpdated(portID);
    }


    public void fetchIfNeed(@NonNull String portID) {
        TideData tideData = portIDToTideData.get(portID);
        if (tideData == null || tideData.needUpdate()) fetch(portID);
    }

    public void fetch(@NonNull String portID) {
        Log.i(TAG, "fetch() | " + portID);

        TideData tideData = portIDToTideData.get(portID);
        if (tideData != null) {
            //Log.i(TAG, "Current tidedata " + tideData.toString());
            tideData.fetched = System.currentTimeMillis();
        } else portIDToTideData.put(portID, new TideData(System.currentTimeMillis()));

        if (DataRetrieversPool.getTask(portID, TideDataRetriever.class) == null) {
            fireLoadingStateChanged(portID, true);
            DataRetrieversPool.addTask(portID, new TideDataRetriever(this, portID));
        }
    }


//    public boolean loadingInProgress(String portID) {
//        return DataRetrieversPool.getTask(portID, TideDataRetriever.class) != null;
//    }


    // --


    protected void fireLoadingStateChanged(String portID, boolean loading) {
        for (TideDataProviderListener listener : listeners) {
            listener.loadingStateChanged(portID, loading);
        }
    }

    protected void fireUpdated(String portID) {
        for (TideDataProviderListener listener : listeners) {
            listener.updated(portID);
        }
    }


    public void init() {
        load();
    }


    //    public TideData getTideData(String portID, Runnable afterFetch) {
//        TideData tideData = portIDToTideData.get(portID);
//        if (tideData == null || tideData.needAndCanUpdate()) fetch(portID, afterFetch);
//        return tideData;
//    }
    public TideData getTideData(String portID) {
        TideData tideData = portIDToTideData.get(portID);
        //Log.i(TAG, "getTideData() " + tideData==null?"null":tideData.toString());
        if (tideData == null || tideData.needAndCanUpdate()) fetch(portID);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }


    // --


    private void load() {
        SharedPreferences sp = MainModel.instance.sharedPreferences;

        Set<String> portIDs = sp.getStringSet(SPKEY_SAVED_PORT_IDS, null);
        if (portIDs == null) return;

        for (String portID : portIDs) {
            String precise = sp.getString(SPKEY_TIDE_DATA_PRECISE + portID, null);
            String extremums = sp.getString(SPKEY_TIDE_DATA_EXTREMUMS + portID, null);
            if (precise != null && extremums != null) {
                TideData tideData = new TideData(precise, extremums);
                if (tideData.hasDays() > 0) portIDToTideData.put(portID, tideData);
            }
        }
    }


    private void save(String id) {
        SharedPreferences sp = MainModel.instance.sharedPreferences;

        SharedPreferences.Editor edit = sp.edit();

        edit.putStringSet(SPKEY_SAVED_PORT_IDS, portIDToTideData.keySet());

        TideData tideData = portIDToTideData.get(id);
        edit.putString(SPKEY_TIDE_DATA_PRECISE + id, tideData.preciseStr);
        edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + id, tideData.extremumsStr);

        edit.apply();
    }
}
