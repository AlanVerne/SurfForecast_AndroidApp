package com.avaa.surfforecast.drawers;


import android.graphics.Paint;
import android.graphics.Rect;

import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;


/**
 * Created by Alan on 25 Jan 2017.
 */


public class MetricsAndPaints {
    public static final float TEXT_K = 1.33333f;

    public static int colorWindBG = 0xfff8f8f8; //0xff25c2e3;
    public static int colorWaveBG = 0xffffffff; //0xff188bc4;
    public static int colorTideBG = 0xFF006283; //0xFF005C86; //ff122D54; //0xff2e393d;
    public static int colorTideChartBG = 0xff0091c1;

    public static int colorRed = 0xffe40036;
    public static int colorGray = 0xff888888;
    public static int colorGreen = 0xff01be7d;

    public static int colorWhite = 0xffffffff;
    public static int colorMinorWhite = 0x88ffffff;

    public static int colorBlack = 0xff003545;
    public static int colorMinorBlack = 0x33000000;

    public static int colorMidBlack = 0x66000000;

    public static int colorWindText = colorBlack;
    public static int colorWaveText = colorBlack;
    public static int colorTideText = colorWhite;

    public static final int MINUTES_IN_DAY = 24 * 60;

    public static final float BEZIER_CIRCLE_K = 1 - 0.5522f;

    // --

    public float density;
    public int dh;

    public float densityDHDependent;

    public float fontSmall;
    public float font;
    public float fontBig;
    public float fontHeader;
    public float fontHeaderBig;

    public int fontSmallH;
    public int fontH;
    public int fontBigH;

    public int fontHDiv2;

    public int fontSmallSpacing;

    public final Paint paintFont = new Paint() {{
        setAntiAlias(true);
        setTextAlign(Align.CENTER);
        setColor(colorBlack);
    }};
    public final Paint paintFontSmall = new Paint(paintFont);
    public final Paint paintFontBig = new Paint(paintFont);


    public MetricsAndPaints(float density, int dh) {
        this.density = density;
        this.dh = dh;

        this.densityDHDependent = dh / 27.67f;

        font = dh / 2f;
        fontSmall = font / TEXT_K;
        fontBig = font * TEXT_K;
        fontHeader = font * TEXT_K * TEXT_K;
        fontHeaderBig = font * TEXT_K * TEXT_K * TEXT_K;

        paintFontSmall.setTextSize(fontSmall);
        paintFont.setTextSize(font);
        paintFontBig.setTextSize(fontBig);

        Rect bounds = new Rect();

        paintFontSmall.getTextBounds("0", 0, 1, bounds);
        fontSmallH = bounds.height();
        paintFont.getTextBounds("0", 0, 1, bounds);
        fontH = bounds.height();
        paintFontBig.getTextBounds("0", 0, 1, bounds);
        fontBigH = bounds.height();

        fontHDiv2 = fontH / 2;

        fontSmallSpacing = fontSmallH / 2;
    }


    public static int getColorMinor(int color) {
        return color == colorWhite ? colorMinorWhite : colorMinorBlack;
    }


    public static int getTextColorForEnergy(int energy) {
        return energy > 1450 ? colorBlack : colorWhite;
    }

    public static int getColorForEnergy(int energy) {
        int r, g, b;
        if (energy <= 500) {
            r = 0;
            g = 0x17 * energy / 500;
            b = 0xff * energy / 500;
        } else if (energy <= 1000) {
            r = 0;
            g = 0x17 + (0xa1 - 0x17) * (energy - 500) / 500;
            b = 0xff;
        } else if (energy <= 1500) {
            energy -= 1000;
            r = 0x51 * energy / 500;
            g = 0xa1 + (0xc1 - 0xa1) * energy / 500;
            b = 0xff + (0xf6 - 0xff) * energy / 500;
        } else if (energy <= 2000) {
            energy -= 1500;
            r = 0x51 + (0xc1 - 0x51) * energy / 500;
            g = 0xc1 + (0xe9 - 0xc1) * energy / 500;
            b = 0xf6 + (0xea - 0xf6) * energy / 500;
        } else if (energy <= 2500) {
            energy -= 2000;
            r = 0xc1 + (0xff - 0xc1) * energy / 500;
            g = 0xe9 + (0xff - 0xe9) * energy / 500;
            b = 0xea + (0xd7 - 0xea) * energy / 500;
        } else if (energy <= 5000) {
            energy -= 2500;
            r = 0xff;
            g = 0xff;
            b = 0xd7 + (0x98 - 0xd7) * energy / 2500;
        } else {
            ;// if (energy <= 10000) {
            energy -= 5000;
            r = 0xff;
            g = 0xff + (0xa2 - 0xff) * energy / 5000;
            b = 0x98 + (0x00 - 0x98) * energy / 5000;
        }

        return r * 0x10000 + g * 0x100 + b;
    }


    // Wind color


    public static int getWindColor(RatedConditions c) {
        float windRating = c != null ? c.windRating : 0.6f;
        return getWindColor(windRating);
    }

    public static int getWindColor(SurfConditions c, SurfSpot s) {
        float windRating = RatedConditions.rateWind(c, s);
        return getWindColor(windRating);
    }

    public static int getWindColor(float windRating) {
        return windRating < 0.5 ? colorRed : windRating > 0.8 ? colorGreen : colorGray;
    }
}
