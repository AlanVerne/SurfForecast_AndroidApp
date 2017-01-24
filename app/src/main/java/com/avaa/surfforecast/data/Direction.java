package com.avaa.surfforecast.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alan on 13 Jul 2016.
 */

public enum Direction {
    E, ENE, NE, NNE,
    N, NNW, NW, WNW,
    W, WSW, SW, SSW,
    S, SSE, SE, ESE;

    public static float directionToAngle(Direction d) {
        return (float)(d.ordinal() * Math.PI * 2 / 16);
    }

    public static int directionToAngleInDegrees(Direction d) {
        return d.ordinal() * 360 / 16;
    }

    public static Direction angleToDirection(float a) {
        return Direction.values()[(int)Math.round(a / Math.PI * 8)];
    }

    public static final Map<Integer, String> ANGLE_TO_LONG_STRING_DIRECTION = new HashMap<Integer, String>() {{
        put(0, "East");
        put(90, "North");
        put(180, "West");
        put(270, "South");
        put(360, "East");
        put(0+45, "North-east");
        put(90+45, "North-west");
        put(180+45, "South-west");
        put(270+45, "South-east");
    }};
}