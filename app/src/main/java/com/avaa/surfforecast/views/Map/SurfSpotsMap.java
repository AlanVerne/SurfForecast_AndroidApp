package com.avaa.surfforecast.views.Map;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.R;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.drawers.SVGPathToAndroidPath;
import com.avaa.surfforecast.views.ParallaxHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.avaa.surfforecast.views.ColorUtils.alpha;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;


/**
 * Created by Alan on 9 Jul 2016.
 */


public class SurfSpotsMap extends View {
    private static final String TAG = "BaliMap";

    private static final android.view.animation.Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new android.support.v4.view.animation.FastOutSlowInInterpolator();

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

    private float overviewState = 1;

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

    private int dh = 0;

    private MetricsAndPaints metrics;

    private MainModel model = null;

    private WindCircle windCircle;
    private SwellCircle swellCircle;
    private TideCircle tideCircle;

    private final Matrix matrix = new Matrix();
    private final Path pathTemp = new Path();
    private Bitmap bmpMapZoomedOut = null;
    private Bitmap bmpMapZoomedIn = null;
    private int bmpMapZoomedInForSpotI = -1;
    private final PointF zoomedInPoint = new PointF(0, 0);
    private PointF zoomedInV = new PointF(0, 0);

    private int insetTop = 0;
    private int insetBottom = 0;
    private static final float paddingBottom = 4.8f;

    private static final float MAP_ZOOMED_OUT = 180;
    private static final float MAP_OVERVIEW = 20;
    private static final float MAP_SPOT_SELECTED = 30;

    private final RectF rectFTemp = new RectF();

    private Map<Integer, Rect> spotsLabels = new HashMap<>();

    private float avex = 0;
    private float avey = 0;

    private RectF shownSpotsBoundRect = null;
    private PointF shownSpotsAverage = null;


    // --


    public SurfSpotsMap(Context context) {
        this(context, null);
    }

    public SurfSpotsMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SurfSpotsMap(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        model = MainModel.instance;

        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        surfSpotsList = model.surfSpots.getAll();

        for (int i = 0; i < surfSpotsList.size(); i++) {
            spotsLabels.put(i, new Rect());
        }

        densityDHDep = getResources().getDisplayMetrics().density;

        model.userStat.addUserLevelListener(this::setHintsVisiblePolicy);

        PointF pointOnSVG = model.surfSpots.getArea(model.getSelectedSpotI()).pointOnSVG;
        zoomedInPoint.set(pointOnSVG.x, pointOnSVG.y);

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

        model.addChangeListener(MainModel.Change.SELECTED_TIME,
                MainModel.Change.SELECTED_CONDITIONS,
                MainModel.Change.SELECTED_DAY,
                changes -> repaint());

        parallaxHelper = new ParallaxHelper(this);
    }


    // --


    public void setMetrics(MetricsAndPaints metrics) {
        int dh = metrics.dh;

        if (dh == 0) return;
        if (this.dh == dh) return;
        if (this.dh != 0) bmpMapZoomedOut.eraseColor(0x00000000);

        this.dh = dh;
        this.metrics = metrics;

        densityDHDep = metrics.densityDHDependent;

        insetTop = 5 * dh;

        bmpMapZoomedInForSpotI = -1;

        Canvas c = new Canvas(bmpMapZoomedOut);

        float scale = 2 * dh / MAP_ZOOMED_OUT;
        matrix.setScale(scale, scale);
        pathTerrain.transform(matrix, pathTemp);
        c.drawPath(pathTemp, paintTerrain);

        if (tideCircle == null) {
            tideCircle = new TideCircle(this);
            swellCircle = new SwellCircle(this);
            windCircle = new WindCircle(this);

            tideCircle.colorWaterColor = colorWaterColor;
        }

        updateScale();

        onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());

        tideCircle.updateMetrics();

//        Log.i(TAG, "setMetrics() | this.getHeight() = " + getHeight());
    }


    public void setHintsVisiblePolicy(int hintsVisiblePolicy) {
        if (this.hintsVisiblePolicy == hintsVisiblePolicy) return;
        if (hintsVisiblePolicy == 1 && awakenedState == 1) rescheduleHintsHide();
        this.hintsVisiblePolicy = hintsVisiblePolicy;
    }


