package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alan on 16 Jan 2017.
 */

public class SurfConditionsOneDayBitmapsAsyncDrawer extends AsyncTask<Void, Void, Map<Integer, SurfConditionsForecastView.SurfConditionsOneDatBitmaps>> {
    private static final String TAG = "ForecastImagesAD";

    private final SurfSpot surfSpot;
    private final int step;
    private final List<Integer> daysToDraw;
    private final Map<Integer, SortedMap<Integer, SurfConditions>> conditions = new HashMap<>();
    private final int orientationF;
    private final SurfConditionsForecastView view;
    private final SurfConditionsOneDayBitmapsDrawer drawer;


    public SurfConditionsOneDayBitmapsAsyncDrawer(SurfSpot surfSpot, int step, List<Integer> daysToDraw, SurfConditionsForecastView view) {
        Log.i(TAG, "SurfConditionsOneDayBitmapsAsyncDrawer() | surfSpot = " + surfSpot.name);

        this.surfSpot = surfSpot;
        this.step = step;
        this.daysToDraw = daysToDraw;
        this.orientationF = view.orientation;
        this.view = view;

        for (Integer day : daysToDraw) {
            SortedMap<Integer, SurfConditions> sc = surfSpot.conditionsProvider.getFixed(day);
            conditions.put(day, sc);
            //Log.i(TAG, "SurfConditionsOneDayBitmapsAsyncDrawer() | day = " + day + (sc == null ? ", sc null" : ", sc ok"));
        }

        drawer = new SurfConditionsOneDayBitmapsDrawer(AppContext.instance.metricsAndPaints);
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.i(TAG, "onCancelled() | " + daysToDraw.toString());
    }


    @Override
    protected Map<Integer, SurfConditionsForecastView.SurfConditionsOneDatBitmaps> doInBackground(Void... params) {
        //Trace.beginSection("doInBackground");
        Log.i(TAG, "doInBackground() | daysToDraw = " + daysToDraw.toString()); //surfSpot.name + " "

        Bitmap b = null;
        if (step == 0) b = drawer.getBitmapForWarming();

        Map<Integer, SurfConditionsForecastView.SurfConditionsOneDatBitmaps> bitmaps = new TreeMap<>();
        for (Integer day : daysToDraw) {
            if (isCancelled()) return null;

            SortedMap<Integer, SurfConditions> sc = conditions.get(day);

            SurfConditionsForecastView.SurfConditionsOneDatBitmaps surfConditionsOneDatBitmaps = new SurfConditionsForecastView.SurfConditionsOneDatBitmaps();

            if (sc != null) {
                surfConditionsOneDatBitmaps.wave = drawer.drawWave(sc, orientationF == 1);

                if (isCancelled()) return null;

                surfConditionsOneDatBitmaps.wind = drawer.drawWind(sc, surfSpot.waveDirection, orientationF == 1);

                if (isCancelled()) return null;

                if (step == 0) {
//                    Trace.beginSection("inBG warming");
                    Canvas c = new Canvas(b);
                    //for (int i = 0; i < 4; i++) {
//                    Trace.beginSection("inBG warming i");
                    c.drawBitmap(surfConditionsOneDatBitmaps.wave, 0, -1, null);
                    c.drawBitmap(surfConditionsOneDatBitmaps.wind, -1, 0, null);
//                    Trace.endSection();
                    //}
//                    Trace.endSection();
                }

                if (isCancelled()) return null;
            }

            bitmaps.put(day, surfConditionsOneDatBitmaps);
        }
//        Trace.endSection();
        return bitmaps;
    }


    @Override
    protected void onPostExecute(Map<Integer, SurfConditionsForecastView.SurfConditionsOneDatBitmaps> forecastBitmaps) {
//        Trace.beginSection("onPostExecute");
        Log.i(TAG, "onPostExecute() | daysToDraw = " + daysToDraw.toString());
        //Log.i("MA FID", "on post exec " + surfSpot.name + " " + forecastBitmaps.size());

        for (Map.Entry<Integer, SurfConditionsForecastView.SurfConditionsOneDatBitmaps> fb : forecastBitmaps.entrySet()) {
            view.bitmaps[fb.getKey()].wind = fb.getValue().wind;
            view.bitmaps[fb.getKey()].wave = fb.getValue().wave;
        }

        if (step == 0) { // TODO тоже нахер отсюда эту логику
            view.postInvalidate();

            ArrayList<Integer> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (!forecastBitmaps.keySet().contains(i)) days.add(i);
            }
            view.surfConditionsOneDayBitmapsAsyncDrawer = new SurfConditionsOneDayBitmapsAsyncDrawer(surfSpot, 1, days, view);
            view.surfConditionsOneDayBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
//        Trace.endSection();
    }
}
