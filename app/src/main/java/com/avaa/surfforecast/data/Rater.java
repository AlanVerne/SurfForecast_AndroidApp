package com.avaa.surfforecast.data;


import android.support.annotation.NonNull;
import android.util.Log;

import com.avaa.surfforecast.MainModel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Created by Alan on 10 Sep 2017.
 */


public class Rater {
    private static final String TAG = "Rater";

    private final Map<SurfSpot, Long> updated = new HashMap<>();
    private final Map<SurfSpot, TreeMap<Long, RatedConditions>> bestBySpot = new HashMap<>();
    private final Map<Long, SortedSet<RatedConditions>> bestByDay = new HashMap<>();

    private long lastUpdate = 0;


    private void initBestByDay() {
        bestByDay.clear();
        for (int plusDays = 0; plusDays <= 6; plusDays++) {
            bestByDay.put(Common.getDay(plusDays, Common.TIME_ZONE), new TreeSet<>());
        }
    }


    public RatedConditions getBest(@NonNull SurfSpot surfSpot, int plusDays) {
        Long updated = this.updated.get(surfSpot);
        if (updated == null || updated < surfSpot.conditionsProvider.lastUpdate) {
            updateBest(surfSpot);
        }

        if (bestBySpot.get(surfSpot) == null) return null;

        return bestBySpot.get(surfSpot).get(Common.getDay(plusDays, Common.TIME_ZONE));
    }


    public RatedConditions getBestForDay(int plusDays) {
        return bestByDay.get(Common.getDay(plusDays, Common.TIME_ZONE)).first();
    }


    public RatedConditions getRating(@NonNull SurfSpot surfSpot, int plusDays, int time) {
        TideData tideData = MainModel.instance.tideDataProvider.getTideData(surfSpot.tidePortID);
        if (tideData == null) return null;

        SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);
        if (surfConditionsOneDay == null) return null;

        SurfConditions conditions = surfConditionsOneDay.get(time * 60);
        if (conditions == null) return null;

        return new RatedConditions(surfSpot, plusDays, time, conditions, tideData);
    }


    public void updateBest() {
//        Log.i(TAG, "updateBest");
        initBestByDay();
        for (SurfSpot surfSpot : MainModel.instance.surfSpots.getAll()) {
            TreeMap<Long, RatedConditions> map = updateBest(surfSpot);
            for (Map.Entry<Long, RatedConditions> entry : map.entrySet()) {
                bestByDay.get(entry.getKey()).add(entry.getValue());
            }
        }
        lastUpdate = System.currentTimeMillis();
    }


    public TreeMap<Long, RatedConditions> updateBest(SurfSpot surfSpot) {
        TreeMap<Long, RatedConditions> map = bestBySpot.get(surfSpot);

        if (map == null) {
            map = new TreeMap<>();
            bestBySpot.put(surfSpot, map);
        } else {
            map.clear();
        }

        TideData tideData = MainModel.instance.tideDataProvider.getTideData(surfSpot.tidePortID);

        int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);

        RatedConditions rc = new RatedConditions();

        for (int plusDays = 0; plusDays <= 6; plusDays++) {
            RatedConditions bestRC = null;
            float bestRate = -1;

            SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);

            if (surfConditionsOneDay == null) continue;

            for (int hour = 6; hour < 18; hour++) {
                SurfConditions conditions = surfConditionsOneDay.get(hour * 60);

                if ((plusDays == 0
                        && hour * 60 < nowTimeInt - 120
                        && nowTimeInt < 18 * 60)
                        || conditions == null) continue;

                float rate = RatedConditions.rate(rc, surfSpot, conditions, tideData, plusDays, hour * 60);

                if (rate > bestRate) {
                    bestRate = rate;
                    bestRC = rc.clone();
                }

//                if ("Green Ball".equals(surfSpot.name))
//                    Log.i(TAG, "day" + plusDays + " best hour " + hour + "  " + conditions.getWaveHeightInFt() + "  " + conditions.windSpeed);
            }

            if (bestRC != null) {
                long day = Common.getDay(plusDays, Common.TIME_ZONE);

                map.put(day, bestRC);

                SortedSet<RatedConditions> set = bestByDay.get(day);
                if (set == null) {
                    set = new TreeSet<>();
                    bestByDay.put(day, set);
                }
                replaceInSet(set, bestRC);
            }
        }

        updated.put(surfSpot, surfSpot.conditionsProvider.lastUpdate);

        return map;
    }


    private void replaceInSet(SortedSet<RatedConditions> set, RatedConditions newRC) {
        SurfSpot surfSpot = newRC.surfSpot;

        Iterator<RatedConditions> setI = set.iterator();
        while (setI.hasNext()) {
            RatedConditions rc = setI.next();
            if (rc.surfSpot == surfSpot) setI.remove();
        }

        set.add(newRC);
    }
}
