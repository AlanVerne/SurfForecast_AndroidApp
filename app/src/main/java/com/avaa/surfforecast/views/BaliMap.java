package com.avaa.surfforecast.views;

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
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Scroller;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.drawers.ConditionsDrawer;
import com.avaa.surfforecast.R;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
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

    private int hintsVisiblePolicy = 2;
    private float hintsVisible = 1;

    private SurfSpots surfSpots = null;
    private List<SurfSpot> surfSpotsList = new ArrayList<>();

    private Point pathTerrainSize = new Point();
    private Path pathTerrain = null;

    private SurfConditions currentConditions = null;
    private METAR currentMETAR = null;

    private float shownI = 0;
    private float firstI = 0;
    private float lastI  = 0;

    public int colorAccent;

    private float awakenedState = 0;

    private float density = 3;

    private float smallTextSize = 14;
    private float bigTextSize = smallTextSize * 1.25f;

    public int colorWaterColor = 0xffacb5b8; //0xffa3b1b6; //0xff819faa; //0xff2e393d;
    private int colorTideWater = ConditionsDrawer.colorWaveBG;
    private int colorTideAir   = ConditionsDrawer.colorWindBG;

    private int colorSwellBG = 0xffffffff;
    private int colorWindBG  = 0xffffffff;

    private int colorSpotDot = 0xffffff;

    private float windArrowVisible = 0;
    private float windArrowAngle = 0;
    private float windArrowVbr = 0;

    private float swellArrowVisible = 0;

    private float tideCircleVisible = 0;


    private Paint paintSmallText = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setTextAlign(Paint.Align.CENTER);
        setColor(ConditionsDrawer.colorWhite);
    }};
    private Paint paintBigText = new Paint(paintSmallText) {{
        setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }};
    private Paint paintBG = new Paint() {{
        setAntiAlias(true);
        setStyle(Paint.Style.FILL);
    }};
    private Paint paintTerrain = new Paint() {{
        setAntiAlias(true);
        setStyle(Paint.Style.FILL);
        setColor(0x11000000);
    }};
    private Paint paintWaveLines = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
        setColor(0x88ffffff);//ConditionsDrawer.colorWhite);
        setStyle(Style.STROKE);
        setStrokeCap(Cap.ROUND);
    }};
    private Paint paintAdditionalText = new Paint() {{
        setFlags(Paint.ANTI_ALIAS_FLAG);
    }};
    private Rect rectSmallTextBounds = new Rect();
    private Rect rectBigTextBounds = new Rect();

    private Timer timerHintsHide = null;
    private Scroller mScrollerHints;

    private int dh = 0;


    public void setDh(int dh) {
        if (dh == 0) return;
        if (this.dh == dh) return;
        if (this.dh != 0) bmpMapZoomedOut.eraseColor(0x00000000);

        this.dh = dh;
        smallTextSize = dh/2;
        bigTextSize = smallTextSize*1.25f;

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
        surfSpots = AppContext.instance.surfSpots;
        surfSpots.addChangeListener(c -> {
            Log.i(TAG, "surfSpots ChangeListener | metar: " + surfSpots.currentMETAR);
            currentConditions = surfSpots.currentConditions;
            currentMETAR = surfSpots.currentMETAR;
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

        mScrollerHints = new Scroller(getContext());

        density = getResources().getDisplayMetrics().density;

        smallTextSize *= density;
        bigTextSize   *= density;

//        if (hintsVisiblePolicy > 0) {
//            hintsVisible = 1;
//        }

        AppContext.instance.usageStat.addUserLevelListener(l -> {
            setHintsVisiblePolicy(l);
        });

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

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        angleX = 0;
        angleY = 0;

        userX = getWidth()/2;
        userY = getHeight()/2;
    }


    Path pathCropMap = null;
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


    public void stop() {
//        mSensorManager.unregisterListener(sel);
    }
    public void resume() {
        timestamp = 0;
//        mSensorManager.registerListener(sel, mSensor, 20*1000);
    }


    public void resetUser() {
        angleX = 0;
        angleY = 0;

        userX = getWidth()/2;
        userY = getHeight()/2;
        userZ = phoneDistance;
    }


    SensorManager mSensorManager;
    Sensor mSensor;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp = 0;
    private float phoneDistance = 6000;
    private float angleX = 0, angleY = 0, userX = 0, userY = 0, userZ = 0;
    SensorEventListener sel = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // This time step's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > 80) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the time step
                // in order to get a delta rotation from this sample over the time step
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;

                //Log.i(TAG, "Deltas: " + axisX + " " + axisY + ", " + deltaRotationVector[0] + " " + deltaRotationVector[1]);

                float maxDA = (float)Math.PI / 30f;

                angleX += Math.max(-maxDA, Math.min(maxDA, axisX * dT));
                angleY += Math.max(-maxDA, Math.min(maxDA, axisY * dT));

                float angleRange = (float)Math.PI *2;// / 3f;
                angleX = Math.max(-angleRange, Math.min(angleRange, angleX));
                angleY = Math.max(-angleRange, Math.min(angleRange, angleY));

                if (axisX*dT < 0.001 && axisY*dT < 0.001) {
                    //Log.i(TAG, "SH");
                    angleX *= 0.995;
                    angleY *= 0.995;
                }

                //Log.i(TAG, "Angles: " + angleX*180/Math.PI + " " + angleY*180/Math.PI);
                userX = (float)Math.sin(-angleY) * phoneDistance + getWidth()/2;
                userY = (float)Math.sin(-angleX) * phoneDistance + getHeight()/2;
                userZ = (float)Math.cos(-(Math.sqrt(angleX*angleX + angleY*angleY))) * phoneDistance;

                repaint();
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };


    @Override
    public void computeScroll() {
        //Log.i("BM", "computeScroll()");
        super.computeScroll();
        if (mScrollerHints.computeScrollOffset()) {
            float newHV = mScrollerHints.getCurrX() / 1000f;
            //Log.i("BM", hintsVisible+" ");
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
                mScrollerHints.startScroll(1000, 0, -1000, 0, 666);
                repaint();
            }
        }, 10000);
    }
    public void setAwakenedState(float awakenedState) {
        if (this.awakenedState == awakenedState) return;
        this.awakenedState = awakenedState;
        if (awakenedState == 0 && hintsVisiblePolicy > 0) {
            cancelScheduledHintsHide();
            mScrollerHints.abortAnimation();
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

    RectF shownSpotsBoundRect = null;
    PointF shownSpotsAverage = null;
    private void updateShownSpotsBoundRect() {
        int i = 0;

        shownSpotsBoundRect = null;

        float x = 0;
        float y = 0;
        float sum = 0;

        for (SurfSpot spot : surfSpotsList) {
            float highlighted = isHighlighted(i);

            if (highlighted > 0) {
                x   += spot.pointOnSVG.x * highlighted;
                y   += spot.pointOnSVG.y * highlighted;
                sum += highlighted;
            }

            i++;
        }

        //Log.i("BM", "updateShownSpotsBoundRect"+x+" "+y+" "+sum);

        if (sum == 0) {
            x = 0;
            y = 0;
        }
        else {
            x /= sum;
            y /= sum;
        }

        double rad = 0;

        i = 0;
        for (SurfSpot spot : surfSpotsList) {
            float highlighted = isHighlighted(i);
            if (highlighted == 1) {
                float dx = spot.pointOnSVG.x - x;
                float dy = spot.pointOnSVG.y - y;
                rad = Math.max(rad, Math.sqrt(dx*dx+dy*dy));
            }
            i++;
        }

        float radf = 0;

        SurfSpot spot = SurfSpots.getInstance().selectedSpot();
        if (spot != null) {
            x = awakenedState * spot.pointOnSVG.x + (1f - awakenedState) * x;
            y = awakenedState * spot.pointOnSVG.y + (1f - awakenedState) * y;
        }

        shownSpotsAverage = new PointF(x, y);
        shownSpotsBoundRect = new RectF(x-radf, y-radf, x+radf, y+radf);
    }


    private final static float SQRT_2 = (float)Math.sqrt(2);
    private Path getArrow(float x, float y, float a, float arrowSize) {
        Path p = new Path();
        p.moveTo(x - (float)Math.cos(a)*arrowSize* SQRT_2, y + (float)Math.sin(a)*arrowSize* SQRT_2);
        p.arcTo(new RectF(x-arrowSize, y-arrowSize, x+arrowSize, y+arrowSize), -a*180/(float)Math.PI + 45 + 180, 360 - 2*45, false);
        p.close();
        return p;
    }

    float circlesH = 0.2f;
    float subcirclesH = 0.1f;
    private void paintSpotCircle(Canvas c, float ox, float oy, float r, float j) {
        PointF pp = Common.applyParallax(userX, userY, userZ, ox, oy, dh*subcirclesH);
        float x = pp.x, y = pp.y;

        float a = (float)(surfSpots.selectedSpot().waveDirection.ordinal() * Math.PI * 2 / 16 + Math.PI);
        float aDegrees = (float)(-a*180/Math.PI);

        Path cp = new Path();
        cp.addCircle(x, y, r, Path.Direction.CCW);

        paintBG.setColor(ConditionsDrawer.colorTideBG);

        final RectF oval = new RectF();
        oval.set(x - r, y - r, x + r, y + r);
        Path myPath = new Path();
        float aa = aDegrees + 90 + 90 - 63.4f;
        myPath.arcTo(oval, aa, 2 * 63.4f, true);
        c.drawPath(myPath, paintBG);

        paintBG.setColor(colorWaterColor); //ConditionsDrawer.colorWaveBG);
        myPath = new Path();
        myPath.arcTo(oval, aa, 2 * 63.4f - 360f, true);
        c.drawPath(myPath, paintBG);

        c.save();
        c.clipPath(cp);
        c.translate(x, y);
        c.rotate(aDegrees + 90);

        paintWaveLines.setStrokeWidth(awakenedState*density*2);

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

        pp = Common.applyParallax(userX, userY, userZ, ax, ay, dh * circlesH);
        ax = pp.x;
        ay = pp.y;

        paintBG.setColor(colorWindBG);

        if (vbr) c.drawCircle(ax, ay, windArrowR, paintBG);
        else c.drawPath(getArrow(ax, ay, a, windArrowR), paintBG);

        paintSmallText.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & ConditionsDrawer.colorWindText);

//        ay = ay + rectSmallTextBounds.height()/2;
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

                Paint paintAdditionalArrow = new Paint(paintAdditionalText) {{
                    setStyle(Style.STROKE);
                    setStrokeJoin(Join.MITER);
                    setStrokeWidth(density);
                }};

                Path p = new Path();
                p.moveTo(cx, cy);
                p.lineTo(bx, by);
                p.lineTo(dx, dy);

                c.drawPath(p, paintAdditionalArrow);
            }

            ay += rectSmallTextBounds.height()/2;
            ay += windArrowVisibleFinal * dh / 24 * hintsVisible;
            c.drawText(strWIND, ax, ay - rectSmallTextBounds.height() - windArrowVisibleFinal*dh / 6 * hintsVisible, paintAdditionalText);
            c.drawText(strKMH, ax, ay + windArrowVisibleFinal*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
        }
        else {
            ay += rectSmallTextBounds.height()/2;
        }

        int windSpeed = currentMETAR != null ? currentMETAR.windSpeed : currentConditions.windSpeed;
        //paintSmallText.setColor(currentMETAR != null ? 0xff000000 : 0x88000000);
        c.drawText(String.valueOf(windSpeed), ax, ay, paintSmallText);

        //c.drawCircle(ax - dh*4/10, ay - rectSmallTextBounds.height()/2, rectSmallTextBounds.height()/6, paintSmallText);
    }


    private void paintSwellCircle(Canvas c, float ox, float oy, float r, float j) {
        if (currentConditions == null) return;

        PointF pp = Common.applyParallax(userX, userY, userZ, ox, oy, dh*circlesH);
        float x = pp.x, y = pp.y;

        r += awakenedState*hintsVisible*dh/3;

        r *= swellArrowVisible;

        float a = currentConditions.waveAngle;

        paintBG.setColor(colorSwellBG);
        c.drawPath(getArrow(x, y, a, r), paintBG);

        String strWaveHeight = String.valueOf(currentConditions.getWaveHeightInFt());
        String strWavePeriod = String.valueOf(currentConditions.wavePeriod);

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

            Paint paintAdditionalArrow = new Paint(paintAdditionalText) {{
                setStyle(Style.STROKE);
                setStrokeJoin(Join.MITER);
                setStrokeWidth(density);
            }};

            Path p = new Path();
            p.moveTo(cx, cy);
            p.lineTo(bx, by);
            p.lineTo(dx, dy);

            c.drawPath(p, paintAdditionalArrow);

            float strWaveHeightWidth = paintBigText.measureText(strWaveHeight);
            float strWavePeriodWidth = paintSmallText.measureText(strWavePeriod);

            strFtWidth = paintAdditionalText.measureText(strFT) * hintsVisible;
            strSWidth = paintAdditionalText.measureText(strS) * hintsVisible;

            float finalVisibility = awakenedState * swellArrowVisible;
            y += dh / 24 * hintsVisible * finalVisibility;

            paintAdditionalText.setTextAlign(Paint.Align.LEFT);
            c.drawText(strFT, x - strFtWidth/3 + strWaveHeightWidth/2, y - (rectBigTextBounds.height() + dh/6 + rectSmallTextBounds.height())/2 + rectBigTextBounds.height(), paintAdditionalText);
            c.drawText(strS,  x - strSWidth/3 + strWavePeriodWidth/2, y + (rectBigTextBounds.height() + dh/6 + rectSmallTextBounds.height())/2, paintAdditionalText);

            paintAdditionalText.setTextAlign(Paint.Align.CENTER);
            c.drawText(strSWELL, x, y - (rectBigTextBounds.height() + finalVisibility*dh/6 + rectSmallTextBounds.height())/2 - finalVisibility*dh / 6 * hintsVisible, paintAdditionalText);
            c.drawText(currentConditions.waveAngleAbbr(), x, y + (rectBigTextBounds.height() + finalVisibility*dh/6 + rectSmallTextBounds.height())/2 + finalVisibility*(dh / 6 + dh / 12 + dh / 6 * hintsVisible), paintAdditionalText);
        }

        paintBigText.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & ConditionsDrawer.colorWaveText);
        c.drawText(strWaveHeight, x - strFtWidth/3, y - (rectBigTextBounds.height() + dh/6 + rectSmallTextBounds.height())/2 + rectBigTextBounds.height(), paintBigText);

        paintSmallText.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff & ConditionsDrawer.colorWaveText);
        c.drawText(String.valueOf(currentConditions.wavePeriod), x - strSWidth/3, y + (rectBigTextBounds.height() + dh/6 + rectSmallTextBounds.height())/2, paintSmallText);
    }

    long nowTideUpdatedTime;
    Integer nowTide;
    TideData tideData;
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
        if (nowTide == null) AppContext.instance.tideDataProvider.fetch(Common.BENOA_PORT_ID);
    }
    private void updateTideData() {
        tideData = AppContext.instance.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
        updateNowTide();
    }


    private void paintTideCircle(Canvas c, float ox, float oy, float r, float j) {
        if (tideData == null || nowTide == null) return;

        checkNowTide();

        float finalVisibility = awakenedState * tideCircleVisible;

        int nowTime = Common.getNowTimeInt(TIME_ZONE);

        PointF pp = Common.applyParallax(userX, userY, userZ, ox, oy, dh*subcirclesH);
        float x = pp.x, y = pp.y;

        float py   = (float)(r / Math.sqrt(2));
        float nowy = py - py*2*nowTide/250;
        float nowx = (float)(-Math.cos(Math.asin(nowy/r))*r);

        float width = r*8;
        int nowH = (nowTime-1) / 60;
        final float pathDX = width * (nowTime - (nowH-1)*60) / 24 / 60 - nowx - x;

        final Path pathTide = tideData.getPath2(Common.getToday(TIME_ZONE), width*8/24, py*2, 0, 250, nowH-1, nowH+7); // new Path();

        if (pathTide == null) return;

        Matrix translateMatrix = new Matrix();
        translateMatrix.setTranslate(-pathDX, y-py);
        pathTide.transform(translateMatrix);

        float dotR = finalVisibility * dh * 0.7f;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // TODO: prekitkat realization
            Paint paint = new Paint() {{
                setAntiAlias(true);
                setColor(colorWaterColor);
                setStyle(Paint.Style.FILL);
            }};

            Path pathCircle = new Path();
            pathCircle.addCircle(x, y, r, Path.Direction.CCW);
            pathTide.op(pathCircle, Path.Op.INTERSECT);

            c.drawPath(pathTide, paint);
        }

        pp = Common.applyParallax(userX, userY, userZ, ox + nowx, oy + nowy, dh*circlesH);
        x = pp.x; y = pp.y;

        paintSmallText.setColor(ConditionsDrawer.colorTideBG);
        c.drawCircle(x, y, dotR + finalVisibility*hintsVisible*dh/4, paintSmallText);

        paintSmallText.setColor((int)(j*0xff)*0x1000000 + 0x00ffffff);

        String strTide = String.valueOf(Math.round(nowTide/10f)/10f);
        float strTideWidth = paintSmallText.measureText(strTide);

        y += rectSmallTextBounds.height() / 2f;

