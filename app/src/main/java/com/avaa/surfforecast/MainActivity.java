package com.avaa.surfforecast;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avaa.surfforecast.MainModel.Change;
import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.ai.VoiceInterfaceFragment;
import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.utils.DT;
import com.avaa.surfforecast.views.HeaderList;
import com.avaa.surfforecast.views.map.SurfSpotsMap;
import com.avaa.surfforecast.views.OneDayConditionsSmallView;
import com.avaa.surfforecast.views.RatingView;
import com.avaa.surfforecast.views.SurfConditionsForecastView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.avaa.surfforecast.ai.CommandsExecutor.capitalize;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Created by Alan on 1 May 2016.
 */


public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    public final static int NDAYS = 7;

    private int colorConditionsPreviews = 0xffffff;

    private MainModel model;

    private Map<Integer, View> spotsTV = new TreeMap<>();

    private float density;

    private int dhd2 = 0;
    private int dh = 0;
    private int dhx2 = 0;

    public List<OneDayConditionsSmallView> smallViews = new ArrayList<>(); // TODO: Separate small views to custom forecastBriefView

    private VoiceInterfaceFragment vif;
    private View mainLayout;
    private SurfSpotsMap map;
    private ImageView daysScroller;
    private HeaderList listSpots;
    private SurfConditionsForecastView forecast;
    private RelativeLayout rlDays;
    private FrameLayout flBtnMenu;
    private FrameLayout flBtnAway;
    private FrameLayout flBtnCam;
    private ProgressBar progressBar;

    private LinearLayout llRating;
    private TextView tvRatingDay;
    private TextView tvRatingTime;
    private RatingView rv;
    private TextView tvPlace;

    private int busyCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "\\/ start constructor");

        super.onCreate(savedInstanceState);

        int colorBG = getResources().getColor(R.color.colorWaterBG);
        int colorAccent = getResources().getColor(R.color.colorWater);

        final SharedPreferences sharedPreferences = getSharedPreferences("com.avaa.surfforecast", MODE_PRIVATE);

        final BusyStateListener bsl = busy -> {
            busyCount += busy ? 1 : -1;
            if (progressBar != null)
                progressBar.setVisibility(busyCount > 0 ? View.VISIBLE : View.INVISIBLE);
            //Log.i(TAG, "busyCount: " + busyCount);
        };

        model = MainModel.getInstance(this, sharedPreferences, bsl);

        model.addChangeListener(Change.SELECTED_SPOT, changes -> {
            listSpots.select(spotsTV.get(model.getSelectedSpotI()));
            updateCamAwayButtons();
        });

        model.addChangeListener(Change.ALL_CONDITIONS, changes -> {
            updateSurfConditionsImages();
        });

        model.addChangeListener(Change.SELECTED_DAY, changes -> {
            int day = model.getSelectedDay();
            tvRatingDay.setText(capitalize(CommandsExecutor.intDayToNL(day)));
        });

        model.addChangeListener(Change.SELECTED_TIME, changes -> {
            tvRatingTime.setText(capitalize(model.isSelectedNow() ? "Right now" : CommandsExecutor.intTimeToNL(model.getSelectedTime(), false)));
        });

        model.addChangeListener(Change.SELECTED_RATING, changes -> {
            RatedConditions ratedConditions = model.getSelectedRatedConditions();
            if (ratedConditions == null) {
                rv.setRating(0, 0);
            } else {
                rv.setRating(ratedConditions.rating, ratedConditions.waveRating);
            }
        });

//            SurfSpot surfSpot = model.getSelectedSpot();
//            SurfConditions now = surfSpot.conditionsProvider.getNow();
//            if (now != null) {
//                now.addMETAR(model.selectedMETAR);
//                TideData tideData = model.tideDataProvider.getTideData(surfSpot.tidePortID);
//                if (tideData != null) { //!!!!!!!!!!!!!!!!!!
//                    float rate = now.rate(surfSpot, tideData, 0, Common.getNowTimeMinutes(Common.TIME_ZONE));
        //((TextView)findViewById(R.id.tvRatingDay)).setText("Rating: " + rate + "\nWave: " + now.waveRating + "\nWind: " + now.windRating + "\nTide: " + now.tideRating);
