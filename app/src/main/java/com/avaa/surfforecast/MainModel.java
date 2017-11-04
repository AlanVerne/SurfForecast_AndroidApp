package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.ai.VoiceRecognitionHelper;
import com.avaa.surfforecast.data.BusyStateListener;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.avaa.surfforecast.MainModel.Change.ALL_CONDITIONS;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_CONDITIONS;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_DAY;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_DAY_FLOAT;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_RATING;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_SPOT;
import static com.avaa.surfforecast.MainModel.Change.SELECTED_TIME;


/**
 * Created by Alan on 7 Oct 2016.
 */


public class MainModel {
    private static final String TAG = "MainModel";
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    private final int willBeSelectedSpotI; //TODO

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
                fireChanged(SELECTED_CONDITIONS);
            }
        });

        willBeSelectedSpotI = getLastSelectedSpotI(sharedPreferences);
    }


    public void init() {
        tideDataProvider.init();
        metarProvider.init();

        surfSpots.init();

        setSelectedSpotI(willBeSelectedSpotI);
        setSelectedDay(-0.01f);
    }


    // --

    private int selectedSpotI = -1;
    private SurfSpot selectedSpot = null;
    private float selectedDay = -1; // plus days
    private int selectedTime = -1; // 24*60

    public SurfConditions selectedConditions = null;
    public METAR selectedMETAR = null;
    public TideData selectedTideData = null;

    private SurfConditions nowConditions = null;
    private METAR nowMETAR = null;

    public float nowH;

    private RatedConditions selectedRatedConditions = null;


    public int getSelectedSpotI() {
        return selectedSpotI == -1 ? willBeSelectedSpotI : selectedSpotI;
    }

    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");

        Changes changes = new Changes(SELECTED_SPOT);
        changes.add(ALL_CONDITIONS); //TODO

        selectedSpotI = i;
        selectedSpot = getSelectedSpot();

        if (selectedSpotI == -1) {
            selectedTime = -1;
        } else {
            selectedTideData = tideDataProvider.getTideData(selectedSpot.tidePortID);

            changes.add(selectBestTime(false));
            changes.add(updateSelectedConditions(false));

//            updateCurrentConditions(false);
            //Log.i("SurfSpots", "setSelectedSpotI() 2");

            userStat.incrementSpotsShownCount();

            fireChanged(changes);

            sharedPreferences.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
        }
    }

    public int getSelectedTime() {
        return selectedTime;
    }

    public Change selectBestTime(boolean fire) {
        RatedConditions best = rater.getBest(selectedSpot, getSelectedDayInt());
        if (best != null) {
            return setSelectedTime(best.time, fire);
        }
        return null;
    }

    public Change setSelectedTime(int selectedTime, boolean fire) {
        if (this.selectedTime != selectedTime) {
            this.selectedTime = selectedTime;
            if (fire) {
                fireChanged(new Changes(SELECTED_TIME).add(updateSelectedConditions(false)));
            }
            return SELECTED_TIME; //TODO think about silent changes
        }
        return null;
    }

    public SurfSpot getSelectedSpot() {
        List<SurfSpot> list = surfSpots.getAll();
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
            nowConditions = surfConditionsProvider.getNow();
            fireChanged(new Changes(ALL_CONDITIONS).add(updateSelectedConditions(false)));
        }
    };


    public int getSelectedWindSpeed() {
        if (selectedMETAR != null) return selectedMETAR.windSpeed;
        if (selectedConditions != null) return selectedConditions.windSpeed;
        return -1;
    }

    public float getSelectedWindAngle() {
        if (selectedMETAR != null) return selectedMETAR.windAngle;
        if (selectedConditions != null) return selectedConditions.windAngle;
        return -1;
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
                fireChanged(SELECTED_DAY_FLOAT);
                return;
            }
        }

        Changes changes = new Changes(SELECTED_DAY_FLOAT, SELECTED_DAY);

        this.selectedDay = selectedDay;
//        Log.i(TAG, "selected day int changed");

        SurfSpot spot = getSelectedSpot();
        SurfConditionsOneDay conditionsOneDay = spot.conditionsProvider.get(getSelectedDayInt());
        SurfConditions newSC; // = null;

