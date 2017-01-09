package com.avaa.surfforecast.data;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Alan on 9 Jan 2017.
 */

public class TideDataRetriever extends AsyncTask<String, Void, TideData> {
    private static final String TAG = "TideDataRetr";

    private final Runnable runnable;
    private final TideDataProvider tideDataProvider;
    private final String portID;


    public TideDataRetriever(TideDataProvider tideDataProvider, String portID, Runnable runAfter) {
        this.tideDataProvider = tideDataProvider;
        this.portID = portID;
        this.runnable = runAfter;
    }


    protected TideData doInBackground(String... addr) {
        URL url;
        BufferedReader reader = null;

        try {
            url = new URL("http://128.199.252.5/ports/" + portID + "/predictions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setReadTimeout(15 * 1000);
            connection.connect();

            InputStream is = connection.getInputStream();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            String[] split = result.toString("UTF-8").split("\n--\n");

//                Log.i("BTW", split.length+" ");
//                Log.i("BTW", split[0]);
//                Log.i("BTW", split[1]);

            return new TideData(split[0], split[1]);
        } catch (Exception e) {
            Log.i(TAG, "Fetch failed");
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return null;
    }

    protected void onPostExecute(TideData tideData) {
        Log.i(TAG, "onPostExecute() | for " + portID + " " + (tideData == null ? "null" : "needAndCanUpdate = " + tideData.needAndCanUpdate()));

        tideDataProvider.fireLoadingStateChanged(portID, false);

        if (tideData == null) return;
        if (tideData.equals(tideDataProvider.portIDToTideData.get(portID))) return;

        tideDataProvider.portIDToTideData.put(portID, tideData);
        tideDataProvider.save();

        tideDataProvider.fireUpdated(portID);

        if (runnable != null) runnable.run();
    }
}