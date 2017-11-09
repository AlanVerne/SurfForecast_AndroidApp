package com.avaa.surfforecast.views;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfConditionsOneDay;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.drawers.SurfConditionsOneDayBitmapsAsyncDrawer;
import com.avaa.surfforecast.drawers.TideChartDrawer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import static com.avaa.surfforecast.data.Common.STR_ENERGY_U;
import static com.avaa.surfforecast.data.Common.STR_FT;
import static com.avaa.surfforecast.data.Common.STR_KJ;
import static com.avaa.surfforecast.data.Common.STR_KMH;
import static com.avaa.surfforecast.data.Common.STR_M;
import static com.avaa.surfforecast.data.Common.STR_NO_SWELL_DATA;
import static com.avaa.surfforecast.data.Common.STR_NO_WIND_DATA;
import static com.avaa.surfforecast.data.Common.STR_S;
import static com.avaa.surfforecast.data.Common.STR_SWELL_U;
import static com.avaa.surfforecast.data.Common.STR_TIDE_U;
import static com.avaa.surfforecast.data.Common.STR_WIND_U;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorBlack;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorMidBlack;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.getColorForEnergy;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.getTextColorForEnergy;
import static com.avaa.surfforecast.views.ColorUtils.alpha;
import static java.lang.Math.pow;
import static java.lang.Math.round;


/**
 * Created by Alan on 1 Aug 2016.
 */


public class SurfConditionsForecastView extends HorizontalScrollView {
    private static final String TAG = "SCForecastView";

    private int orientation = 0;

    private final Paint paintLabels = new Paint() {{
        setAntiAlias(true);
        setTextAlign(Align.CENTER);
        setColor(0xffffffff);
    }};
    private final Paint paintBGs = new Paint() {{
        setAntiAlias(true);
        setColor(0xffe0e0e0);
    }};
    private final Paint paintWhite = new Paint(paintBGs) {{
        setAntiAlias(true);
        setColor(0xffffffff);
    }};
    private final Paint paintHours = new Paint() {{
        setAntiAlias(true);
        setColor(0x66ffffff);
        setTextAlign(Paint.Align.RIGHT);
    }};

    private final Rect hoursBounds = new Rect();
    private int rightPaneX;
    private int fontH;
    private int labelsY;

    private LinearLayout iv = new LinearLayout(getContext());

    private TideChartDrawer tideChartDrawer = null;

    private final SurfConditionsOneDayBitmaps[] bitmaps = new SurfConditionsOneDayBitmaps[7];
    private int dh = 0;

    private MainModel model;

    public Runnable onScrollYChanged = () -> {
    };
    public Runnable onTouchActionDown = null;


    public static class SurfConditionsOneDayBitmaps {
        //public SurfConditionsOneDay forSurfConditionsOneDay = null; // TODO: useless now, uncomment when will preorganize data by days in SurfConditionsProvider
        public Bitmap wave;
        public Bitmap wind;
    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        float offset = (float) (getScrollX() - dh * 8 + getWidth() / 2) / dh / 16;
//        Log.i(TAG, "onScrollChanged " + l + " " + oldl + " " + offset + Log.getStackTraceString(new Exception()));
        model.setSelectedDay(offset);
    }


    // Viewport width to real day width ratio
    public float getScale(int i) {
        return (float) getWidth() / (dh * 16);
    }


    // GESTURES \/


    private static final int SWIPE_MIN_DISTANCE = 5;
    private static final int SWIPE_THRESHOLD_VELOCITY = 300;
    private int mActiveFeature = -1;

    private float fx, fy;
    private boolean isScrollX = false;
    private boolean isScrollY = false;
    private boolean isScrollDirectionDetermined = false;

    public View underview = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int contentTop = getContentTop();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (event.getY() < contentTop - (underview.getAlpha() > 0.25f ? dh * 5 : 0)) {
                return false;
            }

            fx = event.getX();
            fy = event.getY();

            isScrollY = false;
            isScrollX = true;
            isScrollDirectionDetermined = false;

            scrollerY.abortAnimation();

