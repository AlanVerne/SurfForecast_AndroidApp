package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateUtils;

import com.avaa.surfforecast.data.AstronomyProvider;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.Direction;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TidesProvider;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Created by Alan on 25 May 2016.
 */

public class ConditionsDrawer {
    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;

    private static final int MINUTES_IN_DAY = 24*60;

    private static final float BEZIER_CIRCLE_K = 1 - 0.5522f;

    public float density;

    public static int colorWindBG = 0xfff8f8f8; //0xff25c2e3;
    public static int colorWaveBG = 0xffffffff; //0xff188bc4;
    public static int colorTideBG = 0xFF006283; //0xFF005C86; //ff122D54; //0xff2e393d;
    public static int colorTideChartBG = 0xff0091c1;

    public static int colorWhite      = 0xffffffff;
    public static int colorMinorWhite = 0x66ffffff;

    public static int colorBlack      = 0xff000000;
    public static int colorMinorBlack = 0x33000000;

    public static int colorWindText = colorBlack;
    public static int colorWaveText = colorBlack;
    public static int colorTideText = colorWhite;


    public Paint paint = new Paint() {{
        setColor(WHITE);
        setStyle(Style.STROKE);
        setStrokeCap(Cap.BUTT);
        setStyle(Style.FILL);
        setAntiAlias(true);
    }};

    public ConditionsDrawer(float density) {
        this.density = density;
    }

    float rk = 0.7f;

    public float conditionsFontSize;
    public int conditionsFontH;

    public static int getConditionsFontSize(int dh) {
        return (int)(dh*0.5);
    }

    public int dh = 0;
    public void setDH(int dh) {
        this.dh = dh;
        conditionsFontSize = getConditionsFontSize(dh);

        paintDirection = new Paint() {{
            setAntiAlias(true);
        }};
        paintWavePeriod = new Paint() {{
            setAntiAlias(true);
            setColor(colorWaveText);
            setTextSize(conditionsFontSize);
            setTextAlign(Align.CENTER);
        }};
        paintWaveHeightText = new Paint() {{
            setAntiAlias(true);
            setTextSize((int)(conditionsFontSize * 1.25));
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            setTextAlign(Align.CENTER);
        }};
        paintWindText = new Paint() {{
            setAntiAlias(true);
            setColor(colorWindText);
            setTextSize(conditionsFontSize);
            setTextAlign(Align.CENTER);
        }};
        paintHourly3Tides = new Paint() {{
            setAntiAlias(true);
            setTextSize(conditionsFontSize);
            setColor(colorTideText);
            setTextAlign(Align.RIGHT);
        }};
        paintHourlyTides = new Paint(paintHourly3Tides) {{
            setColor(colorMinorTideText);
        }};
        paintHourly3Hour = new Paint(paintHourly3Tides) {{
            setColor(colorMinorTideText);
            setTextAlign(Align.RIGHT);
        }};

        Rect bounds = new Rect();
        paintHourly3Tides.getTextBounds("0", 0, 1, bounds);
        conditionsFontH = bounds.height();
    }
    int colorMinorTideText = getColorMinor(colorTideText);

    Paint paintDirection;
    Paint paintWavePeriod;
    Paint paintWaveHeightText;
    Paint paintWindText;
    Paint paintHourly3Tides;
    public Paint paintHourlyTides;
    Paint paintHourly3Hour;
//    Paint paintHourlyHour = new Paint(paintHourly3Hour) {{
//        //setTextSize(density*conditionsFontSize*0.5f);
//        setColor(0x00);
//    }};


