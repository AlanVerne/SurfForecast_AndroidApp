package com.avaa.surfforecast.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Alan on 9 Jul 2016.
 */

public class FeaturedScrollView extends ScrollView {
    private static final int MAX_SCROLL_CHANGE_INTERVAL = 100;
    private static final int SCROLL_CHANGE_LISTENER_INTERVAL = 100;

    private long lastScrollUpdate = -1;

    public FeaturedScrollView(Context context) {
        super(context);
    }

    public FeaturedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FeaturedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onScrollStart() {
    }

    protected void onScrollEnd() {
    }

    protected boolean isScrolling() {
        return lastScrollUpdate != -1;
    }

    private class ScrollStateHandler implements Runnable {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastScrollUpdate) > MAX_SCROLL_CHANGE_INTERVAL) {
                lastScrollUpdate = -1;
                onScrollEnd();
            } else {
                postDelayed(this, SCROLL_CHANGE_LISTENER_INTERVAL);
            }
        }
    }

    @Override
    protected void onScrollChanged(int l, int kt, int oldl, int oldt) {
        super.onScrollChanged(l, kt, oldl, oldt);

        if (lastScrollUpdate == -1) {
            onScrollStart();
            postDelayed(new ScrollStateHandler(), SCROLL_CHANGE_LISTENER_INTERVAL);
        }

        lastScrollUpdate = System.currentTimeMillis();
    }
}