            if (onTouchActionDown != null) onTouchActionDown.run();
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!isScrollDirectionDetermined) {
                double d = pow(fx - event.getX(), 2) + pow(fy - event.getY(), 2);
                if (d > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE) {
                    if (Math.abs(fx - event.getX()) < Math.abs(fy - event.getY())) {
                        isScrollY = true;
                        isScrollX = false;
                    }
                    isScrollDirectionDetermined = true;
                    if (!isScrollY && event.getY() < contentTop) {
                        isScrollX = false;
                    }
                }
            }
        }

        if (gestureDetector.onTouchEvent(event)) { //If the user swipes
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isScrollDirectionDetermined && !isScrollY && !isScrollX || !isScrollDirectionDetermined && event.getY() < contentTop && underview.getAlpha() > 0.25f) {
                isScrollY = false;
                isScrollX = false;
                underview.dispatchTouchEvent(event);
                quantizeScrollY();
                return true;
            }

            mActiveFeature = xToDay(getScrollX());
            showDaySmooth(mActiveFeature);
            quantizeScrollY();
            isScrollX = false;
            return true;
        }

        return true;
    }

    private int xToDay(int x) {
        return Math.max(0, Math.min(MainActivity.NDAYS - 1, (x + getWidth() / 2) / dh / 16));
    }

    private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isScrollX && Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (velocityX < 0) { //right to left
                    mActiveFeature = xToDay(getScrollX() + dh * 8);
                    smoothScrollTo(mActiveFeature * dh * 16 + dh * 8 - getWidth() / 2, 0);
                } else { //left to right
                    mActiveFeature = xToDay(getScrollX() - dh * 8);
                    smoothScrollTo(mActiveFeature * dh * 16 + dh * 8 - getWidth() / 2, 0);
                }
                isScrollX = false;
                return true;
            } else if (isScrollY) {
                if (velocityY <= 0 && scrollY >= dh * 16) {
                    model.getSelectedSpot().conditionsProvider.update();
                }
                if (Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    if (velocityY < 0) {
                        if (scrollY > getTideScrollY()) scrollY(getWindSwellScrollY());
                        else scrollY(getTideScrollY());
                    } else {
                        if (scrollY < getTideScrollY()) scrollY(0);
                        else if (scrollY < getWindSwellScrollY()) scrollY(getTideScrollY());
                        else scrollY(getWindSwellScrollY());
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e, float distanceX, float distanceY) {
            if (isScrollY) {
                scrollY += scrollY > 15.5f * dh ? distanceY / 5 : distanceY;

                if (scrollY < 0) scrollY = 0;
                else if (scrollY > 16.5f * dh) scrollY = 16.5f * dh;

                setScrollY(scrollY);

                repaint();

                return true;
            } else if (isScrollX) {
                scrollBy((int) distanceX, 0);
                return true;
            } else if (isScrollDirectionDetermined && underview.getAlpha() > 0.25f) {
                underview.dispatchTouchEvent(e);
            }
            return false;
        }
    };

    private final GestureDetector gestureDetector = new GestureDetector(this.getContext(), gestureListener);

    {
        gestureDetector.setIsLongpressEnabled(false);
    }


    // GESTURES /\


    public SurfConditionsForecastView(Context context) {
        this(context, null);
    }

    public SurfConditionsForecastView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfConditionsForecastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        model = MainModel.instance;

        model.tideDataProvider.addListener(
                new TideDataProvider.TideDataProviderListener() {
                    @Override
                    public void updated(String portID) {
                        redrawTide();
                    }

                    @Override
                    public void loadingStateChanged(String portID, boolean loading) {
                    }
                }
        );

        model.addChangeListener(MainModel.Change.SELECTED_DAY, changes -> {
//            Log.i(TAG, "Model.Change.SELECTED_DAY isScrollX=" + isScrollX);
            if (!isScrollX) showDay(model.getSelectedDayInt());
        });

        for (int i = 0; i < bitmaps.length; i++) {
            bitmaps[i] = new SurfConditionsOneDayBitmaps();
        }

        iv.setMinimumWidth(getWidth() * 7);
        iv.setMinimumHeight(getHeight());
        iv.setLayoutParams(new LayoutParams(getWidth() * 7, getHeight()));
        addView(iv);

        setHorizontalScrollBarEnabled(false);

        setOnTouchListener(null);

        scrollerY = new OverScroller(context);
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scrollerY.computeScrollOffset()) {
//            Log.i(TAG, "computeScroll() " + scrollerY.getCurrY() + " " + getTideScrollY() + " " + scrollerY.getFinalY());
            scrollY = scrollerY.getCurrY();
            onScrollYChanged.run();
            repaint();
        }
    }


    public void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }


    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
        redrawSurfConditions();
    }


    public void setMetrics(MetricsAndPaints metrics) {
        int dh = metrics.dh;

        if (dh == 0) return;
        if (this.dh == dh) return;

        this.dh = dh;

        fontH = metrics.fontH;
        labelsY = (dh - fontH) / 2;

        paintLabels.setTextSize(metrics.font);

        paintHours.setTextSize(metrics.font);
        paintHours.getTextBounds("18", 0, 2, hoursBounds);

        int w = dh * 16 * 7;
        iv.setMinimumWidth(w);
        iv.setMinimumHeight(getHeight());
        iv.setLayoutParams(new LayoutParams(w, getHeight()));

        if (tideChartDrawer == null) tideChartDrawer = new TideChartDrawer(this, model);
        else tideChartDrawer.updateDrawer();

        redrawSurfConditions();

        invalidate();
    }


    // --


    private void drawWindSwell(Canvas canvas) {
        int sx = getScrollX();

        float rectY = 0;

        float windY = rectY + dh * 0.5f;
        float swellY = rectY + dh * 3f;

//        Trace.beginSection("DRAW");
        int x = -dh;

        paintBGs.setColor(0xffe0e0e0);
        canvas.translate(0, rectY);
        canvas.drawRect(sx + 0, 0, sx + dh, dh * 12, paintBGs);
        canvas.drawRect(sx + rightPaneX, 0, sx + getWidth(), dh * 12, paintBGs);
        canvas.translate(0, -rectY);

        canvas.save();

        canvas.translate(dh + getScrollX() - labelsY, dh * 10);
        canvas.rotate(-90);
        //canvas.drawText("TIDE", dh * 1, 0, p);

        paintLabels.setColor(0xff006283);
        canvas.drawText(STR_SWELL_U, dh * 5.5f, 0, paintLabels);

        if (orientation == 1) {
            canvas.drawText(STR_FT, dh * 5f, rightPaneX, paintLabels);
            canvas.drawText(STR_S, dh * 6.5f, rightPaneX, paintLabels);
        } else {
            canvas.drawText(STR_FT, dh * 6f, rightPaneX, paintLabels);
            canvas.drawText(STR_S, dh * 4.5f, rightPaneX, paintLabels);
        }

        float alpha = scrollY > dh * 12.5 ? Math.min(1, (scrollY - dh * 12.5f) / (dh)) : 0;
        if (alpha > 0) {
            paintLabels.setColor(alpha(alpha, 0xff006283));
            canvas.drawText(STR_ENERGY_U, dh * 3f, 0, paintLabels);
            canvas.drawText(STR_KJ, dh * 3f, rightPaneX, paintLabels);
        }

        paintLabels.setColor(colorMidBlack);
        canvas.drawText(STR_WIND_U, dh * 8.5f, 0, paintLabels);
        canvas.drawText(STR_KMH, dh * 8.5f, rightPaneX, paintLabels);

        canvas.restore();


        canvas.save();
        canvas.clipRect(getScrollX() + dh, 0, getScrollX() + rightPaneX, getHeight());


        paintBGs.setColor(0xfff4f4f4);

        int rectH = dh * 12;

        int w = dh * 16;
        int i = 0;
        SurfConditionsProvider conditionsProvider = model.getSelectedSpot().conditionsProvider;
        for (SurfConditionsOneDayBitmaps b : bitmaps) {
            if (x + w > getScrollX()) {
                if (x > getScrollX() + getWidth()) break;

                canvas.translate(0, rectY);

                boolean detailed = conditionsProvider.isDetailed(i);
                if (detailed) {
                    canvas.drawRect(x + 0, 0, x + dh * 6, rectH, paintBGs);
                    canvas.drawRect(x + dh * 6, 0, x + dh * 12, rectH, paintWhite);
                    canvas.drawRect(x + dh * 12, 0, x + w, rectH, paintBGs);
                } else {
                    canvas.drawRect(x + 0, 0, x + dh * 3, rectH, paintBGs);
                    canvas.drawRect(x + dh * 3, 0, x + dh * 15, rectH, paintWhite);
                    canvas.drawRect(x + dh * 15, 0, x + w, rectH, paintBGs);
                }
                canvas.translate(0, -rectY);

                paintLabels.setColor(0x33000000);

                float textX = x + dh * 9;

                if (b.wind != null) canvas.drawBitmap(b.wind, x, windY, null);
                else canvas.drawText(STR_NO_WIND_DATA, textX, windY + dh + fontH / 2, paintLabels);

                if (b.wave != null) canvas.drawBitmap(b.wave, x, swellY, null);
                else
                    canvas.drawText(STR_NO_SWELL_DATA, textX, swellY + dh * 1.5f + fontH / 2, paintLabels);

                if (scrollY > dh * 12.5) {
                    alpha = Math.min(1, (scrollY - dh * 12.5f) / (dh));

//                    paintBGs.setAlpha((int) (alpha * 255));
//                    canvas.drawRect(x, swellY + dh * 5f, x + dh * 16, swellY + dh * 9f, paintBGs);
//                    canvas.drawRect(x, swellY + dh * 4.0f, x + dh * 16, swellY + dh * 9.4f, paintBGs);
//                    paintBGs.setAlpha(255);

                    paintLabels.setColor(alpha(alpha, MetricsAndPaints.colorBlack));

                    float ty = swellY + dh * 4f + model.metricsAndPaints.fontHDiv2;

                    SurfConditionsOneDay surfConditionsOneDay = conditionsProvider.get(i);

                    if (surfConditionsOneDay != null) {
                        for (Map.Entry<Integer, SurfConditions> entry : surfConditionsOneDay.getFixed().entrySet()) {
                            if (entry.getValue() == null) continue;
                            int energy = entry.getValue().waveEnergy;
                            float tx = x + 2f * dh * entry.getKey() / 60f / 24f * 8f + dh;
                            paintBGs.setColor(alpha(alpha, getColorForEnergy(energy)));
                            if (detailed) {
                                canvas.drawRect(tx - dh, swellY + dh * 3.5f, tx + dh, swellY + dh * 4.5f, paintBGs);
                            } else {
                                if (entry.getKey() == (int) (7.5 * 60))
                                    canvas.drawRect(x + 0, swellY + dh * 3.5f, x + dh * 7.5f, swellY + dh * 4.5f, paintBGs);
                                else if (entry.getKey() == (int) (12.0 * 60))
                                    canvas.drawRect(x + dh * 7.5f, swellY + dh * 3.5f, x + dh * 10.5f, swellY + dh * 4.5f, paintBGs);
                                else
                                    canvas.drawRect(x + dh * 10.5f, swellY + dh * 3.5f, x + w, swellY + dh * 4.5f, paintBGs);
                            }
                            paintLabels.setColor(alpha(alpha, getTextColorForEnergy(energy)));
                            canvas.drawText(String.valueOf(energy), tx, ty, paintLabels);
                        }
                    }

                    alpha = Math.min(1, (scrollY - dh * 14.5f) / (dh));
                    if (alpha > 0) {
                        paintLabels.setColor(alpha(scrollY > 16f * dh ? alpha : alpha * 0x66 / 0xff, colorBlack));

                        int sec = (int) ((System.currentTimeMillis() - model.getSelectedSpot().conditionsProvider.lastUpdate) / 1000);

                        String time;
                        if (sec > 60 * 60 * 2) time = sec / 60 / 60 + " hours";
                        else if (sec > 60 * 60) time = sec / 60 / 60 + " hour";
                        else if (sec > 60) time = sec / 60 + " min";
                        else time = sec + " sec";

                        int updateDY = (int) ((swellY + dh * 4.5f + (scrollY - dh * 5.5f)) / 2f);

                        canvas.drawText(scrollY > 16f * dh ? "RELEASE TO UPDATE" : "Updated " + time + " ago", textX, updateDY + fontH / 2, paintLabels);

                        alpha = Math.min(1, (scrollY - dh * 15.5f) / (dh));
                        float updateDW = dh * 12 * (alpha);
                        updateDY += dh * 0.5f + model.metricsAndPaints.density / 2;
                        canvas.drawRect(textX - updateDW, updateDY, textX + updateDW, updateDY + Math.round(model.metricsAndPaints.density), paintLabels);
                    }
                }
            }
            x += w;
            i++;
        }

        canvas.restore();
    }


    // --


    public int getContentTop() {
        return getContentTop((int) scrollY);
    }

    public int getContentTop(int scrollY) {
        return getHeight() - (scrollY <= tideChartDrawer.h ? scrollY : (scrollY - tideChartDrawer.h) * 6 / 8 + tideChartDrawer.h); //Math.max(tideChartDrawer.h, scrollY - dh * 2));
    }

    @Override
    public void onDraw(Canvas canvas) {
        int sx = getScrollX();

//        Log.i(TAG, "onDraw() | " + scrollY);

//        Trace.beginSection("DRAW");

        rightPaneX = getWidth() - dh;

        canvas.translate(0, getHeight() - scrollY + dh * 2);
        drawWindSwell(canvas);

        canvas.translate(0, -getHeight() + scrollY - dh * 2);

        canvas.translate(0, Math.min(tideChartDrawer.h, Math.max(0, -scrollY + tideChartDrawer.h)));

        if (tideChartDrawer != null)
            tideChartDrawer.draw(canvas, getWidth(), getHeight(), getScrollX(), orientation);

        int ddd = (dh * 16 - getWidth()) / 2;
        int k = Math.abs((sx - ddd + dh * 8) % (dh * 16) - dh * 8);
        float visible = Math.max(0f, Math.min(1f, (dh - k) / (dh / 2f)));
        if (visible > 0) {  // TODO move to TideChartDrawer
            canvas.save();
            canvas.translate(dh + getScrollX() - labelsY, getHeight());
            canvas.rotate(-90);

            paintLabels.setColor(((int) (0x66 * visible)) << 24 | 0xffffff);

            Calendar calendar = GregorianCalendar.getInstance(Common.TIME_ZONE);
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MINUTE, (getScrollX() + dh / 2) * 60 * 24 / (dh * 16));
            TideData tideData = tideChartDrawer.tideData; //MainModel.instance.tideDataProvider.getTideData(Common.BENOA_PORT_ID);

            if (tideData != null) {
                Integer sh = tideData.getTide(calendar.getTime().getTime() / 1000);
                if (sh == null) sh = 0;

                paintLabels.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(STR_TIDE_U, dh * 1.8f + sh * dh * 3 / 2 / 300, 0, paintLabels);

                calendar.add(Calendar.MINUTE, (getWidth() - dh) * 60 * 24 / (dh * 16));

                sh = tideData.getTide(calendar.getTime().getTime() / 1000);
                if (sh == null) sh = 0;

                canvas.drawText(STR_M, dh * 1.7f + sh * dh * 3 / 2 / 300, rightPaneX, paintLabels);

                paintLabels.setTextAlign(Paint.Align.CENTER);
            }
            canvas.restore();
        }

        //canvas.drawRect(0+sx, getHeight()-dh, getWidth()+sx, getHeight(), new Paint(paintHours){{setColor(0xee000000);}});

        canvas.save();
        canvas.clipRect(getScrollX() + dh * 1.5f, 0, getScrollX() + getWidth() - dh * 1.5f, getHeight());

        int selectedHour = round(model.getSelectedTime() / 60f);

        int selhour = selectedHour + model.getSelectedDayInt() * 24;

        if (orientation == 1) {
            paintHours.setTextAlign(Paint.Align.RIGHT);
            canvas.rotate(-90);
            int hx, hy;
            hy = -getHeight() + dh - (dh - hoursBounds.width()) / 2;
            for (int hour = 3; hour < 24 * 7; hour += 3) {
                hx = hour * dh * 16 / 24;
                String strH = String.valueOf(hour % 24);
                canvas.drawText(strH, hy, hx + hoursBounds.height() / 2, paintHours);
            }
        } else {
            paintHours.setTextAlign(Paint.Align.CENTER);
            int hx, hy;
            hy = getHeight() - (dh - hoursBounds.height()) / 2;
            for (int hour = 3; hour < 24 * 7; hour += 3) {
                hx = hour * dh * 16 / 24;
                String strH = String.valueOf(hour % 24);
                if (MainModel.instance.userStat.userLevel == 2) strH += ":00";
                if (hour == selhour) {
                    paintHours.setColor(MetricsAndPaints.colorWhite);
                }
                canvas.drawText(strH, hx, hy, paintHours);
                if (hour == selhour) {
                    paintHours.setColor(0x66ffffff);
                }
            }
            if (selhour % 3 != 0 && MainModel.instance.userStat.userLevel != 2) {
                hx = selhour * dh * 16 / 24;
                String strH = String.valueOf(selhour % 24);
                paintHours.setColor(MetricsAndPaints.colorWhite);
                canvas.drawText(strH, hx, hy, paintHours);
                paintHours.setColor(0x66ffffff);
            }
        }
        if (selectedHour > 0) {
            paintHours.setColor(MetricsAndPaints.colorWhite);
            int hx, hy;
            hy = (int) (getHeight() - model.metricsAndPaints.density);
            int hour = selectedHour + model.getSelectedDayInt() * 24;
            hx = hour * dh * 16 / 24;
            RectF r = new RectF(hx - dh * 16 / 24 / 2, hy, hx - dh * 16 / 24 / 2 + dh * 16 / 24, hy + model.metricsAndPaints.density);
            canvas.drawRect(r, paintHours);
            paintHours.setColor(0x66ffffff);
        }
        canvas.restore();

