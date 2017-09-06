package com.avaa.surfforecast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.MainActivity;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.drawers.SurfConditionsOneDayBitmapsAsyncDrawer;
import com.avaa.surfforecast.drawers.TideChartDrawer;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import static com.avaa.surfforecast.data.Common.*;


/**
 * Created by Alan on 1 Aug 2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfConditionsForecastView extends HorizontalScrollView { //extends View {
    private static final String TAG = "SCForecastView";

    public int orientation = 0;

    private Paint paintLabels;
    private Paint paintBGs = new Paint(){{
        setAntiAlias(true);
        setColor(0xffe0e0e0);
    }};
    private Paint paintHours;
    private Rect  hoursBounds;

    private LinearLayout iv = new LinearLayout(getContext());

    private TideChartDrawer tideChartDrawer = null;

    public final SurfConditionsOneDayBitmaps[] bitmaps = new SurfConditionsOneDayBitmaps[7];
    private int dh = 0;

    private SurfSpot surfSpot = null;

    private MainModel model;


    public static class SurfConditionsOneDayBitmaps {
        //public SurfConditionsOneDay forSurfConditionsOneDay = null; TODO useless now, uncomment when will preorganize data by days in SurfConditionsProvider
        public Bitmap wave;
        public Bitmap wind;
    }


    public interface OnScrollChangedListener {
        void onScrollChanged(int i, float offset, float scale);
    }
    private OnScrollChangedListener mOnScrollChangedListener;
    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            float offset = (float)(getScrollX() - dh*8 + getWidth()/2) / dh / 16;
            int i = (int)offset;
            if (offset - i < 0.5) {
                offset = offset - i;
                mOnScrollChangedListener.onScrollChanged(i, offset, getScale(i));
            }
            else {
                i++;
                offset = i - offset;
                mOnScrollChangedListener.onScrollChanged(i, -offset, getScale(i));
            }
        }
    }
    public float getScale(int i) {
        return (float)getWidth()/(dh*16);
    }


//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//
//        return super.onTouchEvent(ev);
//    }


    float fx, fy;
    boolean isScrollY = false;

    public Runnable onTouchActionDown = null;
    OnTouchListener tl = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            if (event.getY() < getContentTop()) return false;

            if (event.getAction() == MotionEvent.ACTION_DOWN && onTouchActionDown != null) {
                onTouchActionDown.run();
            }

            if (mGestureDetector.onTouchEvent(event)) { //If the user swipes
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                mActiveFeature = xToDay(getScrollX());
                smoothScrollTo(dayToX(mActiveFeature), 0);
                if (isScrollY) {
                    if (scrollY > (dh*12+dh*4)/2) scrollY(dh*12);
                    if (scrollY > (dh*4+0)/2) scrollY(dh*4);
                    else scrollY(0);
                }
                return true;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (isScrollY) {
                    scrollY -= event.getY() - fy;
                    if (scrollY < 0) scrollY = 0;
                    fy = event.getY();
                    onScrollY.run();
                    postInvalidate();
                    return true;
                }
            }
            return false;
        }
    };
    private int xToDay(int x) {
        return Math.max(0, Math.min(MainActivity.NDAYS-1, (x+getWidth()/2)/ dh/16));
    }
    private static final int SWIPE_MIN_DISTANCE = 5;
    private static final int SWIPE_THRESHOLD_VELOCITY = 300;
    private int mActiveFeature = 0;

    private final GestureDetector.OnGestureListener ogl = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (velocityX < 0) { //right to left
                        mActiveFeature = xToDay(getScrollX() + dh * 8);
                        smoothScrollTo(mActiveFeature * dh * 16 + dh * 8 - getWidth() / 2, 0);
                    }
                    else { //left to right
                        mActiveFeature = xToDay(getScrollX() - dh * 8);
                        smoothScrollTo(mActiveFeature * dh * 16 + dh * 8 - getWidth() / 2, 0);
                    }
                    return true;
                }
                else if (Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    if (velocityY < 0) {
                        scrollY(dh * 12);
                    }
                    else {
                        scrollY(0);
                    }
                    return true;
                }
            }
            catch (Exception e) {
                Log.e("Fling", "There was an error processing the Fling event:" + e.getMessage());
            }
            return false;
        }
    };
    GestureDetector mGestureDetector = new GestureDetector(this.getContext(), ogl);


    public SurfConditionsForecastView(Context context) {
        this(context, null);
    }
    public SurfConditionsForecastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public SurfConditionsForecastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        model = MainModel.instance;

        MainModel.instance.tideDataProvider.addListener(new TideDataProvider.TideDataProviderListener() {
            @Override
            public void updated(String portID) {
                redrawTide();
            }
            @Override
            public void loadingStateChanged(String portID, boolean loading) { }
        }
        );

        for (int i = 0; i < bitmaps.length; i++) {
            bitmaps[i] = new SurfConditionsOneDayBitmaps();
        }

        iv.setMinimumWidth(getWidth()*7);
        iv.setMinimumHeight(getHeight());
        iv.setLayoutParams(new LayoutParams(getWidth()*7, getHeight()));
        addView(iv);

        setHorizontalScrollBarEnabled(false);

        setOnTouchListener(tl);

        scrollerY = new OverScroller(getContext());
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scrollerY.computeScrollOffset()) {
            scrollY = scrollerY.getCurrY();
            onScrollY.run();
            postInvalidateOnAnimation();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getY() < getContentTop()-dh*4) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            fx = event.getX();
            fy = event.getY();
            isScrollY = false;
            scrollerY.abortAnimation();
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!isScrollY && Math.abs(fx - event.getX()) < 15 && Math.abs(fy - event.getY()) > 15) {
                isScrollY = true;
            }
        }

//        if (!isScrollY && event.getY() < getContentTop()) return false;

        return super.onTouchEvent(event);
    }


    public void setOrientation(int orientation) {
        this.orientation = orientation;
        redrawSurfConditions();
    }


    Rect paintLabelsRect = new Rect();
    int textH;
    int labelsY;
    public void setDH(int dh) {
        if (dh == 0) return;
        if (this.dh == dh) return;

        this.dh = dh;
        //condDrawer.setDH(dh);

        paintLabels = new Paint() {{
            setAntiAlias(true);
            setColor(0xffffffff);
            setTextSize(dh/2);
            setTextAlign(Align.CENTER);
        }};
        paintLabels.getTextBounds("W", 0, 1, paintLabelsRect);
        textH = paintLabelsRect.height();
        labelsY = (dh - textH)/2;
        paintHours = new Paint() {{
            setAntiAlias(true);
            setTextSize(MainModel.instance.metricsAndPaints.font);
            setColor(0x66ffffff);
            setTextAlign(Align.RIGHT);
        }};
        hoursBounds = new Rect();
        paintHours.getTextBounds("18", 0, 2, hoursBounds);

        iv.setMinimumWidth(dh * 16 * 7);
        iv.setMinimumHeight(getHeight());
        iv.setLayoutParams(new LayoutParams(dh * 16 * 7, getHeight()));

        if (tideChartDrawer == null) tideChartDrawer = new TideChartDrawer(this, model);
        else tideChartDrawer.updateDrawer();

        redrawSurfConditions();

        //showDay(0);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                showDay(0);
            }
        }, 200);

        invalidate();
    }


    int rightPaneX;

    public Runnable onScrollY = new Runnable() {
        @Override
        public void run() {

        }
    };


    private void drawWindSwell(Canvas canvas) {
        int sx = getScrollX();

        float recty = 0;//getHeight() - dh*10f;

        float windy = recty + dh*0.5f;
        float swelly = recty + dh*3f;


        Trace.beginSection("DRAW");
        int x = -dh;

        paintBGs.setColor(0xffe0e0e0);
        canvas.translate(0, recty);
        canvas.drawRect(sx+0, 0, sx+dh, dh*8, paintBGs);
        canvas.drawRect(sx+rightPaneX, 0, sx+getWidth(), dh*8, paintBGs);
        canvas.translate(0, -recty);

        canvas.save();

        canvas.translate(dh + getScrollX() - labelsY, dh*10);
        canvas.rotate(-90);
        //canvas.drawText("TIDE", dh * 1, 0, p);

        paintLabels.setColor(0xff006283);
        canvas.drawText(STR_SWELL_U, dh * 5.5f, 0, paintLabels);

        if (orientation == 1) {
            canvas.drawText(STR_FT, dh * 5f, rightPaneX, paintLabels);
            canvas.drawText(STR_S, dh * 6.5f, rightPaneX, paintLabels);
        }
        else {
            canvas.drawText(STR_FT, dh * 6f, rightPaneX, paintLabels);
            canvas.drawText(STR_S, dh * 4.5f, rightPaneX, paintLabels);
        }
        paintLabels.setColor(0x66000000);
        canvas.drawText(STR_WIND_U, dh*8.5f, 0, paintLabels);
        canvas.drawText(STR_KMH, dh*8.5f, rightPaneX, paintLabels);

        canvas.restore();


        canvas.save();
        canvas.clipRect(getScrollX() + dh, 0, getScrollX() + rightPaneX, getHeight());



        paintBGs.setColor(0xfff4f4f4);

        Paint paintWhite = new Paint(paintBGs);
        paintWhite.setColor(0xffffffff);

        int w = dh * 16;
        int i = 0;
        SurfConditionsProvider conditionsProvider = model.getSelectedSpot().conditionsProvider;
        for (SurfConditionsOneDayBitmaps b : bitmaps) {
            if (x + w > getScrollX()) {
                if (x > getScrollX() + getWidth()) break;

                canvas.translate(0, recty);
                if (conditionsProvider.isDetailed(i)) {
                    canvas.drawRect(x + 0, 0, x + dh * 6, dh * 8, paintBGs);
                    canvas.drawRect(x + dh * 6, 0, x + dh * 12, dh * 8, paintWhite);
                    canvas.drawRect(x + dh * 12, 0, x + dh * 16, dh * 8, paintBGs);
                }
                else {
                    canvas.drawRect(x + 0, 0, x + dh * 3, dh * 8, paintBGs);
                    canvas.drawRect(x + dh * 3, 0, x + dh * 15, dh * 8, paintWhite);
                    canvas.drawRect(x + dh * 15, 0, x + dh * 16, dh * 8, paintBGs);
                }
                canvas.translate(0, -recty);

                paintLabels.setColor(0x33000000);

                if (b.wind != null) canvas.drawBitmap(b.wind, x, windy, null);
                else canvas.drawText(STR_NO_WIND_DATA, x+dh*9, windy+dh+textH/2, paintLabels);

                if (b.wave != null) canvas.drawBitmap(b.wave, x, swelly, null);
                else canvas.drawText(STR_NO_SWELL_DATA, x+dh*9, swelly+dh*1.5f+textH/2, paintLabels);
            }
            x += w;
            i++;
        }
        canvas.restore();
    }


    public int scrollY = 0;


    public int getContentTop() {
        return getHeight() - (scrollY <= tideChartDrawer.h ? scrollY : Math.max(tideChartDrawer.h, scrollY-dh*2));
    }


    @Override
    public void onDraw(Canvas canvas) {
        int sx = getScrollX();

        Log.i(TAG, "onDraw() | " + scrollY);

        Trace.beginSection("DRAW");

        rightPaneX = getWidth()-dh;

        canvas.translate(0, getHeight()-scrollY+dh*2);
        drawWindSwell(canvas);

        canvas.translate(0, -getHeight()+scrollY-dh*2);

        canvas.translate(0, Math.min(tideChartDrawer.h, Math.max(0, -scrollY + tideChartDrawer.h)));

        if (tideChartDrawer != null) tideChartDrawer.draw(canvas, getWidth(), getHeight(), getScrollX(), orientation);

        int ddd = (dh*16 - getWidth())/2;
        int k = Math.abs((sx-ddd+dh*8)%(dh*16)-dh*8);
        float visible = Math.max(0f, Math.min(1f, (dh - k)/(dh/2f)));
        if (visible > 0) {  // TODO move to TideChartDrawer
            canvas.save();
            canvas.translate(dh + getScrollX() - labelsY, getHeight());
            canvas.rotate(-90);

            paintLabels.setColor(((int)(0x66 * visible)) << 24 | 0xffffff);

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
        canvas.clipRect(getScrollX() + dh*1.5f, 0, getScrollX() + getWidth()- dh*1.5f, getHeight());
        if (orientation == 1) {
            paintHours.setTextAlign(Paint.Align.RIGHT);
            canvas.rotate(-90);
            int hx, hy;
            hy = -getHeight() + dh - (dh - hoursBounds.width())/2;
            for (int hour = 3; hour < 24 * 7; hour += 3) {
                hx = hour * dh * 16 / 24;
                String strH = String.valueOf(hour % 24);
                canvas.drawText(strH, hy, hx + hoursBounds.height()/2, paintHours);
            }
        }
        else {
            paintHours.setTextAlign(Paint.Align.CENTER);
            int hx, hy;
            hy = getHeight() - (dh - hoursBounds.height())/2;
            for (int hour = 3; hour < 24 * 7; hour += 3) {
                hx = hour * dh * 16 / 24;
                String strH = String.valueOf(hour % 24);
                if (MainModel.instance.userStat.userLevel == 2) strH += ":00";
                canvas.drawText(strH, hx, hy, paintHours);
            }
        }
        canvas.restore();

        Trace.endSection();
    }


    public int dayToX(int i) {
        return i * dh*16 + dh*8 - getWidth()/2; // i * dh * 16 + dh * 2;
    }


    private OverScroller scrollerY;
    public void scrollY(int y) {
        scrollerY.startScroll(0, scrollY, 0, y-scrollY);
    }


    public void showDaySmooth(int i) {
        int newScrollX = dayToX(i);
        if (getScrollX() != newScrollX) {
            //Log.i(TAG, "showday " + i + " " + iv.getWidth() + " " + getScrollX() + " " + newScrollX + "   " + iv.getWidth());
            //smoothScrollBy(0, 0);
            smoothScrollTo(newScrollX, 0);
            //Log.i(TAG, "showday " + i + " " + iv.getWidth() + " " + getScrollX());
        }
    }
    public void showDay(int i) {
        int newScrollX = dayToX(i);
        if (getScrollX() != newScrollX) {
            //Log.i(TAG, "showday " + i + " " + iv.getWidth() + " " + getScrollX() + " " + newScrollX + "   " + iv.getWidth());
            smoothScrollBy(0, 0);
            scrollTo(newScrollX, 0);
            //Log.i(TAG, "showday " + i + " " + iv.getWidth() + " " + getScrollX());
        }
    }

//    @Override
//    public void scrollTo(int x, int y) {
//        super.scrollTo(x, y);
//        Log.i(TAG, "SCROLL TO " + x);
//    }
//    @Override
//    public void scrollBy(int x, int y) {
//        super.scrollBy(x, y);
//        Log.i(TAG, "SCROLL BY " + x);
//    }

    public Integer[] getShownDays() {
        float day1 = (float)getScrollX() / dh / 16;
        float day2 = (float)(getScrollX() + getWidth()) / dh / 16;
        if ((int) day1 == (int) day2) return new Integer[]{(int) day1};
        else return new Integer[]{(int)day1, (int)day2};
    }


    // --


    public SurfConditionsOneDayBitmapsAsyncDrawer surfConditionsOneDayBitmapsAsyncDrawer = null;
    public void redrawSurfConditions() {
        Log.i(TAG, "redrawSurfConditions() | 1, dh = " + dh);
        if (dh == 0) return;

        SurfSpot surfSpot = model.getSelectedSpot();

        if (surfConditionsOneDayBitmapsAsyncDrawer != null && surfConditionsOneDayBitmapsAsyncDrawer.getStatus() != AsyncTask.Status.FINISHED) surfConditionsOneDayBitmapsAsyncDrawer.cancel(true);

        if (surfSpot == null || surfSpot.conditionsProvider.isNoData()) {
            Log.i(TAG, "redrawSurfConditions() | cancelled" +
                    ", surfspot = " + (surfSpot == null ? "null" : surfSpot.name) +
                    (surfSpot != null ? ", isNoData = " + surfSpot.conditionsProvider.isNoData() : "")
            );

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

        Log.i(TAG, "redrawSurfConditions() | 2, starting, has data = " + !surfSpot.conditionsProvider.isNoData());

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

        if (step == 0) postInvalidate();
    }


    public void redrawTide() {
        if (tideChartDrawer != null && tideChartDrawer.updateBitmaps()) {
            postInvalidate();
        }
    }
}
