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


public class TideDataRetriever extends AsyncTask<Object, Void, TideData> {
    private static final String TAG = "TideDataRetr";

    private final TideDataProvider tideDataProvider;
    private final String portID;


    public TideDataRetriever(TideDataProvider tideDataProvider, String portID) {
        this.tideDataProvider = tideDataProvider;
        this.portID = portID;
    }


    protected TideData doInBackground(Object... addr) {
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

            String[] split = result.toString("ASCII").split("\n--\n");

            long currentTimeMillis = System.currentTimeMillis();
            return new TideData(split[0].trim(), split[1].trim(), currentTimeMillis, currentTimeMillis);
        } catch (Exception e) {
            Log.i(TAG, "doInBackground() | fetch failed");
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

        if (tideData == null) return;

        tideDataProvider.newDataFetched(portID, tideData);
    }
}