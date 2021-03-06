package com.avaa.surfforecast.views.map;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.view.View;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.ParallaxHelper;

import static com.avaa.surfforecast.data.Common.STR_DASH;
import static com.avaa.surfforecast.data.Common.STR_KMH;
import static com.avaa.surfforecast.data.Common.STR_WIND;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorWindText;
import static com.avaa.surfforecast.views.ColorUtils.alpha;
import static com.avaa.surfforecast.views.map.Arrow.createArrow;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class WindCircle extends MapCircle {
    private static final int COLOR_WIND_BG = MetricsAndPaints.colorWhite;

    private static final float DEFAULT_WIND_ANGLE = (float) (Math.PI * 2 - Math.PI * 1 / 4);

    private String strWindSpeed = STR_DASH;

    private final FloatScroller angle;
    private final FloatScroller vbr;

    private int windLabelColor = colorWindText;


    public WindCircle(View view) {
        super(view);

        paintFont.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        angle = new DirectionScroller(view);
        vbr = new FloatScroller(view);

        final MainModel model = MainModel.instance;
        model.addChangeListener(
                MainModel.Change.SELECTED_CONDITIONS, MainModel.Change.SELECTED_RATING,
                changes -> update(model));

        update(model);
    }


    private void update(MainModel model) {
        int windSpeed = model.getSelectedWindSpeed();

        strWindSpeed = windSpeed == -1 ? STR_DASH : String.valueOf(windSpeed);

        RatedConditions selectedRatedConditions = model.getSelectedRatedConditions();
        windLabelColor = MetricsAndPaints.getWindColor(selectedRatedConditions);

        if (windSpeed != -1) {
            setVisible(true, true);

            float a = model.getSelectedWindAngle();
            boolean vbr = a < 0 || windSpeed <= 0;

            if (a < 0) a = DEFAULT_WIND_ANGLE;

            angle.to(a, true);
            this.vbr.to(vbr ? 1 : 0, true);
        } else {
            setVisible(false, true);
        }
    }


    public float getAngle() {
        return angle.getValue();
    }


    public void paint(Canvas c, float ox, float oy, float visible, ParallaxHelper parallaxHelper, float r) {
        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

        visible *= scrollerVisible.getValue();
        float hintsVisible = scrollerHints.getValue();

        float alpha = getAlpha(visible);

        float dh = metricsAndPaints.dh;
        paintFont.setTextSize(visible * metricsAndPaints.font);
        paintHintsFont.setTextSize(visible * metricsAndPaints.fontSmall);

        float fontH = visible * metricsAndPaints.fontH;

        float a = angle.getValue();
        boolean vbr = this.vbr.getValue() > 0.5;

        double cosA = Math.cos(a);
        double sinA = Math.sin(a);

        float windR = r * (1 + (1 - this.vbr.getValue()) * (2 - visible * 2)) - visible * hintsVisible * dh / 4;
        float windArrowR = (dh * 0.7f + dh * 0.25f * hintsVisible) * visible;
        float ax = ox + (float) (cosA * windR);
        float ay = oy - (float) (sinA * windR);

        setLocation(ax, ay);

        PointF pp = parallaxHelper.applyParallax(ax, ay, dh * z);
        ax = pp.x;
        ay = pp.y;

        paintBG.setColor(alpha(alpha, COLOR_WIND_BG));

        if (vbr) c.drawCircle(ax, ay, windArrowR, paintBG);
        else c.drawPath(createArrow(ax, ay, a, windArrowR), paintBG);

        paintFont.setColor(alpha(alpha, windLabelColor));

        if (hintsVisible > 0) {
            paintHintsFont.setColor(alpha(alpha * hintsVisible * hintsVisible, colorWindText));
            paintHintsFont.setTextAlign(Paint.Align.CENTER);

            if (!vbr) {
                float additionalArrowSize = visible * dh / 4; //r*(SQRT_2-1)/SQRT_2/2;
                windArrowR = windArrowR * SQRT_2 - additionalArrowSize * SQRT_2;
                //windArrowR = r/2 + hintsVisible*dh/4;
                float bx = ax - (float) Math.cos(a) * windArrowR;
                float by = ay + (float) Math.sin(a) * windArrowR;

                paintArrow.setColor(paintHintsFont.getColor());
                paintArrow.setStrokeWidth(metricsAndPaints.densityDHDependent);

                //float size = windArrowR*(SQRT_2-1)/SQRT_2; //hintsVisible*dh/4*SQRT_2;
                Path pathLinedArrow = LinedArrow.get(bx, by, a, additionalArrowSize);
                c.drawPath(pathLinedArrow, paintArrow);
            }

            hintsVisible *= visible;

            ay -= metricsAndPaints.fontSmallH / 16 * hintsVisible;

            c.drawText(STR_WIND, ax, ay - fontH / 2 - metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);
            c.drawText(STR_KMH, ax, ay + fontH / 2 + metricsAndPaints.fontSmallH * visible + metricsAndPaints.fontSmallSpacing * hintsVisible, paintHintsFont);

            ay += metricsAndPaints.fontSmallH / 12 * hintsVisible;
        }

        //paintFont.setColor(currentMETAR != null ? 0xff000000 : 0x88000000);
        c.drawText(strWindSpeed, ax, ay + fontH / 2, paintFont);

        //c.drawCircle(ax - dh*4/10, ay - fontH/2, fontH/6, paintFont);
    }


    public boolean computeScroll() {
        boolean repaint = super.computeScroll();
        repaint |= angle.compute();
        repaint |= vbr.compute();
        return repaint;
    }
}
