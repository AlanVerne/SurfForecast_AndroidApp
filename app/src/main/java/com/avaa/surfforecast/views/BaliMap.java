package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.R;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.drawers.SVGPathToAndroidPath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.avaa.surfforecast.data.Common.*;

/**
 * Created by Alan on 9 Jul 2016.
 */

public class BaliMap extends View {
    private static final String TAG = "BaliMap";

    private static final android.view.animation.Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new android.support.v4.view.animation.FastOutSlowInInterpolator();

    private static final String STR_DASH = "-";

    private static final int colorSwellBG = 0xffffffff;
    private static final int colorWindBG  = 0xffffffff;

    private static final int colorSpotDot = 0xffffff;

    private int hintsVisiblePolicy = 2;
    private float hintsVisible = 1;
    private float hintsVisiblePrev = 1;

    private PowerManager powerManager;

    private SurfSpots surfSpots = null;
    private List<SurfSpot> surfSpotsList = new ArrayList<>();

    private Point pathTerrainSize = new Point();
    private Path pathTerrain = null;

    private SurfConditions currentConditions = null;
    private METAR currentMETAR = null;

    private String strWindSpeed = STR_DASH;
    private String strWaveHeight = STR_DASH;
    private String strWavePeriod = STR_DASH;

    private float shownI = 0;
    private float firstI = 0;
    private float lastI  = 0;

    private float awakenedState = 0;
    private float awakenedStatePrev = 0;
    
    private float densityDHDep = 3;

    private int colorWaterColor = 0xffacb5b8; //0xffa3b1b6; //0xff819faa; //0xff2e393d;

    private float windArrowVisible = 0;
    private float windArrowAngle = 0;
    private float windArrowVbr = 0;

    private float swellArrowVisible = 0;

    private float tideCircleVisible = 0;

    private ParallaxHelper parallaxHelper;

