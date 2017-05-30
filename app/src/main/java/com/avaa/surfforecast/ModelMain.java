package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.util.Log;

import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alan on 30 May 2017.
 */

public class ModelMain {
    private static final String TAG = "ModelMain";

    private static final String SPKEY_FAV_SPOTS = "favSpots";
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    public static final String WADD = "WADD";

    public final int willBeSelectedSpotI = 3; //TODO
    public int selectedSpotI = -1;


    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");
        selectedSpotI = i;
        updateCurrentConditions(false);
        //Log.i("SurfSpots", "setSelectedSpotI() 2");
        AppContext.instance.userStat.incrementSpotsShownCount();
        fireChanged(new HashSet<Change>(){{add(Change.SELECTED_SPOT);add(Change.CONDITIONS);add(Change.CURRENT_CONDITIONS);}});

        SharedPreferences sp = AppContext.instance.sharedPreferences;
        sp.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
    }
    public SurfSpot getSelectedSpot() {
        List<SurfSpot> list = AppContext.instance.surfSpots.getAll();
        if (selectedSpotI == -1) return list.get(willBeSelectedSpotI);
        if (selectedSpotI >= list.size()) selectedSpotI = list.size() - 1;
        return list.get(selectedSpotI);
    }


    public SurfConditions currentConditions = null;
    public METAR currentMETAR = null;
//    public TideData currentTideData = null; // unsupported

    public void updateCurrentConditions() {
        updateCurrentConditions(true);
    }
    public void updateCurrentConditions(boolean fire) {
        SurfSpot spot = getSelectedSpot();

        if (spot == null) return;

        spot.conditionsProvider.updateIfNeed();

        SurfConditions newCC = spot.conditionsProvider.getNow();
        METAR newMETAR = AppContext.instance.metarProvider.get(spot.metarName);

        if (newCC == currentConditions && newMETAR == currentMETAR) return;

        currentConditions = newCC;
        currentMETAR = newMETAR;

        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());

        if (fire) fireChanged(new HashSet<Change>(){{add(Change.CURRENT_CONDITIONS);}});
    }


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }
    public enum Change { SELECTED_SPOT, CONDITIONS, CURRENT_CONDITIONS }
    private Map<ChangeListener, Set<Change>> cls = new HashMap<>();
    public void addChangeListener(ChangeListener l) {
        cls.put(l, null);
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
    private void fireChanged(Set<Change> changes) {
        for (Map.Entry<ChangeListener, Set<Change>> e : cls.entrySet()) {
            if (e.getValue() == null || hasIntersection(e.getValue(), changes)) e.getKey().onChange(changes);
        }
    }
}
