package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

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
            if (name.equals(selectedSpot.metarName) && selectedNow && selectedMETAR != metar) {
                Changes changes = updateSelectedConditions(false);
                changes.add(updateSelectedConditionsRating(false));
                fireChanged(changes);
            }
        });

        tideDataProvider.addListener(portID -> {
            if (portID.equals(getSelectedSpot().tidePortID)) {
                Changes changes = new Changes();
                changes.add(updateSelectedTideData(false));
                if (!changes.isEmpty()) {
                    changes.add(updateSelectedConditions(false));
                }
                fireChanged(changes);
            }
        });

        willBeSelectedSpotI = getLastSelectedSpotI(sharedPreferences);
    }


    public void init() {
        tideDataProvider.init();
        metarProvider.init();

        surfSpots.init();

        setSelectedSpotI(willBeSelectedSpotI);

        rater.updateAll();
//        setSelectedDay(-0.01f);
    }


    // --


    private int selectedSpotI = -1;
    private SurfSpot selectedSpot = null;
    private int selectedDay = -1; // plus days
    private float selectedDayFloat = -1; // plus days
    private int selectedTime = -1; // 24*60
    private boolean selectedNow = true;

    public SurfConditions selectedConditions = null;
    public METAR selectedMETAR = null;
    public TideData selectedTideData = null;

    public float nowH;

    private RatedConditions selectedRatedConditions = null;


    public int getSelectedSpotI() {
        return selectedSpotI == -1 ? willBeSelectedSpotI : selectedSpotI;
    }

    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;

