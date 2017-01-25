package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.avaa.surfforecast.data.Direction;
import com.avaa.surfforecast.data.SurfConditions;

import java.util.Map;

import static com.avaa.surfforecast.drawers.MetricsAndPaints.*;

/**
 * Created by Alan on 25 May 2016.
 */

public class ConditionsBitmapsDrawer {
    private float density;

    private float rk = 0.7f;

    private float font;
    private int fontHDiv2;

    public int dh = 0;

    private Paint paintDirection;
    private Paint paintFontWavePeriod;
    private Paint paintFontBigBoldWaveHeight;
    private Paint paintFontWind;


    public ConditionsBitmapsDrawer(MetricsAndPaints metricsAndPaints) {
        this.density = metricsAndPaints.density;
        this.dh = metricsAndPaints.dh;

        paintDirection = new Paint() {{
            setAntiAlias(true);
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

        fontHDiv2 = metricsAndPaints.fontHDiv2;
    }


    public Bitmap drawWave(Map<Integer, SurfConditions> conditions, boolean vertical) {
        int width  = dh * 16;
        int height = dh * 3;

        int r = (int)(dh * rk);
        int circlesY = dh;

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        int colorMinorWindText = getColorMinor(colorWaveText);
        paintDirection.setColor(colorMinorWindText);
        Rect bounds = new Rect();
        paintFontBigBoldWaveHeight.getTextBounds("0", 0, 1, bounds);
        int whH = bounds.height();

        if (vertical) {
            c.save();
            c.translate(0, circlesY);
            c.rotate(-90);
        }

        for (Map.Entry<Integer, SurfConditions> ic : conditions.entrySet()) {
            int minute = ic.getKey();
            int x = width * (minute + 90) / MINUTES_IN_DAY;

            int tx = vertical ? 0-dh : x;
            int ty = vertical ? x : circlesY;

            SurfConditions iconditions = ic.getValue();

            double a = iconditions != null ? iconditions.waveAngle : -1;

            for (int i = 0; i < 8; i++) {
                double ai = i * Math.PI * 2 / 8;
                if (ai+0.1 < a || ai-0.1 > a) c.drawCircle(tx + (int)(Math.cos(ai) * r), ty - (int)(Math.sin(ai) * r), density, paintDirection);
            }

            if (iconditions == null) continue;

            //paintDirection.setColor(colorWaveText);
            paintDirection.setColor((minute >= 360 && minute <= 1080) ? colorWaveText : colorMinorWindText);

            float cosA = (float) Math.cos(a);
            float sinA = (float) Math.sin(a);
            float ax = tx + cosA * (r - density * 0);
            float ay = ty - sinA * (r - density * 0);

            drawArrow(c, ax, ay, (float) (-a * 180 / Math.PI), cosA, sinA, paintDirection);


            paintDirection.setColor(colorMinorWindText);

            int h2 = Math.round(iconditions.waveHeight * 3.28084f / 5.0f);

            paintFontWavePeriod.setColor(minute >= 360 && minute <= 1080 ? colorWaveText : colorMinorWindText);
            paintFontBigBoldWaveHeight.setColor(minute >= 360 && minute <= 1080 ? colorWaveText : colorMinorWindText);
            String sWaveHeight = String.valueOf(h2 / 2);
            if (h2 % 2 != 0) {
                //c.drawText(String.valueOf(h2 / 2) + "\u00BD", x, circlesY + whH / 2, paintFontBigBoldWaveHeight);
                c.drawText(sWaveHeight, tx, ty + whH / 2, paintFontBigBoldWaveHeight);
                Paint paintWaveHeightTextHalf = new Paint(paintFontBigBoldWaveHeight) {{
                    setColor(colorMinorWindText);
                    //setTextSize(density*font);
                    setTextAlign(Align.LEFT);
                    setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                }};
                paintFontBigBoldWaveHeight.getTextBounds(sWaveHeight, 0, sWaveHeight.length(), bounds);
                c.drawText("+", tx + (int)(bounds.width()*0.6), ty + whH / 2, paintWaveHeightTextHalf);
                //c.drawText("\u00BD", x + (int)(bounds.width()*0.6), circlesY + whH / 2, paintWaveHeightTextHalf);
            }
            else {
                c.drawText(sWaveHeight, tx, ty + whH / 2, paintFontBigBoldWaveHeight);
            }
            if (vertical) c.drawText(String.valueOf(iconditions.wavePeriod), tx + (int)(dh*1.5), ty + fontHDiv2, paintFontWavePeriod);
            else c.drawText(String.valueOf(iconditions.wavePeriod), tx, ty + (int)(dh*1.5) + fontHDiv2, paintFontWavePeriod);
        }

        return b;
    }

    public Bitmap drawWind(Map<Integer, SurfConditions> conditions, Direction offshore, boolean vertical) {
        int width  = dh * 16;
        int height = dh * 2;

        int r = (int)(dh * rk);
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

        for (Map.Entry<Integer, SurfConditions> ic : conditions.entrySet()) {
            int minute = ic.getKey();
            SurfConditions iconditions = ic.getValue();
            int x = width * (minute+90) / MINUTES_IN_DAY;

            int tx = vertical ? 0 : x;
            int ty = vertical ? x : circlesY;

            paintDirection.setColor(colorMinorWindText);
            Paint paintWave = new Paint(paintDirection) {{
                setStyle(Style.STROKE);
                setStrokeWidth(density);
                setStrokeCap(Cap.ROUND);
            }};

            double a  = iconditions != null ? iconditions.windAngle : -1;
            double a1 = offshore.ordinal() * Math.PI * 2 / 16 - Math.PI/2;
            double a2 = offshore.ordinal() * Math.PI * 2 / 16 + Math.PI/2;

            if (a1 < 0) a1 += Math.PI * 2;

            for (int i = 0; i < 8; i++) {
                double ai = i * Math.PI * 2 / 8;

                if (ai+0.1 < a || ai-0.1 > a) c.drawCircle(tx + (float)(Math.cos(ai) * r), ty - (float)(Math.sin(ai) * r), density, paintDirection);
//                c.drawLine(x + (int)(Math.cos(ai) * (r)), circlesY - (int)(Math.sin(ai) * (r)),
//                           x + (int)(Math.cos(ai) * (r-density*2)), circlesY - (int)(Math.sin(ai) * (r-density*2)), paintWave);
            }

            if (iconditions == null) continue;

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
            paintDirection.setColor(minute >= 360 && minute <= 1080 ? colorWindText : colorMinorWindText);
            float ax = tx + cosA * r;
            float ay = ty - sinA * r;

            drawArrow(c, ax, ay, (float)(-a*180/Math.PI), cosA, sinA, paintDirection);

            paintFontWind.setColor(minute >= 360 && minute <= 1080 ? colorWindText : colorMinorWindText);
            c.drawText(String.valueOf(iconditions.windSpeed), tx, ty + fontHDiv2, paintFontWind);
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
