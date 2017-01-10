package com.avaa.surfforecast.drawers;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.DateTimeHelper;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;


/**
 * Created by Alan on 6 Aug 2016.
 */


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TideDrawer {
    private static final String TAG = "TideDrawer";
    private final SurfConditionsForecastView view;

    private long drawnForDay = 0;
    private Bitmap[] bitmaps = new Bitmap[MainActivity.NDAYS];
    private final TideDataProvider tideDataProvider;
    private ConditionsDrawer condDrawer;
    private int dh;

    private int dayWidth;
    public int h;

    Paint paint;
    Rect bounds;


    public TideDrawer(SurfConditionsForecastView view, ConditionsDrawer condDrawer) {
        this.view = view;
        this.tideDataProvider = AppContext.instance.tideDataProvider;

        updateDrawer(condDrawer);
    }


    public void updateDrawer(ConditionsDrawer condDrawer) {
        this.condDrawer = condDrawer;
        this.dh = condDrawer.dh;

        dayWidth = dh*16;
        h        = dh*4;

        paint = new Paint() {{
            setAntiAlias(true);
            setTextSize(condDrawer.conditionsFontSize);
            setColor(0xFFFFFFFF);
            setTextAlign(Align.CENTER);
        }};

//        bounds = new Rect();
//        paint.getTextBounds("0", 0, 1, bounds);

        updateBitmaps();
    }



    int nowx = 0, nowy = 0;
    public void draw(Canvas c, int w, int h, int dx, int orientation) {
        Integer nowTime = Common.getNowTimeInt(TIME_ZONE);

        TideData tideData = tideDataProvider.getTideData(Common.BENOA_PORT_ID);
        Integer now = tideData == null ? null : tideData.getNow();

        if (now != null) {
            nowx = dayWidth * nowTime / 60 / 24;
            nowy = h - this.h + dh * 3 / 2 - now * dh * 3 / 2 / 300;
        }

        //if (bitmaps == null || bitmaps.isEmpty()) return;
        int si = dx / dayWidth;
        int ei = Math.min(MainActivity.NDAYS-1, (dx+w) / dayWidth);
        int x = si * dayWidth;
        for (int i = si; i <= ei; i++) {
            if (bitmaps[i] == null) {
                c.drawRect(x, h-this.h*2/3, x+dayWidth, h, new Paint(){{setColor(ConditionsDrawer.colorTideChartBG);}});
                Paint paintText = new Paint(condDrawer.paintHourlyTides);
                paintText.setTextAlign(Paint.Align.CENTER);
                c.drawText("No tide data", x+dayWidth/2, h-this.h/2 + condDrawer.conditionsFontH, paintText);
            }
            else c.drawBitmap(bitmaps[i], x, h - this.h, null);
            x += dayWidth;
        }

        if (now != null && bitmaps[0] != null) {
            condDrawer.paint.setColor(ConditionsDrawer.colorTideBG); //0xFF000000); //0xff000000);
            c.drawCircle(nowx, nowy, condDrawer.dh*0.6f, condDrawer.paint);

            if (orientation == 1) {
                c.rotate(-90);
                c.drawText(String.valueOf(Math.round(now / 10f) / 10f), -nowy, nowx + condDrawer.conditionsFontH / 2, paint);
                c.rotate(90);
            }
            else {
                c.drawText(String.valueOf(Math.round(now/10f)/10f), nowx, nowy + condDrawer.conditionsFontH/2, paint);
            }
        }
    }


    //


    private static class TideBitmapsAsyncDrawer extends AsyncTask<Void, Void, List<Bitmap>> {
        private int startFromDay;
        private final ConditionsDrawer condDrawer;
        private final TideDrawer tideDrawer;

        public TideBitmapsAsyncDrawer(int startFromDay, TideDrawer tideDrawer) {
            this.startFromDay = startFromDay;
            this.condDrawer = new ConditionsDrawer(tideDrawer.condDrawer.density);
            condDrawer.setDH(tideDrawer.condDrawer.dh);
            this.tideDrawer = tideDrawer;
        }

        @Override
        protected List<Bitmap> doInBackground(Void... params) {
            Trace.beginSection("doInBackground");

            Log.i(TAG, "doInBackground() | startFromDay = " + startFromDay + " ");
            if (isCancelled()) return null;

            Bitmap bitmapForWarming = null;
            if (startFromDay == 0) bitmapForWarming = Bitmap.createBitmap(condDrawer.dh * 16, condDrawer.dh * 4, Bitmap.Config.ARGB_8888);

            List<Bitmap> bitmaps = new ArrayList<>();
            int endDay = startFromDay == 0 ? 2 : MainActivity.NDAYS;

            TideData tideData = tideDrawer.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
            if (tideData == null) return null;

            for (int i = startFromDay; i < endDay; i++) {
                Bitmap bi = condDrawer.drawTide(tideData, i, false);

                if (startFromDay == 0 && bi != null) {
                    Trace.beginSection("inBG warming");
                    Canvas c = new Canvas(bitmapForWarming);
                    //for (int i = 0; i < 4; i++) {
                    Trace.beginSection("inBG warming i");
                    c.drawBitmap(bi, 0, -1, null);
                    Trace.endSection();
                    //}
                    Trace.endSection();
                }
                bitmaps.add(bi);
            }

            Trace.endSection();
            return bitmaps;
        }

        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            Trace.beginSection("onPostExecute");

            if (bitmaps == null) return;

            for (Bitmap bitmap : bitmaps) tideDrawer.bitmaps[startFromDay++] = bitmap;
            tideDrawer.view.postInvalidate();

            if (startFromDay < MainActivity.NDAYS) {
                tideDrawer.tbad = new TideBitmapsAsyncDrawer(startFromDay, tideDrawer);
                tideDrawer.tbad.execute();
            }

            Trace.endSection();
        }
    }


    private TideBitmapsAsyncDrawer tbad = null;
    public boolean updateBitmaps() {
//        if (dh <= 0) {
//            Log.i(TAG, "updateBitmaps() | " + "cancelled. dh = " + dh);
//            return false;
//        }

        if (tideDataProvider.getTideData(Common.BENOA_PORT_ID) == null) {
            Log.i(TAG, "updateBitmaps() | " + "cancelled. tideDataProvider.get() == null");
            return false;
        }

        if (tbad != null && tbad.getStatus() != AsyncTask.Status.FINISHED) {
            tbad.cancel(true);
            Log.i(TAG, "updateBitmaps() | " + "cancelled async drawer");
        }

        Calendar todaysStartTime = DateTimeHelper.getTodaysStartTime();
        long time = todaysStartTime.getTime().getTime();

        if (drawnForDay == 0) {
            tbad = new TideBitmapsAsyncDrawer(0, this);
        }
        else {
//            int offset = (int) ((time - drawnForDay) / 1000 / 60 / 60 / 24);
//            Log.i(TAG, "Offseting " + offset);
//            int si = 0;
//            for (int i = offset; i < MainActivity.NDAYS; i++) {
//                bitmaps[i - offset] = bitmaps[i];
//                if (bitmaps[i-offset] != null) si = i-offset+1;
//            }
//            tbad = new TideBitmapsAsyncDrawer(si, this); //MainActivity.NDAYS - offset);
            tbad = new TideBitmapsAsyncDrawer(0, this);
        }
        tbad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        drawnForDay = time;

        return false;
    }
}
