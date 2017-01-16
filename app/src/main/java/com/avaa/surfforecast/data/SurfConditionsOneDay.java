package com.avaa.surfforecast.data;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;

/**
 * Created by Alan on 10 Nov 2016.
 */

public class SurfConditionsOneDay extends TreeMap<Integer, SurfConditions> {
    private int detailed = -1; // 0 - no, 1 - yes

    public SurfConditions getNow() {
        Calendar calendar = GregorianCalendar.getInstance(TIME_ZONE);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        for (Map.Entry<Integer, SurfConditions> c : entrySet()) {
            if (c.getKey() >= now - 90 - 60) {
                return c.getValue();
            }
        }
        return null;
    }

    public SurfConditionsOneDay getFixed() {
        boolean detailed = isDetailed();

        SurfConditionsOneDay fixedConditions = new SurfConditionsOneDay();
        if (detailed) {
            for (int i = -60; i < 23 * 60; i += 3 * 60) {
                fixedConditions.put(i + 60, get(i));
            }
            if (fixedConditions.get(15*60) == null) fixedConditions.put(15*60, get(11*60));
            if (fixedConditions.get(21*60) == null) fixedConditions.put(21*60, get(17*60));
        }
        else {
            fixedConditions.put((int)( 7.5 * 60), get( 2 * 60));
            fixedConditions.put((int)(12.0 * 60), get(11 * 60));
            fixedConditions.put((int)(16.5 * 60), get(17 * 60));
        }
        return fixedConditions;
    }

    public boolean isDetailed() {
        if (detailed == -1) detailed = (get(5 * 60) != null && get(8 * 60) != null || get(14 * 60) != null || get(23 * 60) != null) ? 1 : 0;
        return detailed == 1;
    }
}