//    public void showHints() {
//        boolean repaint = false;
//        repaint |= windCircle.setHintsVisible(true, !isPowerSavingMode());
//        repaint |= swellCircle.setHintsVisible(true, !isPowerSavingMode());
//        repaint |= tideCircle.setHintsVisible(true, !isPowerSavingMode());
//        if (repaint) repaint();
//    }

    public void hideHints() {
        if (tideCircle == null) return;

        boolean repaint = false;
        repaint |= windCircle.setHintsVisible(false, !isPowerSavingMode());
        repaint |= swellCircle.setHintsVisible(false, !isPowerSavingMode());
        repaint |= tideCircle.setHintsVisible(false, !isPowerSavingMode());

        if (repaint) repaint();
    }


//    public void hideCircles() {
//        windCircle.setVisible(false, true);
//        swellCircle.setVisible(false, true);
//        tideCircle.setVisible(false, true);
//    }
//
//    public void showCircles() {
//        windCircle.setVisible(true, true);
//        swellCircle.setVisible(true, true);
//        tideCircle.setVisible(true, true);
//    }


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


    private float scale = 1;

    private void updateScale() {
        float rIn = overviewState * MAP_OVERVIEW + (1f - overviewState) * MAP_SPOT_SELECTED;
        scale = (1f - awakenedState) * dh * 2 / MAP_ZOOMED_OUT + awakenedState * dh * 2 / rIn;
    }

    private float getScale() {
        return scale;
    }


    // GESTURES \/


    private PointF actionDown = null;
    private boolean moved;

    private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            zoomedInV = new PointF(-velocityX / scale, -velocityY / scale);

