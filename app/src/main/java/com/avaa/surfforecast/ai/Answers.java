package com.avaa.surfforecast.ai;

import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.Direction;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideData;

import static com.avaa.surfforecast.ai.ToNL.*;

/**
 * Created by Alan on 19 Jan 2017.
 */

public class Answers {
    public static final String TAG = "Answers";
    
    public final AppContext appContext;

    public Answers(AppContext appContext) {
        this.appContext = appContext;
    }


    // --


    public Answer windNow() {
        SurfSpots surfSpots = appContext.surfSpots;

        METAR currentMETAR = surfSpots.currentMETAR;
        if (currentMETAR != null) return windAns(currentMETAR.windSpeed, currentMETAR.windAngle, surfSpots.selectedSpot());

        SurfConditions currentConditions = surfSpots.currentConditions;
        if (currentConditions != null) return windAns(currentConditions.windSpeed, currentConditions.windAngle, surfSpots.selectedSpot());

        return null;
    }
    public Answer swellNow() {
        return swellAns(appContext.surfSpots.currentConditions);
    }
    public Answer tideNow() {
        return tideNowAns(appContext.tideDataProvider.getTideData(Common.BENOA_PORT_ID)); // appContext.surfSpots.currentTideData // TODO
    }


    // --


    public Answer swellAns(SurfConditions cc) {
        return new Answer("Swell:   " + cc.getWaveHeightInFt() + "ft in " + cc.wavePeriod + "s",
                floatToNL(cc.getWaveHeightInFt()) + " feet swell, in " + cc.wavePeriod + " seconds.");
    }

    public Answer windAns(int speed, float angle) {
        return windAns(speed, angle, null);
    }
    public Answer windAns(int speed, float angle, SurfSpot spot) {
        Direction angleDir = Direction.angleToDirection(angle);

        int angleDeg = (int) Math.round(angle / Math.PI * 4) * 45;
        String angleNL = Direction.ANGLE_TO_LONG_STRING_DIRECTION.get(angleDeg);

        if (spot == null) {
            String windNL = windToNL(speed, angleNL);

            return new Answer("Wind:   " + speed + "km/h from " + angleDir.toString() + ".", windNL + ".");
        }
        else {
            float windRelativeAngle = spot.getWindRelativeAngle(angle);
            //Log.i(TAG, "windAns() | " + windRelativeAngle);

            String angleRelativeNL = windRelativeToNL(windRelativeAngle);
            String angleRelativeString = windRelativeToString(windRelativeAngle);

            String windNL = windToNL(speed, angleRelativeNL);

            return new Answer("Wind:   " + speed + "km/h " + angleRelativeString + " " + angleDir.toString(), windNL + ".");
        }
    }
    public Answer tideAns(TideData tideData, int plusday, long time) {
        Integer h = tideData.getTide(time);

        if (h == null) return new Answer("Tide:   unknown", "Don't know tide.");

        String state = tideData.getState(Common.getDay(plusday, Common.TIME_ZONE), time);

        return new Answer("Tide:   " + TideData.intToString(h) + "m, " + state.toLowerCase(),
                floatToNL(h / 100f) + " m, " + state.toLowerCase() + " tide.");
    }
    public Answer tideNowAns(TideData tideData) {
        Integer h = tideData.getNow();

        if (h == null) return new Answer("Tide:   unknown", "Don't know tide.");

        String state = tideData.getStateNow().toLowerCase();

        return new Answer("Tide:   " + TideData.intToString(h) + "m, " + state,
                floatToNL(h / 100f) + " m " + state + " tide.");
    }
}
