package com.avaa.surfforecast.data;


import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Alan on 3 Sep 2017.
 */


public class DataRetrieversPool {
    private static final Map<String, AsyncTask> POOL = new HashMap<>();

    public static AsyncTask getTask(String id, Class c) {
        AsyncTask asyncTask = POOL.get(id + c.getName());
//        Log.i("DR POOL", "getTask(" + id + " " + c.getName() + ")" + (asyncTask == null ? "NO" : "YES"));
        if (asyncTask != null) {
            if (!asyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                return asyncTask;
            } else {
                POOL.remove(id);
            }
        }
        return null;
    }

    public static void addTask(String id, @NonNull AsyncTask t) {
        AsyncTask asyncTask = getTask(id, t.getClass());
        if (asyncTask != null) {
            asyncTask.cancel(true);
        }
        POOL.put(id + t.getClass().getName(), t);
//        Log.i("DR POOL", "addTask(" + id + " " + t.getClass().getName() + ")");
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