//            Log.i(TAG, "onFling() " + (int) (zoomedInV.x * 10f / 60f) + " " + (int) (zoomedInV.y * 10f / 60f));

            actionDown = null;
            repaint();

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e, float distanceX, float distanceY) {
//            if (!moved && Math.pow(-e.getX() + actionDown.x, 2) + Math.pow(-e.getY() + actionDown.y, 2) > dh) {
            if (!moved) {
                moved = true;

                if (overviewState < 1f) {
                    model.mainActivity.performHideForecast();
                    toOverview = true;
                }
            }

            zoomedInPoint.offset(distanceX / scale, distanceY / scale);
            repaint();

            return super.onScroll(e1, e, distanceX, distanceY);
        }
    };

    private final GestureDetector gestureDetector = new GestureDetector(this.getContext(), gestureListener);

    {
        gestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
//        Log.i(TAG, "onTouchEvent() | " + e.getAction() + " ovs=" + overviewState);

        if (overviewState > 0f) {
            if (gestureDetector.onTouchEvent(e)) {
                return true;
            }

            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                actionDown = new PointF(e.getX(), e.getY());
                zoomedInV.set(0, 0);
                moved = false;
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                actionDown = null;
                if (!moved && overviewState > 0.75f) {
                    for (Map.Entry<Integer, Rect> rect : spotsLabels.entrySet()) {
                        if (rect.getValue().contains((int) e.getX(), (int) e.getY())) {
                            model.setSelectedSpotI(rect.getKey());
                            model.mainActivity.performShowTide();
                            return true;
                        }
                    }

                    double min = dh * dh * 4;
                    int minI = -1;
                    for (Map.Entry<Integer, Rect> rect : spotsLabels.entrySet()) {
                        double d = Math.pow(rect.getValue().centerX() - e.getX(), 2) + Math.pow(rect.getValue().centerY() - (int) e.getY(), 2);
                        if (d < min) {
                            min = d;
                            minI = rect.getKey();
                        }
                    }
                    if (minI != -1) {
                        model.setSelectedSpotI(minI);
                        model.mainActivity.performShowTide();
                        return true;
                    }
                } else {
                    moved = false;
                }
            }
        } else if (overviewState == 0f) {
            if (gestureDetector.onTouchEvent(e)) {
                return true;
            }

            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                actionDown = new PointF(e.getX(), e.getY());
                zoomedInV.set(0, 0);
                moved = false;
                return true;
            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                actionDown = null;
                if (!moved) {
                    if (tideCircle != null) {
                        if (tideCircle.hit(e.getX(), e.getY())) {
                            tideCircle.setHintsVisible(true, true);
                            postDelayed(() -> swellCircle.setHintsVisible(true, true), 33);
                            postDelayed(() -> windCircle.setHintsVisible(true, true), 66);
                        } else if (swellCircle.hit(e.getX(), e.getY())) {
                            swellCircle.setHintsVisible(true, true);
                            postDelayed(() -> {
                                tideCircle.setHintsVisible(true, true);
                                windCircle.setHintsVisible(true, true);
                            }, 33);
                            model.mainActivity.performShowWindSwell();
                        } else if (windCircle.hit(e.getX(), e.getY())) {
                            windCircle.setHintsVisible(true, true);
                            postDelayed(() -> swellCircle.setHintsVisible(true, true), 33);
                            postDelayed(() -> tideCircle.setHintsVisible(true, true), 66);
                            model.mainActivity.performShowWindSwell();
                        } else {
                            model.mainActivity.onBackPressed();
                        }
                    } else {
                        model.mainActivity.onBackPressed();
                    }

                    if (hintsVisiblePolicy == 1) {
                        rescheduleHintsHide();
                    }

                    return true;
                } else {
                    moved = false;
                }
            }
        }

        return super.onTouchEvent(e);
    }


    // GESTURES /\


    private long prevMS = 0;

    @Override
    public void computeScroll() {
        //Log.i(TAG, "computeScroll()");
        super.computeScroll();

        boolean b = false;

        if (tideCircle != null) {
            b |= tideCircle.computeScroll();
            b |= swellCircle.computeScroll();
            b |= windCircle.computeScroll();
        }

        if (overviewState != 1 && !toOverview && actionDown == null) {
            SurfSpot spot = model.getSelectedSpot();

            zoomedInPoint.x += (spot.pointOnSVG.x - zoomedInPoint.x) / 4;
            zoomedInPoint.y += (spot.pointOnSVG.y - zoomedInPoint.y) / 4;

            if (abs(spot.pointOnSVG.x - zoomedInPoint.x) < 0.05f && abs(spot.pointOnSVG.y - zoomedInPoint.y) < 0.05f) {
                b = true;
            }
        }

        long currentTimeMillis = System.currentTimeMillis();
        float d = currentTimeMillis - prevMS;
        prevMS = currentTimeMillis;

        if (abs(zoomedInV.x) < 0.05f && abs(zoomedInV.y) < 0.05f) zoomedInV.set(0, 0);
        else {
            zoomedInV.set(zoomedInV.x * 0.9f, zoomedInV.y * 0.9f);

            float fd = d > 1000 / 20 ? 20f / 1000f : d / 1000f;

            zoomedInPoint.offset(zoomedInV.x * fd, zoomedInV.y * fd);

            b = true;
        }

//        Log.i(TAG, "compScro() fps="+1000/d);

        if (b) repaint();
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
                hideHints();
            }
        }, 7500);
    }

    public void setAwakenedState(float awakenedState) {
        awakenedState = FAST_OUT_SLOW_IN_INTERPOLATOR.getInterpolation(awakenedState);

        if (this.awakenedState == awakenedState) return;

        this.awakenedState = awakenedState;

        updateScale();

        if (awakenedState == 0 && hintsVisiblePolicy > 0) {
            cancelScheduledHintsHide();
//            hintsVisible = 1;
        }

        if (awakenedState == 1 && hintsVisiblePolicy == 1) {
            rescheduleHintsHide();
        }

        updateShownSpotsBoundRect();

        repaint();
    }

    public void highlightSpots(float shownI, float firstI, float lastI, float awakenedState) {
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

        avex = awakenedState * zoomedInPoint.x + (1f - awakenedState) * avex;
        avey = awakenedState * zoomedInPoint.y + (1f - awakenedState) * avey;

        shownSpotsAverage = new PointF(avex, avey);
        shownSpotsBoundRect = new RectF(avex - radf, avey - radf, avex + radf, avey + radf);
    }


    private final Path pathSpotCircleBG = new Path();
    private final Path pathSpotCircleClip = new Path();

    private void paintSpotCircle(Canvas c, float ox, float oy, float r, float awakenedState) {
        PointF pp = parallaxHelper.applyParallax(ox, oy, dh * MapCircle.subZ);
        float x = pp.x, y = pp.y;

        float a = (float) (model.getSelectedSpot().waveDirection.ordinal() * PI * 2 / 16 + PI);
        float aDegrees = (float) (-a * 180 / PI);

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


    @Override
    protected void onDraw(Canvas canvas) {
        //Log.i("BaliMap", "onDraw" + hintsVisible);

        super.onDraw(canvas);

        int height = getHeight() - insetBottom;

        int h = height - (insetTop + (int) (paddingBottom * dh));

        updateShownSpotsBoundRect();

        if (shownSpotsBoundRect == null) return;

        float dx, dy;
        dx = -shownSpotsBoundRect.left * scale;
        dy = -shownSpotsBoundRect.top * scale;

        dx += getWidth() - (3 * dh);
        dy += awakenedState * (insetTop + h / 2) + (1 - awakenedState) * (height / 2 - dh);

        int mapCenterY = getHeight() / 2 - insetBottom / 2;

        PointF pp = parallaxHelper.applyParallax(getWidth() / 2, mapCenterY, -dh * 1.0f);
        pp.offset(-getWidth() / 2, -mapCenterY);

        if (awakenedState == 0) {
            canvas.drawBitmap(bmpMapZoomedOut, dx + pp.x, dy + pp.y, null);
//        } else if (awakenedState == 1 && bmpMapZoomedIn != null) {
//            if (bmpMapZoomedInForSpotI != model.selectedSpotI) {
//                int h2 = getHeight() - (int) ((insetTop + paddingBottom) * dh);
//
//                updateShownSpotsBoundRect();
//
//                float dx2 = -shownSpotsBoundRect.left * scale;
//                float dy2 = -shownSpotsBoundRect.top * scale;
//
//                dx2 += getWidth() - (3 * dh);
//                dy2 += insetTop * dh + h2 / 2;
//
//                bmpMapZoomedIn.eraseColor(0x00000000);
//                Canvas c = new Canvas(bmpMapZoomedIn);
//
//                matrix.setTranslate(dx2 + 2 * dh, dy2 + 2 * dh);
//                matrix.preScale(scale, scale);
//                pathTerrain.transform(matrix, pathTemp);
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //TODO FIX
//                    pathTemp.op(pathTemp, pathCropMap, Path.Op.INTERSECT);
//                }
//
//                c.drawPath(pathTemp, paintTerrain);
//
//                bmpMapZoomedInForSpotI = model.selectedSpotI;
//            }
//            canvas.drawBitmap(bmpMapZoomedIn, pp.x - 2 * dh, -insetBottom / 2 + pp.y - 2 * dh, null);
        } else {
            float s = scale / (dh * 2f / MAP_ZOOMED_OUT);
            rectFTemp.set(dx + pp.x, dy + pp.y, dx + pp.x + bmpMapZoomedOut.getWidth() * s, dy + pp.y + bmpMapZoomedOut.getHeight() * s);
            canvas.drawBitmap(bmpMapZoomedOut, null, rectFTemp, null);
        }

//        if (awakenedState != awakenedStatePrev) {
//            paintFont.setTextSize(awakenedState * metrics.font);
//        }

        int selectedSpotI = model.getSelectedSpotI();

        pp = parallaxHelper.applyParallax(getWidth() / 2, mapCenterY, -dh * 0.9f * (1 - awakenedState));
        pp.offset(-getWidth() / 2, -mapCenterY);
        dx += pp.x;
        dy += pp.y;

        int i = 0;

        int plusDays = 0;
        if (model != null) {
            plusDays = Math.round(model.getSelectedDay());
        }
        if (awakenedState == 1 && insetBottom < dh * 4) {
            paintFont.setColor(alpha(overviewState, colorSpotDot));
            paintFont.setTextAlign(Paint.Align.LEFT);
        }

        float rectXL = -dh * 2;
        float rectXR = getWidth() + dh * 2;
        float rectYT = -dh;
        float rectYB = getHeight() - paddingBottom * dh;

        float fx1 = dh * 2;
        float fx2 = getWidth() - dh * 2;
        float fy1 = insetTop;
        float fy2 = getHeight() - paddingBottom * dh;

        boolean justOneLabel = false;

        float scaleOverview = 2 * dh / MAP_OVERVIEW;

        float scalesRatio = scale / scaleOverview;

        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scalesRatio, scalesRatio);

        float dx2 = dx / scalesRatio;
        float dy2 = dy / scalesRatio;

        float r = densityDHDep * 1.5f;

        Drawable star = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_24dp);

        for (SurfSpot spot : surfSpotsList) {
            float x = spot.pointOnSVG.x * scaleOverview;
            float y = spot.pointOnSVG.y * scaleOverview;
            float x2 = x + dx2;
            float y2 = y + dy2;

            float highlighted = isHighlighted(i);
            if (highlighted > 0 && i != selectedSpotI) {
                float t = highlighted * (1f - awakenedState);
                paintBG.setColor(alpha(t, colorSpotDot));
                canvas.drawCircle(x, y, r * highlighted, paintBG);
            }

            if (awakenedState == 1 && overviewState > 0f) {
                RatedConditions best = model.rater.getBest(spot, plusDays);
                float bestRating = best != null ? best.rating : 0;
                //r = densityDHDep * (bestRating >= 0.7 ? 2f : bestRating > 0.3 ? 1.5f : 1f);

                paintBG.setColor(alpha(overviewState * (bestRating / 2f + 0.5f), 0x006281)); //colorSpotDot);
                canvas.drawCircle(x, y, r, paintBG);

                if (x2 > rectXL && (y2 > insetTop || x2 > dh * 4 && y2 > rectYT) && x2 < rectXR && y2 < rectYB) {
                    float size = metrics.font; // / ((overviewState * MAP_OVERVIEW + (1f - overviewState) * MAP_SPOT_SELECTED) / MAP_OVERVIEW); // bestRating >= 0.7 ? metrics.fontBig : bestRating > 0.3 ? metrics.font : metrics.fontSmall;
//                    RatingView.drawStatic(canvas, (int) x - dh, (int) y, dh / 4, best.rating, best.waveRating * best.tideRating, (int) (t * 255));
//                    paintFont.setColor(alpha(Math.max(0f, overviewState - 0.33f) / 0.67f * (bestRating / 3f + 0.66f), 0x006281));
                    paintFont.setColor(alpha(Math.max(0f, overviewState - 0.33f) / 0.67f, 0x006281));
                    paintFont.setTextSize(size);

                    y -= paintFont.getFontMetrics().ascent / 3;

                    if (spot.labelLeft) {
                        x -= 3 * r;
                        paintFont.setTextAlign(Paint.Align.RIGHT);
                    } else {
                        x += 3 * r;
                        paintFont.setTextAlign(Paint.Align.LEFT);
                    }

                    canvas.drawText(spot.name, x, y, paintFont);

                    int strWidth = (int) paintFont.measureText(spot.name);

                    if (spot.labelLeft) x2 -= strWidth;

                    Rect rect = spotsLabels.get(i);
                    rect.set((int) x2 - dh / 2, (int) (y2 + paintFont.getFontMetrics().ascent), (int) x2 + strWidth + dh / 2, (int) y2);

                    if (spot.labelLeft) {
                        x -= strWidth + dh / 16 + dh;
                    } else {
                        x += strWidth + dh / 16;
                    }

                    y += paintFont.getFontMetrics().ascent * 1.5f;
                    star.setAlpha((int) (overviewState * (bestRating >= 0.7 ? 255 : bestRating > 0.3 ? 150 : 100)));
                    star.setBounds((int) x, (int) y, (int) (x + dh), (int) (y + dh));
                    star.draw(canvas);

                    y -= paintFont.getFontMetrics().ascent * 1.4f;
                    paintFont.setColor(alpha(overviewState, bestRating >= 0.7 ? 0xff8ae3fc : 0xff4ac3ec));//paintBG.getColor());
                    paintFont.setTextAlign(Paint.Align.CENTER);
                    paintFont.setTextSize(metrics.fontSmall);
                    canvas.drawText(String.valueOf(Math.round(bestRating * 7)), x + dh / 2, y, paintFont);

                    if (!justOneLabel && x2 > fx1 && y2 > fy1 && x2 + dh < fx2 && y2 < fy2) {
                        justOneLabel = true;
                    }
                } else {
                    spotsLabels.get(i).set(-1000, -1000, -1, -1);
                }
            }
            i++;
        }

        canvas.restore();

        if (awakenedState > 0) {
            SurfSpot spot = model.getSelectedSpot();

            float x = spot.pointOnSVG.x * scale + dx;
            float y = spot.pointOnSVG.y * scale + dy;

            float t = awakenedState * (1f - overviewState);

            RatedConditions best = model.rater.getBest(spot, plusDays);
            float bestRating = best != null ? best.rating : 0;
//            float r = densityDHDep * (bestRating >= 0.7 ? 2f : bestRating > 0.3 ? 1.5f : 1f);
            r = (dh * 1.5f - r) * t + r;

            if (t > 0) {
                paintSelectedSpot(canvas, x, y, r, t);
            }

            if (t < 0.5f) {
                paintBG.setColor(alpha(overviewState * (bestRating / 2f + 0.5f) * (1f - t / 0.5f), 0x006281));
                canvas.drawCircle(x, y, r, paintBG);
            }
        }


        if (!justOneLabel && actionDown == null && overviewState > 0f && toOverview) { // Map correction
            SurfSpot magnetSpot = null;
            double magnetDistance = Integer.MAX_VALUE;

            float finalDX = scale * zoomedInV.x * 10f / 60f; // Consider velocity offset
            float finalDY = scale * zoomedInV.y * 10f / 60f;
            fx1 += finalDX;
            fx2 += finalDX;
            fy1 += finalDY;
            fy2 += finalDY;

            float fcx = (fx1 + fx2) / 2f;
            float fcy = (fy1 + fy2) / 2f;

            for (SurfSpot spot : surfSpotsList) {
                float x = spot.pointOnSVG.x * scale + dx;
                float y = spot.pointOnSVG.y * scale + dy;
                double distance = distToRect(fx1, fy1, fx2, fy2, fcx, fcy, x, y);
                if (distance < magnetDistance) {
                    magnetSpot = spot;
                    magnetDistance = distance;
                }
            }

            float forceX = magnetSpot.pointOnSVG.x * scale + dx - getWidth() / 2;
            float forceY = magnetSpot.pointOnSVG.y * scale + dy - getHeight() / 2;

            float force = (float) sqrt(forceX * forceX + forceY * forceY);

            float x = magnetSpot.pointOnSVG.x * scale + dx + finalDX;
            float y = magnetSpot.pointOnSVG.y * scale + dy + finalDY;
            magnetDistance = distToRect(fx1, fy1, fx2, fy2, fcx, fcy, x, y) / 5f;

            forceX *= magnetDistance / force;
            forceY *= magnetDistance / force;

            zoomedInV.set(zoomedInV.x + forceX, zoomedInV.y + forceY);
        }


        if (selectedSpotI != -1) {
            SurfSpot spot = model.getSelectedSpot();

            float x = spot.pointOnSVG.x * scale + dx;
            float y = spot.pointOnSVG.y * scale + dy;

            if (awakenedState == 0) {
                paintBG.setColor(0xff2696bb); //0xff000000 | colorSpotDot);
                canvas.drawCircle(x, y, densityDHDep * (2 + isHighlighted(selectedSpotI)), paintBG);
            }
        }

        awakenedStatePrev = awakenedState;
    }


    private double distToRect(float x1, float y1, float x2, float y2, float xc, float yc, float x, float y) {
        float dx = xc - x;
        float dy = yc - y;

        double d = sqrt(dx * dx + dy * dy);

        double k = 0;

        if (x < x1) k = (x1 - x) / dx;
        else if (x2 < x) k = (x2 - x) / dx;

        if (y < y1) k = max(k, (y1 - y) / dy);
        else if (y2 < y) k = max(k, (y2 - y) / dy);

        return d * k;
    }


