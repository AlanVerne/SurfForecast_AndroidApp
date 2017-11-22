package com.avaa.surfforecast.data;


import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by Alan on 30 Nov 2016.
 */


public class METARRetriever extends AsyncTask<Object, Void, METAR> {
    private static final String TAG = "METARRetriever";

    private final METARProvider metarProvider;
    private final String name;


    public METARRetriever(METARProvider metarProvider, String name) {
//        Log.i(TAG, "METARRetriever(" + name + ")");
        this.metarProvider = metarProvider;
        this.name = name;
    }


    @Override
    protected void onPreExecute() {
        Log.i(TAG, "onPreExecute()");
        metarProvider.bsl.busyStateChanged(true);
    }

    protected METAR doInBackground(Object... addr) {
        METAR metar = null;

        BufferedReader reader = null;

        try {
            FTPClient mFTPClient = new FTPClient();
            mFTPClient.connect("tgftp.nws.noaa.gov");
            mFTPClient.login("anonymous", "nobody");
            mFTPClient.enterLocalPassiveMode();
            //mFTPClient.changeWorkingDirectory("/data/forecasts/taf/stations");
            mFTPClient.changeWorkingDirectory("/data/observations/metar/stations");
            InputStream inStream = mFTPClient.retrieveFileStream(name + ".TXT");
            InputStreamReader isr = new InputStreamReader(inStream, "UTF8");

            reader = new BufferedReader(isr);

            reader.readLine();
            String s = reader.readLine();

            metar = METAR.fromMETARString(s, System.currentTimeMillis());

            Log.i(TAG, "doInBackground() | " + s);
        } catch (Exception e) {
            Log.i(TAG, "doInBackground() | update failed");
            //e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    Log.i(TAG, "doInBackground() | update failed");
                    //ioe.printStackTrace();
                }
            }
        }

        return metar;
    }

    @Override
    protected void onPostExecute(METAR metar) {
        Log.i(TAG, "onPostExecute() | New " + METARProvider.toString(name, metar));
        if (metar != null) metarProvider.newMetar(name, metar);
        metarProvider.bsl.busyStateChanged(false);
    }
}