    public Bitmap drawWave(Map<Integer, SurfConditions> conditions, boolean vertical) {
        int width  = dh * 16;
        int height = dh * 3;

        int r = (int)(dh * rk);
        int circlesY = dh;

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        int colorMinorWindText = getColorMinor(this.colorWaveText);
        paintDirection.setColor(colorMinorWindText);
        Rect bounds = new Rect();
        paintWaveHeightText.getTextBounds("0", 0, 1, bounds);
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

            paintWavePeriod.setColor(minute >= 360 && minute <= 1080 ? colorWaveText : colorMinorWindText);
            paintWaveHeightText.setColor(minute >= 360 && minute <= 1080 ? colorWaveText : colorMinorWindText);
            String sWaveHeight = String.valueOf(h2 / 2);
            if (h2 % 2 != 0) {
                //c.drawText(String.valueOf(h2 / 2) + "\u00BD", x, circlesY + whH / 2, paintWaveHeightText);
                c.drawText(sWaveHeight, tx, ty + whH / 2, paintWaveHeightText);
                Paint paintWaveHeightTextHalf = new Paint(paintWaveHeightText) {{
                    setColor(colorMinorWindText);
                    //setTextSize(density*conditionsFontSize);
                    setTextAlign(Align.LEFT);
                    setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                }};
                paintWaveHeightText.getTextBounds(sWaveHeight, 0, sWaveHeight.length(), bounds);
                c.drawText("+", tx + (int)(bounds.width()*0.6), ty + whH / 2, paintWaveHeightTextHalf);
                //c.drawText("\u00BD", x + (int)(bounds.width()*0.6), circlesY + whH / 2, paintWaveHeightTextHalf);
            }
            else {
                c.drawText(sWaveHeight, tx, ty + whH / 2, paintWaveHeightText);
            }
            if (vertical) c.drawText(String.valueOf(iconditions.wavePeriod), tx + (int)(dh*1.5), ty + conditionsFontH / 2, paintWavePeriod);
            else c.drawText(String.valueOf(iconditions.wavePeriod), tx, ty + (int)(dh*1.5) + conditionsFontH / 2, paintWavePeriod);
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

            paintWindText.setColor(minute >= 360 && minute <= 1080 ? colorWindText : colorMinorWindText);
            c.drawText(String.valueOf(iconditions.windSpeed), tx, ty + conditionsFontH / 2, paintWindText);
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

    public int getColorMinor(int color) {
        return color == colorWhite ? colorMinorWhite : colorMinorBlack;
    }

    public Bitmap drawTide(TideData tideData, int plusDays, boolean vertical) {
        int width = dh * 16;
        int height = dh * 4;
        int chartH = (int) (dh * 1.5);

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        AstronomyProvider.Times sunTimes = AstronomyProvider.getTimes();

        long day = Common.getDay(plusDays, Common.TIME_ZONE);

        final Path area = tideData.getPath(day, width, chartH*2, -300, 300);

        if (area == null) return null;

        Paint paintR = new Paint() {{
            setColor(colorTideChartBG); //colorTideBG);//0xff38a0d0);//AVE_BLUE);
            setAntiAlias(true);
        }};

        c.save();
        c.drawPath(area, paintR);

        int firstlight = sunTimes.TwiS * width / MINUTES_IN_DAY;
        int lastlight = sunTimes.TwiE * width / MINUTES_IN_DAY;
        int sunrise = sunTimes.Sunrise * width / MINUTES_IN_DAY;
        int sunset = sunTimes.Sunset * width / MINUTES_IN_DAY;

        //Region sunReg = new Region(sunrise, 0, sunset, height);
        Region firstLightReg = new Region(firstlight, 0, sunrise, height);
        firstLightReg.op(sunset, 0, lastlight, height, Region.Op.UNION);
        Region nightReg = new Region(0, 0, firstlight, height);
        nightReg.op(lastlight, 0, width, height, Region.Op.UNION);

        paintR.setColor(0x11000000);
        c.save();
        c.clipPath(firstLightReg.getBoundaryPath());
        c.drawPath(area, paintR);
        c.restore();
        paintR.setColor(0x22000000);
        c.save();
        c.clipPath(nightReg.getBoundaryPath());
        c.drawPath(area, paintR);
        c.restore();

        c.restore();


        SortedMap<Integer, Integer> hourlyTides = tideData.getHourly(day, 5, 19); //4, 20);

        if (hourlyTides != null) {
            c.save();
            c.rotate(-90);
            for (Map.Entry<Integer, Integer> entry : hourlyTides.entrySet()) {
                int h = entry.getKey();
                boolean h3 = h % 3 == 0;
                Point point = new Point(width * entry.getKey() / 24, chartH - chartH * entry.getValue() / 300);// + hourlyHoursHeight*2 + hourlyHoursWidth);

                String strTide = String.valueOf(Math.round(entry.getValue() / 10.0) / 10.0);
                //if (h3) {
                //String strH = String.valueOf(h);// + ":00";
                c.drawText(strTide, -point.y - dh * 0.8f, point.x + conditionsFontH / 2, h3 ? paintHourly3Tides : paintHourlyTides);
                //}
                //c.drawText(strTide, -point.y + hourlyHoursHeight, point.x + hourlyHoursHeight/2, h3 ? paintHourly3Hour : paintHourlyHour);
                // - hourlyHoursHeight
                //if (h<10) strH = " " + strH;

                //if (vertical) c.drawText(strH, -height + dh/2 + hourlyHoursWidth, point.x + hourlyHoursHeight/2, h3 ? paintHourly3Hour : paintHourlyHour);
            }
            c.restore();
        }

//        if (!vertical) {
//            paintHourlyHour.setTextAlign(Paint.Align.CENTER);
//            paintHourly3Hour.setTextAlign(Paint.Align.CENTER);
//            for (int h = 5; h < 19; h++) {
//                boolean h3 = h % 3 == 0;
//                Point point = new Point(width * h / 24, height - dh / 2);// + hourlyHoursHeight*2 + hourlyHoursWidth);
//
//                String strH = String.valueOf(h);
//
//                c.drawText(strH, point.x, point.y, h3 ? paintHourly3Hour : paintHourlyHour);
//            }
//        }

        return b;
    }
}
