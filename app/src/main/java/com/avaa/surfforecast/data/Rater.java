package com.avaa.surfforecast.data;


import com.avaa.surfforecast.MainModel;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Created by Alan on 10 Sep 2017.
 */


public class Rater {
    public long lastUpdate = 0;
    public final Map<SurfSpot, TreeMap<Long, RatedConditions>> bestBySpot = new HashMap<>();
    public final Map<Long, SortedSet<RatedConditions>> bestByDay = new HashMap<Long, SortedSet<RatedConditions>>();


    private void initBestByDay() {
        bestByDay.clear();
        for (int plusDays = 0; plusDays <= 6; plusDays++) {
            bestByDay.put(Common.getDay(plusDays, Common.TIME_ZONE), new TreeSet<>());
        }
    }


    public RatedConditions getBest(SurfSpot surfSpot, int plusDays) {
        if (lastUpdate < surfSpot.conditionsProvider.lastUpdate) updateBest();
        if (bestBySpot.get(surfSpot) == null) return null;
        return bestBySpot.get(surfSpot).get(Common.getDay(plusDays, Common.TIME_ZONE));
    }


    public RatedConditions getBest(int plusDays) {
        return bestByDay.get(Common.getDay(plusDays, Common.TIME_ZONE)).first();
    }


    public void updateBest() {
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
        }

        TideData tideData = MainModel.instance.tideDataProvider.getTideData(surfSpot.tidePortID);

        for (int plusDays = 0; plusDays <= 6; plusDays++) {
            SurfConditions surfConditions = null;

            int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);

            float bestRate = -1;
            int bestTime = -1;

            SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);

            if (surfConditionsOneDay == null) continue;

            for (int time = 6; time < 18; time++) {
                SurfConditions conditions = surfConditionsOneDay.get(time * 60);
                if ((plusDays == 0 && time * 60 < nowTimeInt - 120 && nowTimeInt < 18)
                        || time < 5 || time > 19 || conditions == null) continue;
                float rate = RatedConditions.rate(conditions, surfSpot, tideData, plusDays, time * 60);
                if (rate > bestRate) {
                    bestRate = rate;
                    bestTime = time * 60;
                    surfConditions = conditions;
                }
            }

            if (surfConditions != null) {
                map.put(Common.getDay(plusDays, Common.TIME_ZONE), new RatedConditions(surfSpot, plusDays, bestTime, surfConditions, tideData));
            }
        }

        return map;
    }
}
