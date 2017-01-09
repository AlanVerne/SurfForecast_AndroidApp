package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.util.Log;

import com.avaa.surfforecast.data.BusyStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alan on 7 Oct 2016.
 */

public class UsageStat {
    private static final String SPKEY_TIMES_OPENED = "timesOpened"; // since last measure
    private static final String SPKEY_SPOTS_SHOWN_COUNT = "spotsShownCount"; // since last measure
    private static final String SPKEY_LAST_TIME_OPENED = "lastTimeOpened";
    private static final String SPKEY_FIRST_TIME_OPENED = "firstTimeOpened";
    private static final String TAG = "UsageStat";
    private int spotsShownCount;
    public int userLevel = 2;


    public static UsageStat getInstance() {
        return instance;
    }
    public static UsageStat getInstance(SharedPreferences sp) {
        if (instance == null) instance = new UsageStat(sp);
        return instance;
    }


    private static UsageStat instance = null;


    public UsageStat(SharedPreferences sp) {
        spotsShownCount = sp.getInt(SPKEY_SPOTS_SHOWN_COUNT, 0);
        if (spotsShownCount > 10) userLevel = 2;
    }


    public void incrementSpotsShownCount() {
        setSpotsShownCount(spotsShownCount+1);
    }


    public interface UserLevelListener {
        void UserLevelChanged(int l);
    }
    public List<UserLevelListener> ulls = new ArrayList<>();
    public void addUserLevelListener(UserLevelListener ull) {
        ulls.add(ull);
        ull.UserLevelChanged(userLevel);
    }

    public void setSpotsShownCount(int c) {
        if (c > 10 && userLevel > 1) {
            spotsShownCount = c;
            userLevel = 1;
            for (UserLevelListener userLevelListener : ulls) {
                userLevelListener.UserLevelChanged(userLevel);
            }
        }
        else {
            spotsShownCount = c;
        }

        //Log.i(TAG, "spotsShownCount " + spotsShownCount);
    }


    public void save() {
        SharedPreferences.Editor spe = AppContext.instance.sharedPreferences.edit();
        spe.putInt(SPKEY_SPOTS_SHOWN_COUNT, spotsShownCount);
        spe.apply();
    }
}
