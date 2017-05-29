package com.avaa.surfforecast.data;

/**
 * Created by Alan on 6 Nov 2016.
 */

public class SunTimes {
    public final int cSunrise;
    public final int sunrise;
    public final int sunset;
    public final int cSunset;

    public SunTimes(int cSunrise, int sunrise, int sunset, int cSunset) {
        if (cSunrise < 0) cSunrise += 24*60;
        if (sunrise  < 0) sunrise  += 24*60;
        if (sunset   < 0) sunset   += 24*60;
        if (cSunset  < 0) cSunset  += 24*60;

        this.cSunrise = cSunrise;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.cSunset = cSunset;
    }

    @Override
    public String toString() {
        return cSunrise / 60 + ":" + cSunrise % 60 + " " + sunrise / 60 + ":" + sunrise % 60 + " | " + sunset / 60 + ":" + sunset % 60 + " " + cSunset / 60 + ":" + cSunset % 60;
    }
}