    private final Paint paintFont = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setTextAlign(Paint.Align.CENTER);
        setColor(MetricsAndPaints.colorWhite);
    }};
    private final Paint paintFontBig = new Paint(paintFont) {{
        setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }};
    private final Paint paintBG = new Paint() {{
        setAntiAlias(true);
        setStyle(Paint.Style.FILL);
    }};
    private final Paint paintTerrain = new Paint() {{
        setAntiAlias(true);
        setStyle(Paint.Style.FILL);
        setColor(0x11000000);
    }};
    private final Paint paintWaveLines = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setColor(0x88ffffff);
        setStyle(Style.STROKE);
        setStrokeCap(Cap.ROUND);
    }};
    private final Paint paintAdditionalText = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
    }};
    private final Paint paintAdditionalArrow = new Paint(paintAdditionalText) {{
        setStyle(Style.STROKE);
        setStrokeJoin(Join.MITER);
        setStrokeWidth(densityDHDep);
    }};
    private final Paint paintPathTide = new Paint() {{
        setAntiAlias(true);
        setColor(colorWaterColor);
        setStyle(Paint.Style.FILL);
    }};

    private Timer timerHintsHide = null;
    private Scroller scrollerHints;

    private int dh = 0;

    private MetricsAndPaints metricsAndPaints;

    private float fontBigH;
    private float fontH;

    
    public void setDh(int dh) {
        if (dh == 0) return;
        if (this.dh == dh) return;
        if (this.dh != 0) bmpMapZoomedOut.eraseColor(0x00000000);

        this.dh = dh;
        metricsAndPaints = AppContext.instance.metricsAndPaints;

        this.densityDHDep = metricsAndPaints.densityDHDependent;

        updateNowTide();

        bmpMapZoomedInForSpotI = -1;

        Canvas c = new Canvas(bmpMapZoomedOut);
        float scale = 2*dh/200f;
        matrix.setScale(scale, scale);
        pathTerrain.transform(matrix, pathTemp);
        c.drawPath(pathTemp, paintTerrain);

        Log.i(TAG, "setDh() | this.getHeight() = " + getHeight());
    }
    public void setHintsVisiblePolicy(int hintsVisiblePolicy) {
        if (this.hintsVisiblePolicy == hintsVisiblePolicy) return;
        if (hintsVisiblePolicy == 1 && awakenedState == 1) rescheduleHintsHide();
        this.hintsVisiblePolicy = hintsVisiblePolicy;
    }


    public BaliMap(Context context) {
        this(context, null);
    }
    public BaliMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public BaliMap(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);

        surfSpots = AppContext.instance.surfSpots;
        surfSpots.addChangeListener(c -> {
            Log.i(TAG, "surfSpots ChangeListener | metar: " + surfSpots.currentMETAR);
            currentConditions = surfSpots.currentConditions;
            currentMETAR = surfSpots.currentMETAR;

            if (currentMETAR != null) strWindSpeed = String.valueOf(currentMETAR.windSpeed);
            else if (currentConditions != null) strWindSpeed = String.valueOf(currentConditions.windSpeed);
            else strWindSpeed = STR_DASH;

            if (currentConditions != null) {
                strWaveHeight = String.valueOf(currentConditions.getWaveHeightInFt());
                strWavePeriod = String.valueOf(currentConditions.wavePeriod);
            }
            else {
                strWaveHeight = STR_DASH;
                strWavePeriod = STR_DASH;
            }

            updateTideData();
            repaint();
        });
        surfSpotsList = surfSpots.getAll();

        AppContext.instance.tideDataProvider.addListener(new TideDataProvider.TideDataProviderListener() {
            @Override
            public void updated(String portID) {
                if (portID == Common.BENOA_PORT_ID) {
                    updateTideData();
                    repaint();
                }
            }
            @Override
            public void loadingStateChanged(String portID, boolean loading) { }
        });

        currentConditions = surfSpots.currentConditions;
        currentMETAR = surfSpots.currentMETAR;

        scrollerHints = new Scroller(getContext());

        densityDHDep = getResources().getDisplayMetrics().density;

        AppContext.instance.userStat.addUserLevelListener(this::setHintsVisiblePolicy);

        setAwakenedState(1);

        updateTideData();

        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.bali_terrain);

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) result.write(buffer, 0, length);

            pathTerrain = SVGPathToAndroidPath.convert(result.toString("UTF-8"), pathTerrainSize);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        bmpMapZoomedOut = Bitmap.createBitmap(pathTerrainSize.x, pathTerrainSize.y, Bitmap.Config.ARGB_8888);

        parallaxHelper = new ParallaxHelper(this);
    }


    private Path pathCropMap = null;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (dh != 0) {
            if (bmpMapZoomedIn != null && bmpMapZoomedIn.getWidth() == getWidth() + dh * 4 && bmpMapZoomedIn.getHeight() == getHeight() + dh * 4) return;
            bmpMapZoomedIn = Bitmap.createBitmap(getWidth() + dh * 4, getHeight() + dh * 4, Bitmap.Config.ARGB_8888);
            bmpMapZoomedInForSpotI = -1;
            pathCropMap = new Region(0, 0, getWidth() + 4 * dh, getHeight() + 4 * dh).getBoundaryPath();
        }
    }


    public void resume() {
        if (!isPowerSavingMode()) parallaxHelper.resume();
    }
    public void stop() {
        parallaxHelper.stop();
    }


    private boolean isPowerSavingMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && powerManager.isPowerSaveMode();
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.i(TAG, "onTouchEvent() | " + ev.getAction());

        if (hintsVisiblePolicy == 1) {
            if (hintsVisible != 1) {
                if (isPowerSavingMode()) {
                    if (!scrollerHints.isFinished()) scrollerHints.abortAnimation();
                    hintsVisible = 1;
                    repaint();
                }
                else {
                    if (!scrollerHints.isFinished()) {
                        scrollerHints.abortAnimation();
                        int dx = 1000 - scrollerHints.getCurrX();
                        scrollerHints.startScroll(scrollerHints.getCurrX(), 0, dx, 0, isPowerSavingMode() ? 0 : 333 * dx / 1000);
                    } else {
                        scrollerHints.startScroll(0, 0, 1000, 0, isPowerSavingMode() ? 0 : 333);
                    }
                }
            }
            rescheduleHintsHide();
        }

        return super.onTouchEvent(ev);
    }


    @Override
    public void computeScroll() {
        //Log.i("BM", "computeScroll()");
        super.computeScroll();
        if (scrollerHints.computeScrollOffset()) {
            float newHV = scrollerHints.getCurrX() / 1000f;
            //if (hintsVisible == newHV)
            hintsVisible = newHV;
            repaint();
        }
    }


    private void cancelScheduledHintsHide() {
        if (timerHintsHide != null) {
            timerHintsHide.cancel();
            timerHintsHide.purge();
            timerHintsHide = null;
        }
    }
    private void rescheduleHintsHide() {
        //Log.i("BM", "rescheduleHintsHide()");
        cancelScheduledHintsHide();
        timerHintsHide = new Timer();
        timerHintsHide.schedule(new TimerTask() {
            synchronized public void run() {
                //Log.i("BM", "start hiding");
                if (isPowerSavingMode()) {
                    if (hintsVisible != 0) {
                        hintsVisible = 0;
                        repaint();
                    }
                } else {
                    scrollerHints.startScroll(1000, 0, -1000, 0, 666);
                    repaint();
                }
            }
        }, 10000);
    }
    public void setAwakenedState(float awakenedState) {
        awakenedState = FAST_OUT_SLOW_IN_INTERPOLATOR.getInterpolation(awakenedState);

        if (this.awakenedState == awakenedState) return;
        this.awakenedState = awakenedState;
        if (awakenedState == 0 && hintsVisiblePolicy > 0) {
            cancelScheduledHintsHide();
            scrollerHints.abortAnimation();
            hintsVisible = 1;
        }
        if (awakenedState == 1 && hintsVisiblePolicy == 1) {
            rescheduleHintsHide();
        }
        updateShownSpotsBoundRect();
        repaint();
    }
    public void show(float shownI, float firstI, float lastI, float awakenedState) {
        //Log.i("BM", shownI +" "+ firstI +" "+ lastI);

        if (this.shownI == shownI && this.firstI == firstI && this.lastI  == lastI && this.awakenedState == awakenedState) return;

        this.shownI = shownI;
        this.firstI = firstI;
        this.lastI  = lastI;

        if (this.awakenedState != awakenedState) {
            setAwakenedState(awakenedState);
        }
        else {
            updateShownSpotsBoundRect();
            repaint();
        }
    }

    float avex = 0;
    float avey = 0;

    RectF shownSpotsBoundRect = null;
    PointF shownSpotsAverage = null;
    private void updateShownSpotsBoundRect() {
        int i = 0;

        shownSpotsBoundRect = null;

        //if (prevAwakenedState >= awakenedState) {
            float sum = 0;

            avex = 0;
            avey = 0;

            for (SurfSpot spot : surfSpotsList) {
                float highlighted = isHighlighted(i);

                if (highlighted > 0) {
                    avex += spot.pointOnSVG.x * highlighted;
                    avey += spot.pointOnSVG.y * highlighted;
                    sum += highlighted;
                }

                i++;
            }

            //Log.i("BM", "updateShownSpotsBoundRect"+avex+" "+avey+" "+sum);

            if (sum == 0) {
                avex = 0;
                avey = 0;
            } else {
                avex /= sum;
                avey /= sum;
            }
        //}

        //prevAwakenedState = awakenedState;
//        double rad = 0;
//
//        i = 0;
//        for (SurfSpot spot : surfSpotsList) {
//            float highlighted = isHighlighted(i);
//            if (highlighted == 1) {
//                float dx = spot.pointOnSVG.avex - avex;
//                float dy = spot.pointOnSVG.avey - avey;
//                rad = Math.max(rad, Math.sqrt(dx*dx+dy*dy));
//            }
//            i++;
//        }

        float radf = 0;

        SurfSpot spot = surfSpots.selectedSpot();
        if (spot != null) {
            avex = awakenedState * spot.pointOnSVG.x + (1f - awakenedState) * avex;
            avey = awakenedState * spot.pointOnSVG.y + (1f - awakenedState) * avey;
        }

        shownSpotsAverage = new PointF(avex, avey);
        shownSpotsBoundRect = new RectF(avex-radf, avey-radf, avex+radf, avey+radf);
    }


    private final static float SQRT_2 = (float)Math.sqrt(2);
    private final Path pathArrow = new Path();
    private Path getArrow(float x, float y, float a, float arrowSize) {
        pathArrow.reset();
        pathArrow.moveTo(x - (float)Math.cos(a)*arrowSize* SQRT_2, y + (float)Math.sin(a)*arrowSize* SQRT_2);
        pathArrow.arcTo(new RectF(x-arrowSize, y-arrowSize, x+arrowSize, y+arrowSize), -a*180/(float)Math.PI + 45 + 180, 360 - 2*45, false);
        pathArrow.close();
        return pathArrow;
    }

    float circlesH = 0.5f;
    float subcirclesH = 0.2f;

    private final Path pathLinedArrow = new Path();
    private final Path pathSpotCircleBG = new Path();
    private final Path pathSpotCircleClip = new Path();

    private void paintSpotCircle(Canvas c, float ox, float oy, float r, float j) {
        PointF pp = parallaxHelper.applyParallax(ox, oy, dh*subcirclesH);
        float x = pp.x, y = pp.y;

        float a = (float)(surfSpots.selectedSpot().waveDirection.ordinal() * Math.PI * 2 / 16 + Math.PI);
        float aDegrees = (float)(-a*180/Math.PI);

        pathSpotCircleClip.reset();
        pathSpotCircleClip.addCircle(x, y, r, Path.Direction.CCW);

        paintBG.setColor(MetricsAndPaints.colorTideBG);

        final RectF oval = new RectF();
        oval.set(x - r, y - r, x + r, y + r);

        float aa = aDegrees + 90 + 90 - 63.4f;
        pathSpotCircleBG.reset();
        pathSpotCircleBG.arcTo(oval, aa, 2 * 63.4f, true);
        c.drawPath(pathSpotCircleBG, paintBG);

        paintBG.setColor(colorWaterColor);
        pathSpotCircleBG.reset();
        pathSpotCircleBG.arcTo(oval, aa, 2 * 63.4f - 360f, true);
        c.drawPath(pathSpotCircleBG, paintBG);

        c.save();
        c.clipPath(pathSpotCircleClip);
        c.translate(x, y);
        c.rotate(aDegrees + 90);

        paintWaveLines.setStrokeWidth(awakenedState*densityDHDep*2);

        int leftright = surfSpots.selectedSpot().leftright;
        if (leftright == 0) {
            PointF p1 = new PointF(0.20f, -0.70f);
            PointF p2 = new PointF(0.45f, -0.35f);
            PointF p3 = new PointF(0.70f, -0.0f);
            c.drawLine(p1.x*r, p1.y*r, -p1.x*r, p1.y*r, paintWaveLines);
            c.drawLine(p2.x*r, p2.y*r, -p2.x*r, p2.y*r, paintWaveLines);
            c.drawLine(p3.x*r, p3.y*r, -p3.x*r, p3.y*r, paintWaveLines);
        }
        else if (leftright == 2) {
            PointF p1 = new PointF(-0.11f, -0.70f);
            PointF p2 = new PointF(0.23f, -0.35f);
            PointF p3 = new PointF(0.45f, -0.0f);
            c.drawLine(p1.x*r, p1.y*r, -r, p1.y*r, paintWaveLines);
            c.drawLine(-p2.x*r, p2.y*r, r, p2.y*r, paintWaveLines);
            c.drawLine(p3.x*r, p3.y*r, -p3.x*r, p3.y*r, paintWaveLines);
        }
        else {
            PointF p1 = new PointF(-0.11f, -0.70f);
            PointF p2 = new PointF(0.23f, -0.35f);
            PointF p3 = new PointF(0.59f, -0.0f);
            c.drawLine(p1.x * r * leftright, p1.y * r, -1 * r * leftright, p1.y * r, paintWaveLines);
            c.drawLine(p2.x * r * leftright, p2.y * r, -1 * r * leftright, p2.y * r, paintWaveLines);
            c.drawLine(p3.x * r * leftright, p3.y * r, -1 * r * leftright, p3.y * r, paintWaveLines);
        }

        c.restore();

        if (currentConditions == null && currentMETAR == null) return;

        a = windArrowAngle;
        boolean vbr = windArrowVbr > 0.5;

        double cosA = Math.cos(a);
        double sinA = Math.sin(a);

        float windArrowVisibleFinal = windArrowVisible * awakenedState;

        float windR = r * (1 + (1 - windArrowVbr) * (2 - awakenedState * 2)) - awakenedState * hintsVisible * dh / 4;
        float windArrowR = windArrowVisibleFinal * (dh * 0.7f + hintsVisible * dh / 4);
        float ax = ox + (float)(cosA * windR);
        float ay = oy - (float)(sinA * windR);

        pp = parallaxHelper.applyParallax(ax, ay, dh * circlesH);
        ax = pp.x;
        ay = pp.y;

        paintBG.setColor(colorWindBG);

        if (vbr) c.drawCircle(ax, ay, windArrowR, paintBG);
        else c.drawPath(getArrow(ax, ay, a, windArrowR), paintBG);

        paintFont.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & MetricsAndPaints.colorWindText);

