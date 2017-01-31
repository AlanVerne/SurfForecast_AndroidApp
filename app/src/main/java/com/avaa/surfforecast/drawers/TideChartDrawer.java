package com.avaa.surfforecast.drawers;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.DateTimeHelper;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.Calendar;
import java.util.List;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;
import static com.avaa.surfforecast.data.Common.noTideData;
import static com.avaa.surfforecast.data.Common.strM;
import static com.avaa.surfforecast.data.Common.strNOW;
import static com.avaa.surfforecast.data.Common.strTIDE;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.*;

/**
 * Created by Alan on 6 Aug 2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TideChartDrawer {
    private static final String TAG = "TideChartDrwr";

    private final SurfConditionsForecastView view;

    private final Bitmap[] bitmaps = new Bitmap[MainActivity.NDAYS];
    private final TideDataProvider tideDataProvider;

    private MetricsAndPaints metricsAndPaints;
    private int dh;
    private int dayWidth;
    private int h;

    private long drawnForDay = 0;
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


    public TideChartDrawer(SurfConditionsForecastView view) {
        this.view = view;
        this.tideDataProvider = AppContext.instance.tideDataProvider;
        updateDrawer();
    }


    public void updateDrawer() {
        this.metricsAndPaints = AppContext.instance.metricsAndPaints;

        this.dh = metricsAndPaints.dh;

        dayWidth = dh*16;
        h        = dh*4;

        paintFont.setTextSize(metricsAndPaints.font);
        paintFontSmall.setTextSize(metricsAndPaints.fontSmall);

        paintFontNoData = new Paint() {{
            setTextAlign(Paint.Align.CENTER);
            setColor(getColorMinor(colorTideText));
            setTextSize(metricsAndPaints.font);
        }};

        strMWidth = paintFontSmall.measureText(strM);

        updateBitmaps();
    }


    private int nowx = 0, nowy = 0;
    public void draw(Canvas c, int w, int h, int dx, int orientation) {
        Integer nowTime = Common.getNowTimeInt(TIME_ZONE);

        tideData = tideDataProvider.getTideData(Common.BENOA_PORT_ID); // TODO вынести это нахер отсюда, добавить листенер на TideProvider
        Integer now = tideData == null ? null : tideData.getNow();

        if (now != null) {
            nowx = dayWidth * nowTime / 60 / 24;
            nowy = h - this.h + dh * 3 / 2 - now * dh * 3 / 2 / 300;
        }

        int si = dx / dayWidth;
        int ei = Math.min(MainActivity.NDAYS-1, (dx+w) / dayWidth);
        int x = si * dayWidth;
        for (int i = si; i <= ei; i++) {
            if (bitmaps[i] != null) {
                c.drawBitmap(bitmaps[i], x, h - this.h, null);
            }
            else {
                c.drawRect(x, h - this.h * 2/3, x + dayWidth, h, paintBGNoData);
                c.drawText(noTideData, x + dayWidth/2, h - this.h/2 + metricsAndPaints.fontH, paintFontNoData);
            }
            x += dayWidth;
        }

        if (now != null && bitmaps[0] != null) {
            String tide = String.valueOf(Math.round(now / 10f) / 10f);

            if (AppContext.instance.usageStat.getSpotsShownCount() > 2 || orientation == 1) {
                c.drawCircle(nowx, nowy, dh * 0.6f, paintCircle);
                if (orientation == 1) {
                    c.rotate(-90);
                    c.drawText(tide, -nowy, nowx + metricsAndPaints.fontHDiv2, paintFont);
                    c.rotate(90);
                } else {
                    c.drawText(tide, nowx, nowy + metricsAndPaints.fontHDiv2, paintFont);
                }
            }
            else {
                c.drawCircle(nowx, nowy, dh * 1.0f, paintCircle);
                if (orientation == 1) {
                    c.rotate(-90);
                    c.drawText(tide, -nowy, nowx + metricsAndPaints.fontHDiv2, paintFont);
                    c.drawText(tide, -nowy, nowx + metricsAndPaints.fontHDiv2, paintFont);
                    c.rotate(90);
                }
                else {
                    float strTideWidth = paintFont.measureText(tide);

                    paintFontSmall.setTextAlign(Paint.Align.CENTER);
                    c.drawText(strTIDE, nowx, nowy - metricsAndPaints.fontHDiv2 - metricsAndPaints.fontSmallSpacing, paintFontSmall);
                    c.drawText(strNOW, nowx , nowy + metricsAndPaints.fontHDiv2 + metricsAndPaints.fontSmallH + metricsAndPaints.fontSmallSpacing, paintFontSmall);

                    nowy += metricsAndPaints.fontSmallH/12;

                    paintFontSmall.setTextAlign(Paint.Align.LEFT);
                    c.drawText(tide, nowx - strMWidth/3, nowy + metricsAndPaints.fontHDiv2, paintFont);
                    c.drawText(strM, nowx - strMWidth/3 + strTideWidth/2, nowy + metricsAndPaints.fontHDiv2, paintFontSmall);
                }
            }
        }
    }


    private TideChartBitmapsAsyncDrawer tideChartBitmapsAsyncDrawer = null;
    public boolean updateBitmaps() {
        if (tideDataProvider.getTideData(Common.BENOA_PORT_ID) == null) {
            Log.i(TAG, "updateBitmaps() | " + "cancelled. tideDataProvider.get() == null");
            return false;
        }

        if (tideChartBitmapsAsyncDrawer != null && tideChartBitmapsAsyncDrawer.getStatus() != AsyncTask.Status.FINISHED) {
            tideChartBitmapsAsyncDrawer.cancel(true);
            Log.i(TAG, "updateBitmaps() | " + "cancelled async drawer");
        }

        Calendar todaysStartTime = DateTimeHelper.getTodaysStartTime();
        long time = todaysStartTime.getTime().getTime();

        if (drawnForDay == 0) {
            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(0, this);
        }
        else {
//            int offset = (int) ((time - drawnForDay) / 1000 / 60 / 60 / 24);
//            Log.i(TAG, "Offseting " + offset);
//            int si = 0;
//            for (int i = offset; i < MainActivity.NDAYS; i++) {
//                bitmaps[i - offset] = bitmaps[i];
//                if (bitmaps[i-offset] != null) si = i-offset+1;
//            }
//            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(si, this); //MainActivity.NDAYS - offset);
            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(0, this);
        }
        tideChartBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        drawnForDay = time;

        return false;
    }
    public void bitmapsReady(List<Bitmap> bitmaps, int fromDay) {
        for (Bitmap bitmap : bitmaps) this.bitmaps[fromDay++] = bitmap;
        view.postInvalidate();

        if (fromDay < MainActivity.NDAYS) {
            tideChartBitmapsAsyncDrawer = new TideChartBitmapsAsyncDrawer(fromDay, this);
            tideChartBitmapsAsyncDrawer.execute();
        }
    }
}
