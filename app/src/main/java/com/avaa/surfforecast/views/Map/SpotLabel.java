package com.avaa.surfforecast.views.Map;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
                setTextAlign(Align.CENTER);
            }};
            paintNumber = new Paint(metricsAndPaints.paintFontSmall) {{
                setTextAlign(Align.CENTER);
            }};
            paintDot = new Paint() {{
                setAntiAlias(true);
            }};

            float ascent = paintLabel.getFontMetrics().ascent;

            labelDY = ascent / 3;

            starDY = ascent * 1.5f;
            starTextDY = ascent * 0.075f;

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

    private final SurfSpot spot;

    private float rating;
    private String labelRating;

    private int labelWidth;
    private int labelX, labelY;
    private int starX;
    private Rect rect;


    public SpotLabel(SurfSpot surfSpot) {
        this.spot = surfSpot;
    }


    public void setRating(RatedConditions ratedConditions) {
        if (ratedConditions == null) return;
        this.rating = ratedConditions.rating;
        labelRating = String.valueOf(Math.round(this.rating * 7));
    }


    public void updateDimension(SpotLabelsCommon common) {
        labelWidth = (int) common.paintLabel.measureText(spot.name);

        if (spot.labelLeft) {
            labelY = (int) common.labelDY;
            if (spot.labelLeft) {
                labelX = (int) (-3 * common.dotR);
                labelX -= labelWidth;
//                x2 -= labelWidth - (rating >= 0 ? (int) (dh * 1.5f) : 0);
            } else {
                labelX = (int) (3 * common.dotR);
            }

            starX = labelX;
            if (spot.labelLeft) {
                starX -= common.dh / 16 + common.dh;
            } else {
                starX += labelWidth + common.dh / 16;
            }
        }

        rect.set(
                (int) (spot.pointOnSVG.x - common.dh / 2),
                (int) (spot.pointOnSVG.y + common.paintLabel.getFontMetrics().ascent),
                (int) (spot.pointOnSVG.x + labelWidth + common.dh / 2 + (rating >= 0 ? (common.dh * 1.5f) : 0)),
                (int) spot.pointOnSVG.y);
    }


    public boolean draw(Canvas canvas, SpotLabelsCommon common, float scaleOverview, boolean isPressed) {
        float dh = common.dh;

        float x = spot.pointOnSVG.x * scaleOverview;
        float y = spot.pointOnSVG.y * scaleOverview;
//        float x2 = x + dx2;
//        float y2 = y + dy2;

        float labelsAlpha = common.labelsAlpha;

        if (isPressed) labelsAlpha *= 0.4f;

        common.paintDot.setColor(isPressed ? common.circleColorPressed : common.circleColor);
        canvas.drawCircle(x, y, common.dotR, common.paintDot);

////        if (x2 > rectXL && (y2 > insetTop || x2 > dh * 4 && y2 > rectYT) && x2 < rectXR && y2 < rectYB) {
//        y -= common.labelDY;
//        if (spot.labelLeft) {
//            x -= 3 * common.dotR;
//            x -= labelWidth;
////                x2 -= labelWidth - (rating >= 0 ? (int) (dh * 1.5f) : 0);
//        } else {
//            x += 3 * common.dotR;
//        }

        common.paintLabel.setColor(alpha(labelsAlpha, 0x004055));
        canvas.drawText(spot.name, x + labelX, y + labelY, common.paintLabel);

        //Rect rect = spotsLabelsRects.get(i);
        //rect.set((int) x2 - dh / 2, (int) (y2 + paintFont.getFontMetrics().ascent), (int) x2 + labelWidth + dh / 2 + (rating >= 0 ? (int) (dh * 1.5f) : 0), (int) y2);

//        if (spot.labelLeft) {
//            x -= dh / 16 + dh;
//        } else {
//            x += labelWidth + dh / 16;
//        }

        if (rating >= 0) {
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
//        if (isPressed) labelsAlpha /= 0.4f;
    }
}
