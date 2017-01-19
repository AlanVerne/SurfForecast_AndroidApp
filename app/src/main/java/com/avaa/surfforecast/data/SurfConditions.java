package com.avaa.surfforecast.data;

/**
 * Created by Alan on 15 Jul 2016.
 */

public class SurfConditions {
//    public int rating;

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
}