//        Trace.endSection();
    }


    public int dayToX(int i) {
        return i * dh * 16 + dh * 8 - getWidth() / 2; // i * dh * 16 + dh * 2;
    }

    public float scrollY = 0;
    private OverScroller scrollerY;

    public void scrollY(int y) {
        scrollY(y, 333);
    }

    public void scrollY(int y, int d) {
//        Log.i(TAG, "scrollY " + y + " now " + scrollY + " " + scrollerY.getCurrY() + " " + scrollerY.getFinalY());
        if (!scrollerY.isFinished()) {
            if (scrollerY.getFinalY() == y) return;
            scrollerY.abortAnimation();
        } else if (scrollY == y) return;

        scrollerY.startScroll(0, (int) scrollY, 0, y - (int) scrollY, d);
        repaint();
    }


//    @Override
//    public float getScrollY() {
//        return scrollY;
//    }

    public void setScrollY(float y) {
        scrollY = y;
        onScrollYChanged.run();
    }


    public void quantizeScrollY() {
        if (scrollY >= 16 * dh) model.updateSelectedSpotAll();

        if (scrollY > (getWindSwellScrollY() - getTideScrollY()) / 2 + getTideScrollY())
            scrollY(getWindSwellScrollY());
        else if (scrollY > getTideScrollY() / 2) scrollY(getTideScrollY());
        else scrollY(0);
    }


    public int getTideScrollY() {
        return 4 * dh;
    }

    public int getWindSwellScrollY() {
        return 12 * dh;
    }


    public float getTideVisibility() {
        return Math.max(0f, Math.min(1f, scrollY / getTideScrollY()));
    }


    public void showDaySmooth(int plusDays) {
        int newScrollX = dayToX(plusDays);
        if (getScrollX() != newScrollX) {
            Log.i(TAG, "showDaySmooth(" + plusDays + ") " + iv.getWidth() + " " + getScrollX() + " " + newScrollX + "   " + iv.getWidth());
            mActiveFeature = plusDays;
            smoothScrollTo(newScrollX, 0);
            Log.i(TAG, "showday " + iv.getWidth() + " " + getScrollX());
        }
    }


    public void showDay(int plusDays) {
        int newScrollX = dayToX(plusDays);
        if (getScrollX() != newScrollX && mActiveFeature != plusDays) {
            mActiveFeature = plusDays;
//            Log.i(TAG, "showDay(" + plusDays + ") " + getScrollX());
            smoothScrollBy(0, 0);
            smoothScrollTo(getScrollX(), 0); // KILL ME BUT IT WORKS ONLY THIS WAY!
            scrollTo(newScrollX, 0);
//            Log.i(TAG, "showDay<<");
        }
    }


    public Integer[] getShownDays() {
        float day1 = (float) getScrollX() / dh / 16;
        float day2 = (float) (getScrollX() + getWidth()) / dh / 16;
        if ((int) day1 == (int) day2) return new Integer[]{(int) day1};
        else return new Integer[]{(int) day1, (int) day2};
    }


    // --


    public SurfConditionsOneDayBitmapsAsyncDrawer surfConditionsOneDayBitmapsAsyncDrawer = null;

    public void redrawSurfConditions() {
//        Log.i(TAG, "redrawSurfConditions() | 1, dh = " + dh);
        if (dh == 0) return;

        SurfSpot surfSpot = model.getSelectedSpot();

        if (surfConditionsOneDayBitmapsAsyncDrawer != null && surfConditionsOneDayBitmapsAsyncDrawer.getStatus() != AsyncTask.Status.FINISHED)
            surfConditionsOneDayBitmapsAsyncDrawer.cancel(true);

        if (surfSpot == null || surfSpot.conditionsProvider.isNoData()) {
//            Log.i(TAG, "redrawSurfConditions() | cancelled" +
//                    ", surfspot = " + (surfSpot == null ? "null" : surfSpot.name) +
//                    (surfSpot != null ? ", isNoData = " + surfSpot.conditionsProvider.isNoData() : "")
//            );

            for (SurfConditionsOneDayBitmaps bitmap : bitmaps) {
//                bitmap.forSurfConditionsOneDay = null;
                bitmap.wind = null;
                bitmap.wave = null;
            }

            return;
        }

//        boolean allTheSame = true;
//        for (int i = 0; i < bitmaps.length; i++) {
//            if (bitmaps[i].forSurfConditionsOneDay != surfSpot.conditionsProvider.get(i)) {
//                allTheSame = false;
//                break;
//            }
//        }
//        if (allTheSame) {
//            Log.i(TAG, "redrawSurfConditions() | 2, allTheSame");
//            return;
//        }

//        Log.i(TAG, "redrawSurfConditions() | 2, starting, has data = " + !surfSpot.conditionsProvider.isNoData());

        redrawCurrentBitmaps();
    }


    public void redrawCurrentBitmaps() {
        SurfSpot surfSpot = model.getSelectedSpot();
        Integer[] shownDays = getShownDays();
        surfConditionsOneDayBitmapsAsyncDrawer = new SurfConditionsOneDayBitmapsAsyncDrawer(surfSpot, 0, new ArrayList<>(Arrays.asList(shownDays)), this);
        surfConditionsOneDayBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void redrawOtherBitmaps(ArrayList<Integer> days) {
        SurfSpot surfSpot = model.getSelectedSpot();
        surfConditionsOneDayBitmapsAsyncDrawer = new SurfConditionsOneDayBitmapsAsyncDrawer(surfSpot, 1, days, this);
        surfConditionsOneDayBitmapsAsyncDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public void newBitmaps(Map<Integer, SurfConditionsOneDayBitmaps> forecastBitmaps, int step) {
        for (Map.Entry<Integer, SurfConditionsForecastView.SurfConditionsOneDayBitmaps> fb : forecastBitmaps.entrySet()) {
//            view.bitmaps[fb.getKey()].forSurfConditionsOneDay = fb.getValue().forSurfConditionsOneDay;
            bitmaps[fb.getKey()].wind = fb.getValue().wind;
            bitmaps[fb.getKey()].wave = fb.getValue().wave;
        }

        if (step == 0) repaint();
    }


    public void redrawTide() {
        if (tideChartDrawer != null && tideChartDrawer.updateBitmaps()) {
            repaint();
        }
    }
}