//        ay = ay + fontH/2;
//        if (hintsVisible > 0) {
//            ay -= dh/3/2 * hintsVisible;
//            c.drawText("km/h", ax, ay + dh / 3, paintAdditionalText);
//        }

        if (hintsVisible > 0) {
            paintAdditionalText.setColor((int)(j * hintsVisible * hintsVisible * 0xff) << 24 | 0x000000);
            paintAdditionalText.setTextAlign(Paint.Align.CENTER);

            if (!vbr) {
                float additionalArrowSize = windArrowVisibleFinal * dh / 4; //r*(SQRT_2-1)/SQRT_2/2;
                windArrowR = windArrowR * SQRT_2 - additionalArrowSize * SQRT_2;
                //windArrowR = r/2 + hintsVisible*dh/4;
                float bx = ax - (float)Math.cos(a) * windArrowR;
                float by = ay + (float)Math.sin(a) * windArrowR;

                //float additionalArrowSize = windArrowR*(SQRT_2-1)/SQRT_2; //hintsVisible*dh/4*SQRT_2;
                float cx = bx - (float)Math.cos(a - Math.PI * 3 / 4) * additionalArrowSize;
                float cy = by + (float)Math.sin(a - Math.PI * 3 / 4) * additionalArrowSize;
                float dx = bx - (float)Math.sin(a - Math.PI * 3 / 4) * additionalArrowSize;
                float dy = by - (float)Math.cos(a - Math.PI * 3 / 4) * additionalArrowSize;

                paintAdditionalArrow.setColor(paintAdditionalText.getColor());

                pathLinedArrow.reset();
                pathLinedArrow.moveTo(cx, cy);
                pathLinedArrow.lineTo(bx, by);
                pathLinedArrow.lineTo(dx, dy);

                c.drawPath(pathLinedArrow, paintAdditionalArrow);
            }

            ay -= metricsAndPaints.fontSmallH/16 * hintsVisible * windArrowVisibleFinal;
            c.drawText(STR_WIND, ax, ay - fontH/2 - windArrowVisibleFinal*metricsAndPaints.fontSmallSpacing * hintsVisible, paintAdditionalText);
            c.drawText(STR_KMH, ax, ay + fontH/2 + metricsAndPaints.fontSmallH + windArrowVisibleFinal*metricsAndPaints.fontSmallSpacing * hintsVisible, paintAdditionalText);
            ay += metricsAndPaints.fontSmallH/12 * hintsVisible * windArrowVisibleFinal;
        }

        //paintFont.setColor(currentMETAR != null ? 0xff000000 : 0x88000000);
        c.drawText(strWindSpeed, ax, ay + fontH/2, paintFont);

        //c.drawCircle(ax - dh*4/10, ay - fontH/2, fontH/6, paintFont);
    }


    private void paintSwellCircle(Canvas c, float ox, float oy, float r, float j) {
        if (currentConditions == null) return;

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh*(circlesH+subcirclesH)/2);
        float x = pp.x, y = pp.y;

        r += awakenedState*hintsVisible*dh/3;

        r *= swellArrowVisible;

        float a = currentConditions.waveAngle;

        paintBG.setColor(colorSwellBG);
        c.drawPath(getArrow(x, y, a, r), paintBG);

        float strFtWidth = 0;
        float strSWidth  = 0;

        if (hintsVisible > 0) {
            paintAdditionalText.setColor((int)(j * hintsVisible * hintsVisible * 0xff) << 24 | 0x000000);

            float additionalArrowSize = swellArrowVisible*awakenedState*dh/4; //r*(SQRT_2-1)/SQRT_2/2;
            float windArrowR = r*SQRT_2 - additionalArrowSize*SQRT_2;
            float bx = x - (float)Math.cos(a)*windArrowR;
            float by = y + (float)Math.sin(a)*windArrowR;

            float cx = bx - (float)Math.cos(a-Math.PI*3/4)*additionalArrowSize;
            float cy = by + (float)Math.sin(a-Math.PI*3/4)*additionalArrowSize;
            float dx = bx - (float)Math.sin(a-Math.PI*3/4)*additionalArrowSize;
            float dy = by - (float)Math.cos(a-Math.PI*3/4)*additionalArrowSize;

            pathLinedArrow.reset();
            pathLinedArrow.moveTo(cx, cy);
            pathLinedArrow.lineTo(bx, by);
            pathLinedArrow.lineTo(dx, dy);

            paintAdditionalArrow.setColor(paintAdditionalText.getColor());
            c.drawPath(pathLinedArrow, paintAdditionalArrow);

            float strWaveHeightWidth = paintFontBig.measureText(strWaveHeight);
            float strWavePeriodWidth = paintFont.measureText(strWavePeriod);

            strFtWidth = this.strFtWidth * hintsVisible; //paintAdditionalText.measureText(STR_FT) * hintsVisible;
            strSWidth = this.strSWidth * hintsVisible; //paintAdditionalText.measureText(STR_S) * hintsVisible;

            float finalVisibility = awakenedState * swellArrowVisible;
            y += dh / 24 * hintsVisible * finalVisibility;

            paintAdditionalText.setTextAlign(Paint.Align.LEFT);
            c.drawText(STR_FT, x - strFtWidth/3 + strWaveHeightWidth/2, y - (fontBigH + dh/6 + fontH)/2 + fontBigH, paintAdditionalText);
            c.drawText(STR_S,  x - strSWidth/3 + strWavePeriodWidth/2, y + (fontBigH + dh/6 + fontH)/2, paintAdditionalText);

            paintAdditionalText.setTextAlign(Paint.Align.CENTER);
            c.drawText(STR_SWELL, x, y - (fontBigH + finalVisibility*dh/6 + fontH)/2 - finalVisibility*dh / 6 * hintsVisible, paintAdditionalText);
            c.drawText(currentConditions.waveAngleAbbr(), x, y + (fontBigH + finalVisibility*dh/6 + fontH)/2 + finalVisibility*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
            //c.drawText(currentConditions.waveEnergy+"kJ", x, y + (fontBigH + finalVisibility*dh/6 + fontH)/2 + finalVisibility*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
        }

        paintFontBig.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & MetricsAndPaints.colorWaveText);
        c.drawText(strWaveHeight, x - strFtWidth/3, y - (fontBigH + dh/6 + fontH)/2 + fontBigH, paintFontBig);

        paintFont.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & MetricsAndPaints.colorWaveText);
        c.drawText(strWavePeriod, x - strSWidth/3, y + (fontBigH + dh/6 + fontH)/2, paintFont);
    }

    private long nowTideUpdatedTime;
    private Integer nowTide;
    private TideData tideData;
    private Path pathTide;
    private void checkNowTide() {
        long nowTime = System.currentTimeMillis();
        if (nowTideUpdatedTime + 60*1000 < nowTime) updateNowTide(nowTime);
    }
    private void updateNowTide() {
        updateNowTide(System.currentTimeMillis());
    }
    private void updateNowTide(long nowTime) {
        nowTideUpdatedTime = nowTime;
        nowTide = tideData == null ? null : tideData.getNow();

        if (tideData != null && nowTide != null) {
            int nowTimeInt = Common.getNowTimeInt(TIME_ZONE);
            float r = dh;
            float width = r * 8;
            float py = (float) (r / Math.sqrt(2));
            float nowy = py - py * 2 * nowTide / 250;
            float nowx = (float)(-Math.cos(Math.asin(nowy / r)) * r);
            int nowH = nowTimeInt / 60;
            final float pathDX = width * (nowTimeInt - (nowH - 2) * 60) / 24 / 60 - nowx;
            pathTide = tideData.getPath2(Common.getToday(TIME_ZONE), width * 9 / 24, py * 2, 0, 250, nowH - 2, nowH + 7);
            if (pathTide != null) {
                Matrix translateMatrix = new Matrix();
                translateMatrix.setTranslate(-pathDX, -py);
                pathTide.transform(translateMatrix);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Path pathCircle = new Path();
                    pathCircle.addCircle(0, 0, r, Path.Direction.CCW);
                    pathTide.op(pathCircle, Path.Op.INTERSECT);
                }
            }
        }

        if (nowTide == null) AppContext.instance.tideDataProvider.fetch(Common.BENOA_PORT_ID);
    }
    private void updateTideData() {
        tideData = AppContext.instance.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
        updateNowTide();
    }


    private void paintTideCircle(Canvas c, float ox, float oy, float j) {
        if (tideData == null || nowTide == null) return;

        checkNowTide();

        float finalVisibility = awakenedState * tideCircleVisible;
        float r = (dh-densityDHDep) * finalVisibility + densityDHDep;

        PointF pp = parallaxHelper.applyParallax(ox, oy, dh*subcirclesH);
        float x = pp.x, y = pp.y;

        float py   = (float)(r / Math.sqrt(2));
        float nowy = py - py*2*nowTide/250;
        float nowx = (float)(-Math.cos(Math.asin(nowy/r))*r);

        if (pathTide != null) {
            c.save();
            c.translate(x, y);
            c.scale(finalVisibility, finalVisibility);
            c.drawPath(pathTide, paintPathTide);
            c.restore();
        }

        // value

        pp = parallaxHelper.applyParallax(ox + nowx, oy + nowy, dh*circlesH);
        x = pp.x; y = pp.y;

        float dotR = finalVisibility * (dh * 0.7f + hintsVisible * dh / 4);
        paintFont.setColor(MetricsAndPaints.colorTideBG);
        c.drawCircle(x, y, dotR, paintFont);

        paintFont.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff);

        String strTide = String.valueOf(Math.round(nowTide/10f)/10f);

