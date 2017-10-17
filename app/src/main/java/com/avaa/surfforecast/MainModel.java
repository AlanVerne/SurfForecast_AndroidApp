package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.util.Log;

import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.ai.VoiceRecognitionHelper;
import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.METARProvider;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.Rater;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfConditionsOneDay;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideData;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.data.UserStat;
import com.avaa.surfforecast.drawers.MetricsAndPaints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;


/**
 * Created by Alan on 7 Oct 2016.
 */


public class MainModel {
    private static final String TAG = "MainModel";
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    private final int willBeSelectedSpotI; //TODO
    public int selectedSpotI = -1;

    public final SharedPreferences sharedPreferences;

    public final MainActivity mainActivity;

    public final UserStat userStat;

    public final METARProvider metarProvider;
    public final SurfSpots surfSpots;
    public final TideDataProvider tideDataProvider;

    public final Rater rater;

    public VoiceRecognitionHelper voiceRecognitionHelper;
    public CommandsExecutor commandsExecutor;

    public MetricsAndPaints metricsAndPaints;


    public static MainModel instance = null;

    public static MainModel getInstance(MainActivity ma, SharedPreferences sharedPreferences, BusyStateListener bsl) {
        if (instance == null) instance = new MainModel(ma, sharedPreferences, bsl);
        return instance;
    }


    private MainModel(MainActivity ma, SharedPreferences sharedPreferences, BusyStateListener bsl) {
        mainActivity = ma;

        this.sharedPreferences = sharedPreferences;

        userStat = new UserStat(sharedPreferences);
        metarProvider = new METARProvider(bsl);
        surfSpots = new SurfSpots();
        tideDataProvider = new TideDataProvider();

        rater = new Rater();

        for (SurfSpot surfSpot : surfSpots.getAll()) {
            surfSpot.conditionsProvider.setBsl(bsl);
            surfSpot.conditionsProvider.addUpdateListener(scpul);
        }

        metarProvider.addUpdateListener((name, metar) -> {
            SurfSpot selectedSpot = getSelectedSpot();
            if (selectedSpot == null) return;
            if (name.equals(selectedSpot.metarName) && selectedMETAR != metar) {
                selectedMETAR = metar;
                fireChanged(Change.SELECTED_CONDITIONS);
            }
        });

        willBeSelectedSpotI = getLastSelectedSpotI(sharedPreferences);
    }


    public void init() {
        tideDataProvider.init();
        metarProvider.init();

        surfSpots.init();

        setSelectedSpotI(willBeSelectedSpotI);
        setSelectedDay(0);
    }


    // --


    public SurfConditions selectedConditions = null;
    public METAR selectedMETAR = null;
    public TideData selectedTideData = null;

    public SurfConditions nowConditions = null;
    public METAR nowMETAR = null;
    public TideData nowTideData = null;

    public float nowH;

    private float selectedDay = -1; // plus days
    public int selectedTime = 0; // 24*60

    public float selectedRating = 0;
    public RatedConditions selectedRatedConditions = null;


    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");
        selectedSpotI = i;
        updateCurrentConditions(false);
        //Log.i("SurfSpots", "setSelectedSpotI() 2");
        MainModel.instance.userStat.incrementSpotsShownCount();

        fireChanged(Change.SELECTED_SPOT, Change.ALL_CONDITIONS, Change.SELECTED_CONDITIONS);