//        Log.i(TAG, "setSelectedSpotI() " + i);

        Changes changes = new Changes(SELECTED_SPOT);
        changes.add(ALL_CONDITIONS); //TODO

        selectedSpotI = i;
        selectedSpot = getSelectedSpot();

        if (selectedDay == -1) {
            selectedDayFloat = -0.01f;
            selectedDay = 0;
            changes.add(SELECTED_DAY);
            changes.add(SELECTED_DAY_FLOAT);
        }

        if (selectedSpotI == -1) {
            selectedTime = -1;
        } else {
            changes.add(updateSelectedTideData(false));

            if (selectedNow) {
                changes.add(setSelectedTime(Common.getNowTimeInt(Common.TIME_ZONE), false));
            } else {
                changes.add(selectBestTime(false));
            }

            changes.add(updateSelectedConditions(false));
            changes.add(updateSelectedConditionsRating(false));

            userStat.incrementSpotsShownCount();

            fireChanged(changes);

            sharedPreferences.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
        }
    }

    public boolean isSelectedNow() {
        return selectedNow;
    }

    public int getSelectedTime() {
        return selectedTime;
    }

    public Change selectBestTime(boolean fire) {
        RatedConditions best = rater.getBest(selectedSpot, getSelectedDay());
        if (best != null) {
            return setSelectedTime(best.time, fire);
        }
        return null;
    }

    public Change setSelectedTime(int selectedTime, boolean fire) {
        if (this.selectedTime != selectedTime) {
            if (getSelectedDay() == 0 && Math.abs(this.selectedTime - selectedTime) <= 30) {
                selectedNow = true;
            } else {
                selectedNow = false;
            }
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


    private Change updateSelectedTideData(boolean fire) {
        final TideData newData = tideDataProvider.getTideData(getSelectedSpot().tidePortID);
        if (selectedTideData != newData) {
            selectedTideData = newData;
            if (fire) fireChanged(Change.SELECTED_TIDE_DATA);
            else return Change.SELECTED_TIDE_DATA;
        }
        return null;
    }


    private final SurfConditionsProvider.UpdateListener scpul = surfConditionsProvider -> {
        if (getSelectedSpot().conditionsProvider == surfConditionsProvider) {
            Changes changes = new Changes(ALL_CONDITIONS);
            changes.add(updateSelectedConditions(false));
            changes.add(updateSelectedConditionsRating(false));
            fireChanged(changes);
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


    public float getSelectedDayFloat() {
        return selectedDayFloat;
    }

    public void setSelectedDayFloat(float selectedDayFloat) {
//        Log.i(TAG, "setSelectedDay(" + selectedDay);

        if (this.selectedDayFloat != selectedDayFloat) {
            this.selectedDayFloat = selectedDayFloat;
            fireChanged(SELECTED_DAY_FLOAT);
        }
    }

    public int getSelectedDay() {
        return selectedDay;
    }

    public void setSelectedDay(int selectedDay, boolean fast) {
//        Log.i(TAG, "setSelectedDay(" + selectedDay);

        if (this.selectedDay == selectedDay) return;

        Changes changes = new Changes(SELECTED_DAY);

        this.selectedDay = selectedDay;

        if (fast && this.selectedDayFloat != selectedDay) {
            this.selectedDayFloat = selectedDay;
            changes.add(SELECTED_DAY_FLOAT);
        }
//        Log.i(TAG, "selected day int changed");

        SurfSpot spot = getSelectedSpot();

//        int nowTimeInt = Common.getNowTimeInt(TIME_ZONE);

        if (getSelectedDay() == 0) {
            selectedNow = true;
            selectedTime = Common.getNowTimeInt(Common.TIME_ZONE);
            changes.add(SELECTED_TIME);

            changes.add(updateSelectedConditions(false));
            changes.add(updateSelectedConditionsRating(false));
        } else {
            SurfConditionsOneDay conditionsOneDay = spot.conditionsProvider.get(getSelectedDay());
            SurfConditions newSC; // = null;

            selectedNow = false;

            if (selectedMETAR != null) {
                selectedMETAR = null;
                changes.add(SELECTED_CONDITIONS);
            }

            if (conditionsOneDay == null) {
//            selectedTime = -1; //nowTimeInt;
                newSC = null;
            } else {
                RatedConditions best = rater.getBest(spot, getSelectedDay());
                if (best != null) {
                    selectedTime = best.time;
                    changes.add(SELECTED_TIME);

                    selectedRatedConditions = best;
                    changes.add(SELECTED_RATING);

//                    Log.i(TAG, "rate " + best.rating + " best time " + best.time / 60 + "  " + best.surfConditions.getWaveHeightInFt() + "  " + best.surfConditions.windSpeed);
                    newSC = best.surfConditions;
                } else {
                    newSC = conditionsOneDay.get(selectedTime);
                }
            }

            if (selectedConditions != newSC) {
                selectedConditions = newSC;
                changes.add(SELECTED_CONDITIONS);
            }
        }

        fireChanged(changes);
    }


    public Changes updateSelectedConditions(boolean fire) {
        SurfConditionsOneDay conditionsOneDay = selectedSpot.conditionsProvider.get(getSelectedDay());
        SurfConditions newConditions = conditionsOneDay == null ? null : conditionsOneDay.get(selectedTime);

        METAR newMetar = selectedNow ? metarProvider.get(selectedSpot.metarName) : null;

        if (!SurfConditions.equals(selectedConditions, newConditions) || selectedMETAR != newMetar) {
            selectedConditions = newConditions;
            selectedMETAR = newMetar;

            Changes changes = new Changes(SELECTED_CONDITIONS);

            if (fire) {
                changes.add(updateSelectedConditionsRating(false));
                fireChanged(changes);
            }
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
        if (!selectedNow) return null;

        selectedTime = Common.getNowTimeInt(Common.TIME_ZONE);

        if (selectedSpot == null) return null;

        selectedSpot.conditionsProvider.updateIfNeed();

        SurfConditions newCC = selectedSpot.conditionsProvider.getNow();
        METAR newMETAR = metarProvider.get(selectedSpot.metarName);

        Change selectedConditionsChanged = null;

        if (newCC != selectedConditions || newMETAR != selectedMETAR) {
            selectedConditionsChanged = SELECTED_CONDITIONS;
            selectedConditions = newCC;
            selectedMETAR = newMETAR;
        }

        Change ratingChanged = updateSelectedConditionsRating(false);

        if (fire) {
            fireChanged(SELECTED_TIME, selectedConditionsChanged, ratingChanged);
            return null;
        } else {
            return new Changes(SELECTED_TIME, selectedConditionsChanged, ratingChanged);
        }
//        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());
    }


    public Change updateSelectedConditionsRating(boolean fire) {
        RatedConditions old = selectedRatedConditions;

        SurfConditions surfConditions = this.selectedConditions;

        if (selectedNow && selectedMETAR != null) {
            surfConditions = new SurfConditions(surfConditions);
            surfConditions.windSpeed = selectedMETAR.windSpeed;
            surfConditions.waveAngle = selectedMETAR.windAngle;
        }

        selectedRatedConditions = RatedConditions.create(getSelectedSpot(), getSelectedDay(), selectedTime, surfConditions, selectedTideData);

        if (!RatedConditions.sameEstimate(old, selectedRatedConditions)) {
            if (fire) fireChanged(SELECTED_RATING);
            return SELECTED_RATING;
        }

        return null;
    }


    public void updateSelectedSpotAll() {
        if (selectedSpot == null) return;
        selectedSpot.conditionsProvider.update();
        metarProvider.update(selectedSpot.metarName);
        tideDataProvider.fetchIfNeed(selectedSpot.tidePortID);
    }


    public void updateAll() {
        for (SurfSpot s : surfSpots.getAll()) {
            s.conditionsProvider.update();
            metarProvider.update(s.metarName);
            tideDataProvider.fetchIfNeed(s.tidePortID);
        }
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


    public void allRatingsUpdated() {
        fireChanged(Change.SELECTED_RATING);
    }


    public enum Change {SELECTED_SPOT, ALL_CONDITIONS, SELECTED_CONDITIONS, SELECTED_DAY, SELECTED_DAY_FLOAT, SELECTED_TIME, SELECTED_TIDE_DATA, SELECTED_RATING}


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }

    private Map<ChangeListener, Set<Change>> changeListeners = new HashMap<>();


    public void addChangeListener(ChangeListener l) { // for all changes
        changeListeners.put(l, null);
    }

    public void addChangeListener(Change change, ChangeListener l) {
        changeListeners.put(l, new Changes(change));
    }

    public void addChangeListener(Change change, Change change2, ChangeListener l) { // if change or change2 occur
        changeListeners.put(l, new Changes(change, change2));
    }

    public void addChangeListener(Change change, Change change2, Change change3, ChangeListener l) {
        changeListeners.put(l, new Changes(change, change2, change3));
    }

    public void addChangeListener(Set<Change> changes, ChangeListener l) {
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
//        Log.i(TAG, "fire");
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