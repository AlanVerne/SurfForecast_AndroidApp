package com.avaa.surfforecast.data;


/**
 * Created by Alan on 15 Jul 2016.
 */


public class SurfConditions {
    private static final String TAG = "SurfCond";

    public int waveHeight;
    public float waveAngle;
    public int wavePeriod;
    public int waveEnergy;

    public int windSpeed;
    public float windAngle;


    public SurfConditions() {
    }

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
        } catch (IllegalArgumentException ignored) {
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

    @Override
    public String toString() {
        return waveHeight +
                "\t" + waveAngle +
                "\t" + wavePeriod +
                "\t" + waveEnergy +
                "\t" + windSpeed +
                "\t" + windAngle;
    }


    public float getWaveHeightInFt() {
        return Math.round(waveHeight * 3.28084f / 5.0f) / 2f;
    }

    public String getWaveAngleAbbr() {
        Direction direction = Direction.values()[(int) Math.round(waveAngle / Math.PI * 8)];
        return direction.toString().toLowerCase();
    }


    public void addMETAR(METAR metar) {
        if (metar != null) {
            windSpeed = metar.windSpeed;
            windAngle = metar.windAngle;
        }
    }


    public static int windSpeedToBeaufort(int windSpeed) {
        if (windSpeed <= 2) return 0;
        if (windSpeed <= 5) return 1;
        if (windSpeed <= 11) return 2;
        if (windSpeed <= 19) return 3;
        if (windSpeed <= 28) return 4;
        if (windSpeed <= 38) return 5;
        return 6;
    }


    public static boolean equals(SurfConditions a, SurfConditions b) {
        if (a==b) return true;
        if (a==null) return false;
        if (a.waveHeight != b.waveHeight) return false;
        if (Float.compare(a.waveAngle, b.waveAngle) != 0) return false;
        if (a.wavePeriod != b.wavePeriod) return false;
        if (a.waveEnergy != b.waveEnergy) return false;
        if (a.windSpeed != b.windSpeed) return false;
        return Float.compare(a.windAngle, a.windAngle) == 0;
    }
}