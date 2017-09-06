package com.avaa.surfforecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfConditionsOneDay;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;


/**
 * Created by Alan on 30 May 2017.
 */


public class MainModel {
    private static final String TAG = "MainModel";

    private static final String SPKEY_FAV_SPOTS = "favSpots";
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    public final int willBeSelectedSpotI = 3; //TODO
    public int selectedSpotI = -1;


    public MainModel(Context context, BusyStateListener bsl) {
        for (SurfSpot surfSpot : AppContext.instance.surfSpots.getAll()) {
            surfSpot.conditionsProvider.setBsl(bsl);
            surfSpot.conditionsProvider.addUpdateListener(scpul);
        }

        AppContext.instance.metarProvider.addUpdateListener((name, metar) -> {
            SurfSpot selectedSpot = getSelectedSpot();
            if (selectedSpot == null) return;
            if (name.equals(selectedSpot.metarName) && selectedMETAR != metar) {
                selectedMETAR = metar;
                fireChanged(Change.CURRENT_CONDITIONS);
            }
        });
    }


    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");
        selectedSpotI = i;
        updateCurrentConditions(false);
        //Log.i("SurfSpots", "setSelectedSpotI() 2");
        AppContext.instance.userStat.incrementSpotsShownCount();
        fireChanged(new HashSet<Change>() {{
            add(Change.SELECTED_SPOT);
            add(Change.CONDITIONS);
            add(Change.CURRENT_CONDITIONS);
        }});

        SharedPreferences sp = AppContext.instance.sharedPreferences;
        sp.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
    }

    public SurfSpot getSelectedSpot() {
        List<SurfSpot> list = AppContext.instance.surfSpots.getAll();
//        selectedSpotI = AppContext.instance.surfSpots.selectedSpotI;
        if (selectedSpotI == -1) return list.get(willBeSelectedSpotI);
        if (selectedSpotI >= list.size()) selectedSpotI = list.size() - 1;
        return list.get(selectedSpotI);
    }


    private final SurfConditionsProvider.UpdateListener scpul = surfConditionsProvider -> {
        if (getSelectedSpot().conditionsProvider == surfConditionsProvider) {
            selectedConditions = surfConditionsProvider.getNow();
            fireChanged(new HashSet<Change>() {{
                add(Change.CONDITIONS);
                add(Change.CURRENT_CONDITIONS);
            }});
        }
    };


    public SurfConditions selectedConditions = null;
    public METAR selectedMETAR = null;
    public TideData selectedTideData = null;

    public SurfConditions nowConditions = null;
    public METAR nowMETAR = null;
    public TideData nowTideData = null;

    public float nowH;

    private float day = 0;
    public float time = -1;

    public float selectedRating = -1;
    public int selectedTime = -1;


    public int getDayInt() {
        return Math.round(day);
    }

    public float getDay() {
        return day;
    }

    public void setDay(float day) {
        if (this.day == day) return;

        if (Math.round(this.day) == Math.round(day)) {
            this.day = day;
            return;
        } else {
            this.day = day;
            fireChanged(Change.SELECTED_DAY);
            Log.i(TAG, "selected day changed");
        }

        SurfSpot spot = getSelectedSpot();
        SurfConditionsOneDay conditionsOneDay = spot.conditionsProvider.get(Math.round(day));
        SurfConditions newSC = null;

        if (time == -1) {
            int nowTimeInt = Common.getNowTimeInt(TIME_ZONE);

            if (conditionsOneDay == null) {
                selectedTime = -1;
                selectedRating = -1;
                selectedConditions = null;
                return; //continue;
            }

            selectedRating = -1;

            for (int time = 6; time < 18; time++) {
                SurfConditions conditions = conditionsOneDay.get(time * 60);
                if ((day == 0 && time * 60 < nowTimeInt - 120 && nowTimeInt < 18) || time < 5 || time > 19)
                    continue;
                float rate = conditions.rate(spot, AppContext.instance.tideDataProvider.getTideData(spot.tidePortID), Math.round(day), time * 60);
                if (rate > selectedRating) {
                    selectedRating = rate;
                    selectedTime = time * 60;
                    newSC = conditions;
                }
            }
//            for (Map.Entry<Integer, SurfConditions> entry : conditionsOneDay.entrySet()) {
//                Integer time = entry.getKey();
//                if ((day == 0 && time < nowTimeInt - 120 && nowTimeInt < 18*60) || time < 5 * 60 || time > 19 * 60) continue;
//                float rate = entry.getValue().rate(spot, AppContext.instance.tideDataProvider.getTideData(spot.tidePortID), Math.round(day), time);
//                if (rate > selectedRating) {
//                    selectedRating = rate;
//                    selectedTime = time;
//                    newSC = entry.getValue();
//                }
//            }
        }

        if (selectedConditions != newSC) {
            selectedConditions = newSC;
            fireChanged(new HashSet<Change>() {{
                add(Change.CURRENT_CONDITIONS);
            }});
        }

        fireChanged(Change.SELECTED_TIME);
    }


    public void updateCurrentConditions() {
        updateCurrentConditions(true);
    }

    public void updateCurrentConditions(boolean fire) {
        SurfSpot spot = getSelectedSpot();

        if (spot == null) return;

        spot.conditionsProvider.updateIfNeed();

        SurfConditions newCC = spot.conditionsProvider.getNow();
        METAR newMETAR = AppContext.instance.metarProvider.get(spot.metarName);

        if (newCC == nowConditions && newMETAR == nowMETAR) return;

        nowConditions = newCC;
        nowMETAR = newMETAR;

        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());

        if (fire) fireChanged(new HashSet<Change>() {{
            add(Change.CURRENT_CONDITIONS);
        }});
    }


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }

    public enum Change {SELECTED_SPOT, CONDITIONS, CURRENT_CONDITIONS, SELECTED_DAY, SELECTED_TIME, TIDE}

    private Map<ChangeListener, Set<Change>> cls = new HashMap<>();

    public void addChangeListener(ChangeListener l) {
        cls.put(l, null);
    }

    public void addChangeListener(ChangeListener l, Change change) {
        cls.put(l, new HashSet<Change>() {{
            add(change);
        }});
    }

    public void addChangeListener(ChangeListener l, Set<Change> changes) {
        cls.put(l, changes);
    }

    private static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        for (T ai : a) {
            for (T bi : b) {
                if (ai == bi) return true;
            }
        }
        return false;
    }

    public void fireChanged(Change change) {
        fireChanged(new HashSet<Change>() {{
            add(change);
        }});
    }

    private void fireChanged(Set<Change> changes) {
        for (Map.Entry<ChangeListener, Set<Change>> e : cls.entrySet()) {
            if (e.getValue() == null || hasIntersection(e.getValue(), changes))
                e.getKey().onChange(changes);
        }
    }
}
