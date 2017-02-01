package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.Direction;
import com.avaa.surfforecast.data.SurfConditions;

import java.util.Map;

import static com.avaa.surfforecast.drawers.MetricsAndPaints.*;

/**
 * Created by Alan on 25 May 2016.
 */

public class SurfConditionsOneDayBitmapsDrawer {
    private static final float RK = 0.7f;

    private final float density;
    private final int dh;

    private final boolean drawMeasures;

    private final int fontHDiv2;
    private final int fontBigHDiv2;

    private final Paint paintDirection;
    private final Paint paintWave;
    private final Paint paintFontWavePeriod;
    private final Paint paintFontBigBoldWaveHeight;
    private final Paint paintFontWind;

    private final int colorMinorWindText;
    private final Paint paintWaveHeightTextHalf;

    private final Paint paintFontSmallBlack;
    private final Paint paintFontSmallGray;


    public SurfConditionsOneDayBitmapsDrawer(MetricsAndPaints metricsAndPaints) {
        this.density = metricsAndPaints.density;
        this.dh = metricsAndPaints.dh;

        drawMeasures = AppContext.instance.userStat.userLevel == 2;

        paintDirection = new Paint() {{
            setAntiAlias(true);
        }};
        paintWave = new Paint(paintDirection) {{
            setStyle(Style.STROKE);
            setStrokeWidth(density);
            setStrokeCap(Cap.ROUND);
        }};

        paintFontWavePeriod = new Paint() {{
            setAntiAlias(true);
            setColor(colorWaveText);
            setTextSize(metricsAndPaints.font);
            setTextAlign(Align.CENTER);
        }};

        paintFontBigBoldWaveHeight = new Paint() {{
            setAntiAlias(true);
            setTextSize(metricsAndPaints.fontBig);
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            setTextAlign(Align.CENTER);
        }};

        paintFontWind = new Paint() {{
            setAntiAlias(true);
            setColor(colorWindText);
            setTextSize(metricsAndPaints.font);
            setTextAlign(Align.CENTER);
        }};

        colorMinorWindText = getColorMinor(colorWaveText);

        paintWaveHeightTextHalf = new Paint(paintFontBigBoldWaveHeight) {{
            setColor(colorMinorWindText);
            //setTextSize(density*font);
            setTextAlign(Align.LEFT);
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        }};

        paintFontSmallBlack = new Paint(metricsAndPaints.paintFontSmall);
        paintFontSmallBlack.setTextAlign(Paint.Align.LEFT);
        paintFontSmallGray = new Paint(paintFontSmallBlack);
        paintFontSmallGray.setColor(colorMinorBlack);

        fontHDiv2 = metricsAndPaints.fontHDiv2;
        fontBigHDiv2 = metricsAndPaints.fontBigH / 2;
    }


    public Bitmap getBitmapForWarming() {
        return Bitmap.createBitmap(dh * 16, dh * 4, Bitmap.Config.ARGB_8888);
    }


    public Bitmap drawWave(Map<Integer, SurfConditions> surfConditionsOneDay, boolean vertical) {
        int width  = dh * 16;
        int height = dh * 3;

        int r = (int)(dh * RK);
        int circlesY = dh;

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        if (vertical) {
            c.save();
            c.translate(0, circlesY);
            c.rotate(-90);
        }

        for (Map.Entry<Integer, SurfConditions> entry : surfConditionsOneDay.entrySet()) {
            int minute = entry.getKey();
            int x = width * (minute + 90) / MINUTES_IN_DAY;

            boolean isDay = minute >= 360 && minute <= 1080;

            int tx = vertical ? 0-dh : x;
            int ty = vertical ? x : circlesY;

            SurfConditions surfConditions = entry.getValue();

            double a = surfConditions != null ? surfConditions.waveAngle : -1;

            paintDirection.setColor(colorMinorWindText);

            for (int i = 0; i < 8; i++) {
                double ai = i * Math.PI * 2 / 8;
                if (ai+0.1 < a || ai-0.1 > a) c.drawCircle(tx + (int)(Math.cos(ai) * r), ty - (int)(Math.sin(ai) * r), density, paintDirection);
            }

            if (surfConditions == null) continue;


            if (isDay) paintDirection.setColor(colorWaveText);

            float cosA = (float) Math.cos(a);
            float sinA = (float) Math.sin(a);
            float ax = tx + cosA * (r - density * 0);
            float ay = ty - sinA * (r - density * 0);

            drawArrow(c, ax, ay, (float) (-a * 180 / Math.PI), cosA, sinA, paintDirection);


            int h2 = Math.round(surfConditions.waveHeight * 3.28084f / 5.0f);

            paintFontWavePeriod.setColor(isDay ? colorWaveText : colorMinorWindText);
            paintFontBigBoldWaveHeight.setColor(isDay ? colorWaveText : colorMinorWindText);
            String sWaveHeight = String.valueOf(h2 / 2);
            if (h2 % 2 != 0 && !drawMeasures) {
                //c.drawText(String.valueOf(h2 / 2) + "\u00BD", x, circlesY + whH / 2, paintFontBigBoldWaveHeight);
                c.drawText(sWaveHeight, tx, ty + fontBigHDiv2, paintFontBigBoldWaveHeight);

                float w = paintFontBigBoldWaveHeight.measureText(sWaveHeight);
                c.drawText("+", tx + w*0.5f, ty + fontBigHDiv2, paintWaveHeightTextHalf);
                //c.drawText("\u00BD", x + (int)(bounds.width()*0.6), circlesY + whH / 2, paintWaveHeightTextHalf);
            }
            else {
                c.drawText(sWaveHeight, tx, ty + fontBigHDiv2, paintFontBigBoldWaveHeight);
            }

            if (drawMeasures) {
                float w = paintFontBigBoldWaveHeight.measureText(sWaveHeight);
                c.drawText(Common.strFT, tx + w*0.5f, ty + fontBigHDiv2, isDay ? paintFontSmallBlack : paintFontSmallGray);
            }

            String sWavePeriod = String.valueOf(surfConditions.wavePeriod);

            if (drawMeasures && vertical) tx -= dh*0.125;

            if (vertical) c.drawText(sWavePeriod, tx + (int)(dh*1.5), ty + fontHDiv2, paintFontWavePeriod);
            else c.drawText(sWavePeriod, tx, ty + (int)(dh*1.5) + fontHDiv2, paintFontWavePeriod);

            if (drawMeasures) {
                float w = paintFontWavePeriod.measureText(sWavePeriod);
                if (vertical) c.drawText(Common.strS, tx + w*0.5f + dh*1.5f, ty + fontHDiv2, isDay ? paintFontSmallBlack : paintFontSmallGray);
                else c.drawText(Common.strS, tx + w*0.5f, ty + dh*1.5f + fontHDiv2, isDay ? paintFontSmallBlack : paintFontSmallGray);
            }
        }

        return b;
    }


