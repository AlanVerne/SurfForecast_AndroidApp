package com.avaa.surfforecast.views.Map;


import android.content.Context;
import android.graphics.Paint;

import com.avaa.surfforecast.drawers.MetricsAndPaints;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class MapCircle {
    protected static final float DENSITY_DH_DEP = 3;
    protected static final float SQRT_2 = (float) Math.sqrt(2);

    protected FloatScroller scrollerVisible;
    protected FloatScroller scrollerHints;

    protected static float z = 0.5f;
    protected static float subZ = 0.2f;


    protected final Paint paintBG = new Paint() {{
        setAntiAlias(true);
        setStyle(Paint.Style.FILL);
    }};
    protected final Paint paintFont = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setTextAlign(Paint.Align.CENTER);
        setColor(MetricsAndPaints.colorWhite);
    }};
    protected final Paint paintHintsFont = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
    }};
    protected final Paint paintArrow = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setStyle(Style.STROKE);
        setStrokeJoin(Join.MITER);
        setStrokeWidth(DENSITY_DH_DEP);
    }};


    public MapCircle(Context context) {
        this.scrollerVisible = new LogicalScroller(context, 0);
        this.scrollerHints = new LogicalScroller(context, 1);
    }


    public boolean setVisible(boolean visible, boolean smooth) {
        return scrollerVisible.to(visible ? 1 : 0, smooth);
    }

    public boolean setHintsVisible(boolean visible, boolean smooth) {
        return scrollerHints.to(visible ? 1 : 0, smooth);
    }


    public boolean computeScroll() {
        boolean repaint = false;
        repaint |= scrollerVisible.compute();
        repaint |= scrollerHints.compute();
        return repaint;
    }


    protected float getAlpha(float visible) {
        return (float)(Math.pow(visible, 5));
    }
}
