package com.avaa.surfforecast.data;

import android.graphics.PointF;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.views.RatingView;

import java.util.HashMap;
import java.util.Map;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;

/**
 * Created by Alan on 12 Jul 2016.
 */

public class SurfSpot {
    public final SurfConditionsProvider conditionsProvider;

    public final String name;

    public String[] altNames;

    public PointF pointOnSVG;
    private PointF pointEarth;
    public Direction waveDirection;

    public int leftright = 0; // 0 = all, -1 - right, 1 - left, 2 - peaky

    public int tides = 0; // 1-low 2-mid 3-high
    public int minSwell, maxSwell;

    public int brakeType = 0;
    public int difficulty = 1;

    public boolean favorite;

    public double la, lo;

    public final String urlMSW;
    public final String urlCam;

    public String metarName = null;

    public String tidePortID = Common.BENOA_PORT_ID;

    public boolean labelLeft = false;


    public String getSFURL() {
        return conditionsProvider.getURL();
    }


    public SurfSpot(String name, String conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String urlMSW, String urlCam, double la, double lo) {
        this.name = name;
        this.conditionsProvider = SurfConditionsProviders.get(conditionsProvider);
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
        this.tides = tides;
        this.minSwell = minSwell;
        this.maxSwell = maxSwell;
        this.urlMSW = urlMSW;
        this.urlCam = urlCam.isEmpty() ? null : urlCam;
        this.la = la;
        this.lo = lo;
    }

    public SurfSpot(String name, String[] alt, String conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String urlMSW, String urlCam, double la, double lo) {
        this.name = name;
        this.altNames = alt;
        this.conditionsProvider = SurfConditionsProviders.get(conditionsProvider);
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
        this.tides = tides;
        this.minSwell = minSwell;
        this.maxSwell = maxSwell;
        this.urlMSW = urlMSW;
        this.urlCam = urlCam.isEmpty() ? null : urlCam;
        this.la = la;
        this.lo = lo;
    }


//    public void updateConditionsRatings() {
//
//    }


    public String getShortName() {
        String name = this.name;
        if (altNames != null && altNames[0].length() < name.length()) name = altNames[0];
        return name;
    }


    public float getWindRelativeAngle(float windAngle) {
        float a = windAngle - (float) Math.PI - Direction.directionToAngle(waveDirection);

        if (a < 0) a += Math.PI * 2;
        else if (a >= Math.PI * 2) a -= Math.PI * 2;

        return a;
    }
}
