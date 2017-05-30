package com.avaa.surfforecast.data;

import java.util.TimeZone;

/**
 * Created by Alan on 5 Nov 2016.
 */

public class SunTimesProvider {
    private static final String TAG = "SunTimesProvider";

    //  dawn:          julianDateToDate(Jciv2)
    //  sunrise start: julianDateToDate(Jrise)
    //  sunrise end:   julianDateToDate(Jriseend)
    //  transit:       julianDateToDate(Jtransit)
    //  sunset start:  julianDateToDate(Jsetstart)
    //  sunset end:    julianDateToDate(Jset)
    //  dusk:          julianDateToDate(Jnau)

    public static SunTimes get(final double latitude, final double longitude, TimeZone timeZone) {
        long time = Common.getDay(0, timeZone);
        return get(latitude, longitude, time, timeZone);
    }
    public static SunTimes get(final double latitude, final double longitude, long date, TimeZone timeZone) {
        double  lw = -longitude * deg2rad,
                phi = latitude * deg2rad,
                J = unixToJulian(date),
                n = getJulianCycle(J, lw),
                Js = getApproxSolarTransit(0, lw, n),
                M = getSolarMeanAnomaly(Js),
                C = getEquationOfCenter(M),
                Lsun = getEclipticLongitude(M, C),
                d = getSunDeclination(Lsun),
                Jtransit = getSolarTransit(Js, M, Lsun),
                w0 = getHourAngle(h0, phi, d),
                w1 = getHourAngle(h0 + d0, phi, d),
                Jset = getSunsetJulianDate(w0, M, Lsun, lw, n),
                Jsetstart = getSunsetJulianDate(w1, M, Lsun, lw, n),
                Jrise = getSunriseJulianDate(Jtransit, Jset),
                Jriseend = getSunriseJulianDate(Jtransit, Jsetstart),
                w2 = getHourAngle(h1, phi, d),
                Jnau = getSunsetJulianDate(w2, M, Lsun, lw, n),
                Jciv2 = getSunriseJulianDate(Jtransit, Jnau);

        final SunTimes sunTimes = new SunTimes(
                Math.round((julianToUnix(Jciv2) - date) / 60f),
                Math.round((julianToUnix(Jrise) - date) / 60f),
                Math.round((julianToUnix(Jsetstart) - date) / 60f),
                Math.round((julianToUnix(Jnau) - date) / 60f)
        );

//        Log.i(TAG, position.toString() + " - " + timeZone.toString() + " - " + sunTimes.toString());

        return sunTimes;
    }


    // --


    private static final double J1970 = 2440588;
    private static final double J2000 = 2451545;
    private static final double deg2rad = Math.PI / 180;
    private static final double M0 = 357.5291 * deg2rad;
    private static final double M1 = 0.98560028 * deg2rad;
    private static final double J0 = 0.0009;
    private static final double J1 = 0.0053;
    private static final double J2 = -0.0069;
    private static final double C1 = 1.9148 * deg2rad;
    private static final double C2 = 0.0200 * deg2rad;
    private static final double C3 = 0.0003 * deg2rad;
    private static final double P = 102.9372 * deg2rad;
    private static final double e = 23.45 * deg2rad;
    private static final double th0 = 280.1600 * deg2rad;
    private static final double th1 = 360.9856235 * deg2rad;
    private static final double h0 = -0.83 * deg2rad; //sunset angle
    private static final double d0 = 0.53 * deg2rad; //sun diameter
    private static final double h1 = -6 * deg2rad; //nautical twilight angle
    private static final double h2 = -12 * deg2rad; //astronomical twilight angle
    private static final double h3 = -18 * deg2rad; //darkness angle
    private static final double msInDay = 1000 * 60 * 60 * 24;


    private static double unixToJulian(long unixSecs) {
        return (int)(unixSecs / 86400.0f) + 2440587.5f;
    }

    private static long julianToUnix(double unixSecs) {
        return (long)((unixSecs - 2440587.5) * 86400.0);
    }

    private static double getJulianCycle(double J, double lw) {
        return Math.round(J - J2000 - J0 - lw / (2 * Math.PI));
    }

    private static double getApproxSolarTransit(double Ht, double lw, double n) {
        return J2000 + J0 + (Ht + lw) / (2 * Math.PI) + n;
    }

    private static double getSolarMeanAnomaly(double Js) {
        return M0 + M1 * (Js - J2000);
    }

    private static double getEquationOfCenter(double M) {
        return C1 * Math.sin(M) + C2 * Math.sin(2 * M) + C3 * Math.sin(3 * M);
    }

    private static double getEclipticLongitude(double M, double C) {
        return M + P + C + Math.PI;
    }

    private static double getSolarTransit(double Js, double M, double Lsun) {
        return Js + (J1 * Math.sin(M)) + (J2 * Math.sin(2 * Lsun));
    }

    private static double getSunDeclination(double Lsun) {
        return Math.asin(Math.sin(Lsun) * Math.sin(e));
    }

    private static double getRightAscension(double Lsun) {
        return Math.atan2(Math.sin(Lsun) * Math.cos(e), Math.cos(Lsun));
    }

    private static double getSiderealTime(double J, double lw) {
        return th0 + th1 * (J - J2000) - lw;
    }

    private static double getAzimuth(double th, double a, double phi, double d) {
        double H = th - a;
        return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(d) * Math.cos(phi));
    }

    private static double getAltitude(double th, double a, double phi, double d) {
        double H = th - a;
        return Math.asin(Math.sin(phi) * Math.sin(d) + Math.cos(phi) * Math.cos(d) * Math.cos(H));
    }

    private static double getHourAngle(double h, double phi, double d) {
        return Math.acos((Math.sin(h) - Math.sin(phi) * Math.sin(d)) / (Math.cos(phi) * Math.cos(d)));
    }

    private static double getSunsetJulianDate(double w0, double M, double Lsun, double lw, double n) {
        return getSolarTransit(getApproxSolarTransit(w0, lw, n), M, Lsun);
    }

    private static double getSunriseJulianDate(double Jtransit, double Jset) {
        return Jtransit - (Jset - Jtransit);
    }

    private static double[] getSunPosition(double J, double lw, double phi) {
        double  M = getSolarMeanAnomaly(J),
                C = getEquationOfCenter(M),
                Lsun = getEclipticLongitude(M, C),
                d = getSunDeclination(Lsun),
                a = getRightAscension(Lsun),
                th = getSiderealTime(J, lw);

        double  azimuth = getAzimuth(th, a, phi, d),
                altitude = getAltitude(th, a, phi, d);

        return new double[]{azimuth, altitude};
    }
}
