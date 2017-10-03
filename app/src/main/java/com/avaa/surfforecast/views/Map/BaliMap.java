package com.avaa.surfforecast.views.Map;

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
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.R;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.drawers.SVGPathToAndroidPath;
import com.avaa.surfforecast.views.ParallaxHelper;
import com.avaa.surfforecast.views.RatingView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Alan on 9 Jul 2016.
 */


public class BaliMap extends View {
    private static final String TAG = "BaliMap";

    private static final android.view.animation.Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new android.support.v4.view.animation.FastOutSlowInInterpolator();

    public static final String STR_DASH = "-";

    private static final int colorSpotDot = 0xffffff;

    private int hintsVisiblePolicy = 2;

    private PowerManager powerManager;

    private List<SurfSpot> surfSpotsList = new ArrayList<>();

    private Point pathTerrainSize = new Point();
    private Path pathTerrain = null;

    private float shownI = 0;
    private float firstI = 0;
    private float lastI = 0;

    private float awakenedState = 0;
    private float awakenedStatePrev = 0;

    private float densityDHDep = 3;

    private int colorWaterColor = 0xffacb5b8; //0xffa3b1b6; //0xff819faa; //0xff2e393d;

    private ParallaxHelper parallaxHelper;

    private final Paint paintFont = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setTextAlign(Paint.Align.CENTER);
        setColor(MetricsAndPaints.colorWhite);
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


    private Timer timerHintsHide = null;
    private Scroller scrollerHints;

    private int dh = 0;

    private MetricsAndPaints metricsAndPaints;


    private MainModel model = null;


    public void setDh(int dh) {
        if (dh == 0) return;
        if (this.dh == dh) return;
        if (this.dh != 0) bmpMapZoomedOut.eraseColor(0x00000000);

        this.dh = dh;
        metricsAndPaints = MainModel.instance.metricsAndPaints;

        this.densityDHDep = metricsAndPaints.densityDHDependent;

        bmpMapZoomedInForSpotI = -1;

        Canvas c = new Canvas(bmpMapZoomedOut);
        float scale = 2 * dh / 200f;
        matrix.setScale(scale, scale);
        pathTerrain.transform(matrix, pathTemp);
        c.drawPath(pathTemp, paintTerrain);

        onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());

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
        windCircle = new WindCircle(getContext());
        swellCircle = new SwellCircle(getContext());
        tideCircle = new TideCircle(getContext());

        model = MainModel.instance;

        powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);

        SurfSpots surfSpots = MainModel.instance.surfSpots;
        surfSpotsList = surfSpots.getAll();

        scrollerHints = new Scroller(getContext());

        densityDHDep = getResources().getDisplayMetrics().density;

        MainModel.instance.userStat.addUserLevelListener(this::setHintsVisiblePolicy);

        setAwakenedState(1);

        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.bali_terrain);

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) result.write(buffer, 0, length);

            pathTerrain = SVGPathToAndroidPath.convert(result.toString("UTF-8"), pathTerrainSize);
        } catch (IOException e) {
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
            if (bmpMapZoomedIn != null && bmpMapZoomedIn.getWidth() == getWidth() + dh * 4 && bmpMapZoomedIn.getHeight() == getHeight() + dh * 4)
                return;
            bmpMapZoomedIn = Bitmap.createBitmap(getWidth() + dh * 4, getHeight() + dh * 4, Bitmap.Config.ARGB_8888);
            bmpMapZoomedInForSpotI = -1;
            pathCropMap = new Region(0, 0, getWidth() + 4 * dh, getHeight() + 4 * dh).getBoundaryPath();
        }
