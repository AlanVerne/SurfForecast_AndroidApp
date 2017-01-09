package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public static final Long ONE_DAY = 1000*60*60*24*1L;

    protected final SortedMap<String, TideData> portIDToTideData = new TreeMap<>();
    private final Map<String, TideDataRetriever> asyncTasks = new HashMap<>();
    private final SharedPreferences sharedPreferences;


    public interface TideDataProviderListener {
        void updated(String portID);
        void loadingStateChanged(String portID, boolean loading);
    }
    private final List<TideDataProviderListener> listeners = new ArrayList<>();
    public void addListener(TideDataProviderListener l) { listeners.add(l); }


    public static TideDataProvider instance = null;
    public static TideDataProvider getInstance() {
        return instance;
    }
    public static TideDataProvider getInstance(SharedPreferences sharedPreferences) {
        if (instance == null) {
            instance = new TideDataProvider(sharedPreferences);
        }
        return instance;
    }


    public void fetchIfNeed(@NonNull String portID) {
        TideData tideData = portIDToTideData.get(portID);
        if (tideData == null || tideData.needUpdate()) fetch(portID, null);
    }
    public void fetch(@NonNull String portID) {
        fetch(portID, null);
    }
    public void fetch(@NonNull String portID, @Nullable final Runnable widgetsRunnable) {
        Log.i(TAG, "Fetch " + portID);

        TideData tideData = portIDToTideData.get(portID);
        if (tideData != null) {
            //Log.i(TAG, "Current tidedata " + tideData.toString());
            tideData.fetched = System.currentTimeMillis();
        }
        else portIDToTideData.put(portID, new TideData(System.currentTimeMillis()));

        TideDataRetriever asyncTask = asyncTasks.get(portID);
        if (asyncTask == null || asyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            fireLoadingStateChanged(portID, true);
            asyncTask = new TideDataRetriever(getInstance(), portID, widgetsRunnable);
            asyncTasks.put(portID, asyncTask);
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public boolean loadingInProgress(String portID) {
        TideDataRetriever asyncTask = asyncTasks.get(portID);
        return asyncTask != null && !asyncTask.getStatus().equals(AsyncTask.Status.FINISHED);
    }


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


    private TideDataProvider(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
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
        if (tideData == null || tideData.needAndCanUpdate()) fetch(portID, null);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }


    private void load() {
        Set<String> portIDs = sharedPreferences.getStringSet(SPKEY_SAVED_PORT_IDS, null);
        if (portIDs == null) return;

        for (String portID : portIDs) {
            portIDToTideData.put(portID, new TideData(sharedPreferences.getString(SPKEY_TIDE_DATA_PRECISE + portID, null), sharedPreferences.getString(SPKEY_TIDE_DATA_EXTREMUMS + portID, null)));
        }

        //Log.i(TAG, portIDToTideData.toString());
    }


    protected void save() {
        SharedPreferences.Editor edit = sharedPreferences.edit();

        Map<String, TideData> toSave = new HashMap<>();
        for (Map.Entry<String, TideData> entry : portIDToTideData.entrySet()) {
            if (!entry.getValue().isEmpty()) toSave.put(entry.getKey(), entry.getValue());
        }

        edit.putStringSet(SPKEY_SAVED_PORT_IDS, toSave.keySet());

        for (Map.Entry<String, TideData> entry : toSave.entrySet()) {
            edit.putString(SPKEY_TIDE_DATA_PRECISE  + entry.getKey(),  entry.getValue().preciseToString());
            edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + entry.getKey(), entry.getValue().extremumsToString());
        }

        edit.apply();
    }
}
