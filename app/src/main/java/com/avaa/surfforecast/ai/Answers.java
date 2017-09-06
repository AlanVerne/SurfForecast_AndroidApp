package com.avaa.surfforecast.ai;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.MainModel;
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
    public final MainModel mainModel;

    public Answers(AppContext appContext, MainModel mainModel) {
        this.appContext = appContext;
        this.mainModel = mainModel;
    }

    // --


    public Answer windNow() {
        SurfSpots surfSpots = appContext.surfSpots;

        METAR currentMETAR = surfSpots.currentMETAR;
        if (currentMETAR != null) return windAns(currentMETAR.windSpeed, currentMETAR.windAngle, mainModel.getSelectedSpot());

        SurfConditions currentConditions = surfSpots.currentConditions;
        if (currentConditions != null) return windAns(currentConditions.windSpeed, currentConditions.windAngle, mainModel.getSelectedSpot());

        return null;
    }
    public Answer swellNow() {
        return swellAns(appContext.surfSpots.currentConditions);
    }
    public Answer tideNow() {
        return tideNowAns(appContext.tideDataProvider.getTideData(Common.BENOA_PORT_ID)); // appContext.surfSpots.currentTideData // TODO
    }


    // --


    public Answer swell(int pd, int time) {
        SurfConditions conditions = mainModel.getSelectedSpot().conditionsProvider.get(pd).get(time);
        if (conditions != null) return swellAns(conditions);

        return null;
    }
    public Answer tide(int plusDays, int time) {
        SurfSpot surfSpot = mainModel.getSelectedSpot();
        return tideAns(appContext.tideDataProvider.getTideData(surfSpot.tidePortID), plusDays, time); // appContext.surfSpots.currentTideData // TODO
    }
    public Answer wind(int pd, int time) {
        SurfSpot surfSpot = mainModel.getSelectedSpot();
        SurfConditions conditions = surfSpot.conditionsProvider.get(pd).get(time);
        if (conditions != null) return windAns(conditions.windSpeed, conditions.windAngle, surfSpot);

        return null;
    }


    // --


    public Answer swellAns(SurfConditions c) {
        return new Answer("Swell:   " + c.getWaveHeightInFt() + "ft in " + c.wavePeriod + "s",
                floatToNL(c.getWaveHeightInFt()) + " feet swell, in " + c.wavePeriod + " seconds.");
    }

    public Answer windAns(int speed, float angle) {
        return windAns(speed, angle, null);
    }
    public Answer windAns(int speed, float angle, SurfSpot spot) {
        Direction angleDir = null;
        String angleDirStr = "";
        String angleNL = "";

        if (angle >= 0) {
            angleDir = Direction.angleToDirection(angle);
            angleDirStr = angleDir.toString();

            int angleDeg = (int) Math.round(angle / Math.PI * 4) * 45;
            angleNL = Direction.ANGLE_TO_LONG_STRING_DIRECTION.get(angleDeg);
        }

        if (spot == null) {
            String windNL = windToNL(speed, angleNL);

            return new Answer("Wind:   " + speed + "km/h from " + angleDirStr + ".", windNL + ".");
        }
        else {
            float windRelativeAngle = spot.getWindRelativeAngle(angle);
            //Log.i(TAG, "windAns() | " + windRelativeAngle);

            String angleRelativeNL = "";
            String angleRelativeString = "";

            if (angle >= 0) {
                angleRelativeNL = windRelativeToNL(windRelativeAngle);
                angleRelativeString = windRelativeToString(windRelativeAngle);
            }

            String windNL = windToNL(speed, angleRelativeNL);

            return new Answer("Wind:   " + speed + "km/h " + angleDirStr + "\n" + angleRelativeString, windNL + ".");
        }
    }
    public Answer tideAns(TideData tideData, int plusDays, int time) {
        Integer h = tideData.getTide(plusDays, time);

        if (h == null) return new Answer("Tide:   unknown", "Don't know tide.");

        String state = tideData.getState(Common.getDay(plusDays, Common.TIME_ZONE), time);

        return new Answer("Tide:   " + TideData.intToString(h) + "m, " + state.toLowerCase(),
                floatToMeters(h / 100f) + ", " + state + " tide.");
    }
    public Answer tideNowAns(TideData tideData) {
        Integer h = tideData.getNow();

        if (h == null) return new Answer("Tide:   unknown", "Don't know tide.");

        String state = tideData.getStateNow().toLowerCase();

        return new Answer("Tide:   " + TideData.intToString(h) + "m, " + state,
                floatToMeters(h / 100f) + ", " + state + " tide.");
    }

    // --

    private static String floatToMeters(float f) {
        String floatToNL = floatToNL(f);
        return ("1".equals(floatToNL) ? "1 meter" : floatToNL + " meters");
    }
}
