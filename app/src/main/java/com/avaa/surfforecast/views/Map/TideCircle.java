package com.avaa.surfforecast.views.Map;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.ParallaxHelper;

import static com.avaa.surfforecast.MainModel.Change.SELECTED_DAY;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_SPOT;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_TIME;
import static com.avaa.surfforecast.data.Common.STR_DASH;
import static com.avaa.surfforecast.data.Common.STR_M;
import static com.avaa.surfforecast.data.Common.STR_TIDE;
import static com.avaa.surfforecast.views.ColorUtils.alpha;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class TideCircle extends MapCircle {
    private static final String TAG = "TideCircle";

    public int colorWaterColor = 0xffacb5b8; //0xffa3b1b6; //0xff819faa; //0xff2e393d;

    private final Paint paintPathTide = new Paint() {{
        setAntiAlias(true);
        setColor(colorWaterColor);
        setStyle(Paint.Style.FILL);
    }};

    private int time = -1;
    private Integer tide;
    private TideData tideData;
    private Path pathTide;


    public TideCircle(View view) {
        super(view);

        paintFont.setColor(MetricsAndPaints.colorWhite);
        paintHintsFont.setColor(MetricsAndPaints.colorWhite);

        MainModel model = MainModel.instance;

        model.addChangeListener(SELECTED_TIME, SELECTED_DAY, SELECTED_SPOT, changes -> {
//            Log.i(TAG, "change " + changes.toString());
            if (changes.contains(SELECTED_SPOT)) updateTideData();
            updateNowTide();
//            Log.i(TAG, "change " + model.getSelectedTime() + " " + (tideData != null) + " " + tide);
        });

        updateTideData();
        updateNowTide();
    }


    private void checkNowTide() {
        if (time + 1 < MainModel.instance.getSelectedTime()) updateNowTide();
    }

    private void updateTideData() {
        MainModel model = MainModel.instance;
        tideData = model.selectedTideData; //tideDataProvider.getTideData(model.getSelectedSpot().tidePortID);
    }


    public void paint(Canvas c, float ox, float oy, float visible, ParallaxHelper parallaxHelper) {
        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

        visible *= scrollerVisible.getValue();
        float hintsVisible = scrollerHints.getValue();

        float alpha = getAlpha(visible);

        int dh = metricsAndPaints.dh;
        paintFont.setTextSize(visible * metricsAndPaints.font);
        paintHintsFont.setTextSize(visible * metricsAndPaints.fontSmall);

        float fontH = visible * metricsAndPaints.fontH;

        if (tide == null) return;

        checkNowTide();

        float r = dh * visible; //(dh - DENSITY_DH_DEP) * visible + DENSITY_DH_DEP;

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * subZ);
        float x = pp.x, y = pp.y;

        float py = (float) (r / Math.sqrt(2));
        float nowy = py - py * 2 * tide / 250;
        float nowx = (float) (-Math.cos(Math.asin(nowy / r)) * r);

        setLocation(ox + nowx, oy + nowy);

        if (pathTide != null) {
            paintPathTide.setColor(alpha(alpha, colorWaterColor));
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
        paintFont.setColor(alpha(alpha, MetricsAndPaints.colorTideBG));
        c.drawCircle(x, y, dotR, paintFont);

        paintFont.setColor(alpha(alpha, 0xffffffff));

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
            paintHintsFont.setColor(alpha(alpha * hintsVisible * hintsVisible, 0xffffff));
            paintHintsFont.setTextAlign(Paint.Align.CENTER);

            hintsVisible *= visible;

            y += metricsAndPaints.fontSmallH / 12 * hintsVisible;

            c.drawText(STR_TIDE, x, y - fontH / 2 - metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);
            c.drawText(STR_M, x, y + fontH / 2 + metricsAndPaints.fontSmallH * visible + metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);

            y += metricsAndPaints.fontSmallH / 8 * hintsVisible;
        }

        c.drawText(strTide, x, y + fontH / 2, paintFont);
    }


    public void updateMetrics() {
        updateNowTide();
    }


    private void updateNowTide() {
        MainModel model = MainModel.instance;

        time = model.getSelectedTime();

        if (time < 0) return;

        Integer newTide = null;

        if (tideData != null) {
            newTide = tideData.getTide(model.getSelectedDayInt(), time);
            if (newTide != null) tide = newTide;
        }

//        if (model != null) Log.i(TAG, "updateNowTide " + model.selectedTime);

        if (tideData != null && tide != null && model.metricsAndPaints != null) {
            setVisible(true, true);
            float r = model.metricsAndPaints.dh;
            float width = r * 8;
            float py = (float) (r / Math.sqrt(2));
            float nowY = py - py * 2 * tide / 250;
            float nowX = (float) (-Math.cos(Math.asin(nowY / r)) * r);
            int nowH = time / 60;
            final float pathDX = width * (time - (nowH - 2) * 60) / 24 / 60 - nowX;

            Path pathTide = tideData.getPath2(model.getSelectedDayInt(), width * 9 / 24, py * 2, 0, 250, nowH - 2, nowH + 7);
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

        ((SurfSpotsMap) view).repaint();

        if (newTide == null) {
            setVisible(false, true);
        }
    }
}
