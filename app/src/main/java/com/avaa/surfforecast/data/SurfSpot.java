package com.avaa.surfforecast.data;

import android.graphics.PointF;

/**
 * Created by Alan on 12 Jul 2016.
 */

public class SurfSpot {
    public SurfConditionsProvider conditionsProvider;

    public String name;

    public String[] altNames;

    public PointF pointOnSVG;
    public PointF pointEarth;
    public Direction waveDirection;

    public int leftright = 0; // 0 = all, -1 - right, 1 - left, 2 - peaky

    public int tides = 0; // 1-low 2-mid 3-high
    public int minSwell, maxSwell;

    public int brakeType  = 0;
    public int difficulty = 1;

    public boolean favorite;

    public double la, lo;

    public String urlMSW = null;
    public String urlCam = "";

    public String metarName = null;

    public String tidePortName = Common.BENOA_PORT_ID;


    public String getSFURL() {
        return conditionsProvider.getURL();
    }


    public SurfSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection) {
        this.name = name;
        this.conditionsProvider = conditionsProvider;
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
    }

    public SurfSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright) {
        this.name = name;
        this.conditionsProvider = conditionsProvider;
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
    }

    public SurfSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell) {
        this.name = name;
        this.conditionsProvider = conditionsProvider;
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
        this.tides = tides;
        this.minSwell = minSwell;
        this.maxSwell = maxSwell;
    }

    public SurfSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String urlMSW, String urlCam, double la, double lo) {
        this.name = name;
        this.conditionsProvider = conditionsProvider;
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
        this.tides = tides;
        this.minSwell = minSwell;
        this.maxSwell = maxSwell;
        this.urlMSW = urlMSW;
        this.urlCam = urlCam;
        this.la = la;
        this.lo = lo;
    }


    public SurfSpot(String name, String[] alt, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String urlMSW, String urlCam, double la, double lo) {
        this.name = name;
        this.altNames = alt;
        this.conditionsProvider = conditionsProvider;
        this.pointOnSVG = pointOnSVG;
        this.pointEarth = pointEarth;
        this.waveDirection = waveDirection;
        this.leftright = leftright;
        this.tides = tides;
        this.minSwell = minSwell;
        this.maxSwell = maxSwell;
        this.urlMSW = urlMSW;
        this.urlCam = urlCam;
        this.la = la;
        this.lo = lo;
    }


    public float getWindRelativeAngle(float windAngle) {
        float a = windAngle - (float)Math.PI - Direction.directionToAngle(waveDirection);

        if (a < 0) a += Math.PI*2;
        else if (a >= Math.PI*2) a -= Math.PI*2;

        return a;
    }
}