//        Log.i(TAG, "onSizeChanged() | " + bmpMapZoomedIn == null ? "null" : "ok");
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
            rescheduleHintsHide();
            if (windCircle.setHintsVisible(true, !isPowerSavingMode())) {
                repaint();
            }
            if (swellCircle.setHintsVisible(true, !isPowerSavingMode())) {
                repaint();
            }
            if (tideCircle.setHintsVisible(true, !isPowerSavingMode())) {
                repaint();
            }
        }

        return super.onTouchEvent(ev);
    }


    @Override
    public void computeScroll() {
        //Log.i(TAG, "computeScroll()");
        super.computeScroll();
        if (windCircle.computeScroll()) repaint();
        if (swellCircle.computeScroll()) repaint();
        if (tideCircle.computeScroll()) repaint();
    }

    private void cancelScheduledHintsHide() {
        if (timerHintsHide != null) {
            timerHintsHide.cancel();
            timerHintsHide.purge();
            timerHintsHide = null;
        }
    }

    private void rescheduleHintsHide() {
        //Log.i(TAG, "rescheduleHintsHide()");
        cancelScheduledHintsHide();
        timerHintsHide = new Timer();
        timerHintsHide.schedule(new TimerTask() {
            synchronized public void run() {
                //Log.i(TAG, "start hiding");
                if (windCircle.setHintsVisible(false, !isPowerSavingMode())) {
                    repaint();
                }
                if (swellCircle.setHintsVisible(false, !isPowerSavingMode())) {
                    repaint();
                }
                if (tideCircle.setHintsVisible(false, !isPowerSavingMode())) {
                    repaint();
                }
            }
        }, 7500);
    }

    public void setAwakenedState(float awakenedState) {
        awakenedState = FAST_OUT_SLOW_IN_INTERPOLATOR.getInterpolation(awakenedState);

        if (this.awakenedState == awakenedState) return;
        this.awakenedState = awakenedState;
        if (awakenedState == 0 && hintsVisiblePolicy > 0) {
            cancelScheduledHintsHide();
            scrollerHints.abortAnimation();
//            hintsVisible = 1;
        }
        if (awakenedState == 1 && hintsVisiblePolicy == 1) {
            rescheduleHintsHide();
        }
        updateShownSpotsBoundRect();
        repaint();
    }

    public void show(float shownI, float firstI, float lastI, float awakenedState) {
        //Log.i(TAG, shownI +" "+ firstI +" "+ lastI);

        if (this.shownI == shownI && this.firstI == firstI && this.lastI == lastI && this.awakenedState == awakenedState)
            return;

        this.shownI = shownI;
        this.firstI = firstI;
        this.lastI = lastI;

        if (this.awakenedState != awakenedState) {
            setAwakenedState(awakenedState);
        } else {
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

        //Log.i(TAG, "updateShownSpotsBoundRect"+avex+" "+avey+" "+sum);

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

        SurfSpot spot = model.getSelectedSpot();
        avex = awakenedState * spot.pointOnSVG.x + (1f - awakenedState) * avex;
        avey = awakenedState * spot.pointOnSVG.y + (1f - awakenedState) * avey;

        shownSpotsAverage = new PointF(avex, avey);
        shownSpotsBoundRect = new RectF(avex - radf, avey - radf, avex + radf, avey + radf);
    }


    private final static float SQRT_2 = (float) Math.sqrt(2);
    private final static Path pathArrow = new Path();

    public static Path getArrow(float x, float y, float a, float arrowSize) {
        pathArrow.reset();
        pathArrow.moveTo(x - (float) Math.cos(a) * arrowSize * SQRT_2, y + (float) Math.sin(a) * arrowSize * SQRT_2);
        pathArrow.arcTo(new RectF(x - arrowSize, y - arrowSize, x + arrowSize, y + arrowSize), -a * 180 / (float) Math.PI + 45 + 180, 360 - 2 * 45, false);
        pathArrow.close();
        return pathArrow;
    }

    private final Path pathSpotCircleBG = new Path();
    private final Path pathSpotCircleClip = new Path();

    private void paintSpotCircle(Canvas c, float ox, float oy, float r) {
        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * MapCircle.subZ);
        float x = pp.x, y = pp.y;

        float a = (float) (model.getSelectedSpot().waveDirection.ordinal() * Math.PI * 2 / 16 + Math.PI);
        float aDegrees = (float) (-a * 180 / Math.PI);

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

        paintWaveLines.setStrokeWidth(awakenedState * densityDHDep * 2);

        int leftright = model.getSelectedSpot().leftright;
        if (leftright == 0) {
            PointF p1 = new PointF(0.20f, -0.70f);
            PointF p2 = new PointF(0.45f, -0.35f);
            PointF p3 = new PointF(0.70f, -0.0f);
            c.drawLine(p1.x * r, p1.y * r, -p1.x * r, p1.y * r, paintWaveLines);
            c.drawLine(p2.x * r, p2.y * r, -p2.x * r, p2.y * r, paintWaveLines);
            c.drawLine(p3.x * r, p3.y * r, -p3.x * r, p3.y * r, paintWaveLines);
        } else if (leftright == 2) {
            PointF p1 = new PointF(-0.11f, -0.70f);
            PointF p2 = new PointF(0.23f, -0.35f);
            PointF p3 = new PointF(0.45f, -0.0f);
            c.drawLine(p1.x * r, p1.y * r, -r, p1.y * r, paintWaveLines);
            c.drawLine(-p2.x * r, p2.y * r, r, p2.y * r, paintWaveLines);
            c.drawLine(p3.x * r, p3.y * r, -p3.x * r, p3.y * r, paintWaveLines);
        } else {
            PointF p1 = new PointF(-0.11f, -0.70f);
            PointF p2 = new PointF(0.23f, -0.35f);
            PointF p3 = new PointF(0.59f, -0.0f);
            c.drawLine(p1.x * r * leftright, p1.y * r, -1 * r * leftright, p1.y * r, paintWaveLines);
            c.drawLine(p2.x * r * leftright, p2.y * r, -1 * r * leftright, p2.y * r, paintWaveLines);
            c.drawLine(p3.x * r * leftright, p3.y * r, -1 * r * leftright, p3.y * r, paintWaveLines);
        }

        c.restore();

        windCircle.paint(c, ox, oy, awakenedState, parallaxHelper, r);
    }


    private WindCircle windCircle;
    private SwellCircle swellCircle;
    private TideCircle tideCircle;


    private final Matrix matrix = new Matrix();
    private final Path pathTemp = new Path();
    private Bitmap bmpMapZoomedOut = null;
    private Bitmap bmpMapZoomedIn = null;
    private int bmpMapZoomedInForSpotI = -1;

    private static final float paddingTop = 4f;
    private static final float paddingBottom = 4.8f;

    private static final float rOut = 200;
    private static final float rIn = 25;

    private final RectF rectFTemp = new RectF();


    public int insetY = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.i("BaliMap", "onDraw" + hintsVisible);

        super.onDraw(canvas);

        int height = getHeight() - insetY;

        int h = height - (int) ((paddingTop + paddingBottom) * dh);

        updateShownSpotsBoundRect();

        if (shownSpotsBoundRect == null) return;

        float scale = (1 - awakenedState) * dh * 2 / rOut + awakenedState * dh * 2 / rIn;

        float dx, dy;
        dx = -shownSpotsBoundRect.left * scale;
        dy = -shownSpotsBoundRect.top * scale;

        dx += awakenedState * (getWidth() - (3 * dh));
        dy += awakenedState * (paddingTop * dh + h / 2);

        dx += (1 - awakenedState) * (getWidth() - (3 * dh));
        dy += (1 - awakenedState) * (height - 2 * dh) / 2;

        int mapCenterY = getHeight() / 2 - insetY / 2;

        PointF pp = parallaxHelper.applyParallax(getWidth() / 2, mapCenterY, -dh * 1.0f);
        pp.offset(-getWidth() / 2, -mapCenterY);

        if (awakenedState == 0) {
            canvas.drawBitmap(bmpMapZoomedOut, dx + pp.x, dy + pp.y, null);
        } else if (awakenedState == 1 && bmpMapZoomedIn != null) {
            if (bmpMapZoomedInForSpotI != model.selectedSpotI) {
                int h2 = getHeight() - (int) ((paddingTop + paddingBottom) * dh);

                updateShownSpotsBoundRect();

                float dx2 = -shownSpotsBoundRect.left * scale;
                float dy2 = -shownSpotsBoundRect.top * scale;

                dx2 += getWidth() - (3 * dh);
                dy2 += paddingTop * dh + h2 / 2;

                bmpMapZoomedIn.eraseColor(0x00000000);
                Canvas c = new Canvas(bmpMapZoomedIn);

                matrix.setTranslate(dx2 + 2 * dh, dy2 + 2 * dh);
                matrix.preScale(scale, scale);
                pathTerrain.transform(matrix, pathTemp);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //TODO FIX
                    pathTemp.op(pathTemp, pathCropMap, Path.Op.INTERSECT);
                }

                c.drawPath(pathTemp, paintTerrain);

                bmpMapZoomedInForSpotI = model.selectedSpotI;
            }
            canvas.drawBitmap(bmpMapZoomedIn, pp.x - 2 * dh, -insetY/2+pp.y - 2 * dh, null);
        } else {
            float s = scale / (dh * 2f / rOut);
            rectFTemp.set(dx + pp.x, dy + pp.y, dx + pp.x + bmpMapZoomedOut.getWidth() * s, dy + pp.y + bmpMapZoomedOut.getHeight() * s);
            canvas.drawBitmap(bmpMapZoomedOut, null, rectFTemp, null);
        }

        if (awakenedState != awakenedStatePrev) {
            paintFont.setTextSize(awakenedState * metricsAndPaints.font);
        }

        int selectedSpotI = model.selectedSpotI;

        pp = parallaxHelper.applyParallax(getWidth() / 2, mapCenterY, -dh * 0.9f * (1 - awakenedState));
        pp.offset(-getWidth() / 2, -mapCenterY);
        dx += pp.x;
        dy += pp.y;

        int i = 0;

        int plusDays = 0;
        if (model != null) {
            plusDays = Math.round(model.getDay());
        }
        if (awakenedState == 1 && insetY < dh * 4) {
            float t = Math.max(0, 1f - (float) insetY / (dh * 4));
            paintFont.setColor((int) (t * 0xff) * 0x1000000 + colorSpotDot);
            paintFont.setTextAlign(Paint.Align.LEFT);
        }

        for (SurfSpot spot : surfSpotsList) {
            float highlighted = isHighlighted(i);
            if (highlighted > 0 && i != selectedSpotI) {
                float t = highlighted * (1f - awakenedState);
                float x = spot.pointOnSVG.x * scale + dx;
                float y = spot.pointOnSVG.y * scale + dy;
                float r = densityDHDep * highlighted * 1.5f;

                paintBG.setColor((int) (t * 0xff) * 0x1000000 + colorSpotDot);
                canvas.drawCircle(x, y, r, paintBG);
            }
            if (awakenedState == 1 && insetY < dh * 4) {
                float t = Math.max(0, 1f - (float) insetY / (dh * 4)); //(1f - awakenedState);
                float x = spot.pointOnSVG.x * scale + dx;
                float y = spot.pointOnSVG.y * scale + dy;
                if (x > 0 && y > paddingTop*dh && x < getWidth() && y < getHeight()-paddingBottom*dh) {
                    float r = densityDHDep * 1.5f;

                    paintBG.setColor((int) (t * 0xff) * 0x1000000 + colorSpotDot);
                    canvas.drawCircle(x, y, r, paintBG);

                    x += dh / 5;
                    y += dh / 5;

                    canvas.drawText(spot.name.substring(0, 3), x, y, paintFont);

                    RatedConditions best = MainModel.instance.rater.getBest(spot, plusDays);
                    if (best != null)
                        RatingView.drawStatic(canvas, (int) x - dh, (int) y, dh / 4, best.rating, best.waveRating * best.tideRating, (int) (t * 255));
                }
            }
            i++;
        }
        paintFont.setTextAlign(Paint.Align.CENTER);

        if (selectedSpotI != -1) {
            SurfSpot spot = model.getSelectedSpot();

            float x = spot.pointOnSVG.x * scale + dx;
            float y = spot.pointOnSVG.y * scale + dy;

            if (awakenedState == 0) {
                paintBG.setColor(0xff2696bb); //0xff000000 | colorSpotDot);
                canvas.drawCircle(x, y, densityDHDep * (2 + isHighlighted(selectedSpotI)), paintBG);
            } else {
                float t = awakenedState;
                awakenedState *= Math.min(1, (float) insetY / (dh * 4));

                float r = (dh * 1.5f - densityDHDep * 3) * awakenedState + densityDHDep * 3;
                if (awakenedState > 0) {
                    paintSelectedSpot(canvas, x, y, r);
                }
                if (awakenedState < 0.2) {
                    paintBG.setColor(((int) ((1f - awakenedState / 0.2f) * 0xff) << 24) | 0x2696bb); //colorWaterColor
                    canvas.drawCircle(x, y, r, paintBG);
                }

                awakenedState = t;
            }
        }

        awakenedStatePrev = awakenedState;
    }


    private void paintSelectedSpot(Canvas canvas, float x, float y, float r) {
        float windArrowVisible = windCircle.scrollerVisible.getValue();

        paintSpotCircle(canvas, x, y, r);

        float k = windArrowVisible * (float) Math.max(0, (Math.PI - Math.abs(windCircle.angle.getValue() - Math.PI) - 2));
        x -= awakenedState * Math.max(0, k * dh / 2f);

        x -= awakenedState * (1.5 + 1 + 0.75 + 0.33 * swellCircle.scrollerHints.getValue()) * dh;
        y += awakenedState * dh / 2;

        swellCircle.paint(canvas, x, y, awakenedState, parallaxHelper);

        x -= awakenedState * (1 + 1 + 0.66 + 0.33 * swellCircle.scrollerHints.getValue()) * dh;

        tideCircle.paint(canvas, x, y, awakenedState, parallaxHelper);
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
        } else return 0;
    }


    public void setAccentColor(int accentColor) {
        colorWaterColor = accentColor;
        tideCircle.paintPathTide.setColor(accentColor);
    }
}
