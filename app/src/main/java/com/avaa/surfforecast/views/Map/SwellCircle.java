package com.avaa.surfforecast.views.Map;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.ParallaxHelper;

import static com.avaa.surfforecast.data.Common.STR_FT;
import static com.avaa.surfforecast.data.Common.STR_S;
import static com.avaa.surfforecast.data.Common.STR_SWELL;
import static com.avaa.surfforecast.views.Map.BaliMap.STR_DASH;
import static com.avaa.surfforecast.views.Map.BaliMap.getArrow;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class SwellCircle extends MapCircle {
    private static final int COLOR_SWELL_BG = MetricsAndPaints.WHITE;

    private final Paint paintFontBig = new Paint(paintFont) {{
        setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }};

    private SurfConditions conditions = null;
    private String strWaveHeight = STR_DASH;
    private String strWavePeriod = STR_DASH;
    private float strFtWidth;
    private float strSWidth;
    private final FloatScroller angle;


    public SwellCircle(Context context) {
        super(context);

        angle = new DirectionScroller(context);

        MainModel model = MainModel.instance;

        model.addChangeListener(changes -> {
            conditions = model.selectedConditions;

            if (conditions != null) {
                setVisible(true, true);
                strWaveHeight = String.valueOf(conditions.getWaveHeightInFt());
                strWavePeriod = String.valueOf(conditions.wavePeriod);
                angle.to(conditions.waveAngle, true);
            } else {
                setVisible(false, true);
                strWaveHeight = STR_DASH;
                strWavePeriod = STR_DASH;
            }
        });
    }


    public void paint(Canvas c, float ox, float oy, float awakenedState, ParallaxHelper parallaxHelper, float r) {
        if (conditions == null) return;

        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

        int dh = metricsAndPaints.dh;
        paintFontBig.setTextSize(awakenedState * metricsAndPaints.fontBig);
        paintFont.setTextSize(awakenedState * metricsAndPaints.font);
        paintHintsFont.setTextSize(awakenedState * metricsAndPaints.fontSmall);

        float fontH = awakenedState * metricsAndPaints.fontH;
        float fontBigH = awakenedState * metricsAndPaints.fontBigH;

        float visible = scrollerVisible.getValue();
        float hintsVisible = scrollerHints.getValue();

        strFtWidth = paintHintsFont.measureText(STR_FT);
        strSWidth = paintHintsFont.measureText(STR_S);

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * (z + z) / 2);
        float x = pp.x, y = pp.y;

        r += awakenedState * hintsVisible * dh / 3;

        r *= visible;

        float a = angle.getValue(); //conditions.waveAngle;

        paintBG.setColor(COLOR_SWELL_BG);
        c.drawPath(getArrow(x, y, a, r), paintBG);

        float strFtWidth = 0;
        float strSWidth = 0;

        float j = 1;

        if (hintsVisible > 0) {
            paintHintsFont.setColor((int) (j * hintsVisible * hintsVisible * 0xff) << 24 | 0x000000);

            float additionalArrowSize = visible * awakenedState * dh / 4; //r*(SQRT_2-1)/SQRT_2/2;
            float windArrowR = r * SQRT_2 - additionalArrowSize * SQRT_2;
            float bx = x - (float) Math.cos(a) * windArrowR;
            float by = y + (float) Math.sin(a) * windArrowR;

            float cx = bx - (float) Math.cos(a - Math.PI * 3 / 4) * additionalArrowSize;
            float cy = by + (float) Math.sin(a - Math.PI * 3 / 4) * additionalArrowSize;
            float dx = bx - (float) Math.sin(a - Math.PI * 3 / 4) * additionalArrowSize;
            float dy = by - (float) Math.cos(a - Math.PI * 3 / 4) * additionalArrowSize;

            Path pathLinedArrow = new Path();
            pathLinedArrow.reset();
            pathLinedArrow.moveTo(cx, cy);
            pathLinedArrow.lineTo(bx, by);
            pathLinedArrow.lineTo(dx, dy);

            paintArrow.setColor(paintHintsFont.getColor());
            c.drawPath(pathLinedArrow, paintArrow);

            float strWaveHeightWidth = paintFontBig.measureText(strWaveHeight);
            float strWavePeriodWidth = paintFont.measureText(strWavePeriod);

            strFtWidth = this.strFtWidth * hintsVisible; //paintAdditionalText.measureText(STR_FT) * hintsVisible;
            strSWidth = this.strSWidth * hintsVisible; //paintAdditionalText.measureText(STR_S) * hintsVisible;

            float finalVisibility = awakenedState * visible;
            y += dh / 24 * hintsVisible * finalVisibility;

            paintHintsFont.setTextAlign(Paint.Align.LEFT);
            c.drawText(STR_FT, x - strFtWidth / 3 + strWaveHeightWidth / 2, y - (fontBigH + dh / 6 + fontH) / 2 + fontBigH, paintHintsFont);
            c.drawText(STR_S, x - strSWidth / 3 + strWavePeriodWidth / 2, y + (fontBigH + dh / 6 + fontH) / 2, paintHintsFont);

            paintHintsFont.setTextAlign(Paint.Align.CENTER);
            c.drawText(STR_SWELL, x, y - (fontBigH + finalVisibility * dh / 6 + fontH) / 2 - finalVisibility * dh / 6 * hintsVisible, paintHintsFont);
            c.drawText(conditions.waveAngleAbbr(), x, y + (fontBigH + finalVisibility * dh / 6 + fontH) / 2 + finalVisibility * (dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintHintsFont);
            //c.drawText(conditions.waveEnergy+"kJ", x, y + (fontBigH + finalVisibility*dh/6 + fontH)/2 + finalVisibility*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
        }

        paintFontBig.setColor((int) (j * 0xff) * 0x1000000 + 0x00ffffff & MetricsAndPaints.colorWaveText);
        c.drawText(strWaveHeight, x - strFtWidth / 3, y - (fontBigH + dh / 6 + fontH) / 2 + fontBigH, paintFontBig);

        paintFont.setColor((int) (j * 0xff) * 0x1000000 + 0x00ffffff & MetricsAndPaints.colorWaveText);
        c.drawText(strWavePeriod, x - strSWidth / 3, y + (fontBigH + dh / 6 + fontH) / 2, paintFont);
    }


    public boolean computeScroll() {
        boolean repaint = super.computeScroll();
        repaint |= angle.compute();
        return repaint;
    }
}
