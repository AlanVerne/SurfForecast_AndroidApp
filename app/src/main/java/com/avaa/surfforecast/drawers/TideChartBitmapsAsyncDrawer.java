package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alan on 25 Jan 2017.
 */

public class TideChartBitmapsAsyncDrawer extends AsyncTask<Void, Void, List<Bitmap>> {
    private static final String TAG = "TideChartBmpsAD";

    private final TideChartDrawer tideChartDrawer;

    private final double la, lo;

    private final int startFromDay;

    private final TideData tideData;
    private final TideChartBitmapsDrawer drawer;

    private final int dh;


    public TideChartBitmapsAsyncDrawer(int startFromDay, SurfSpot surfSpot, TideChartDrawer tideChartDrawer) {
        this.tideChartDrawer = tideChartDrawer;

        this.startFromDay = startFromDay;

        this.tideData = AppContext.instance.tideDataProvider.getTideData(surfSpot.tidePortID);
        this.drawer = new TideChartBitmapsDrawer(AppContext.instance.metricsAndPaints);

        this.la = surfSpot.la;
        this.lo = surfSpot.lo;

        this.dh = AppContext.instance.metricsAndPaints.dh;
    }

    @Override
    protected List<Bitmap> doInBackground(Void... params) {
        //Trace.beginSection("doInBackground");
        Log.i(TAG, "doInBackground() | startFromDay = " + startFromDay + " ");

        if (tideData == null) return null;

        List<Bitmap> bitmaps = new ArrayList<>();
        int endDay = startFromDay == 0 ? 2 : MainActivity.NDAYS;

        Canvas canvasForWarming = null;
        if (startFromDay == 0) {
            Bitmap bitmapForWarming;
            bitmapForWarming = Bitmap.createBitmap(dh * 16, dh * 4, Bitmap.Config.ARGB_8888);
            canvasForWarming = new Canvas(bitmapForWarming);
        }

        for (int i = startFromDay; i < endDay; i++) {
            if (isCancelled()) return null;

            Bitmap bi = drawer.drawTide(tideData, la, lo, i, false);
            bitmaps.add(bi);

            if (canvasForWarming != null && bi != null) {
                //Trace.beginSection("inBG warming");
                //for (int i = 0; i < 4; i++) {
                //Trace.beginSection("inBG warming i");
                canvasForWarming.drawBitmap(bi, 0, -1, null);
                //Trace.endSection();
                //}
                //Trace.endSection();
            }
        }

//        Trace.endSection();
        return bitmaps;
    }

    @Override
    protected void onPostExecute(List<Bitmap> bitmaps) {
//        Trace.beginSection("onPostExecute");
        if (bitmaps == null) return;
        tideChartDrawer.bitmapsReady(bitmaps, startFromDay);
//        Trace.endSection();
    }
}
