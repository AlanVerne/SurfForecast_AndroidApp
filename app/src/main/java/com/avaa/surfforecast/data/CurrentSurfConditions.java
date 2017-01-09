package com.avaa.surfforecast.data;

/**
 * Created by Alan on 15 Jul 2016.
 */

@Deprecated
public class CurrentSurfConditions extends SurfConditions {
    public CurrentSurfConditions(SurfConditions sc) {
        this.waveHeight = sc.waveHeight;
        this.waveAngle = sc.waveAngle;
        this.wavePeriod = sc.wavePeriod;
        this.waveEnergy = sc.waveEnergy;
        this.windSpeed = sc.windSpeed;
        this.windAngle = sc.windAngle;
    }
}