//                    rv.setRating(rate, now.waveRating * now.tideRating);
//                }
//            }

        density = getResources().getDisplayMetrics().density;

//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Window w = getWindow(); // in Activity's onCreate() for instance
//            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        }

        setContentView(R.layout.activity_main);

        vif = (VoiceInterfaceFragment) getSupportFragmentManager().findFragmentById(R.id.vif);
        mainLayout = findViewById(R.id.mainlayout);
        map = (SurfSpotsMap) findViewById(R.id.map);
        daysScroller = (ImageView) findViewById(R.id.ivDaysScroller);
        listSpots = (HeaderList) findViewById(R.id.svSpots);
        forecast = (SurfConditionsForecastView) findViewById(R.id.scfv);
        progressBar = (MaterialProgressBar) findViewById(R.id.progressBar);
        rlDays = (RelativeLayout) findViewById(R.id.rlDays);
        flBtnMenu = (FrameLayout) findViewById(R.id.flBtnMenu);
        flBtnAway = (FrameLayout) findViewById(R.id.flBtnAway);
        flBtnCam = (FrameLayout) findViewById(R.id.flBtnCam);

        llRating = (LinearLayout) findViewById(R.id.llRating);
        tvRatingDay = (TextView) findViewById(R.id.tvRatingDay);
        tvRatingTime = (TextView) findViewById(R.id.tvRatingTime);
        rv = (RatingView) findViewById(R.id.ratingView);
        tvPlace = (TextView) findViewById(R.id.tvPlace);

        vif.commandsExecutor = new CommandsExecutor(model);

        progressBar.getIndeterminateDrawable().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN);
        progressBar.setVisibility(busyCount > 0 ? View.VISIBLE : View.INVISIBLE);

        initMenuButton();
        initAwayButton();
        initCamButton();

        mainLayout.setBackgroundColor(colorBG);
        map.setAccentColor(colorAccent);

        //baliMap.surfSpotsList = surfSpots.getAll();

        daysScroller.setLayoutParams(new RelativeLayout.LayoutParams(0, (int) (2 * density)));
        daysScroller.setBackgroundColor(0xdd000000 | colorConditionsPreviews);

        forecast.onTouchActionDown = () -> listSpots.sleep();

        initSmallForecastViews();

        listSpots.scrollListener = new HeaderList.ScrollListener() {
            @Override
            public void scrolled(float shownI, float firstI, float lastI, float awakeState) {
                awakeState = 1f - awakeState;
                map.highlightSpots(shownI, firstI, lastI, awakeState);
                changed(awakeState);
            }

            @Override
            public void scrolled(float awakeState) {
                awakeState = 1f - awakeState;
                map.setAwakenedState(awakeState);
                changed(awakeState);
            }

            private void changed(float awakeState) {
                awakeState = awakeState * 2 - 1;
                if (awakeState < 0) awakeState = 0;

                int visibility = awakeState == 0 ? View.INVISIBLE : View.VISIBLE;

                rlDays.setVisibility(visibility);
                rlDays.setAlpha(awakeState);

                flBtnMenu.setVisibility(visibility);
                flBtnMenu.setAlpha(awakeState);

                llRating.setVisibility(visibility);
                llRating.setAlpha(awakeState);

                updateCamAwayButtons();
            }
        };

        rlDays.setOnTouchListener((v, event) -> {
            float x = event.getX();
            int day = 0;
            for (OneDayConditionsSmallView v1 : smallViews) {
                if (day >= 0) {
                    if (x < v1.getRight() + ((LinearLayout.LayoutParams) v1.getLayoutParams()).rightMargin) {
                        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                            model.setSelectedDay(day, true);
                        }
                        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                            v1.setAlpha(0.5f);
                        } else {
                            v1.setAlpha(1f);
                        }
                        day = Integer.MIN_VALUE;
                        continue;
                    }
                    day++;
                }
                v1.setAlpha(1);
            }
            return true;
        });

        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new OGLL());

        forecast.underview = rlDays;
        forecast.onScrollYChanged = () -> {
            float v = forecast.getTideVisibility();

            ViewGroup.LayoutParams p = listSpots.getLayoutParams();
            p.height = Math.max(forecast.getContentTop(), forecast.getHeight() - (int) (forecast.scrollY - 1.5f * dh));
            listSpots.setLayoutParams(p);
            listSpots.setAlpha(max(0, v * 1.5f - 0.5f));
            listSpots.setVisibility(v == 0 ? View.INVISIBLE : View.VISIBLE);
            listSpots.setX((int) (dh * (1 - v) / 2));

            tvRatingTime.setAlpha(v);

            rv.setAlpha(v);

            tvPlace.setAlpha(1f - min(1, 1.5f * v));
            tvPlace.setPadding((int) (dh / 2 + dh * (1 - v) / 2), dh / 2, 0, 0);

            float alpha = max(0f, min(1f, 1f - (forecast.scrollY - dh * 12.5f) / (dh)));
            rlDays.setAlpha(alpha);
            rlDays.setY(forecast.getContentTop() - rlDays.getHeight());

            updateCamAwayButtons();

            if (forecast.scrollY > forecast.getWindSwellScrollY()) {
                map.setInsets((int) (5 * dh), forecast.getHeight() - forecast.getContentTop(forecast.getWindSwellScrollY()));
                map.setOverviewState(0f);
//                1baliMap.hideCircles();
            } else {
                float tideVisibility = forecast.getTideVisibility();
                map.setInsets((int) (4 * dh + 2 * dh * tideVisibility - 1 * dh * forecast.getWindSwellVisibility()), forecast.getHeight() - forecast.getContentTop());
                map.setOverviewState(1f - tideVisibility);
//                baliMap.showCircles();
            }

            if (getState() == 2) {
                map.hideHints();
            }
        };

        Log.i(TAG, "\\/ before init");
        model.init();
        Log.i(TAG, "/\\ after init");

        resetDates();

        OrientationEventListener orientationListener = new
                OrientationEventListener(getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                    public void onOrientationChanged(int angle) {
                        //angle comes in 360 degrees, convert that to a 0-3 rotation
                        //Get the angle in 90 degree increments, 0,90,180,270
                        //with 45 degrees tolerance in each direction (straight up is 0)
                        //this is the same as the data coming from getWindowManager().getDefaultDisplay().getRotation()

                        angle = angle + 45;
                        if (angle > 360) angle = angle - 360;
                        int orientation = angle / 90;

                        //I use a history in order to smooth out noise
                        //and don't start sending events about the change until this history is filled
                        if (_orientationIndex > _highestIndex) {
                            _highestIndex = _orientationIndex;
                        }
                        _orientationHistory[_orientationIndex] = orientation;
                        _orientationIndex++;

                        if (_orientationIndex == _orientationHistory.length) {
                            _orientationIndex = 0;
                        }

                        int lastOrientation = _currentOrientation;
                        //compute the orientation using above method
                        _currentOrientation = getOrientation();

                        if (_highestIndex == _orientationHistory.length - 1 && lastOrientation != _currentOrientation) {
                            //enough data to say things changed
                            orientationChanged(lastOrientation, _currentOrientation);
                        }
                    }
                };
        orientationListener.enable();

        Log.i(TAG, "/\\ end constructor");
    }


    @Override
    protected void onResume() {
        super.onResume();
        resetDates();
        model.updateCurrentConditions();
        map.resume();
    }


    @Override
    protected void onStop() {
        model.userStat.save();
        model.metarProvider.save();
        super.onStop();
        map.stop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        vif.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onBackPressed() {
        if (vif.onBackPressed()) return;
        if (backLocal()) return;
        super.onBackPressed();
    }


    public boolean backLocal() {
        int state = getState();

        if (state == 2) {
            performShowTide();
            return true;
        } else if (state == 1) {
            performOverview();
            return true;
        }
        return false;
    }


    private Calendar c = null;

    private void resetDates() {
        Calendar c = DT.getCalendarTodayStart(DT.TIME_ZONE);
        if (this.c != null && this.c.get(Calendar.DATE) == c.get(Calendar.DATE)) return;

        this.c = DT.getCalendarTodayStart(DT.TIME_ZONE);
        int i = 0;
        for (OneDayConditionsSmallView v : smallViews) {
            v.setDate(c, i++);
            v.setBold(v.plusDays == model.getSelectedDay());
            c.add(Calendar.DATE, 1);
        }

        updateSurfConditionsImages();
        forecast.redrawTide();
    }


    private void setMetrics(MetricsAndPaints metrics) {
        forecast.setMetrics(metrics);

        final int dhx25 = (int) (2.5f * dh);

        float fontRating = metrics.font;
        int starH = (int) (1.1f * fontRating);
        LinearLayout.LayoutParams rvlp = new LinearLayout.LayoutParams(10 * starH, starH);
        rvlp.gravity = Gravity.CENTER_VERTICAL;
        rvlp.topMargin = dhd2;
        rvlp.bottomMargin = dh / 8;
        rv.setLayoutParams(rvlp);

        RelativeLayout.LayoutParams llRatingLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dhx25);
        llRatingLP.topMargin = dhx25;
        llRatingLP.leftMargin = dh;
        llRating.setLayoutParams(llRatingLP);

        View ivBtnMenu = findViewById(R.id.ivBtnMenu);
        ViewGroup.LayoutParams layoutParams = ivBtnMenu.getLayoutParams();
        layoutParams.width = (int) metrics.fontHeader;
        layoutParams.height = (int) metrics.fontHeader;
        ivBtnMenu.setLayoutParams(layoutParams);

        tvRatingDay.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontRating);
        tvRatingTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontRating);

        tvPlace.setLayoutParams(new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dhx25) {{
            setMargins(0, 0, 0, 0);
        }});
        tvPlace.setPadding(dh, dhd2, 0, 0);
        tvPlace.setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.fontHeader);
