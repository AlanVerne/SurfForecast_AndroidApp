package com.avaa.surfforecast.data;

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
}