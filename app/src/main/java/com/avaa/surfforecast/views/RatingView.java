package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.avaa.surfforecast.R;

import static android.R.attr.x;

/**
 * Created by Alan on 28 Jan 2017.
 */

public class RatingView extends View {
    private final static int N = 10;

    private float rating;       // final
    private float minorRating;  // wave rating

    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRating(float rating, float minorRating) {
        this.rating = rating*N;
        this.minorRating = minorRating*N;
        repaint();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_24dp);
        Drawable dTr = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_tr_24dp);
        Drawable dB = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_border_white_24dp);

        int height = getHeight();
        for (int i = 0; i < N; i++) {
            int x = i * height;
            if (rating-0.75f > i) {
                d.setBounds(x, 0, x + height, height);
                d.draw(canvas);
            }
            else if (minorRating-0.75f > i) {
                dTr.setBounds(x, 0, x + height, height);
                dTr.draw(canvas);
                dB.setBounds(x, 0, x + height, height);
                dB.draw(canvas);
            }
            else {
                dTr.setBounds(x, 0, x + height, height);
                dTr.draw(canvas);
            }
        }

    }

    public void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }
}