//        if (hintsVisible > 0) {
//            float strMWidth = paintAdditionalText.measureText(strM);
//            x -= strMWidth / 3f * hintsVisible;
//            paintAdditionalText.setColor((int)(hintsVisible*0xff)<<24 | 0xffffff);
//            paintAdditionalText.setTextAlign(Paint.Align.LEFT);
//            c.drawText(strM, x + strTideWidth / 2f, y, paintAdditionalText);
//        }

        if (hintsVisible > 0) {
            y += dh/24 * hintsVisible * finalVisibility;
            paintAdditionalText.setColor((int)(j*hintsVisible*0xff)<<24 | 0xffffff);
            paintAdditionalText.setTextAlign(Paint.Align.CENTER);
            c.drawText(strTIDE, x, y - rectSmallTextBounds.height() - finalVisibility*dh/6*hintsVisible, paintAdditionalText);

            c.drawText(strM, x, y + finalVisibility*(dh/6 + dh/12 + dh/6*hintsVisible), paintAdditionalText);
        }

        c.drawText(strTide, x, y, paintSmallText);
    }

    private final Matrix matrix = new Matrix();
    private final Path pathTemp = new Path();
    private Bitmap bmpMapZoomedOut = null;
    private Bitmap bmpMapZoomedIn = null;
    private int bmpMapZoomedInForSpotI = -1;

    private static final float paddingTop = 3;
    private static final float paddingBottom = 4.8f;

    private static final float rOut = 200;
    private static final float rIn  = 100;

    private static final Paint paint = new Paint() {{
        setAntiAlias(false);
        setFilterBitmap(false);
        setDither(false);
    }};

    private final RectF rectfTemp = new RectF();


    private boolean computeArrowsAnimation() {
        boolean needRepaint = false;

        if (currentConditions != null || currentMETAR != null) {
            float a = currentMETAR != null ? currentMETAR.windAngle : currentConditions.windAngle;
            boolean vbr = a < 0 || (currentMETAR != null ? currentMETAR.windSpeed : currentConditions.windSpeed) <= 0;

            if (a < 0) a = (float)(Math.PI*2 - Math.PI * 1 / 4);

            if (windArrowVisible == 0) {
                windArrowAngle = a;
                windArrowVbr = vbr ? 1 : 0;
            }
            else {
                double dA = (a - windArrowAngle);
                if (dA > Math.PI) { windArrowAngle += Math.PI*2; dA -= Math.PI*2; }
                else if (dA < -Math.PI) { windArrowAngle -= Math.PI*2; dA += Math.PI*2; }
                windArrowAngle += dA * 0.05;
                if (dA > 0.01) needRepaint = true;

                double dVbr = ((vbr ? 1 : 0) - windArrowVbr) * 0.05;
                windArrowVbr += dVbr;
                if (dVbr > 0.01) needRepaint = true;
            }
            windArrowVisible += (1 - windArrowVisible) * 0.22;
        }
        else {
            windArrowVisible += (0 - windArrowVisible) * 0.22;
        }

        if (!needRepaint) if (windArrowVisible >= 0.01 || windArrowVisible <= 0.99) needRepaint = true;


        if (currentConditions != null) {
            swellArrowVisible += (1 - swellArrowVisible) * 0.185;
        }
        else {
            swellArrowVisible += (0 - swellArrowVisible) * 0.185;
        }
        if (!needRepaint) if (swellArrowVisible >= 0.01 || swellArrowVisible <= 0.99) needRepaint = true;


        if (tideData != null) {
            tideCircleVisible += (1 - tideCircleVisible) * 0.13;
        }
        else {
            tideCircleVisible += (0 - tideCircleVisible) * 0.13;
        }
        if (!needRepaint) if (tideCircleVisible >= 0.01 || tideCircleVisible <= 0.99) needRepaint = true;


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

        PointF pp = Common.applyParallax(userX, userY, userZ, getWidth()/2, getHeight()/2, -dh*0.3f);
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
            rectfTemp.set(dx, dy, dx + bmpMapZoomedOut.getWidth()*s, dy+ bmpMapZoomedOut.getHeight()*s);
            canvas.drawBitmap(bmpMapZoomedOut, null, rectfTemp, paint);
        }

        paintBigText.setTextSize(awakenedState*bigTextSize);
        paintBigText.getTextBounds("00.0", 0, 4, rectBigTextBounds);

        paintSmallText.setTextSize(awakenedState*smallTextSize);
        paintSmallText.getTextBounds("0.00", 0, 4, rectSmallTextBounds);

        int selectedSpotI = surfSpots.selectedSpotI;

        int i = 0;
        for (SurfSpot spot : surfSpotsList) {
            float highlighted = isHighlighted(i);
            if (highlighted > 0 && i != selectedSpotI) {
                float t = highlighted * (1f - awakenedState);
                float x = spot.pointOnSVG.x * scale + dx;
                float y = spot.pointOnSVG.y * scale + dy;
                float r = density * highlighted * 1.5f;

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
                canvas.drawCircle(x, y, density * (2 + isHighlighted(selectedSpotI)), paintBG);
            }
            else {
                float r = (dh * 1.5f - density * 3) * awakenedState + density * 3;
                if (awakenedState > 0) {
                    paintSelectedSpot(canvas, x, y, r);
                }
                if (awakenedState < 0.2) {
                    paintBG.setColor(((int)((1f - awakenedState / 0.2f) * 0xff) << 24) | 0x2696bb); //colorWaterColor
                    canvas.drawCircle(x, y, r, paintBG);
                }
            }
        }

        if (needRepaint) repaint();
    }


    private void paintSelectedSpot(Canvas canvas, float x, float y, float r) {
        //if (true) return;

        float hintsVisbleToAwakened = Math.max(0, Math.min((awakenedState - 0.66f) * 3f, 1f));
        float smallr = (dh-density) * awakenedState + density;

        final float finalHintsVisbleToAwakened = hintsVisbleToAwakened;
        paintAdditionalText = new Paint(paintSmallText) {{
            setColor((int)(hintsVisible * finalHintsVisbleToAwakened *0xff)<<24 | 0x00ffffff & ConditionsDrawer.colorWindText);
            setTextSize(awakenedState * dh/3);
        }};

        hintsVisbleToAwakened = Math.max(0, Math.min((windArrowVisible * awakenedState - 0.66f) * 3f, 1f));
        paintSpotCircle(canvas, x, y, r, hintsVisbleToAwakened);

        float k = windArrowVisible * (float)Math.max(0, (Math.PI - Math.abs(windArrowAngle - Math.PI) - 2));
        x -= Math.max(0, k * dh / 2f);
        //Log.i(TAG, "wind spacing: " + a + " | " + k);

        x -= awakenedState * (1.5+1+0.75+0.25*hintsVisible) * dh;
        y += awakenedState * dh/2;

        hintsVisbleToAwakened = Math.max(0, Math.min((swellArrowVisible * awakenedState - 0.66f) * 3f, 1f));
        smallr = (dh-density) * swellArrowVisible * awakenedState + density;
        paintSwellCircle(canvas, x, y, smallr, hintsVisbleToAwakened);

        x -= awakenedState * (1+1+0.5+0.5*hintsVisible) * dh;

        hintsVisbleToAwakened = Math.max(0, Math.min((tideCircleVisible * awakenedState - 0.66f) * 3f, 1f));
        smallr = (dh-density) * tideCircleVisible * awakenedState + density;
        paintTideCircle(canvas, x, y, smallr, hintsVisbleToAwakened);
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
}
