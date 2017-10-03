package com.avaa.surfforecast.views.Map;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.ParallaxHelper;

import static com.avaa.surfforecast.data.Common.STR_M;
import static com.avaa.surfforecast.data.Common.STR_TIDE;
import static com.avaa.surfforecast.data.Common.TIME_ZONE;
import static com.avaa.surfforecast.views.ColorUtils.alpha;
import static com.avaa.surfforecast.views.Map.BaliMap.STR_DASH;

/**
 * Created by Alan on 6 Sep 2017.
 */


public class TideCircle extends MapCircle {
    private int colorWaterColor = 0xffacb5b8; //0xffa3b1b6; //0xff819faa; //0xff2e393d;
    public final Paint paintPathTide = new Paint() {{
        setAntiAlias(true);
        setColor(colorWaterColor);
        setStyle(Paint.Style.FILL);
    }};

    public TideCircle(Context context) {
        super(context);

        paintFont.setColor(MetricsAndPaints.WHITE);
        paintHintsFont.setColor(MetricsAndPaints.WHITE);

        MainModel model = MainModel.instance;

        model.addChangeListener(changes -> {
            updateTideData();
        }, MainModel.Change.SELECTED_CONDITIONS);

        updateTideData();
    }


    public void paint(Canvas c, float ox, float oy, float visible, ParallaxHelper parallaxHelper) {
        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

        visible *= scrollerVisible.getValue();
        float hintsVisible = scrollerHints.getValue();

        int dh = metricsAndPaints.dh;
        paintFont.setTextSize(visible * metricsAndPaints.font);
        paintHintsFont.setTextSize(visible * metricsAndPaints.fontSmall);

        float fontH = visible * metricsAndPaints.fontH;

        if (tide == null) return;

        checkNowTide();

        float r = (dh - DENSITY_DH_DEP) * visible + DENSITY_DH_DEP;

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * subZ);
        float x = pp.x, y = pp.y;

        float py = (float) (r / Math.sqrt(2));
        float nowy = py - py * 2 * tide / 250;
        float nowx = (float) (-Math.cos(Math.asin(nowy / r)) * r);

        if (pathTide != null) {
            c.save();
            c.translate(x, y);
            c.scale(visible, visible);
            c.drawPath(pathTide, paintPathTide);
            c.restore();
        }

        // value

        pp = parallaxHelper.applyParallax(ox + nowx, oy + nowy, dh * z);
        x = pp.x;
        y = pp.y;

        float dotR = visible * (dh * 0.7f + hintsVisible * dh / 4);
        paintFont.setColor(MetricsAndPaints.colorTideBG);
        c.drawCircle(x, y, dotR, paintFont);

        paintFont.setColor(alpha(visible, 0xffffffff));

        String strTide = tide == null ? STR_DASH : String.valueOf(Math.round(tide / 10f) / 10f);

//        float strTideWidth = paintFont.measureText(strTide);
//        if (hintsVisible > 0) {
//            float strMWidth = paintHintsFont.measureText(STR_M);
//            x -= strMWidth / 3f * hintsVisible;
//            paintHintsFont.setColor((int)(hintsVisible*0xff)<<24 | 0xffffff);
//            paintHintsFont.setTextAlign(Paint.Align.LEFT);
//            c.drawText(STR_M, x + strTideWidth / 2f, y, paintHintsFont);
//        }

        if (hintsVisible > 0) {
            paintHintsFont.setColor(alpha(visible * hintsVisible * hintsVisible, 0xffffff));
            paintHintsFont.setTextAlign(Paint.Align.CENTER);

            hintsVisible *= visible;

            y += metricsAndPaints.fontSmallH / 12 * hintsVisible;

            c.drawText(STR_TIDE, x, y - fontH / 2 - metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);
            c.drawText(STR_M, x, y + fontH / 2 + metricsAndPaints.fontSmallH * visible + metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);

            y += metricsAndPaints.fontSmallH / 8 * hintsVisible;
        }

        c.drawText(strTide, x, y + fontH / 2, paintFont);
    }


    private long nowTideUpdatedTime = -1;
    private Integer tide;
    private TideData tideData;
    private Path pathTide;

    private void checkNowTide() {
        long nowTime = System.currentTimeMillis();
        if (nowTideUpdatedTime + 60 * 1000 < nowTime) updateNowTide(nowTime);
    }

    private void updateNowTide() {
        updateNowTide(System.currentTimeMillis());
    }

    private void updateNowTide(long nowTime) {
        MainModel model = MainModel.instance;

        nowTideUpdatedTime = nowTime;

        Integer newTide = null;

        if (tideData != null) {
            newTide = tideData.getTide(model.getDayInt(), model.selectedTime);
            if (newTide != null) tide = newTide;
        }
//        if (model != null) Log.i(TAG, "updateNowTide " + model.selectedTime);

        if (tideData != null && tide != null && model.metricsAndPaints != null) {
            setVisible(true, true);
            int nowTimeInt = model.selectedTime; //Common.getNowTimeInt(TIME_ZONE);
            float r = model.metricsAndPaints.dh;
            float width = r * 8;
            float py = (float) (r / Math.sqrt(2));
            float nowy = py - py * 2 * tide / 250;
            float nowx = (float) (-Math.cos(Math.asin(nowy / r)) * r);
            int nowH = nowTimeInt / 60;
            final float pathDX = width * (nowTimeInt - (nowH - 2) * 60) / 24 / 60 - nowx;

            Path pathTide = tideData.getPath2(Common.getToday(TIME_ZONE), width * 9 / 24, py * 2, 0, 250, nowH - 2, nowH + 7);
            if (pathTide != null) {
                this.pathTide = pathTide;
                Matrix translateMatrix = new Matrix();
                translateMatrix.setTranslate(-pathDX, -py);
                pathTide.transform(translateMatrix);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Path pathCircle = new Path();
                    pathCircle.addCircle(0, 0, r, Path.Direction.CCW);
                    pathTide.op(pathCircle, Path.Op.INTERSECT);
                }
            }
        }

        if (newTide == null) {
            setVisible(false, true);
        }
    }


    private void updateTideData() {
        MainModel model = MainModel.instance;

        tideData = MainModel.instance.tideDataProvider.getTideData(model.getSelectedSpot().tidePortID);

        updateNowTide();
    }
}