//    private double distance(double alpha, double beta) {
//        double phi = Math.abs(beta - alpha) % (2 * PI);       // This is either the distance or 360 - distance
//        double distance = phi > PI ? 2 * PI - phi : phi;
//        return distance;
//    }


    private void paintSelectedSpot(Canvas canvas, float x, float y, float r, float awakenedState) {
        if (tideCircle == null) return;

        float windArrowVisible = windCircle.scrollerVisible.getValue();

        paintSpotCircle(canvas, x, y, r, awakenedState);

        float k = windArrowVisible * (float) Math.max(0, (PI - abs(windCircle.getAngle() - PI) - 2));
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
    }


    public void setInsets(int top, int bottom) {
        if (insetTop == top && insetBottom == bottom) return;
        if (actionDown != null || abs(zoomedInV.x) > 0 || abs(zoomedInV.y) > 0) {
            float dy = -(bottom - insetBottom) + (top - insetTop);
            zoomedInPoint.offset(0, dy / scale / 2);
        }
        insetTop = top;
        insetBottom = bottom;
        repaint();
    }


    private boolean toOverview = false;

    public void setOverviewState(float overviewState) {
        if (this.overviewState == overviewState) return;
        toOverview = this.overviewState < overviewState;
        this.overviewState = overviewState;
        updateScale();
        repaint();
    }
}
