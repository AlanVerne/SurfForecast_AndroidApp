package com.avaa.surfforecast.views.Map;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.view.View;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.ParallaxHelper;

import static com.avaa.surfforecast.data.Common.STR_DASH;
import static com.avaa.surfforecast.data.Common.STR_FT;
import static com.avaa.surfforecast.data.Common.STR_S;
import static com.avaa.surfforecast.data.Common.STR_SWELL;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorWaveText;
import static com.avaa.surfforecast.views.ColorUtils.alpha;
import static com.avaa.surfforecast.views.Map.Arrow.createArrow;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class SwellCircle extends MapCircle {
    private static final int COLOR_SWELL_BG = MetricsAndPaints.colorWhite;

    private final Paint paintFontBig = new Paint(paintFont) {{
        setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }};

    private String strWaveHeight = STR_DASH;
    private String strWavePeriod = STR_DASH;
    private String strWaveAngleAbbr = STR_DASH;
    private float ftWidth;
    private float sWidth;
    private final FloatScroller angle;


    public SwellCircle(View view) {
        super(view);

        angle = new DirectionScroller(view);

        MainModel.instance.addChangeListener(
                MainModel.Change.SELECTED_CONDITIONS,
                changes -> update());

        update();
    }


    private void update() {
        final SurfConditions conditions = MainModel.instance.selectedConditions;

        if (conditions != null) {
            strWaveHeight = String.valueOf(conditions.getWaveHeightInFt());
            strWavePeriod = String.valueOf(conditions.wavePeriod);
            strWaveAngleAbbr = conditions.getWaveAngleAbbr();
            angle.to(conditions.waveAngle, scrollerVisible.getValue() != 0);
            setVisible(true, true);
        } else {
            setVisible(false, true);
        }
    }


    public void paint(Canvas c, float ox, float oy, float visible, ParallaxHelper parallaxHelper) {
        setLocation(ox, oy);

        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

        visible *= scrollerVisible.getValue();
        float hintsVisible = scrollerHints.getValue();

        float alpha = getAlpha(visible);

        int dh = metricsAndPaints.dh;
        paintFontBig.setTextSize(visible * metricsAndPaints.fontBig);
        paintFont.setTextSize(visible * metricsAndPaints.font);
        paintHintsFont.setTextSize(visible * metricsAndPaints.fontSmall);

        float fontH = visible * metricsAndPaints.fontH;
        float fontBigH = visible * metricsAndPaints.fontBigH;

        ftWidth = paintHintsFont.measureText(STR_FT);
        sWidth = paintHintsFont.measureText(STR_S);

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * (z + z) / 2);
        float x = pp.x, y = pp.y;

        float r = dh * visible; //(dh - metricsAndPaints.densityDHDependent) * visible + metricsAndPaints.densityDHDependent;
        r += visible * hintsVisible * dh / 3;

        float a = angle.getValue(); //conditions.waveAngle;

        paintBG.setColor(alpha(alpha, COLOR_SWELL_BG));
        c.drawPath(createArrow(x, y, a, r), paintBG);

        float strFtWidth = 0;
        float strSWidth = 0;

        float yDelta = (fontBigH + dh / 6 * visible + fontH) / 2;

        if (hintsVisible > 0) {
            paintHintsFont.setColor(alpha(alpha * hintsVisible * hintsVisible, colorWaveText));

            float additionalArrowSize = visible * dh / 4; //r*(SQRT_2-1)/SQRT_2/2;
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
            paintArrow.setStrokeWidth(metricsAndPaints.densityDHDependent);
            c.drawPath(pathLinedArrow, paintArrow);

            float strWaveHeightWidth = paintFontBig.measureText(strWaveHeight);
            float strWavePeriodWidth = paintFont.measureText(strWavePeriod);

            strFtWidth = this.ftWidth * hintsVisible; //paintAdditionalText.measureText(STR_FT) * hintsVisible;
            strSWidth = this.sWidth * hintsVisible; //paintAdditionalText.measureText(STR_S) * hintsVisible;

            y += dh / 24 * hintsVisible * visible;

            paintHintsFont.setTextAlign(Paint.Align.LEFT);
            c.drawText(STR_FT, x - strFtWidth / 3 + strWaveHeightWidth / 2, y - yDelta + fontBigH, paintHintsFont);
            c.drawText(STR_S, x - strSWidth / 3 + strWavePeriodWidth / 2, y + yDelta, paintHintsFont);

            paintHintsFont.setTextAlign(Paint.Align.CENTER);
            c.drawText(STR_SWELL, x, y - (fontBigH + visible * dh / 6 + fontH) / 2 - visible * dh / 6 * hintsVisible, paintHintsFont);
            c.drawText(strWaveAngleAbbr, x, y + (fontBigH + visible * dh / 6 + fontH) / 2 + visible * (dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintHintsFont);
            //c.drawText(conditions.waveEnergy+"kJ", x, y + (fontBigH + visible*dh/6 + fontH)/2 + visible*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
        }

        paintFontBig.setColor(alpha(alpha, colorWaveText));
        c.drawText(strWaveHeight, x - strFtWidth / 3, y - yDelta + fontBigH, paintFontBig);

        paintFont.setColor(alpha(alpha, colorWaveText));
        c.drawText(strWavePeriod, x - strSWidth / 3, y + yDelta, paintFont);
    }


    public boolean computeScroll() {
        boolean repaint = super.computeScroll();
        repaint |= angle.compute();
        return repaint;
    }


    public boolean hit(float x, float y) {
        x -= this.x;
        y -= this.y;
        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;
        return (x * x + y * y < metricsAndPaints.dh * metricsAndPaints.dh * 2);
    }
}
