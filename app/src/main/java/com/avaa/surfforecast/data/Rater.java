package com.avaa.surfforecast.data;


import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.utils.DT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.avaa.surfforecast.MainModel.N_DAYS;


/**
 * Created by Alan on 10 Sep 2017.
 */


public class Rater {
    private static final String TAG = "Rater";

    private final Map<SurfSpot, Long> updated = new HashMap<>();
    private Map<SurfSpot, TreeMap<Long, RatedConditions>> bestBySpot = new HashMap<>();
    private final Map<Long, SortedSet<RatedConditions>> bestByDay = new HashMap<>();


    private void initBestByDay() {
        bestByDay.clear();
        for (int plusDays = 0; plusDays <= N_DAYS; plusDays++) {
            bestByDay.put(DT.getDay(plusDays, DT.TIME_ZONE), new TreeSet<>());
        }
    }


    public RatedConditions getBest(@NonNull SurfSpot surfSpot, int plusDays) {
        final Long updated = this.updated.get(surfSpot);
        if (updated == null || updated < surfSpot.conditionsProvider.lastUpdate) {
            updateBest(surfSpot);
        }

        if (bestBySpot.get(surfSpot) == null) return null;

        return bestBySpot.get(surfSpot).get(DT.getDay(plusDays, DT.TIME_ZONE));
    }


    public RatedConditions getBestForDay(int plusDays) {
        if (bestByDay.isEmpty()) {
            return null;
        }
        return bestByDay.get(DT.getDay(plusDays, DT.TIME_ZONE)).first();
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


    public void updateBestByDay() {
//        Log.i(TAG, "updateBestByDay()");
        initBestByDay();

        for (Map.Entry<SurfSpot, TreeMap<Long, RatedConditions>> ei : bestBySpot.entrySet()) {
            TreeMap<Long, RatedConditions> map = ei.getValue();
            for (Map.Entry<Long, RatedConditions> entry : map.entrySet()) {
                bestByDay.get(entry.getKey()).add(entry.getValue());
            }
        }
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

        int nowTimeInt = DT.getNowTimeMinutes(DT.TIME_ZONE);

        RatedConditions rc = new RatedConditions();

        for (int plusDays = 0; plusDays <= N_DAYS; plusDays++) {
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
                long day = DT.getDay(plusDays, DT.TIME_ZONE);

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


    // Contains rated conditions for spots for one specific day
    private static void replaceInSet(SortedSet<RatedConditions> set, RatedConditions newRC) {
        SurfSpot surfSpot = newRC.surfSpot;

        Iterator<RatedConditions> setI = set.iterator();
        while (setI.hasNext()) {
            RatedConditions rc = setI.next();
            if (rc.surfSpot == surfSpot) setI.remove();
        }

        set.add(newRC);
    }


    // --


    private static TreeMap<Long, RatedConditions> rateSpot(SurfSpot surfSpot) {
        TreeMap<Long, RatedConditions> map = new TreeMap<>();

        final TideData tideData = MainModel.instance.tideDataProvider.getTideData(surfSpot.tidePortID);

        final RatedConditions rc = new RatedConditions();

        for (int plusDays = 0; plusDays <= N_DAYS; plusDays++) {
            RatedConditions bestRC = null;
            float bestRate = -1;

            final SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);

            if (surfConditionsOneDay == null) continue;

            for (int min = 6 * 60; min < 18 * 60; min += 60) {
                SurfConditions conditions = surfConditionsOneDay.get(min);

                if (conditions == null) continue;

                float rate = RatedConditions.rate(rc, surfSpot, conditions, tideData, plusDays, min);

                if (rate > bestRate) {
                    bestRate = rate;
                    bestRC = rc.clone();
                }

//                if ("Green Ball".equals(surfSpot.name))
//                    Log.i(TAG, "day" + plusDays + " best min " + min + "  " + conditions.getWaveHeightInFt() + "  " + conditions.windSpeed);
            }

            if (bestRC != null) {
                long day = DT.getDay(plusDays, DT.TIME_ZONE);
                map.put(day, bestRC);
            }
        }

        return map;
    }


    // --


    public void updateAll() {
        if (DataRetrieversPool.getTask(TAG, RaterAsyncTask.class) == null) {
            DataRetrieversPool.addTask(TAG, new RaterAsyncTask(MainModel.instance.surfSpots.getAll()));
        }
    }


    public static class RaterAsyncTask extends AsyncTask<Object, Void, Map<SurfSpot, TreeMap<Long, RatedConditions>>> {
        private final List<SurfSpot> spots;
        private long time;

        public RaterAsyncTask(List<SurfSpot> spots) {
            this.spots = spots;
        }

        @Override
        protected Map<SurfSpot, TreeMap<Long, RatedConditions>> doInBackground(Object... lists) {
            time = System.currentTimeMillis();

            final Map<SurfSpot, TreeMap<Long, RatedConditions>> bestBySpot = new HashMap<>();

            for (SurfSpot surfSpot : spots) {
                if (surfSpot.conditionsProvider.hasData()) {
                    bestBySpot.put(surfSpot, rateSpot(surfSpot));
                }
            }

            return bestBySpot;
        }

        @Override
        protected void onPostExecute(Map<SurfSpot, TreeMap<Long, RatedConditions>> surfSpotTreeMapMap) {
            MainModel.instance.rater.asyncFinished(surfSpotTreeMapMap, time);
        }
    }


    private void asyncFinished(Map<SurfSpot, TreeMap<Long, RatedConditions>> surfSpotTreeMapMap, long time) {
        bestBySpot = surfSpotTreeMapMap;

        for (SurfSpot surfSpot : surfSpotTreeMapMap.keySet()) {
            updated.put(surfSpot, time);
        }

        updateBestByDay();

        MainModel.instance.allRatingsUpdated();
    }
}
