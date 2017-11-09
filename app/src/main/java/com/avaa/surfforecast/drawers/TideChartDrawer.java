package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;

import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.List;

import static com.avaa.surfforecast.data.Common.STR_M;
import static com.avaa.surfforecast.data.Common.STR_NOW;
import static com.avaa.surfforecast.data.Common.STR_NO_TIDE_DATA;
import static com.avaa.surfforecast.data.Common.STR_TIDE;
import static com.avaa.surfforecast.data.Common.TIME_ZONE;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorTideBG;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorTideChartBG;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorTideText;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.getColorMinor;


/**
 * Created by Alan on 6 Aug 2016.
 */


public class TideChartDrawer {
    private static final String TAG = "TideChartDrwr";

    private final SurfConditionsForecastView view;

    private final Bitmap[] bitmaps = new Bitmap[MainActivity.NDAYS];
    private final TideDataProvider tideDataProvider;
    private final MainModel model;

    private String tidePortID = null;

    private MetricsAndPaints metricsAndPaints;
    private int dh;
    private int dayWidth;
    public int h;

    public TideData tideData = null;


    private Paint paintBGNoData = new Paint() {{
        setColor(colorTideChartBG);
        setStyle(Style.FILL);
    }};
    private Paint paintFontNoData = new Paint() {{
        setColor(colorTideChartBG);
        setStyle(Style.FILL);
    }};

    private Paint paintCircle = new Paint() {{
        setAntiAlias(true);
        setColor(colorTideBG);
        setStyle(Style.FILL);
    }};
    private Paint paintFont = new Paint() {{
        setAntiAlias(true);
        setColor(0xFFFFFFFF);
        setTextAlign(Align.CENTER);
    }};
    private Paint paintFontSmall = new Paint(paintFont) {{
        setColor(0xFFFFFFFF);
        setTextAlign(Align.LEFT);
    }};

    private float strMWidth;


    public TideChartDrawer(SurfConditionsForecastView view, MainModel mainModel) {
        this.view = view;
        this.tideDataProvider = MainModel.instance.tideDataProvider;
        this.model = mainModel;
        setPortID(mainModel.getSelectedSpot().tidePortID);

        updateDrawer();

        mainModel.addChangeListener(MainModel.Change.SELECTED_SPOT, changes -> setPortID(mainModel.getSelectedSpot().tidePortID));
    }


    public void setPortID(String tidePortID) {
        if (this.tidePortID != tidePortID) {
            this.tidePortID = tidePortID;
            tideData = tideDataProvider.getTideData(tidePortID);
            updateBitmaps();
        }
    }


    public void updateDrawer() {
        this.metricsAndPaints = MainModel.instance.metricsAndPaints;

        this.dh = metricsAndPaints.dh;

        dayWidth = dh * 16;
        h = dh * 4;

        paintFont.setTextSize(metricsAndPaints.font);
        paintFontSmall.setTextSize(metricsAndPaints.fontSmall);

        paintFontNoData = new Paint() {{
            setTextAlign(Paint.Align.CENTER);
            setColor(getColorMinor(colorTideText));
            setTextSize(metricsAndPaints.font);
        }};

        strMWidth = paintFontSmall.measureText(STR_M);

        updateBitmaps();
    }


