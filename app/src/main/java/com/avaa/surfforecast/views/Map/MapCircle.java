package com.avaa.surfforecast.views.Map;


import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.drawers.MetricsAndPaints;


/**
 * Created by Alan on 6 Sep 2017.
 */


public class MapCircle {
    protected static final float SQRT_2 = (float) Math.sqrt(2);

    protected static float z = 0.5f;
    protected static float subZ = 0.2f;

    protected final View view;

    protected FloatScroller scrollerVisible;
    protected FloatScroller scrollerHints;

    protected float x, y;


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
    }};


    public MapCircle(View view) {
        this.view = view;
        this.scrollerVisible = new LogicalScroller(view, 0);
        this.scrollerHints = new LogicalScroller(view, 1);
    }


    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public boolean setVisible(boolean visible, boolean smooth) {
//        Log.i("MapCircle", this.getClass().getName() + " " + visible);
        return scrollerVisible.to(visible ? 1 : 0, smooth);
    }


    public boolean isHintsVisible() {
        return scrollerHints.getDestination() == 1;
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
        return (float) (Math.pow(visible, 5));
    }


    public boolean hit(float x, float y) {
        x -= this.x;
        y -= this.y;
        MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;
        return (x * x + y * y < metricsAndPaints.dh * metricsAndPaints.dh);
    }
}
