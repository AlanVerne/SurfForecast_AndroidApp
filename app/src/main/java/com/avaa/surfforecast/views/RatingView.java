package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.avaa.surfforecast.R;


/**
 * Created by Alan on 28 Jan 2017.
 */


public class RatingView extends View {
    private static Drawable d = null;
    private static Drawable dTr;
    private static Drawable dB;

    private final static int N = 7;

    private float rating;       // final
    private float minorRating;  // wave rating


    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRating(float rating, float minorRating) {
        this.rating = rating * N;
        this.minorRating = minorRating * N;
        repaint();
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (d == null) {
            d = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_white_24dp);
            dTr = ContextCompat.getDrawable(getContext(), R.drawable.ic_remove_white_24dp); //ic_star_white_tr_24dp);
            dB = ContextCompat.getDrawable(getContext(), R.drawable.ic_star_border_white_24dp);
        }

        d.setAlpha(255);
        dTr.setAlpha(255);
        dB.setAlpha(255);

        int height = getHeight();
        for (int i = 0; i < N; i++) {
            int x = i * height;
            if (rating - 0.75f > i) {
                d.setBounds(x, 0, x + height, height);
                d.draw(canvas);
            } else if (minorRating - 0.75f > i) {
//                dTr.setBounds(x, 0, x + height, height);
//                dTr.draw(canvas);
                dB.setBounds(x, 0, x + height, height);
                dB.draw(canvas);
            } else {
                dTr.setBounds(x, 0, x + height, height);
                dTr.draw(canvas);
            }
        }
    }

    public static void drawStatic(Canvas canvas, int x, int y, int height, float rating, float minorRating, int alpha) {
        if (d == null) return;
        rating *= N;
        minorRating *= N;
        for (int i = 0; i < N; i++) {
            x += height;
            if (rating - 0.75f > i) {
                d.setBounds(x, y, x + height, y + height);
                d.setAlpha(alpha);
                d.draw(canvas);
            } else if (minorRating - 0.75f > i) {
//                dTr.setBounds(x, y, x + height, y + height);
//                dTr.setAlpha(alpha);
//                dTr.draw(canvas);
                dB.setBounds(x, y, x + height, y + height);
                dB.setAlpha(alpha);
                dB.draw(canvas);
            } else {
                dTr.setBounds(x, y, x + height, y + height);
                dTr.setAlpha(alpha);
                dTr.draw(canvas);
            }
        }
    }

    public void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postInvalidateOnAnimation();
        else postInvalidate();
    }
}