    public void draw(Canvas c, int w, int h, int dx, int orientation) {
        Integer nowTime = Common.getNowTimeInt(TIME_ZONE);

        if (tidePortID == null) return;

        Integer now = tideData == null ? null : tideData.getNow();

        int si = dx / dayWidth;
        int ei = Math.min(MainActivity.NDAYS - 1, (dx + w) / dayWidth);
        int x = si * dayWidth;
        for (int i = si; i <= ei; i++) {
            if (bitmaps[i] != null) {
                c.drawBitmap(bitmaps[i], x, h - this.h, null);
            } else {
                c.drawRect(x, h - this.h * 2 / 3, x + dayWidth, h, paintBGNoData);
                c.drawText(STR_NO_TIDE_DATA, x + dayWidth / 2, h - this.h / 2 + metricsAndPaints.fontH, paintFontNoData);
            }
            x += dayWidth;
        }

        if (now != null && bitmaps[0] != null) {
            int nowX = dayWidth * nowTime / 60 / 24;
            int nowY = h - this.h + dh * 3 / 2 - now * dh * 3 / 2 / 300;

            String tide = String.valueOf(Math.round(now / 10f) / 10f);

            if (MainModel.instance.userStat.getSpotsShownCount() > 2 || orientation == 1) {
                c.drawCircle(nowX, nowY, dh * 0.6f, paintCircle);
                if (orientation == 1) {
                    c.rotate(-90);
                    c.drawText(tide, -nowY, nowX + metricsAndPaints.fontHDiv2, paintFont);
                    c.rotate(90);
                } else {
                    c.drawText(tide, nowX, nowY + metricsAndPaints.fontHDiv2, paintFont);
                }
            } else {
                c.drawCircle(nowX, nowY, dh * 1.0f, paintCircle);
//                if (orientation == 1) {
//                    c.rotate(-90);
//                    c.drawText(tide, -nowY, nowX + metricsAndPaints.fontHDiv2, paintFont);
//                    c.drawText(tide, -nowY, nowX + metricsAndPaints.fontHDiv2, paintFont);
//                    c.rotate(90);
//                }
//                else {
                float strTideWidth = paintFont.measureText(tide);

                paintFontSmall.setTextAlign(Paint.Align.CENTER);
                c.drawText(STR_TIDE, nowX, nowY - metricsAndPaints.fontHDiv2 - metricsAndPaints.fontSmallSpacing, paintFontSmall);
                c.drawText(STR_NOW, nowX, nowY + metricsAndPaints.fontHDiv2 + metricsAndPaints.fontSmallH + metricsAndPaints.fontSmallSpacing, paintFontSmall);

                nowY += metricsAndPaints.fontSmallH / 12;

                paintFontSmall.setTextAlign(Paint.Align.LEFT);
                c.drawText(tide, nowX - strMWidth / 3, nowY + metricsAndPaints.fontHDiv2, paintFont);
                c.drawText(STR_M, nowX - strMWidth / 3 + strTideWidth / 2, nowY + metricsAndPaints.fontHDiv2, paintFontSmall);
//                }
            }
        }
    }


    private TideChartBitmapsAsyncDrawer tideChartBitmapsAsyncDrawer = null;


    public boolean updateBitmaps() {
        SurfSpot surfSpot = model.getSelectedSpot();

        tidePortID = surfSpot.tidePortID;

        if (tideDataProvider.getTideData(surfSpot.tidePortID) == null) {
//            Log.i(TAG, "updateBitmaps() | " + "cancelled. tideDataProvider.get() == null");
            return false;
        }

        if (tideChartBitmapsAsyncDrawer != null && tideChartBitmapsAsyncDrawer.getStatus() != AsyncTask.Status.FINISHED) {
            tideChartBitmapsAsyncDrawer.cancel(true);
//            Log.i(TAG, "updateBitmaps() | " + "cancelled async drawer");
        }

//        Calendar todaysStartTime = DateTimeHelper.getTodaysStartTime();
//        long time = todaysStartTime.getTime().getTime();

//        if (drawnForDay == 0) {
        tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(0, surfSpot, this);
//        }
//        else {
//            int offset = (int) ((time - drawnForDay) / 1000 / 60 / 60 / 24);
//            Log.i(TAG, "Offseting " + offset);
//            int si = 0;
//            for (int i = offset; i < MainActivity.NDAYS; i++) {
//                bitmaps[i - offset] = bitmaps[i];
//                if (bitmaps[i-offset] != null) si = i-offset+1;
//            }
//            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(si, this); //MainActivity.NDAYS - offset);
//            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(0, MainModel.instance.surfSpots.getSelectedSpot(), this);
//        }
        tideChartBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

//        drawnForDay = time;

        return false;
    }


    public void bitmapsReady(List<Bitmap> bitmaps, int fromDay) {
        for (Bitmap bitmap : bitmaps) this.bitmaps[fromDay++] = bitmap;
        view.postInvalidate();

        if (fromDay < MainActivity.NDAYS) {
            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(fromDay, model.getSelectedSpot(), this);
            tideChartBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); //tideChartBitmapsAsyncDrawer.execute();
        }
    }
}
