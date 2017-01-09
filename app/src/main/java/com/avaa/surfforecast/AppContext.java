package com.avaa.surfforecast;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.METARProvider;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.data.TidesProvider;

/**
 * Created by Alan on 7 Oct 2016.
 */

public class AppContext {
    public final SharedPreferences sharedPreferences;

    public final UsageStat usageStat;
    public final METARProvider metarProvider;
    public final SurfSpots surfSpots;
    //public final TidesProvider tidesProvider;
    public final TideDataProvider tideDataProvider;


    public static AppContext instance = null;


    public static AppContext getInstance(SharedPreferences sharedPreferences, BusyStateListener bsl) {
        instance = new AppContext(sharedPreferences, bsl);
        return instance;
    }


    public AppContext(SharedPreferences sharedPreferences, BusyStateListener bsl) {
        this.sharedPreferences = sharedPreferences;

        usageStat = UsageStat.getInstance(sharedPreferences);
        metarProvider = METARProvider.getInstance(sharedPreferences, bsl);
        surfSpots = SurfSpots.getInstance(sharedPreferences, bsl);
        //tidesProvider = TidesProvider.getInstance(sharedPreferences, bsl);
        tideDataProvider = TideDataProvider.getInstance(sharedPreferences);
    }
}
