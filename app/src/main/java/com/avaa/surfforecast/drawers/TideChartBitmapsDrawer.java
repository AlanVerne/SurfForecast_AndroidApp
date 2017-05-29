package com.avaa.surfforecast.drawers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Region;

import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SunTimes;
import com.avaa.surfforecast.data.SunTimesProvider;
import com.avaa.surfforecast.data.TideData;

import java.util.Map;
import java.util.SortedMap;

import static com.avaa.surfforecast.drawers.MetricsAndPaints.MINUTES_IN_DAY;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorTideChartBG;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.colorTideText;
import static com.avaa.surfforecast.drawers.MetricsAndPaints.getColorMinor;

/**
 * Created by Alan on 25 Jan 2017.
 */

public class TideChartBitmapsDrawer {
    private final int dh;
    private final float fontHDiv2;

    private final Paint paintHourly3Tides;
    private final Paint paintHourlyTides;
    private final Paint paintHourly3Hour;


    public TideChartBitmapsDrawer(MetricsAndPaints metricsAndPaints) {
        dh = metricsAndPaints.dh;
        fontHDiv2 = metricsAndPaints.fontHDiv2;

        paintHourly3Tides = new Paint() {{
            setAntiAlias(true);
            setTextSize(metricsAndPaints.font);
            setColor(colorTideText);
            setTextAlign(Align.RIGHT);
        }};

        int colorMinorTideText = getColorMinor(colorTideText);

        paintHourlyTides = new Paint(paintHourly3Tides) {{
            setColor(colorMinorTideText);
        }};
        paintHourly3Hour = new Paint(paintHourly3Tides) {{
            setColor(colorMinorTideText);
        }};
    }


    public Bitmap drawTide(TideData tideData, int plusDays, boolean vertical) {
        int width = dh * 16;
        int height = dh * 4;
        int chartH = (int) (dh * 1.5);

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        //AstronomyProvider.Times sunTimes = AstronomyProvider.getTimes();

        long day = Common.getDay(plusDays, Common.TIME_ZONE);

        SunTimes sunTimes = SunTimesProvider.get(Common.LATITUDE, Common.LONGITUDE, day, Common.TIME_ZONE);

        final Path area = tideData.getPath(day, width, chartH*2, -300, 300);

        if (area == null) return null;

        Paint paintR = new Paint() {{
            setColor(colorTideChartBG); //colorTideBG);//0xff38a0d0);//AVE_BLUE);
            setAntiAlias(true);
        }};

        c.save();
        c.drawPath(area, paintR);

        int firstlight = sunTimes.cSunrise * width / MINUTES_IN_DAY;
        int lastlight  = sunTimes.cSunset  * width / MINUTES_IN_DAY;
        int sunrise    = sunTimes.sunrise  * width / MINUTES_IN_DAY;
        int sunset     = sunTimes.sunset   * width / MINUTES_IN_DAY;

        //Region sunReg = new Region(sunrise, 0, sunset, height);
        Region firstLightReg = new Region(firstlight, 0, sunrise, height);
        firstLightReg.op(sunset, 0, lastlight, height, Region.Op.UNION);
        Region nightReg = new Region(0, 0, firstlight, height);
        nightReg.op(lastlight, 0, width, height, Region.Op.UNION);

        paintR.setColor(0x11000000);
        c.save();
        c.clipPath(firstLightReg.getBoundaryPath());
        c.drawPath(area, paintR);
        c.restore();
        paintR.setColor(0x22000000);
        c.save();
        c.clipPath(nightReg.getBoundaryPath());
        c.drawPath(area, paintR);
        c.restore();

        c.restore();


        SortedMap<Integer, Integer> hourlyTides = tideData.getHourly(day, 5, 19); //4, 20);

        if (hourlyTides != null) {
            c.save();
            c.rotate(-90);
            for (Map.Entry<Integer, Integer> entry : hourlyTides.entrySet()) {
                int h = entry.getKey();
                boolean h3 = h % 3 == 0;
                Point point = new Point(width * entry.getKey() / 24, chartH - chartH * entry.getValue() / 300);// + hourlyHoursHeight*2 + hourlyHoursWidth);

                String strTide = String.valueOf(Math.round(entry.getValue() / 10.0) / 10.0);
                //if (h3) {
                //String strH = String.valueOf(h);// + ":00";
                c.drawText(strTide, -point.y - dh * 0.8f, point.x + fontHDiv2, h3 ? paintHourly3Tides : paintHourlyTides);
                //}
                //c.drawText(strTide, -point.y + hourlyHoursHeight, point.x + hourlyHoursHeight/2, h3 ? paintHourly3Hour : paintHourlyHour);
                // - hourlyHoursHeight
                //if (h<10) strH = " " + strH;

                //if (vertical) c.drawText(strH, -height + dh/2 + hourlyHoursWidth, point.x + hourlyHoursHeight/2, h3 ? paintHourly3Hour : paintHourlyHour);
            }
            c.restore();
        }

//        if (!vertical) {
//            paintHourlyHour.setTextAlign(Paint.Align.CENTER);
//            paintHourly3Hour.setTextAlign(Paint.Align.CENTER);
//            for (int h = 5; h < 19; h++) {
//                boolean h3 = h % 3 == 0;
//                Point point = new Point(width * h / 24, height - dh / 2);// + hourlyHoursHeight*2 + hourlyHoursWidth);
//
//                String strH = String.valueOf(h);
//
//                c.drawText(strH, point.x, point.y, h3 ? paintHourly3Hour : paintHourlyHour);
//            }
//        }

        return b;
    }
}
