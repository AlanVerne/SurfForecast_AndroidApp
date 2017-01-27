package com.avaa.surfforecast;

import android.content.SharedPreferences;

import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.METARProvider;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.data.UsageStat;
import com.avaa.surfforecast.drawers.MetricsAndPaints;

/**
 * Created by Alan on 7 Oct 2016.
 */

public class AppContext {
    public final SharedPreferences sharedPreferences;

    public final MainActivity mainActivity;

    public final UsageStat usageStat;

    public final METARProvider metarProvider;
    public final SurfSpots surfSpots;
    public final TideDataProvider tideDataProvider;

    public CommandsExecutor commandsExecutor;

    public MetricsAndPaints metricsAndPaints;


    public static AppContext instance = null;


    public static AppContext getInstance(MainActivity ma, SharedPreferences sharedPreferences, BusyStateListener bsl) {
        if (instance == null) instance = new AppContext(ma, sharedPreferences, bsl);
        return instance;
    }


    public void init() {
        surfSpots.init();
        tideDataProvider.init();
        metarProvider.init();
    }


    private AppContext(MainActivity ma, SharedPreferences sharedPreferences, BusyStateListener bsl) {
        mainActivity = ma;

        this.sharedPreferences = sharedPreferences;

        usageStat = new UsageStat(sharedPreferences);
        metarProvider = new METARProvider(bsl);
        surfSpots = new SurfSpots(bsl);
        tideDataProvider = new TideDataProvider();
    }
}
