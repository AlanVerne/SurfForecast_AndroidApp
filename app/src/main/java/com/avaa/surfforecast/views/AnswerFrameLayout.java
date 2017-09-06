package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.R;
import com.avaa.surfforecast.ai.Answer;
import com.avaa.surfforecast.drawers.MetricsAndPaints;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alan on 24 Jan 2017.
 */

public class AnswerFrameLayout extends FrameLayout {
    private final MetricsAndPaints metricsAndPaints;

    private ScrollView sv;
    private LinearLayout ll;
    private TextView tvHeader1;
    private TextView tvHeader2;

    private TextView tvText;

    private RelativeLayout[] rlOpts;


    public AnswerFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        metricsAndPaints = MainModel.instance.metricsAndPaints;

        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sv = new ScrollView(context);
        sv.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.setPadding(0, 0, 0, 60 * 3);

        tvHeader1 = new TextView(context);
        tvHeader1.setTextColor(0xffffffff);
        tvHeader1.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontHeader);
        tvHeader1.setPadding(60 * 3, 60 * 3, 90 * 3, 23 * 3);
        tvHeader1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvHeader2 = new TextView(context);
        tvHeader2.setTextColor(0xffffffff);
        tvHeader2.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontBig);
        tvHeader2.setPadding(60 * 3, 0 * 3, 90 * 3, 43 * 3);
        tvHeader2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvText = new TextView(context);
        tvText.setTextColor(0xff000000);
        tvText.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontBig);
        tvText.setPadding(60 * 3, 43 * 3, 90 * 3, 23 * 3);
        tvText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ll.addView(tvHeader1);
        ll.addView(tvHeader2);
        ll.addView(tvText);

        sv.addView(ll);

        addView(sv);

        setBackgroundColor(0x00000000);

        tvHeader1.setOnClickListener(v -> bgClick.run());
        tvHeader2.setOnClickListener(v -> bgClick.run());
    }


    public static final Map<String, Integer> STRING_TO_DRAWABLE_RESOURCE = new HashMap<String, Integer>() {{
        put("spot", R.drawable.ic_place_black_24dp);
        put("cam", R.drawable.ic_camera_alt_black_24dp);
        put("time", R.drawable.ic_access_time_black_24dp);
        put("date", R.drawable.ic_event_black_24dp);
        put("cond", R.drawable.ic_equalizer_black_24dp);
        put("q", R.drawable.ic_help_outline_black_24dp);
        put("i", R.drawable.ic_info_outline_black_24dp);
        put("close", R.drawable.ic_clear_black_24dp);
        put("ok", R.drawable.ic_check_black_24dp);
    }};


    public void set(Answer a) {
        tvHeader1.setText(a.forCommand);

        tvHeader2.setText(a.clarification);
        final int padding2 = a.clarification == null ? 13 * 3 : 60 * 3;
        tvHeader2.setPadding(60 * 3, 0 * 3, 90 * 3, padding2);

        tvText.setText(a.toShow);

        if (a.toShow.length() > 50)
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontBig);
        else tvText.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontHeader);

        final int padding = a.waitForReply ? 23 * 3 : 43 * 3;
        tvText.setPadding(60 * 3, 63 * 3, 90 * 3, padding);

        if (a.replyVariants != null) {
            rlOpts = new RelativeLayout[a.replyVariants.length];
            for (int i = 0; i < a.replyVariants.length; i++) {
                String s = a.replyVariants[i];

                if (s.startsWith("-")) {
                    s = s.substring(1);
                    rlOpts[i] = (RelativeLayout) inflate(getContext(), R.layout.opt_red, null);
                } else {
                    rlOpts[i] = (RelativeLayout) inflate(getContext(), R.layout.opt, null);
                }

                if (s.startsWith("[")) {
                    String image = s.substring(1, s.indexOf("]"));
                    s = s.substring(s.indexOf("]") + 1);

                    int d = STRING_TO_DRAWABLE_RESOURCE.get(image);
                    if (d != 0) rlOpts[i].findViewById(R.id.iv).setBackgroundResource(d);
                }

                ((TextView) rlOpts[i].findViewById(R.id.tv)).setText(s);
                ((TextView) rlOpts[i].findViewById(R.id.tv)).setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontBig);
                ;

                int finalI = i;
                rlOpts[i].findViewById(R.id.tv).setOnClickListener(v -> {
                    optClick.onClick(rlOpts[finalI].findViewById(R.id.tv));
                });

                ll.addView(rlOpts[i]);
            }
        }
    }


    private static final FastOutSlowInInterpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

    private final Paint bgShadow = new Paint() {{
        setAntiAlias(false);
        setColor(0xffffffff);
    }};
    private final Paint bgMain = new Paint() {{
        setAntiAlias(true);
        setColor(0xffffffff);
    }};
    private final Paint bgHeader = new Paint() {{
        setAntiAlias(false);
        setColor(0xff0091c1);
    }};

    private long prevTime = -1;
    private int state = 0;
    private float i = 0;

    public Runnable bgClick = null;
    public OnClickListener optClick = null;
    public Runnable onShown = null;
    public Runnable onHidden = null;

    Path p = new Path();
    float pi = -1;


    @Override
    public void draw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        long now = SystemClock.uptimeMillis();
        i = state == 1 ? i + (now - prevTime) / 300f : i - (now - prevTime) / 200f;
        prevTime = now;

        if (i <= 0) {
            super.setVisibility(INVISIBLE);
            i = 0;
        } else if (i >= 1) i = 1;
        else repaint();

        float i = state == 1 ? FAST_OUT_SLOW_IN_INTERPOLATOR.getInterpolation(this.i) : 1 - FAST_OUT_SLOW_IN_INTERPOLATOR.getInterpolation(1 - this.i);

        float r = (height + 300); // - getPaddingTop());
        float r2 = height - tvText.getTop() + sv.getScrollY(); //(getPaddingBottom());

        float ar = r2; //(r+r2)/2;

        if (state == 2) ar *= i;
        else ar = ar / 2 + ar / 2 * i;

        r = ar + (r - ar) * i;
        r2 = ar;


        int x = width * 1 / 3;

        float y = height; //+width*i;