//        float strTideWidth = paintFont.measureText(strTide);
//        if (hintsVisible > 0) {
//            float strMWidth = paintAdditionalText.measureText(STR_M);
//            x -= strMWidth / 3f * hintsVisible;
//            paintAdditionalText.setColor((int)(hintsVisible*0xff)<<24 | 0xffffff);
//            paintAdditionalText.setTextAlign(Paint.Align.LEFT);
//            c.drawText(STR_M, x + strTideWidth / 2f, y, paintAdditionalText);
//        }

        if (hintsVisible > 0) {
            y += metricsAndPaints.fontSmallH/12 * hintsVisible * finalVisibility;
            paintAdditionalText.setColor((int)(j*hintsVisible*0xff)<<24 | 0xffffff);
            paintAdditionalText.setTextAlign(Paint.Align.CENTER);
            c.drawText(STR_TIDE, x, y - fontH/2 - finalVisibility*metricsAndPaints.fontSmallSpacing*hintsVisible, paintAdditionalText);
            c.drawText(STR_M, x, y + fontH/2 + metricsAndPaints.fontSmallH + finalVisibility*metricsAndPaints.fontSmallSpacing*hintsVisible, paintAdditionalText);
            y += metricsAndPaints.fontSmallH/8 * hintsVisible * finalVisibility;
        }

        c.drawText(strTide, x, y + fontH/2, paintFont);
    }

    private final Matrix matrix = new Matrix();
    private final Path pathTemp = new Path();
    private Bitmap bmpMapZoomedOut = null;
    private Bitmap bmpMapZoomedIn = null;
    private int bmpMapZoomedInForSpotI = -1;

    private static final float paddingTop = 4f;
    private static final float paddingBottom = 4.8f;

    private static final float rOut = 200;
    private static final float rIn  = 100;

    private final RectF rectfTemp = new RectF();


    private boolean computeArrowsAnimation() {
        boolean needRepaint = false;

        float windArrowVisibleDest = (currentConditions != null || currentMETAR != null) ? 1 : 0;
        if (isPowerSavingMode()) {
            if (windArrowVisible != windArrowVisibleDest) {
                windArrowVisible = windArrowVisibleDest;
                needRepaint = true;
            }
        }
        else {
            windArrowVisible += (windArrowVisibleDest - windArrowVisible) * 0.22;
            if (!needRepaint) if (windArrowVisible >= 0.01 || windArrowVisible <= 0.99) needRepaint = true;
        }

        if (currentConditions != null || currentMETAR != null) {
            float a = currentMETAR != null ? currentMETAR.windAngle : currentConditions.windAngle;
            boolean vbr = a < 0 || (currentMETAR != null ? currentMETAR.windSpeed : currentConditions.windSpeed) <= 0;

            if (a < 0) a = (float) (Math.PI * 2 - Math.PI * 1 / 4);

            if (windArrowVisible == 0) {
                windArrowAngle = a;
                windArrowVbr = vbr ? 1 : 0;
            } else {
                if (isPowerSavingMode() && windArrowAngle != a) {
                    windArrowAngle = a;
                    needRepaint = true;
                } else {
                    double dA = (a - windArrowAngle);
                    if (dA > Math.PI) {
                        windArrowAngle += Math.PI * 2;
                        dA -= Math.PI * 2;
                    } else if (dA < -Math.PI) {
                        windArrowAngle -= Math.PI * 2;
                        dA += Math.PI * 2;
                    }
                    windArrowAngle += dA * 0.05;
                    if (dA > 0.01) needRepaint = true;
                }

                if (isPowerSavingMode() && windArrowVbr != (vbr ? 1 : 0)) {
                    windArrowVbr = vbr ? 1 : 0;
                    needRepaint = true;
                } else {
                    double dVbr = ((vbr ? 1 : 0) - windArrowVbr) * 0.05;
                    windArrowVbr += dVbr;
                    if (dVbr > 0.01) needRepaint = true;
                }
            }
        }

        float swellArrowVisibleDest = (currentConditions != null ? 1 : 0);
        if (isPowerSavingMode()) {
            if (swellArrowVisible != swellArrowVisibleDest) {
                swellArrowVisible = swellArrowVisibleDest; needRepaint = true;
            }
        }
        else {
            swellArrowVisible += (swellArrowVisibleDest - swellArrowVisible) * 0.185;
            if (!needRepaint) if (swellArrowVisible >= 0.01 || swellArrowVisible <= 0.99) needRepaint = true;
        }

        float tideCircleVisibleDest = (tideData != null ? 1 : 0);
        if (isPowerSavingMode()) {
            if (tideCircleVisible != tideCircleVisibleDest) {
                tideCircleVisible = tideCircleVisibleDest; needRepaint = true;
            }
        }
        else {
            tideCircleVisible += (tideCircleVisibleDest - tideCircleVisible) * 0.13;
            if (!needRepaint) if (tideCircleVisible >= 0.01 || tideCircleVisible <= 0.99) needRepaint = true;
        }

        return needRepaint;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //if (true) return;
        //Log.i("BaliMap", "onDraw" + hintsVisible);

        super.onDraw(canvas);

        boolean needRepaint = computeArrowsAnimation();

        int h = getHeight() - (int)((paddingTop+paddingBottom) * dh);

        updateShownSpotsBoundRect();

        if (shownSpotsBoundRect == null) return;

        float scale = (1 - awakenedState) * dh*2 / rOut + awakenedState * h/rIn;

        float dx, dy;
        dx = -shownSpotsBoundRect.left * scale;
        dy = -shownSpotsBoundRect.top  * scale;

        dx += awakenedState * (getWidth() - (3*dh));
        dy += awakenedState * (paddingTop*dh + h/2);
        dx += (1-awakenedState) * (getWidth() - (3*dh));
        dy += (1-awakenedState) * (getHeight() - 2*dh) / 2;

        PointF pp = parallaxHelper.applyParallax(getWidth()/2, getHeight()/2, -dh*1.0f);
        pp.offset(-getWidth()/2, -getHeight()/2);

        if (awakenedState == 0) {
            canvas.drawBitmap(bmpMapZoomedOut, dx+pp.x, dy+pp.y, null);
        }
        else if (awakenedState == 1) {
            if (bmpMapZoomedIn != null) {
                if (bmpMapZoomedInForSpotI != surfSpots.selectedSpotI) {
                    bmpMapZoomedIn.eraseColor(0x00000000);
                    Canvas c = new Canvas(bmpMapZoomedIn);

                    matrix.setTranslate(dx + 2*dh, dy + 2*dh);
                    matrix.preScale(scale, scale);
                    pathTerrain.transform(matrix, pathTemp);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //TODO FIX
                        pathTemp.op(pathTemp, pathCropMap, Path.Op.INTERSECT);
                    }

                    c.drawPath(pathTemp, paintTerrain);

                    bmpMapZoomedInForSpotI = surfSpots.selectedSpotI;
                }
                canvas.drawBitmap(bmpMapZoomedIn, pp.x - 2 * dh, pp.y - 2 * dh, null);
            }
        }
        else {
            float s = scale/(dh*2f/rOut);
            rectfTemp.set(dx+pp.x, dy+pp.y, dx+pp.x + bmpMapZoomedOut.getWidth()*s, dy+pp.y + bmpMapZoomedOut.getHeight()*s);
            canvas.drawBitmap(bmpMapZoomedOut, null, rectfTemp, null);
        }

        if (awakenedState != awakenedStatePrev) {
            paintFontBig.setTextSize(awakenedState * metricsAndPaints.fontBig);
            fontBigH = awakenedState * metricsAndPaints.fontBigH;
            paintFont.setTextSize(awakenedState * metricsAndPaints.font);
            fontH = awakenedState * metricsAndPaints.fontH;
        }

        int selectedSpotI = surfSpots.selectedSpotI;

        pp = parallaxHelper.applyParallax(getWidth()/2, getHeight()/2, -dh*0.9f*(1-awakenedState));
        pp.offset(-getWidth()/2, -getHeight()/2);
        dx += pp.x; dy += pp.y;

        int i = 0;
        for (SurfSpot spot : surfSpotsList) {
            float highlighted = isHighlighted(i);
            if (highlighted > 0 && i != selectedSpotI) {
                float t = highlighted * (1f - awakenedState);
                float x = spot.pointOnSVG.x * scale + dx;
                float y = spot.pointOnSVG.y * scale + dy;
                float r = densityDHDep * highlighted * 1.5f;

                paintBG.setColor((int)(t * 0xff) * 0x1000000 + colorSpotDot);
                canvas.drawCircle(x, y, r, paintBG);
            }
            i++;
        }

        if (selectedSpotI != -1) {
            SurfSpot spot = surfSpots.selectedSpot();

            float x = spot.pointOnSVG.x * scale + dx;
            float y = spot.pointOnSVG.y * scale + dy;

            if (awakenedState == 0) {
                paintBG.setColor(0xff2696bb); //0xff000000 | colorSpotDot);
                canvas.drawCircle(x, y, densityDHDep * (2 + isHighlighted(selectedSpotI)), paintBG);
            }
            else {
                float r = (dh * 1.5f - densityDHDep * 3) * awakenedState + densityDHDep * 3;
                if (awakenedState > 0) {
                    paintSelectedSpot(canvas, x, y, r);
                }
                if (awakenedState < 0.2) {
                    paintBG.setColor(((int)((1f - awakenedState / 0.2f) * 0xff) << 24) | 0x2696bb); //colorWaterColor
                    canvas.drawCircle(x, y, r, paintBG);
                }
            }
        }

        awakenedStatePrev = awakenedState;
        hintsVisiblePrev = hintsVisible;

        if (needRepaint) repaint();
    }


    float strFtWidth;
    float strSWidth;
    private void paintSelectedSpot(Canvas canvas, float x, float y, float r) {
        //if (true) return;

        float hintsVisbleToAwakened = Math.max(0, Math.min((awakenedState - 0.66f) * 3f, 1f));

        final float finalHintsVisibleToAwakened = hintsVisbleToAwakened;

        if (awakenedState != awakenedStatePrev || hintsVisible != hintsVisiblePrev) paintAdditionalText.setColor((int)(hintsVisible * finalHintsVisibleToAwakened *0xff)<<24 | 0x00ffffff & MetricsAndPaints.colorWindText);
        if (awakenedState != awakenedStatePrev) {
            paintAdditionalText.setTextSize(awakenedState * metricsAndPaints.fontSmall);

            strFtWidth = paintAdditionalText.measureText(STR_FT);
            strSWidth = paintAdditionalText.measureText(STR_S);
        }

        hintsVisbleToAwakened = Math.max(0, Math.min((windArrowVisible * awakenedState - 0.66f) * 3f, 1f));
        paintSpotCircle(canvas, x, y, r, hintsVisbleToAwakened);

        float k = windArrowVisible * (float)Math.max(0, (Math.PI - Math.abs(windArrowAngle - Math.PI) - 2));
        x -= awakenedState * Math.max(0, k * dh / 2f);
        //Log.i(TAG, "wind spacing: " + a + " | " + k);

        x -= awakenedState * (1.5+1+0.75+0.25*hintsVisible) * dh;
        y += awakenedState * dh/2;

        hintsVisbleToAwakened = Math.max(0, Math.min((swellArrowVisible * awakenedState - 0.66f) * 3f, 1f));
        float smallr = (dh-densityDHDep) * swellArrowVisible * awakenedState + densityDHDep;
        paintSwellCircle(canvas, x, y, smallr, hintsVisbleToAwakened);

        x -= awakenedState * (1+1+0.5+0.5*hintsVisible) * dh;

        hintsVisbleToAwakened = Math.max(0, Math.min((tideCircleVisible * awakenedState - 0.66f) * 3f, 1f));
        paintTideCircle(canvas, x, y, hintsVisbleToAwakened);
    }


    public void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }


    private float isHighlighted(int i) {
        if (i >= Math.floor(firstI) && i <= Math.ceil(lastI)) {
            if (i < Math.ceil(firstI)) return 1 - (firstI - i);
            if (i > Math.floor(lastI)) return 1 - (i - lastI);
            return 1;
        }
        else return 0;
    }


    public void setAccentColor(int accentColor) {
        colorWaterColor = accentColor;
        paintPathTide.setColor(accentColor);
    }
}