    public Bitmap drawWind(Map<Integer, SurfConditions> surfConditionsOneDay, Direction offshore, boolean vertical) {
        int width  = dh * 16;
        int height = dh * 2;

        int r = (int)(dh * RK);
        int circlesY = dh;

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        int colorMinorWindText = getColorMinor(colorWindText);
        paintDirection.setColor(colorMinorWindText);


        if (vertical) {
            c.save();
            c.translate(0, circlesY);
            c.rotate(-90);
        }

        for (Map.Entry<Integer, SurfConditions> entry : surfConditionsOneDay.entrySet()) {
            int minute = entry.getKey();
            SurfConditions surfConditions = entry.getValue();

            boolean isDay = minute >= 360 && minute <= 1080;

            int x = width * (minute+90) / MINUTES_IN_DAY;

            int tx = vertical ? 0 : x;
            int ty = vertical ? x : circlesY;

            paintDirection.setColor(colorMinorWindText);
            paintWave.setColor(colorMinorWindText);

            double a  = surfConditions != null ? surfConditions.windAngle : -1;
            double a1 = offshore.ordinal() * Math.PI * 2 / 16 - Math.PI/2;
            double a2 = offshore.ordinal() * Math.PI * 2 / 16 + Math.PI/2;

            if (a1 < 0) a1 += Math.PI * 2;

            for (int i = 0; i < 8; i++) {
                double ai = i * Math.PI * 2 / 8;

                if (ai+0.1 < a || ai-0.1 > a) c.drawCircle(tx + (float)(Math.cos(ai) * r), ty - (float)(Math.sin(ai) * r), density, paintDirection);
//                c.drawLine(x + (int)(Math.cos(ai) * (r)), circlesY - (int)(Math.sin(ai) * (r)),
//                           x + (int)(Math.cos(ai) * (r-density*2)), circlesY - (int)(Math.sin(ai) * (r-density*2)), paintWave);
            }

            if (surfConditions == null) continue;

            float cosA = (float)Math.cos(a1);
            float sinA = (float)Math.sin(a1);

            float sr = r - density*7;
            float br = r - density*4;

            c.drawLine(tx + cosA * br, ty - sinA * br, tx + cosA * sr, ty - sinA * sr, paintWave);

            cosA = (float)Math.cos(a2);
            sinA = (float)Math.sin(a2);
            c.drawLine(tx + cosA * br, ty - sinA * br, tx + cosA * sr, ty - sinA * sr, paintWave);
            
            cosA = (float)Math.cos(a);
            sinA = (float)Math.sin(a);

            paintDirection.setColor(isDay ? colorWindText : colorMinorWindText);
            float ax = tx + cosA * r;
            float ay = ty - sinA * r;

            drawArrow(c, ax, ay, (float)(-a*180/Math.PI), cosA, sinA, paintDirection);

            paintFontWind.setColor(isDay ? colorWindText : colorMinorWindText);
            c.drawText(String.valueOf(surfConditions.windSpeed), tx, ty + fontHDiv2, paintFontWind);
        }

        if (vertical) c.restore();

        return b;
    }


    private void drawArrow(Canvas c, float x, float y, float aInDegrees, float cosA, float sinA, Paint paint) {
        float arrowSize = density * 3;
        Path p = new Path();
        p.moveTo(x - cosA*arrowSize*2, y + sinA*arrowSize*2);
        p.arcTo(new RectF(x-arrowSize, y-arrowSize, x+arrowSize, y+arrowSize), aInDegrees + 60 - 180, 240, false);
        p.close();
        c.drawPath(p, paint);
    }
}
