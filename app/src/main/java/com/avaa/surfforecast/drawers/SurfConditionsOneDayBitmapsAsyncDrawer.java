package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfConditionsOneDay;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Alan on 16 Jan 2017.
 */

public class SurfConditionsOneDayBitmapsAsyncDrawer extends AsyncTask<Void, Void, Map<Integer, SurfConditionsForecastView.SurfConditionsOneDayBitmaps>> {
    private static final String TAG = "SurfCondAD";

    private final SurfSpot surfSpot;
    private final int step;
    private final List<Integer> daysToDraw;
    private final Map<Integer, SurfConditionsOneDay> conditions = new HashMap<>();
    private final int orientationF;
    private final SurfConditionsForecastView view;
    private final SurfConditionsOneDayBitmapsDrawer drawer;


    public SurfConditionsOneDayBitmapsAsyncDrawer(SurfSpot surfSpot, int step, List<Integer> daysToDraw, SurfConditionsForecastView view) {
//        Log.i(TAG, "SurfConditionsOneDayBitmapsAsyncDrawer() | surfSpot = " + surfSpot.name);

        this.surfSpot = surfSpot;
        this.step = step;
        this.daysToDraw = daysToDraw;
        this.orientationF = view.getOrientation();
        this.view = view;

        for (Integer day : daysToDraw) {
            SurfConditionsOneDay sc = surfSpot.conditionsProvider.get(day);
            conditions.put(day, sc);
            //Log.i(TAG, "SurfConditionsOneDayBitmapsAsyncDrawer() | day = " + day + (sc == null ? ", sc null" : ", sc ok"));
        }

        drawer = new SurfConditionsOneDayBitmapsDrawer(MainModel.instance.metricsAndPaints);
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
//        Log.i(TAG, "onCancelled() | " + daysToDraw.toString());
    }


    @Override
    protected Map<Integer, SurfConditionsForecastView.SurfConditionsOneDayBitmaps> doInBackground(Void... params) {
        //Trace.beginSection("doInBackground");
//        Log.i(TAG, "doInBackground() | daysToDraw = " + daysToDraw.toString()); //surfSpot.name + " "

        Bitmap b = null;
        if (step == 0) b = drawer.getBitmapForWarming();

        Map<Integer, SurfConditionsForecastView.SurfConditionsOneDayBitmaps> bitmaps = new TreeMap<>();
        for (Integer day : daysToDraw) {
            if (isCancelled()) return null;

            SurfConditionsOneDay sc = conditions.get(day);

            SurfConditionsForecastView.SurfConditionsOneDayBitmaps surfConditionsOneDayBitmaps = new SurfConditionsForecastView.SurfConditionsOneDayBitmaps();

            if (sc != null) {
                Map<Integer, SurfConditions> scFixed = sc.getFixed();

                surfConditionsOneDayBitmaps.wave = drawer.drawWave(scFixed, orientationF == 1);

                if (isCancelled()) return null;

                surfConditionsOneDayBitmaps.wind = drawer.drawWind(scFixed, surfSpot.waveDirection, orientationF == 1);

//                surfConditionsOneDayBitmaps.forSurfConditionsOneDay = sc;

                if (isCancelled()) return null;

                if (step == 0) {
//                    Trace.beginSection("inBG warming");
                    Canvas c = new Canvas(b);
                    //for (int i = 0; i < 4; i++) {
//                    Trace.beginSection("inBG warming i");
                    c.drawBitmap(surfConditionsOneDayBitmaps.wave, 0, -1, null);
                    c.drawBitmap(surfConditionsOneDayBitmaps.wind, -1, 0, null);
//                    Trace.endSection();
                    //}
//                    Trace.endSection();
                }

                if (isCancelled()) return null;
            }

            bitmaps.put(day, surfConditionsOneDayBitmaps);
        }
//        Trace.endSection();
        return bitmaps;
    }


    @Override
    protected void onPostExecute(Map<Integer, SurfConditionsForecastView.SurfConditionsOneDayBitmaps> forecastBitmaps) {
//        Trace.beginSection("onPostExecute");
//        Log.i(TAG, "onPostExecute() | daysToDraw = " + daysToDraw.toString());
        //Log.i(TAG, "onPostExecute() | " + surfSpot.name + " " + forecastBitmaps.size());
        view.newBitmaps(forecastBitmaps, step);

        if (step == 0) {
            ArrayList<Integer> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (!forecastBitmaps.keySet().contains(i)) days.add(i);
            }
            view.redrawOtherBitmaps(days);
        }
//        Trace.endSection();
    }
}
