package com.avaa.surfforecast.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Alan on 7 Jul 2016.
 */

public class MyList extends FeaturedScrollView {
    private static final int DELAY_BEFORE_SLEEP = 60000;
    private static final int FALLING_ASLEEP_TIME = 80;
    private static final int AWAKENING_TIME = 240;

    public static int spacing = 0;
    public static int paddingLeft = 0;
    public static int paddingTop  = 0; //paddingLeft - spacing/2;

    private final LinearLayout layout;

    private List<View> views = new ArrayList<>();
    private View selectedView = null;
    private boolean firstSelection = true;

    volatile private boolean awake = false;
    volatile private float   awakeState = 0;


    public MyList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layout = new LinearLayout(context);
        init();
    }
    public MyList(Context context, AttributeSet attrs) {
        super(context, attrs);
        layout = new LinearLayout(context);
        init();
    }
    public MyList(Context context) {
        this(context, null);
    }

    private void init() {
        //float density = getResources().getDisplayMetrics().density;

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutTransition(null); //new LayoutTransition());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addView(layout);
    }

    int dh = 0;
    public void setDH(int dh) {
        this.dh = dh;

        spacing = dh/2;
        paddingLeft = dh;
        paddingTop  = dh/2; //paddingLeft - spacing/2;
    }

    long prevTime = -1;

    @Override
    public void computeScroll() {
        super.computeScroll();
        //Log.i("ML", "computeScroll() " + awake + " " + prevTime + " " + awakeState);
        if (awake && awakeState != 1 || !awake && awakeState != 0) {
            long now = SystemClock.uptimeMillis();

            if (awakeState == 0) {
                for (final View view : views) {
                    if (view != selectedView) view.setVisibility(VISIBLE);
                }
            }

            awakeState += (float)(now - prevTime) * (awake ? 1f / FALLING_ASLEEP_TIME : -1f / AWAKENING_TIME);
            awakeState = Math.max(0, Math.min(1, awakeState));

            if (awakeState == 0) {
                for (final View view : views) {
                    if (view != selectedView) view.setVisibility(INVISIBLE);
                }
            }
            else {
                for (final View view : views) {
                    if (view != selectedView) view.setAlpha(awakeState*0.99f);
                }
            }

            prevTime = SystemClock.uptimeMillis();

            if (sl != null) sl.scrolled(awakeState);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
            else postInvalidate();
        }
        else {
            prevTime = -1;
        }
    }

    private Timer timerSleep = null;

    private final Handler handlerSleep = new Handler(msg -> {
        sleep();
        return true;
    });

    private class TimerTaskSleep extends TimerTask {
        @Override
        public void run() {
            handlerSleep.sendEmptyMessage(0);
        }
    }


    private void resetSleepTimer() {
        if (timerSleep != null) timerSleep.cancel();
        if (!awake) return;
        timerSleep = new Timer();
        timerSleep.schedule(new TimerTaskSleep(), DELAY_BEFORE_SLEEP);
    }
    private void cancelSleepTimer() {
        if (timerSleep != null) {
            timerSleep.cancel();
            timerSleep = null;
        }
    }


    private void awake() {
        //Log.i("ML", "awake() " + awake + " " + prevTime + " " + awakeState);

        cancelSleepTimer();
        if (awake) if (prevTime != -1 || awakeState == 1) return;
        awake = true;
        if (prevTime == -1) {
            prevTime = SystemClock.uptimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
            else postInvalidate();
        }
    }
    public void sleep() {
        cancelSleepTimer();
        //smoothScrollTo(0, selectedView.getTop() - paddingTop);
        if (firstSelection) {
            firstSelection = false;
            ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "scrollY", selectedView.getTop() - paddingTop).setDuration(0);
            objectAnimator.start();
        }
        else {
            ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "scrollY", selectedView.getTop() - paddingTop).setDuration(AWAKENING_TIME);
            objectAnimator.start();
        }
        awake = false;
        if (prevTime == -1) {
            prevTime = SystemClock.uptimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
            else postInvalidate();
        }
    }


    public void setViews(List<View> views) {
        layout.removeAllViews();
        this.views = views;
        for (View view : views) {
            view.setPadding(MyList.paddingLeft, 0, MyList.spacing, 0);
            layout.addView(view);
        }
//        views.get(views.size()-1).addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updatePadding());
//        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updatePadding());

        selectedView = views.get(0);

        layout.invalidate();
        layout.requestLayout();
        invalidate();
        requestLayout();
    }


    public void select(View view) {
        if (ignoreSelectedViewSelection) {
            ignoreSelectedViewSelection = false;
            if (selectedView == view) {
                resetSleepTimer();
                return;
            }
        }
        selectedView = view;
        selectedView.setAlpha(1);
        sleep();
    }


    public void updatePadding(int h) {
        if (!views.isEmpty()) {
            int pb = h - dh*2 - paddingTop; //views.get(views.size() - 1).getHeight() - paddingTop*2;
            if (layout.getPaddingBottom() != pb) {
                //layout.setPadding(90, 990, 90, 9990);
                layout.setPadding(0, paddingTop, 0, pb);
            }
        }
    }
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(w, h, oldw, oldh);
//        if (views != null && !views.isEmpty()) updatePadding();
//    }


    public interface ScrollListener {
        void scrolled(float shownI, float firstI, float lastI, float awakeState);
        void scrolled(float awakeState);
    }
    public ScrollListener sl = null;


    protected void onScrollStart() {
        cancelSleepTimer();
    }
    protected void onScrollEnd() {
        if (pointers == 0) resetSleepTimer();
    }


    public TextView getView(int i) {
        for (View view : views) {
            if (((TextView)view).getTextSize() >= dh) {
                if (i-- == 0) return (TextView)view;
            }
        }
        return null;
    }


    @Override
    protected void onScrollChanged(int l, int kt, int oldl, int oldt) {
        super.onScrollChanged(l, kt, oldl, oldt);

        float fi = -1;
        float li = -1;

        float si = -1;

        int i = 0;

        int bottom = kt + getHeight();
        for (View view : views) {
            if (((TextView)view).getTextSize() >= dh) {
                if (view.getTop() <= kt + paddingTop && si == -1)
                    si = i + (float)(kt + paddingTop - view.getTop()) / view.getHeight();

                if (fi == -1 && view.getTop() + view.getHeight() > kt)
                    fi = Math.max(i, i + (float)(kt - view.getTop()) / view.getHeight());
                if (li == -1 && view.getTop() + view.getHeight() > bottom)
                    li = Math.max(i, i + (float)(bottom - view.getTop()) / view.getHeight())-1;

                i++;
            }
        }

        //Log.i("ML", getHeight() + "-h li-"+ li + " si-"+si + "   " +i);

        if (li == -1) li = i - 1f;

        si = Math.max(0f, Math.min(i - 1f, si));
        if (sl != null) sl.scrolled(si, fi, Math.min(i-1f, li), awakeState);
    }

    int pointers = 0;


    boolean ignoreSelectedViewSelection = false;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //Log.i("ML", "onInterceptTouchEvent " + awake + " " + prevTime + " " + awakeState);
            if (!awake) {
                ignoreSelectedViewSelection = true;
            }
            awake();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        pointers = ev.getPointerCount();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) awake();
        if (ev.getAction() == MotionEvent.ACTION_MOVE) awake();
        else if (ev.getAction() == MotionEvent.ACTION_UP) {
            pointers--;
            if (pointers == 0 && !isScrolling()) resetSleepTimer();
        }

        ignoreSelectedViewSelection = false;

        return super.onTouchEvent(ev);
    }
}
