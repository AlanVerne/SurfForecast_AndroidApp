package com.avaa.surfforecast.drawers;

import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by Alan on 25 Jan 2017.
 */

public class MetricsAndPaints {
    public static final float TEXT_K = 1.33333f;

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;

    public static int colorWindBG = 0xfff8f8f8; //0xff25c2e3;
    public static int colorWaveBG = 0xffffffff; //0xff188bc4;
    public static int colorTideBG = 0xFF006283; //0xFF005C86; //ff122D54; //0xff2e393d;
    public static int colorTideChartBG = 0xff0091c1;

    public static int colorWhite      = 0xffffffff;
    public static int colorMinorWhite = 0x88ffffff;

    public static int colorBlack      = 0xff000000;
    public static int colorMinorBlack = 0x33000000;

    public static int colorWindText = colorBlack;
    public static int colorWaveText = colorBlack;
    public static int colorTideText = colorWhite;

    public static final int MINUTES_IN_DAY = 24*60;

    public static final float BEZIER_CIRCLE_K = 1 - 0.5522f;

    // --

    public float density;
    public int dh;

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
    }};
    public final Paint paintFontSmall = new Paint(paintFont);
    public final Paint paintFontBig = new Paint(paintFont);

    public MetricsAndPaints(float density, int dh) {
        this.density = density;
        this.dh = dh;

        font = dh/2f;
        fontSmall = font/TEXT_K;
        fontBig = font*TEXT_K;
        fontHeader = font*TEXT_K*TEXT_K;
        fontHeaderBig = font*TEXT_K*TEXT_K*TEXT_K;

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

        fontHDiv2 = fontH/2;

        fontSmallSpacing = fontSmallH/2;
    }

    public static int getColorMinor(int color) {
        return color == colorWhite ? colorMinorWhite : colorMinorBlack;
    }
}