//        int nowTimeInt = Common.getNowTimeInt(TIME_ZONE);

        if (conditionsOneDay == null) {
//            selectedTime = -1; //nowTimeInt;
            newSC = null;
        } else {
            RatedConditions best = rater.getBest(spot, getSelectedDayInt());
            if (best != null) {
                selectedTime = best.time;
                changes.add(SELECTED_TIME);

                selectedRatedConditions = best;
                changes.add(SELECTED_RATING);

//                Log.i(TAG, "best time " + best.time / 60 + "  " + best.surfConditions.getWaveHeightInFt() + "  " + best.surfConditions.windSpeed);
                newSC = best.surfConditions;
            } else {
                newSC = conditionsOneDay.get(selectedTime);
            }
        }

        if (selectedConditions != newSC) {
            selectedConditions = newSC;
            changes.add(SELECTED_CONDITIONS);
        }

        fireChanged(changes);
    }


    public Set<Change> updateSelectedConditions(boolean fire) {
        SurfConditionsOneDay conditionsOneDay = selectedSpot.conditionsProvider.get(getSelectedDayInt());
        SurfConditions newConditions = conditionsOneDay == null ? null : conditionsOneDay.get(selectedTime);

        if (!SurfConditions.equals(selectedConditions, newConditions)) {
            selectedConditions = newConditions;

            Changes changes = new Changes(SELECTED_CONDITIONS);
            changes.add(updateSelectedConditionsRating(false));

            if (fire) fireChanged(changes);
            return changes;
        }

        return null;
    }


    public RatedConditions getSelectedRatedConditions() {
        return selectedRatedConditions;
    }


    public void updateCurrentConditions() {
        updateCurrentConditions(true);
    }

    public Set<Change> updateCurrentConditions(boolean fire) {
        if (selectedSpot == null) return null;

        selectedSpot.conditionsProvider.updateIfNeed();

        SurfConditions newCC = selectedSpot.conditionsProvider.getNow();
        METAR newMETAR = metarProvider.get(selectedSpot.metarName);

        if (newCC == nowConditions && newMETAR == nowMETAR) return null;

        nowConditions = newCC;
        nowMETAR = newMETAR;

        Change ratingChanged = null; //updateSelectedConditionsRating(false);

        if (fire) {
            if (ratingChanged != null) fireChanged(SELECTED_CONDITIONS, ratingChanged);
            else fireChanged(SELECTED_CONDITIONS);
        }

        return new Changes(SELECTED_CONDITIONS);
//        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());
    }


    public Change updateSelectedConditionsRating(boolean fire) {
        RatedConditions old = selectedRatedConditions;

        selectedRatedConditions = RatedConditions.create(getSelectedSpot(), getSelectedDayInt(), selectedTime, selectedConditions, selectedTideData);

        if (!RatedConditions.sameEstimate(old, selectedRatedConditions)) {
            if (fire) fireChanged(SELECTED_RATING);
            return SELECTED_RATING;
        }

        return null;
    }


    public void updateSelectedSpotAll() {
        selectedSpot.conditionsProvider.update();
        metarProvider.update(selectedSpot.metarName);
        tideDataProvider.fetchIfNeed(selectedSpot.tidePortID);
    }


    // Change support


    public class Changes extends HashSet<Change> {
        public Changes() {
        }

        public Changes(Change c1) {
            super(1);
            add(c1);
        }

        public Changes(Change c1, Change c2) {
            super(2);
            add(c1);
            add(c2);
        }

        public Changes(Change c1, Change c2, Change c3) {
            super(3);
            add(c1);
            add(c2);
            add(c3);
        }

        public Changes(@NonNull Collection<? extends Change> c) {
            super(c);
        }

        @Override
        public boolean add(Change change) {
            return change != null && super.add(change);
        }

        public Changes add(Collection<? extends Change> changes) {
            if (changes != null) super.addAll(changes);
            return this;
        }
    }


    public enum Change {SELECTED_SPOT, ALL_CONDITIONS, SELECTED_CONDITIONS, SELECTED_DAY, SELECTED_DAY_FLOAT, SELECTED_TIME, TIDE, SELECTED_RATING}


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }

    private Map<ChangeListener, Set<Change>> changeListeners = new HashMap<>();


    public void addChangeListener(ChangeListener l) { // for all changes
        changeListeners.put(l, null);
    }

    public void addChangeListener(ChangeListener l, Change change) {
        changeListeners.put(l, new Changes(change));
    }

    public void addChangeListener(ChangeListener l, Change change, Change change2) { // if change or change2 occured
        changeListeners.put(l, new Changes(change, change2));
    }

    public void addChangeListener(ChangeListener l, Change change, Change change2, Change change3) {
        changeListeners.put(l, new Changes(change, change2, change3));
    }

    public void addChangeListener(ChangeListener l, Set<Change> changes) {
        changeListeners.put(l, changes);
    }


    private void fireChanged(Change change) {
        fireChanged(new Changes(change));
    }

    private void fireChanged(Change change, Change change2) {
        fireChanged(new Changes(change, change2));
    }

    private void fireChanged(Change change, Change change2, Change change3) {
        fireChanged(new Changes(change, change2, change3));
    }

    private void fireChanged(Set<Change> changes) {
        Log.i(TAG, "fire");
        for (Map.Entry<ChangeListener, Set<Change>> e : changeListeners.entrySet()) {
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