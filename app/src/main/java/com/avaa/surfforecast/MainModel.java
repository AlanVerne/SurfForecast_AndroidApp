package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.util.Log;

import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.ai.VoiceRecognitionHelper;
import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.METARProvider;
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
import static com.avaa.surfforecast.views.BaliMap.STR_DASH;


/**
 * Created by Alan on 7 Oct 2016.
 */


public class MainModel {
    private static final String TAG = "MainModel";
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    private final int willBeSelectedSpotI = 3; //TODO
    public int selectedSpotI = -1;

    public final SharedPreferences sharedPreferences;

    public final MainActivity mainActivity;

    public final UserStat userStat;

    public final METARProvider metarProvider;
    public final SurfSpots surfSpots;
    public final TideDataProvider tideDataProvider;

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
        surfSpots = new SurfSpots(sharedPreferences);
        tideDataProvider = new TideDataProvider();

        for (SurfSpot surfSpot : surfSpots.getAll()) {
            surfSpot.conditionsProvider.setBsl(bsl);
            surfSpot.conditionsProvider.addUpdateListener(scpul);
        }

        metarProvider.addUpdateListener((name, metar) -> {
            SurfSpot selectedSpot = getSelectedSpot();
            if (selectedSpot == null) return;
            if (name.equals(selectedSpot.metarName) && selectedMETAR != metar) {
                selectedMETAR = metar;
                fireChanged(Change.CURRENT_CONDITIONS);
            }
        });
    }

    public void init() {
        tideDataProvider.init();
        metarProvider.init();

        surfSpots.init();
    }


    // --


    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");
        selectedSpotI = i;
        updateCurrentConditions(false);
        //Log.i("SurfSpots", "setSelectedSpotI() 2");
        MainModel.instance.userStat.incrementSpotsShownCount();
        fireChanged(new HashSet<Change>() {{
            add(Change.SELECTED_SPOT);
            add(Change.CONDITIONS);
            add(Change.CURRENT_CONDITIONS);
        }});

        SharedPreferences sp = MainModel.instance.sharedPreferences;
        sp.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
    }

    public SurfSpot getSelectedSpot() {
        List<SurfSpot> list = MainModel.instance.surfSpots.getAll();
//        selectedSpotI = MainModel.instance.surfSpots.selectedSpotI;
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


    public int getSelectedWindSpeed() {
        if (selectedMETAR != null) return selectedMETAR.windSpeed;
        else if (selectedConditions != null) return selectedConditions.windSpeed;
        else return -1;
    }

    public float getSelectedWindAngle() {
        if (selectedConditions == null) return -1;
        return selectedMETAR != null ? selectedMETAR.windAngle : selectedConditions.windAngle;
    }


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
                if ((day == 0 && time * 60 < nowTimeInt - 120 && nowTimeInt < 18) || time < 5 || time > 19 || conditions == null)
                    continue;
                float rate = conditions.rate(spot, MainModel.instance.tideDataProvider.getTideData(spot.tidePortID), Math.round(day), time * 60);
                if (rate > selectedRating) {
                    selectedRating = rate;
                    selectedTime = time * 60;
                    newSC = conditions;
                }
            }
//            for (Map.Entry<Integer, SurfConditions> entry : conditionsOneDay.entrySet()) {
//                Integer time = entry.getKey();
//                if ((day == 0 && time < nowTimeInt - 120 && nowTimeInt < 18*60) || time < 5 * 60 || time > 19 * 60) continue;
//                float rate = entry.getValue().rate(spot, MainModel.instance.tideDataProvider.getTideData(spot.tidePortID), Math.round(day), time);
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
        METAR newMETAR = MainModel.instance.metarProvider.get(spot.metarName);

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