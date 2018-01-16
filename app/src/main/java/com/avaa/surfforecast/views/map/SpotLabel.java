package com.avaa.surfforecast.views.map;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.drawers.MetricsAndPaints;

import static com.avaa.surfforecast.views.ColorUtils.alpha;


/**
 * Created by Alan on 3 Jan 2018.
 */


public class SpotLabel {
    public static class SpotLabelsCommon {
        float labelsAlpha;

        final Paint paintDot;
        final Paint paintLabel;
        final Paint paintNumber;

        int circleColorPressed;
        int circleColor;

        final Drawable star;

        float starDY;
        float starTextDY;
        float labelDY;

        float dotR;

        float dh;


        public SpotLabelsCommon(final Drawable star) {
            this.star = star;

            MetricsAndPaints metricsAndPaints = MainModel.instance.metricsAndPaints;

            paintLabel = new Paint(metricsAndPaints.paintFont) {{
                setTextAlign(Align.LEFT);
            }};
            paintNumber = new Paint(metricsAndPaints.paintFontSmall) {{
                setTextAlign(Align.CENTER);
            }};
            paintDot = new Paint() {{
                setAntiAlias(true);
            }};

            float ascent = paintLabel.getFontMetrics().ascent;

            labelDY = -ascent / 3;

            starDY = labelDY + ascent * 1.5f;
            starTextDY = labelDY + ascent * 0.075f;

            dh = metricsAndPaints.dh;

            float densityDHDep = metricsAndPaints.densityDHDependent;
            dotR = densityDHDep * 2.5f;
        }

        public void update(float overviewState) {
            circleColor = alpha(overviewState * 0.8f, 0x006281);
            circleColorPressed = alpha(overviewState * 0.3f, 0x006281);

            labelsAlpha = Math.max(0f, overviewState - 0.33f) / 0.67f;
        }
    }

    public final SurfSpot spot;

    private float rating = -1;
    private String labelRating = "";

    private int labelWidth;
    private int labelX;
    private int starX;
    public final Rect rect = new Rect();


    public SpotLabel(SurfSpot surfSpot) {
        this.spot = surfSpot;
    }


    public void setRating(RatedConditions ratedConditions) {
        if (ratedConditions == null) return;

        this.rating = ratedConditions.rating;
        this.labelRating = String.valueOf(Math.round(this.rating * 7));
    }


    public void updateDimension(SpotLabelsCommon common, float scale) {
        labelWidth = (int) common.paintLabel.measureText(spot.name);

        if (spot.labelLeft) {
            labelX = (int) (-3 * common.dotR);
            labelX -= labelWidth;
        } else {
            labelX = (int) (3 * common.dotR);
        }

        starX = labelX;
        if (spot.labelLeft) {
            starX -= common.dh / 16 + common.dh;
        } else {
            starX += labelWidth + common.dh / 16;
        }

        float w = labelWidth + (rating >= 0 ? (common.dh * 1.5f) : 0);
        if (spot.labelLeft) {
            rect.set(
                    (int) (spot.pointOnSVG.x - (w + common.dh / 2) / scale),
                    (int) (spot.pointOnSVG.y + common.paintLabel.getFontMetrics().ascent / scale),
                    (int) (spot.pointOnSVG.x + (common.dh / 2) / scale),
                    (int) (spot.pointOnSVG.y + common.paintLabel.getFontMetrics().descent / scale));
        } else {
            rect.set(
                    (int) (spot.pointOnSVG.x - (common.dh / 2) / scale),
                    (int) (spot.pointOnSVG.y + common.paintLabel.getFontMetrics().ascent / scale),
                    (int) (spot.pointOnSVG.x + (w + common.dh / 2) / scale),
                    (int) (spot.pointOnSVG.y + common.paintLabel.getFontMetrics().descent / scale));
        }
    }


    public boolean isVisible(RectF screen) {
        return rect.right > screen.left &&
                rect.left < screen.right &&
                rect.top < screen.bottom &&
                rect.bottom > screen.top;
    }


    public boolean draw(Canvas canvas, SpotLabelsCommon common, float scaleOverview, boolean isPressed) {
        float dh = common.dh;

        float x = spot.pointOnSVG.x * scaleOverview;
        float y = spot.pointOnSVG.y * scaleOverview;

        float labelsAlpha = common.labelsAlpha;

        if (isPressed) labelsAlpha *= 0.4f;

        common.paintDot.setColor(isPressed ? common.circleColorPressed : common.circleColor);
        canvas.drawCircle(x, y, common.dotR, common.paintDot);

        common.paintLabel.setColor(alpha(labelsAlpha, 0x004055));
        canvas.drawText(spot.name, x + labelX, y + common.labelDY, common.paintLabel);

        if (rating != -1) {
            common.star.setAlpha((int) (labelsAlpha * (80 + 175 * rating)));
            common.star.setBounds(
                    (int) x + starX,
                    (int) (y + common.starDY),
                    (int) (x + starX + dh),
                    (int) (y + common.starDY + dh));
            common.star.draw(canvas);

            common.paintNumber.setColor(alpha(labelsAlpha * (0.66f + 0.33f * rating), 0xffffffff)); //bestRating >= 0.7 ? 0xff8ae3fc : 0xff4ac3ec)); //paintBG.getColor());
            canvas.drawText(labelRating, x + starX + dh / 2, y + common.starTextDY, common.paintNumber);
        }

        //c.drawRect(rect, paintFont);

//            if (!justOneLabel && x2 > fx1 && y2 > fy1 && x2 + dh < fx2 && y2 < fy2) {
//                justOneLabel = true;
//                return true;
//            }
//        } else {
//            spotsLabelsRects.get(i).set(-1000, -1000, -1, -1);
//        }
        return true;
    }
}
