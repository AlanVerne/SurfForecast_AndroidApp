package com.avaa.surfforecast.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Trace;
import android.util.Log;

import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.drawers.ConditionsDrawer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alan on 16 Jan 2017.
 */

public class ForecastImagesAsyncDrawer extends AsyncTask<Void, Void, Map<Integer, SurfConditionsForecastView.ForecastBitmaps>> {
    private static final String TAG = "ForecastImagesAD";

    final SurfSpot surfSpot;
    final int step;
    final List<Integer> daysToDraw;
    final Map<Integer, SortedMap<Integer, SurfConditions>> conditions = new HashMap<>();
    final int orientationF;
    final SurfConditionsForecastView view;
    final ConditionsDrawer drawer;

    public ForecastImagesAsyncDrawer(SurfSpot surfSpot, int step, List<Integer> daysToDraw, SurfConditionsForecastView view) {
        Log.i(TAG, "ForecastImagesAsyncDrawer() | surfSpot = " + surfSpot.name);

        this.surfSpot = surfSpot;
        this.step = step;
        this.daysToDraw = daysToDraw;
        this.orientationF = view.orientation;
        this.view = view;

        for (Integer day : daysToDraw) {
            SortedMap<Integer, SurfConditions> sc = surfSpot.conditionsProvider.getFixed(day);
            conditions.put(day, sc);
            //Log.i(TAG, "ForecastImagesAsyncDrawer() | day = " + day + (sc == null ? ", sc null" : ", sc ok"));
        }

        drawer = new ConditionsDrawer(view.condDrawer.density);
        drawer.setDH(view.condDrawer.dh);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.i(TAG, "onCancelled() | " + daysToDraw.toString());
    }

    @Override
    protected Map<Integer, SurfConditionsForecastView.ForecastBitmaps> doInBackground(Void... params) {
        //Trace.beginSection("doInBackground");
        Log.i(TAG, "doInBackground() | daysToDraw = " + daysToDraw.toString()); //surfSpot.name + " "

        Bitmap b = null;
        if (step == 0) b = Bitmap.createBitmap(drawer.dh * 16, drawer.dh * 16, Bitmap.Config.ARGB_8888);

        Map<Integer, SurfConditionsForecastView.ForecastBitmaps> bitmaps = new TreeMap<>();
        for (Integer day : daysToDraw) {
            if (isCancelled()) return null;

            SortedMap<Integer, SurfConditions> sc = conditions.get(day);

            SurfConditionsForecastView.ForecastBitmaps fb = new SurfConditionsForecastView.ForecastBitmaps();

            if (sc != null) {
                fb.wave = drawer.drawWave(sc, orientationF == 1);

                if (isCancelled()) return null;

                fb.wind = drawer.drawWind(sc, surfSpot.waveDirection, orientationF == 1);

                if (isCancelled()) return null;

                if (step == 0) {
//                    Trace.beginSection("inBG warming");
                    Canvas c = new Canvas(b);
                    //for (int i = 0; i < 4; i++) {
//                    Trace.beginSection("inBG warming i");
                    c.drawBitmap(fb.wave, 0, -1, null);
                    c.drawBitmap(fb.wind, -1, 0, null);
//                    Trace.endSection();
                    //}
//                    Trace.endSection();
                }

                if (isCancelled()) return null;
            }

            bitmaps.put(day, fb);
        }
//        Trace.endSection();
        return bitmaps;
    }

    @Override
    protected void onPostExecute(Map<Integer, SurfConditionsForecastView.ForecastBitmaps> forecastBitmaps) {
//        Trace.beginSection("onPostExecute");
        Log.i(TAG, "onPostExecute() | daysToDraw = " + daysToDraw.toString());
        //Log.i("MA FID", "on post exec " + surfSpot.name + " " + forecastBitmaps.size());

        for (Map.Entry<Integer, SurfConditionsForecastView.ForecastBitmaps> fb : forecastBitmaps.entrySet()) {
            view.bitmaps[fb.getKey()].wind = fb.getValue().wind;
            view.bitmaps[fb.getKey()].wave = fb.getValue().wave;
        }

        if (step == 0) {
            view.postInvalidate();

            ArrayList<Integer> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (!forecastBitmaps.keySet().contains(i)) days.add(i);
            }
            view.fiad = new ForecastImagesAsyncDrawer(surfSpot, 1, days, view);
            view.fiad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
//        Trace.endSection();
    }
}