//        r  += width*i;
//        r2 += width*i;

        if (pi != i) {
            pi = i;
            p.reset();
            p.addCircle(state == 2 ? width * (1 - i * 2 / 3) : x, y, r, Path.Direction.CCW);
            //p.addCircle(state == 2 ? width *(1 - i*2/3) : x, y, r2, Path.Direction.CW);
        }

        bgShadow.setColor((int) (0xaa * i) * 0x1000000 | 0x333333);
        canvas.drawRect(0, 0, width, height, bgShadow);

        sv.setAlpha(Math.max(0, i * 4 - 3));

        if (this.i == 1) {
            if (onShown != null) {
                onShown.run();
                onShown = null;
            }
            canvas.drawRect(0, 0, getWidth(), tvText.getTop() + 200 * 3, bgHeader);
            canvas.drawCircle(state == 2 ? width * (1 - i * 2 / 3) : x, y, r2, bgMain);
            super.draw(canvas);
        } else {
            if (this.i == 0 && state == 2) {
                onHidden.run();
                onHidden = null;
            }

            canvas.save();
            canvas.clipPath(p);

            canvas.drawRect(0, height - r, width, height, bgHeader);

            canvas.drawCircle(state == 2 ? width * (1 - i * 2 / 3) : x, y, r2, bgMain);

            //canvas.translate(0, (1-i)*getHeight()/8);

            super.draw(canvas);

            canvas.restore();
        }
    }


    private void repaint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }


    @Override
    public void setVisibility(int visibility) {
        if (visibility == VISIBLE) {
            super.setVisibility(visibility);
            state = 1;
        } else if (visibility == INVISIBLE) state = 2;

        prevTime = SystemClock.uptimeMillis();

        repaint();
    }


//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
//        if (event.getY() < getPaddingTop()) bgClick.run();
//        return getVisibility() == VISIBLE;
//    }


    public int getState() {
        return state;
    }
}
