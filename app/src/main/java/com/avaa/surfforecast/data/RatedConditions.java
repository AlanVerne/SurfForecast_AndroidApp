package com.avaa.surfforecast.data;

import android.support.annotation.NonNull;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;


/**
 * Created by Alan on 10 Sep 2017.
 */


public class RatedConditions implements Comparable<RatedConditions> {
    public final float rating;

    public final float waveRating;
    public final float windRating;
    public final float tideRating;

    public final int time;
    public final SurfConditions surfConditions;
    public final int tide;
    public final SurfSpot surfSpot;


    public RatedConditions(@NonNull SurfSpot surfSpot, int plusDays, int time, @NonNull SurfConditions surfConditions, @NonNull TideData tideData) {
        waveRating = rateWave(surfConditions, surfSpot);
        tideRating = rateTide(surfSpot, tideData, plusDays, time);
//        if (windRating == -1)
        windRating = rateWind(surfConditions, surfSpot);

//        Log.i("SurfConditions", "rate(" + surfSpot.getShortName() + ", " + time / 60 + ":00), rated: " + waveRating + ", " + windRating + ", " + tideRating);

        rating = waveRating * windRating * tideRating;

        this.time = time;
        this.surfConditions = surfConditions;
        this.tide = 0;
        this.surfSpot = surfSpot;
    }


    public static RatedConditions create(SurfSpot surfSpot, int plusDays, int time, SurfConditions surfConditions, TideData tideData) {
        if (surfSpot == null || surfConditions == null || tideData == null || time < 0) return null;
        return new RatedConditions(surfSpot, plusDays, time, surfConditions, tideData);
    }


    private static float rateWave(SurfConditions surfConditions, SurfSpot spot) {
//        Log.i(TAG, "wave " + waveHeight);
        if (spot.maxSwell - spot.minSwell != 0) {
            float swellHeightAve = (spot.maxSwell + spot.minSwell) / 2f;

            float waveRating = (surfConditions.getWaveHeightInFt() - swellHeightAve) / (swellHeightAve - spot.minSwell);
//            Log.i(TAG, "waveRating " + waveRating);
            waveRating = max(0f, 1f - waveRating * waveRating);
//            Log.i(TAG, "waveRating " + waveRating);
            waveRating = max(0f, min(1f, waveRating * ((surfConditions.wavePeriod - 7) / 7f)));
//            Log.i(TAG, "waveRating " + waveRating);
            return waveRating;
        }
        return -1;
    }

    private static float rateTide(SurfSpot spot, TideData tideData, int plusDays, int time) {
        //String state = tideData.getState(plusDays, time);

        Integer tide = tideData.getTide(plusDays, time);

//        Log.i(TAG, "tide " + tide);

        float tideRating = 0;

        if (tide != null) {
            int t0 = TideData.tideToHML(tide);
            if ((spot.tides & t0) != 0) tideRating = 0.8f;

//            Log.i(TAG, "tideRating " + tideRating);

            time += 60;
            if (time > 24 * 60) {
                plusDays++;
                time -= 24 * 60;
            }
            tide = tideData.getTide(plusDays, time);
//            Log.i(TAG, "tide " + tide);
            if (tide != null) {
                int t1 = TideData.tideToHML(tide);
                if ((spot.tides & t1) != 0) tideRating += 0.2f;
                else if (tideRating > 0.3f) tideRating -= 0.3f;
            }

//            Log.i(TAG, "tideRating " + tideRating);
            return tideRating;
        }
        return -1;
    }

    private static float rateWind(SurfConditions surfConditions, SurfSpot spot) {
        //windRating = 1f - (float)Math.abs((spot.getWindRelativeAngle(windAngle) - Math.PI) / Math.PI); // angle component

        float windRating = (float) cos(abs(spot.getWindRelativeAngle(surfConditions.windAngle) - PI)) / 2f + 0.5f;
        windRating = windRating * 0.8f + 0.1f;
//        Log.i("SurfConditions", "rateWind() | " + windRating + " " + windAngle);
        windRating = (float) pow(windRating, pow(10, ((surfConditions.windSpeed - 20) / 10)));
//        Log.i("SurfConditions", "rateWind() | " + ((windSpeed - 20) / 10));
//        Log.i("SurfConditions", "rateWind() | " + Math.pow(10, ((windSpeed - 20) / 10)));
        return windRating;
    }


    public static float rate(SurfConditions surfConditions, SurfSpot spot, TideData tideData, int plusDays, int time) {
        if (tideData == null) return 0;

//        if (waveRating == -1)
        float waveRating = rateWave(surfConditions, spot);
        float tideRating = rateTide(spot, tideData, plusDays, time);
//        if (windRating == -1)
        float windRating = rateWind(surfConditions, spot);

//        Log.i("SurfConditions", "rate(" + spot.getShortName() + ", " + time / 60 + ":00), rated: " + waveRating + ", " + windRating + ", " + tideRating);

        return waveRating * windRating * tideRating;
    }


    @Override
    public int compareTo(@NonNull RatedConditions ratedConditions) {
        return (int) (1000 * (rating - ratedConditions.rating));
    }

    public static boolean sameEstimate(RatedConditions a, RatedConditions b) {
        return a == null && b == null ||
                a != null && b != null &&
                        round(7 * a.rating) == round(7 * b.rating) &&
                        round(7 * a.waveRating) == round(7 * b.waveRating);
    }
}