//        tvPlace.setTypeface(tvPlace.getTypeface(), Typeface.BOLD);
        tvPlace.setGravity(Gravity.CENTER_VERTICAL);
        tvPlace.setTextColor(MetricsAndPaints.colorWhite);

        forecast.invalidate();
        mainLayout.invalidate();

        int daysBottom = (int) (0.75f * dh);

        RelativeLayout.LayoutParams vllDaysLayoutParams = (RelativeLayout.LayoutParams) rlDays.getLayoutParams();
        vllDaysLayoutParams.height = 3 * dh + daysBottom;

        View rlDaysScroller = findViewById(R.id.rlDaysScroller);
        RelativeLayout.LayoutParams rlDaysScrollerLayoutParams = (RelativeLayout.LayoutParams) rlDaysScroller.getLayoutParams();
        rlDaysScrollerLayoutParams.bottomMargin = (int) (daysBottom - density);
        rlDaysScrollerLayoutParams.height = (int) (3.5f * metrics.densityDHDependent);
        rlDaysScroller.setLayoutParams(rlDaysScrollerLayoutParams);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(dhx2, 3 * dh);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        flBtnMenu.setLayoutParams(lp);

        lp = new RelativeLayout.LayoutParams(dhx2, dhx2);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        flBtnAway.setLayoutParams(lp);
        flBtnAway.setY(dhx25);

        lp = new RelativeLayout.LayoutParams(dhx2, dhx2);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        flBtnCam.setLayoutParams(lp);
        flBtnCam.setY(4.5f * dh);

        int i = 2;
        for (OneDayConditionsSmallView scv : smallViews) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scv.getLayoutParams();
            params.width = (int) (dh * MetricsAndPaints.TEXT_K);
            if (i == 2) {
                params.setMargins(dhd2, 0, 0, daysBottom + dhd2);
                params.width = dhx2;
            } else if (i == 1) {
                params.setMargins(0, 0, 0, daysBottom + dhd2);
                params.width = dhx2;
            } else params.setMargins(0, 0, 0, daysBottom + dhd2);

            scv.setLayoutParams(params);
            scv.setMetrics(model.metricsAndPaints);
            i--;
        }
        map.setMetrics(metrics);

        initListSpots();

        forecast.postDelayed(() -> forecast.showDay(model.getSelectedDay()), 100);
    }

    //private boolean olclWorked = false;

    private final class OGLL implements ViewTreeObserver.OnGlobalLayoutListener {
        private int w, h;

        @Override
        public void onGlobalLayout() {
//            if (olclWorked) {
//                Log.i(TAG, "OGLL:  if (olclWorked) {");
//                return;
//            }
//            else olclWorked = true;

            if (w == mainLayout.getWidth() && h == mainLayout.getHeight()) return;
            w = mainLayout.getWidth();
            h = mainLayout.getHeight();
//            Log.i(TAG, "onGlobalLayout: " + w + "x" + h);

            dh = max(w, h) - min(w, h) * 14 / 15;
            dh = (int) (dh / 10.7);

            int minDH = (int) (min(w, h) / 15f);
            int maxDH = (int) (min(w, h) / 13f);
            dh = min(maxDH, max(minDH, dh));

            dhd2 = dh / 2;
            dhx2 = dh * 2;

            model.metricsAndPaints = new MetricsAndPaints(density, dh);

            setMetrics(model.metricsAndPaints);
        }
    }


    private void initSmallForecastViews() {
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs0));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs1));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs2));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs3));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs4));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs5));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs6));

        for (OneDayConditionsSmallView smallView : smallViews) {
            smallView.setColorText(colorConditionsPreviews);
        }

        model.addChangeListener(Change.SELECTED_DAY, changes -> {
            for (OneDayConditionsSmallView smallView : smallViews) {
                smallView.setBold(smallView.plusDays == model.getSelectedDay());
            }
        });
        model.addChangeListener(Change.SELECTED_DAY_FLOAT, changes -> {
            float offset = model.getSelectedDayFloat();

            int j = (int) offset;
            if (offset - j < 0.5) {
                offset = offset - j;
            } else {
                j++;
                offset = j - offset;
                offset = -offset;
            }

            OneDayConditionsSmallView smallView = smallViews.get(j);
            int w = (int) (smallView.getWidth() * forecast.getScale(j));
            float x = smallView.getX() + smallView.getWidth() / 2;

            if (offset > 0) {
                j++;
            } else {
                j--;
                offset = -offset;
            }

            if (j >= 0 && j <= 6) {
                smallView = smallViews.get(j);
                x = x - (x - (smallView.getX() + smallView.getWidth() / 2)) * offset;
                w = w - (int) ((w - (int) (smallView.getWidth() * forecast.getScale(j))) * offset);
            }

            ViewGroup.LayoutParams layoutParams = daysScroller.getLayoutParams();
            layoutParams.width = w;
            daysScroller.setLayoutParams(layoutParams);
            daysScroller.setX((int) (x - w / 2));
        });
    }


    private void initCamButton() {
        flBtnCam.setOnClickListener(v -> {
            if (forecast.getTideVisibility() > 0.5f) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getSelectedSpot().urlCam));
                startActivity(browserIntent);
            }
        });
    }


    private void updateCamAwayButtons() {
        float alpha;

        alpha = max(0f, min(1f, 1f - (forecast.scrollY - dh * 5f) / (dh * 3f))) * (1f - listSpots.getAwakeState());
        flBtnAway.setAlpha(alpha);
        flBtnAway.setVisibility(alpha == 0 ? View.INVISIBLE : View.VISIBLE);

        if (model.getSelectedSpot().urlCam == null) alpha = 0;
        else if (forecast.getTideVisibility() < 1f) alpha = forecast.getTideVisibility();
        flBtnCam.setAlpha(alpha);
        flBtnCam.setVisibility(alpha == 0f ? View.INVISIBLE : View.VISIBLE);
    }


    private final PopupMenu.OnMenuItemClickListener onMenuItemClickListener = item -> {
        SurfSpot spot = model.getSelectedSpot();
        switch (item.getItemId()) {
            case 0:
                if (forecast.scrollY > 0) {
                    model.updateSelectedSpotAll();
                } else {
                    model.updateAll();
                }
                return true;
            case 1:
                model.surfSpots.swapFavorite(spot);
                listSpots.getView(model.getSelectedSpotI()).setText(spot.name + (spot.favorite ? "   " + "\u2605" : ""));
                return true;
            case 2:
                Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + spot.la + "," + spot.lo + "?q=" + spot.la + "," + spot.lo + "(" + spot.name + ")"));
                startActivity(geoIntent);
                return true;
            case 3:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spot.urlMSW)));
                return true;
            case 4:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spot.getSFURL())));
                return true;
            case 5:
                flBtnCam.performClick();
                return true;
            default:
                return true;
        }
    };


    private void initMenuButton() {
        flBtnMenu.setOnClickListener(v -> {
            SurfSpot spot = model.getSelectedSpot();

            PopupMenu menu = new PopupMenu(this, flBtnMenu);

            menu.setOnMenuItemClickListener(onMenuItemClickListener);

            if (forecast.scrollY > 0) {
                menu.getMenu().add(0, 0, 0, "Update");
            } else {
                menu.getMenu().add(0, 0, 0, "Update all");
            }

            if (forecast.scrollY > 0) {
                menu.getMenu().add(0, 1, 0, spot.favorite ? "Remove star" : "Mark with a star");
            }

            if (forecast.scrollY > forecast.getTideScrollY()) {
                menu.getMenu().add(1, 2, 0, "Open in Maps");
                menu.getMenu().add(1, 3, 0, "Open Magicseaweed");
                menu.getMenu().add(1, 4, 0, "Open Surf-forecast");

                if (spot.urlCam != null) menu.getMenu().add(2, 5, 0, "Camera");
            }

            menu.show();
        });
    }


    private void initAwayButton() {
        flBtnAway.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, flBtnAway);

            menu.setOnMenuItemClickListener(onMenuItemClickListener);

            menu.getMenu().add(0, 2, 0, "Open in Maps");
            menu.getMenu().add(1, 3, 0, "Open Magicseaweed");
            menu.getMenu().add(1, 4, 0, "Open Surf-forecast");

            menu.show();
        });
    }


    private void initListSpots() {
        List<View> views = new ArrayList<>();
        View selected = null;
        int i = 0;
        SurfSpots surfSpots = model.surfSpots;
        int ssi = model.getSelectedSpotI();

        MetricsAndPaints metricsAndPaints = model.metricsAndPaints;

        ColorStateList colorStateList = getResources().getColorStateList(R.color.text_color_white);

        for (com.avaa.surfforecast.data.SurfSpot SurfSpot : surfSpots.getAll()) {
            if (surfSpots.areas.containsKey(i)) {
                TextView textView = new TextView(this);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dhx2);
                layoutParams.setMargins(0, 0, 0, 0);
                textView.setLayoutParams(layoutParams);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.font);
                textView.setTextColor(0x99000000 | MetricsAndPaints.colorWhite);
                textView.setText(surfSpots.areas.get(i).name);
                textView.setAlpha(0);
                textView.setVisibility(View.INVISIBLE);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                views.add(textView);
            }

            TextView textView = new TextView(this);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dhx2);
            layoutParams.setMargins(0, 0, 0, 0);
            textView.setLayoutParams(layoutParams);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontHeader);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setTextColor(colorStateList);
            textView.setText(SurfSpot.name + (SurfSpot.favorite ? "   " + "\u2605" : "")); //2605");
            textView.setAlpha(i == ssi ? 1 : 0);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setVisibility(i == ssi ? View.VISIBLE : View.INVISIBLE);
            textView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.setPressed(true); // to apply color form ColorStateList immediately
                        break;
                    }
                }
                return false; // we return false so that the click listener will process the event
            });

            if (i == ssi) selected = textView;

            spotsTV.put(i, textView);
            views.add(textView);

            i++;
        }

        listSpots.setDH(dh);
        listSpots.setViews(views);
        listSpots.onSelected = model::setSelectedSpotI;

        final View finalSelected = selected;
        listSpots.post(() -> listSpots.select(finalSelected));
    }


    // --


    //keep a history
    private int[] _orientationHistory = new int[5];
    private int _orientationIndex;
    private int _highestIndex = -1;
    private int _currentOrientation;

    //essentially compute the mode of the data. This could be improved
    protected int getOrientation() {
        if (_highestIndex < 0) return 0;
        Arrays.sort(_orientationHistory);
        return _orientationHistory[_highestIndex / 2];
    }

    protected void orientationChanged(int lastOrientation, int currentOrientation) {
        forecast.setOrientation(currentOrientation);
    }


    // --


    private void updateConditionsSmallViews() {
        SurfConditionsProvider conditionsProvider = model.getSelectedSpot().conditionsProvider;
        for (int i = 0; i < smallViews.size(); i++) {
            smallViews.get(i).setConditions(conditionsProvider.get(i));
        }
    }

    private void updateSurfConditionsImages() {
        updateConditionsSmallViews();
        if (dh == 0) return;
        forecast.redrawSurfConditions();
    }


    // --


    public void performSelectSpot(SurfSpot spot, Runnable after) {
//        Log.i(TAG, "performSelectSpot(" + spot.name + ")");
        int spotI = model.surfSpots.indexOf(spot);

        if (model.getSelectedSpotI() != spotI) {
            if (forecast.getTideVisibility() > 0) {
                performOverview();
                map.postDelayed(() -> {
                    model.setSelectedSpotI(spotI);
                    performShowTide();
                    if (after != null) {
                        map.postDelayed(() -> {
                            after.run();
                        }, 333);
                    }
                }, 333);
            } else {
                model.setSelectedSpotI(spotI);
                performShowTide();
                if (after != null) {
                    map.postDelayed(() -> {
                        after.run();
                    }, 333);
                }
            }
//            View view = spotsTV.get(spotI);
//            listSpots.awake(() -> listSpots.scrollTo(view, () -> {
//                performShowTide();
//                listSpots.select(view, after);
//            }));
        } else {
            performShowTide();
            if (after != null) {
                map.postDelayed(() -> {
                    after.run();
                }, 333);
            }
        }
    }


    public void performSelectDay(int day) {
        forecast.showDaySmooth(day);
    }


    public void performOverview() {
        forecast.scrollY(0, 666);
    }

    public void performShowTide() {
        forecast.scrollY(forecast.getTideScrollY(), 666);
    }

    public void performShowWindSwell() {
        forecast.scrollY(forecast.getWindSwellScrollY(), 666);
    }


    // 0 - overview, 1 - tide, 2 - forecast
    public int getState() {
        float y = forecast.scrollY;
        if (!forecast.scrollerY.isFinished()) y = forecast.scrollerY.getFinalY();

        if (y < forecast.getTideScrollY() / 2) return 0;
        else if (y < (forecast.getWindSwellScrollY() - forecast.getTideScrollY()) / 2 + forecast.getTideScrollY())
            return 1;
        else return 2;
    }
}