        sharedPreferences.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
    }

    public SurfSpot getSelectedSpot() {
        List<SurfSpot> list = MainModel.instance.surfSpots.getAll();
//        selectedSpotI = MainModel.instance.surfSpots.selectedSpotI;
        if (selectedSpotI == -1) return list.get(willBeSelectedSpotI);
        if (selectedSpotI >= list.size()) selectedSpotI = list.size() - 1;
        return list.get(selectedSpotI);
    }

    private static int getLastSelectedSpotI(SharedPreferences sp) {
        return sp.getInt(SPKEY_SELECTED_SPOT, 3);
    }


    private final SurfConditionsProvider.UpdateListener scpul = surfConditionsProvider -> {
        if (getSelectedSpot().conditionsProvider == surfConditionsProvider) {
            selectedConditions = surfConditionsProvider.getNow();
            fireChanged(new HashSet<Change>() {{
                add(Change.ALL_CONDITIONS);
                add(Change.SELECTED_CONDITIONS);
            }});
        }
    };


    public int getSelectedWindSpeed() {
        if (selectedMETAR != null) return selectedMETAR.windSpeed;
        else if (selectedConditions != null) return selectedConditions.windSpeed;
        else return -1;
    }

    public float getSelectedWindAngle() {
        if (selectedConditions == null) return -1;
        return selectedMETAR != null ? selectedMETAR.windAngle : selectedConditions.windAngle;
    }


    public float getSelectedDay() {
        return selectedDay;
    }

    public int getSelectedDayInt() {
        return Math.round(selectedDay);
    }

    public void setSelectedDay(float selectedDay) {
//        Log.i(TAG, "setSelectedDay(" + selectedDay);

        if (this.selectedDay == selectedDay) return;

        if (Math.round(this.selectedDay) == Math.round(selectedDay)) {
            if (this.selectedDay != selectedDay) {
                this.selectedDay = selectedDay;
                fireChanged(Change.SELECTED_DAY_FLOAT);
                return;
            }
        }

        HashSet<Change> changes = new HashSet<>();
        changes.add(Change.SELECTED_DAY_FLOAT);
        changes.add(Change.SELECTED_DAY);

        this.selectedDay = selectedDay;
//        Log.i(TAG, "selected day int changed");

        SurfSpot spot = getSelectedSpot();
        SurfConditionsOneDay conditionsOneDay = spot.conditionsProvider.get(Math.round(selectedDay));
        SurfConditions newSC = null;

        int nowTimeInt = Common.getNowTimeInt(TIME_ZONE);

        if (conditionsOneDay == null) {
//            selectedTime = -1; //nowTimeInt;
            selectedRating = -1;
            newSC = null;
        }
        else {
            selectedRating = -1;

            RatedConditions best = rater.getBest(spot, Math.round(selectedDay));
            if (best != null) {
                selectedTime = best.time;
                changes.add(Change.SELECTED_TIME);

                selectedRating = best.rating;
                selectedRatedConditions = best;
                changes.add(Change.SELECTED_RATING);

                newSC = best.surfConditions;
            } else {
                newSC = conditionsOneDay.get(selectedTime);
            }
        }

        if (selectedConditions != newSC) {
            selectedConditions = newSC;
            changes.add(Change.SELECTED_CONDITIONS);
        }

        fireChanged(changes);
    }


    public void updateCurrentConditions() {
        updateCurrentConditions(true);
    }

    public void updateCurrentConditions(boolean fire) {
        SurfSpot spot = getSelectedSpot();

        if (spot == null) return;

        spot.conditionsProvider.updateIfNeed();

        SurfConditions newCC = spot.conditionsProvider.getNow();
        METAR newMETAR = MainModel.instance.metarProvider.get(spot.metarName);

        if (newCC == nowConditions && newMETAR == nowMETAR) return;

        nowConditions = newCC;
        nowMETAR = newMETAR;

        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());

        if (fire) fireChanged(new HashSet<Change>() {{
            add(Change.SELECTED_CONDITIONS);
        }});
    }


    // Change support


    public enum Change {SELECTED_SPOT, ALL_CONDITIONS, SELECTED_CONDITIONS, SELECTED_DAY, SELECTED_DAY_FLOAT, SELECTED_TIME, TIDE, SELECTED_RATING}


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }

    private Map<ChangeListener, Set<Change>> cls = new HashMap<>();


    public void addChangeListener(ChangeListener l) { // for all changes
        cls.put(l, null);
    }

    public void addChangeListener(ChangeListener l, Change change) {
        cls.put(l, new HashSet<Change>() {{
            add(change);
        }});
    }

    public void addChangeListener(ChangeListener l, Change change, Change change2) { // if change or change2 occured
        cls.put(l, new HashSet<Change>() {{
            add(change);
            add(change2);
        }});
    }

    public void addChangeListener(ChangeListener l, Change change, Change change2, Change change3) {
        cls.put(l, new HashSet<Change>() {{
            add(change);
            add(change2);
            add(change3);
        }});
    }

    public void addChangeListener(ChangeListener l, Set<Change> changes) {
        cls.put(l, changes);
    }


    private void fireChanged(Change change) {
        fireChanged(new HashSet<Change>() {{
            add(change);
        }});
    }

    private void fireChanged(Change change, Change change2) {
        fireChanged(new HashSet<Change>() {{
            add(change);
            add(change2);
        }});
    }

    private void fireChanged(Change change, Change change2, Change change3) {
        fireChanged(new HashSet<Change>() {{
            add(change);
            add(change2);
            add(change3);
        }});
    }

    private void fireChanged(Set<Change> changes) {
        for (Map.Entry<ChangeListener, Set<Change>> e : cls.entrySet()) {
            if (e.getValue() == null || hasIntersection(e.getValue(), changes))
                e.getKey().onChange(changes);
        }
    }


    private static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        for (T ai : a) {
            for (T bi : b) {
                if (ai == bi) return true;
            }
        }
        return false;
    }
}