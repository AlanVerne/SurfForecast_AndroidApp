package com.avaa.surfforecast.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
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
    private static final String TAG = "MyList";

    private static final int DELAY_BEFORE_SLEEP = 60000;
    private static final int FALLING_ASLEEP_TIME = 160;
    private static final int AWAKENING_TIME = 240;

    private int dh = 0;

    private int spacing = 0;
    private int paddingLeft = 0;
    private int paddingTop = 0; //paddingLeft - spacing/2;

    private final LinearLayout layout;

    private PowerManager powerManager;

    private List<View> views = new ArrayList<>();
    private View selectedView = null;
    private boolean firstSelection = true;

    volatile private boolean awake = false;
    volatile private float awakeState = 0; // 0 - not awakened - no list, 1 - awakened - all spots list

    private long prevTime = -1;

    public interface OnSelectedListener {
        void onSelected(int i);
    }

    public OnSelectedListener onSelected = (i) -> {
    };

    private Runnable afterAwakened = null;
    private Runnable afterSlept = null;

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

    public interface ScrollListener {
        void scrolled(float shownI, float firstI, float lastI, float awakeState);

        void scrolled(float awakeState);
    }

    public ScrollListener sl = null;

    int pointers = 0;

    boolean ignoreSelectedViewSelection = false;


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
        powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutTransition(null);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addView(layout);
    }


    public void setDH(int dh) {
        this.dh = dh;

        spacing = dh / 2;
        paddingLeft = dh;
        paddingTop = dh / 2; //paddingLeft - spacing/2;
    }


    private boolean isPowerSavingMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && powerManager.isPowerSaveMode();
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        //Log.i(TAG, "computeScroll() " + awake + " " + prevTime + " " + awakeState);
        if (awake && awakeState != 1 || !awake && awakeState != 0) {
            long now = SystemClock.uptimeMillis();

            if (awakeState == 0) {
                for (final View view : views) {
                    if (view != selectedView) view.setVisibility(VISIBLE);
                }
            }

            if (isPowerSavingMode()) awakeState = awake ? 1f : 0f;
            else {
                awakeState += (float) (now - prevTime) * (awake ? 1f / FALLING_ASLEEP_TIME : -1f / AWAKENING_TIME);
                awakeState = Math.max(0, Math.min(1, awakeState));
            }

            if (awakeState == 1 && afterAwakened != null) {
                afterAwakened.run();
                afterAwakened = null;
            } else if (awakeState == 0 && afterSlept != null) {
                afterSlept.run();
                afterSlept = null;
            }

            if (awakeState == 0) {
                for (final View view : views) {
                    if (view != selectedView) view.setVisibility(INVISIBLE);
                }
            } else {
                for (final View view : views) {
                    if (view != selectedView) view.setAlpha(awakeState * 0.99f);
                }
            }

            prevTime = SystemClock.uptimeMillis();

            if (sl != null) sl.scrolled(awakeState);

            repaint();
        } else {
            prevTime = -1;
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


    public void awake(Runnable r) {
        if (awakeState == 1) r.run();
        else {
            afterAwakened = r;
            awake();
        }
    }

    public void awake() {
        //Log.i(TAG, "awake() " + awake + " " + prevTime + " " + awakeState);

        cancelSleepTimer();
        if (awake) if (prevTime != -1 || awakeState == 1) return;
        awake = true;
        if (prevTime == -1) {
            prevTime = SystemClock.uptimeMillis();
            repaint();
        }
    }

    public void scrollTo(View v, Runnable after) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "scrollY", v.getTop() - paddingTop).setDuration(AWAKENING_TIME);

        Animator.AnimatorListener al = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                if (after != null) after.run();
                objectAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (after != null) after.run();
                objectAnimator.removeAllListeners();
            }
        };

        objectAnimator.addListener(al);

        objectAnimator.start();
    }

    public void sleep() {
        cancelSleepTimer();
        //smoothScrollTo(0, selectedView.getTop() - paddingTop);
        if (firstSelection) {
            firstSelection = false;
            ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "scrollY", selectedView.getTop() - paddingTop).setDuration(0);
            objectAnimator.start();
        } else {
            ObjectAnimator objectAnimator = ObjectAnimator.ofInt(this, "scrollY", selectedView.getTop() - paddingTop).setDuration(AWAKENING_TIME);
            objectAnimator.start();
        }
        awake = false;
        if (prevTime == -1) {
            prevTime = SystemClock.uptimeMillis();
            repaint();
        }
    }


    private void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }


    private boolean isViewSelectable(View v) {
        return ((TextView) v).getTextSize() > dh / 2f;
    }


    public void setViews(List<View> views) {
        layout.removeAllViews();
        this.views = views;
        for (View view : views) {
            view.setPadding(paddingLeft, 0, spacing, 0);
            if (isViewSelectable(view)) view.setOnClickListener(this::select); //(v -> select(v));
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


    public void select(View view, Runnable after) {
        afterSlept = after;
        select(view);
    }

    public void select(View view) {
        if (view == null) return;
        //Log.i(TAG, "select() | " + "ignoreSelectedViewSelection = " + ignoreSelectedViewSelection);
        if (ignoreSelectedViewSelection) {
            ignoreSelectedViewSelection = false;
            if (selectedView == view) {
                resetSleepTimer();
                return;
            }
        }
        selectedView = view;
        selectedView.setAlpha(1);
        onSelected.onSelected(getIndex(selectedView));
        sleep();
    }


    public void updatePadding(int h) {
        if (!views.isEmpty()) {
            int pb = h - dh * 2 - paddingTop; //views.get(views.size() - 1).getHeight() - paddingTop*2;
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


    protected void onScrollStart() {
        cancelSleepTimer();
    }

    protected void onScrollEnd() {
        if (pointers == 0) resetSleepTimer();
    }


    public TextView getView(int i) {
        for (View view : views) {
            if (isViewSelectable(view)) {
                if (i-- == 0) return (TextView) view;
            }
        }
        return null;
    }

    public int getIndex(View v) {
        int i = 0;
        for (View view : views) {
            if (isViewSelectable(view)) {
                if (view == v) return i;
                i++;
            }
        }
        return -1;
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
            if (isViewSelectable(view)) {
                if (view.getTop() <= kt + paddingTop && si == -1)
                    si = i + (float) (kt + paddingTop - view.getTop()) / view.getHeight();

                if (fi == -1 && view.getTop() + view.getHeight() > kt)
                    fi = Math.max(i, i + (float) (kt - view.getTop()) / view.getHeight());
                if (li == -1 && view.getTop() + view.getHeight() > bottom)
                    li = Math.max(i, i + (float) (bottom - view.getTop()) / view.getHeight()) - 1;

                i++;
            }
        }

        //Log.i(TAG, getHeight() + "-h li-"+ li + " si-"+si + "   " +i);

        if (li == -1) li = i - 1f;

        si = Math.max(0f, Math.min(i - 1f, si));
        if (sl != null) sl.scrolled(si, fi, Math.min(i - 1f, li), awakeState);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //Log.i(TAG, "onInterceptTouchEvent() | " + awake + " " + prevTime + " " + awakeState);
            if (awakeState == 0 && ev.getY() > dh * 3.5) return false;
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

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (awakeState == 0 && ev.getY() > dh * 3.5) return false;
            awake();
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE) awake();
        else if (ev.getAction() == MotionEvent.ACTION_UP) {
            pointers--;
            if (pointers == 0 && !isScrolling()) resetSleepTimer();
        }

        //Log.i(TAG, "onTouchEvent() | " + ev.getAction() + awake + " " + prevTime + " " + awakeState);
        ignoreSelectedViewSelection = false;

        return super.onTouchEvent(ev);
    }
}

