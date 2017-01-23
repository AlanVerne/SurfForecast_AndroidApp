package com.avaa.surfforecast.data;

import android.util.Log;

/**
 * Created by Alan on 15 Jul 2016.
 */

public class SurfConditions {
    public float waveRating = -1;
    public float windRating = -1;
    public float tideRating = -1;

    public int   waveHeight;
    public float waveAngle;
    public int   wavePeriod;
    public int   waveEnergy;

    public int   windSpeed;
    public float windAngle;


    public SurfConditions() {}
    public SurfConditions(int waveHeight, Direction waveDirection, int wavePeriod, int waveEnergy, int windSpeed, Direction windDirection) {
        this.waveHeight = waveHeight;
        this.waveAngle = Direction.directionToAngle(waveDirection);
        this.wavePeriod = wavePeriod;
        this.waveEnergy = waveEnergy;
        this.windSpeed = windSpeed;
        this.windAngle = Direction.directionToAngle(windDirection);
    }
    public SurfConditions(int waveHeight, float waveAngle, int wavePeriod, int waveEnergy, int windSpeed, float windAngle) {
        this.waveHeight = waveHeight;
        this.waveAngle = waveAngle;
        this.wavePeriod = wavePeriod;
        this.waveEnergy = waveEnergy;
        this.windSpeed = windSpeed;
        this.windAngle = windAngle;
    }


    public static SurfConditions fromString(String s) {
        String[] split = s.split("\t");
        try {
            Direction.valueOf(split[1]);
        }
        catch (IllegalArgumentException ignored) {
            return new SurfConditions(Integer.valueOf(split[0]),
                    Float.valueOf(split[1]),
                    Integer.valueOf(split[2]),
                    Integer.valueOf(split[3]),
                    Integer.valueOf(split[4]),
                    Float.valueOf(split[5]));
        }
        return new SurfConditions(Integer.valueOf(split[0]),
                Direction.valueOf(split[1]),
                Integer.valueOf(split[2]),
                Integer.valueOf(split[3]),
                Integer.valueOf(split[4]),
                Direction.valueOf(split[5]));
    }

    public float getWaveHeightInFt() {
        return Math.round(waveHeight * 3.28084f / 5.0f)/2f;
    }

    @Override
    public String toString() {
        return waveHeight +
                "\t" + waveAngle +
                "\t" + wavePeriod +
                "\t" + waveEnergy +
                "\t" + windSpeed +
                "\t" + windAngle;
    }

    public String waveAngleAbbr() {
        Direction direction = Direction.values()[(int)Math.round(waveAngle / Math.PI * 8)];
        return direction.toString().toLowerCase();
    }

    public static int windSpeedToBeaufort(int windSpeed) {
        if (windSpeed <=  2) return 0;
        if (windSpeed <=  5) return 1;
        if (windSpeed <= 11) return 2;
        if (windSpeed <= 19) return 3;
        if (windSpeed <= 28) return 4;
        if (windSpeed <= 38) return 5;
        return 6;
    }


    public void resetRating() {
        waveRating = -1;
        windRating = -1;
        tideRating = -1;
    }
    private void rateWave(SurfSpot spot) {
        if (spot.maxSwell - spot.minSwell != 0) {
            float sweelHeightAve = (spot.maxSwell + spot.minSwell) / 2f;

            waveRating = (getWaveHeightInFt() - sweelHeightAve) / (sweelHeightAve - spot.minSwell);
            Log.i("SurfConditions", "rateWave() | " + spot.minSwell + "-" + sweelHeightAve + "-" + spot.maxSwell + ", " + getWaveHeightInFt() + " " + waveRating);
            waveRating *= waveRating;
            waveRating = 1 - waveRating;
        }
    }
    private void rateTide(SurfSpot spot, TideData tideData, int day, int time) {
        //String state = tideData.getState(day, time);

        Integer tide = tideData.getTide(day, time);

        tideRating = 0;

        if (tide != null) {
            int t0 = TideData.tideToHML(tide);
            if ((spot.tides & t0) != 0) tideRating = 0.8f;

            time += 60;
            if (time > 24 * 60) {
                day++;
                time -= 24 * 60;
            }
            tide = tideData.getTide(day, time);
            if (tide != null) {
                int t1 = TideData.tideToHML(tide);
                if ((spot.tides & t1) != 0) tideRating += 0.2f;
                else tideRating -= 0.3f;
            }
        }
    }
    private void rateWind(SurfSpot spot) {
        windRating = 1f - (float) Math.abs((spot.getWindRelativeAngle(windAngle) - Math.PI) / Math.PI); // angle component
        windRating = (float)Math.pow(windRating, windSpeed/10); // let 10km\h be a normal wind
    }


    public float rate(SurfSpot spot, TideData tideData, int day, int time) {
        if (waveRating == -1) rateWave(spot);
        rateTide(spot, tideData, day, time);
        if (windRating == -1) rateWind(spot);

        Log.i("SurfConditions", "rate() | " + spot.getShortName() + ", rated: " + waveRating + ", " + windRating + ", " + tideRating);

        return waveRating * windRating * tideRating;
    }
}