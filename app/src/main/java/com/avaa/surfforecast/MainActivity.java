package com.avaa.surfforecast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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

import com.avaa.surfforecast.ai.CommandsExecutor;
import com.avaa.surfforecast.data.BusyStateListener;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METARProvider;
import com.avaa.surfforecast.data.SurfConditionsProvider;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideDataProvider;
import com.avaa.surfforecast.drawers.MetricsAndPaints;
import com.avaa.surfforecast.views.BaliMap;
import com.avaa.surfforecast.views.MyList;
import com.avaa.surfforecast.views.OneDayConditionsSmallView;
import com.avaa.surfforecast.views.SurfConditionsForecastView;
import com.avaa.surfforecast.ai.VoiceInterfaceFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    public final static int NDAYS = 7;

    int colorBG;
    int colorAccent;

    int colorTextSpotNames = 0xffffff;
    int colorConditionsPreviews = 0xffffff;

    AppContext appContext;
//    TidesProvider tidesProvider;
//    SurfSpots surfSpots;
//    UsageStat usageStat;

    int dh = 0;

    Map<Integer, View> spotsTV = new TreeMap<>();

    private List<OneDayConditionsSmallView> smallViews = new ArrayList<>();

    float density;

    VoiceInterfaceFragment vif;
    View mainLayout;
    BaliMap baliMap;
    ImageView daysScroller;
    MyList listSpots;
    SurfConditionsForecastView forecast;
    RelativeLayout rlDays;
    FrameLayout btnMenu;
    ProgressBar progressBar;

    SharedPreferences sharedPreferences;

    int busyCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colorBG     = getResources().getColor(R.color.colorWaterBG);
        colorAccent = getResources().getColor(R.color.colorWater);

        sharedPreferences = getSharedPreferences("com.avaa.surfforecast", MODE_PRIVATE);

        BusyStateListener bsl = busy -> {
            busyCount += busy ? 1 : -1;
            if (progressBar != null) progressBar.setVisibility(busyCount > 0 ? View.VISIBLE : View.INVISIBLE);
            //Log.i(TAG, "busyCount: " + busyCount);
        };


        appContext = AppContext.getInstance(this, sharedPreferences, bsl);

        appContext.surfSpots.addChangeListener(changes -> {
            if (changes.contains(SurfSpots.Change.SELECTED_SPOT)) {
                listSpots.select(spotsTV.get(appContext.surfSpots.selectedSpotI));
            }
            if (changes.contains(SurfSpots.Change.CONDITIONS)) {
                updateSurfConditionsImages();
            }
        });


        density = getResources().getDisplayMetrics().density;

//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Window w = getWindow(); // in Activity's onCreate() for instance
//            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        }
        setContentView(R.layout.activity_main);

        vif = (VoiceInterfaceFragment)getSupportFragmentManager().findFragmentById(R.id.vif);
        mainLayout = findViewById(R.id.mainlayout);
        baliMap = (BaliMap) findViewById(R.id.balimap);
        daysScroller = (ImageView) findViewById(R.id.ivDaysScroller);
        listSpots = (MyList) findViewById(R.id.spots);
        forecast = (SurfConditionsForecastView) findViewById(R.id.rlBottom);
        progressBar = (MaterialProgressBar)findViewById(R.id.progressBar);
        rlDays = (RelativeLayout)findViewById(R.id.vllDays);
        btnMenu = (FrameLayout)findViewById(R.id.menu);

        vif.commandsExecutor = new CommandsExecutor(appContext);

        progressBar.getIndeterminateDrawable().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN );
        progressBar.setVisibility(busyCount > 0 ? View.VISIBLE : View.INVISIBLE);

        btnMenu.setOnClickListener(v2 -> {
            SurfSpot spot = appContext.surfSpots.selectedSpot();

            PopupMenu menu = new PopupMenu(this, btnMenu);

            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                case 0:
                    spot.conditionsProvider.update();
                    appContext.metarProvider.update(spot.metarName);
                    appContext.tideDataProvider.fetchIfNeed(Common.BENOA_PORT_ID);
                    return true;
                case 1:
                    appContext.surfSpots.swapFavorite(spot);
                    listSpots.getView(appContext.surfSpots.selectedSpotI).setText(spot.name + (spot.favorite ? "   " + "\u2605" : ""));
                    return true;
                case 2: {
                    Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + spot.la + "," + spot.lo + "?q=" + spot.la + "," + spot.lo + "(" + spot.name + ")"));
                    startActivity(geoIntent);
                    return true;
                }
                case 3: {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spot.urlMSW));
                    startActivity(browserIntent);
                    return true;
                }
                case 4: {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spot.getSFURL()));
                    startActivity(browserIntent);
                    return true;
                }
                case 5: {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spot.urlCam));
                    startActivity(browserIntent);
                    return true;
                }
                default:
                    return true;
                }
            });

            menu.getMenu().add(0, 0, 0, "Update");
            menu.getMenu().add(0, 1, 0, spot.favorite ? "Remove star" : "Star");
            menu.getMenu().add(0, 2, 0, "Show on map");

            menu.getMenu().add(1, 3, 0, "MSW.com");
            menu.getMenu().add(1, 4, 0, "SF.com");

            if (spot.urlCam != null) menu.getMenu().add(2, 5, 0, "Camera");

            menu.show();
        });

        mainLayout.setBackgroundColor(colorBG);
        baliMap.setAccentColor(colorAccent);

        //baliMap.surfSpotsList = surfSpots.getAll();

        daysScroller.setLayoutParams(new RelativeLayout.LayoutParams(0, (int) (density * 2)));
        daysScroller.setBackgroundColor(0xbb000000 | colorConditionsPreviews);

        forecast.onTouchActionDown = () -> listSpots.sleep();

        forecast.setOnScrollChangedListener((j, offset, scale) -> {
            //Log.i("MA", j+" "+offset+" "+scale);
            OneDayConditionsSmallView smallView = smallViews.get(j);
            int w = (int) (smallView.getWidth() * scale);
            float x = smallView.getX() + smallView.getWidth() / 2;

            if (offset > 0) {
                j++;
            }
            else {
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

        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs0));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs1));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs2));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs3));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs4));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs5));
        smallViews.add((OneDayConditionsSmallView) findViewById(R.id.odcs6));
        for (OneDayConditionsSmallView smallView : smallViews) smallView.setColorText(colorConditionsPreviews);

        listSpots.sl = new MyList.ScrollListener() {
            @Override
            public void scrolled(float shownI, float firstI, float lastI, float awakeState) {
                awakeState = 1f - awakeState;
                baliMap.show(shownI, firstI, lastI, awakeState);
                rlDays.setVisibility(awakeState == 0 ? View.INVISIBLE : View.VISIBLE);
                rlDays.setAlpha(awakeState);
                btnMenu.setVisibility(awakeState == 0 ? View.INVISIBLE : View.VISIBLE);
                btnMenu.setAlpha(awakeState);
            }
            @Override
            public void scrolled(float awakeState) {
                awakeState = 1f - awakeState;
                baliMap.setAwakenedState(awakeState);
                rlDays.setVisibility(awakeState == 0 ? View.INVISIBLE : View.VISIBLE);
                rlDays.setAlpha(awakeState);
                btnMenu.setVisibility(awakeState == 0 ? View.INVISIBLE : View.VISIBLE);
                btnMenu.setAlpha(awakeState);
            }
        };

        appContext.init();

        resetDates();

        findViewById(R.id.hllDays).setOnTouchListener((v, event) -> {
            float x = event.getX();
            int k = 0;
            for (OneDayConditionsSmallView v1 : smallViews) {
                if (x < v1.getRight() + ((LinearLayout.LayoutParams) v1.getLayoutParams()).rightMargin) {
                    forecast.showDay(k);
                    break;
                }
                k++;
            }
            return true;
        });

        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new OLCL()); //LayoutChangeListener(new OLCL());

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
    }


    @Override
    protected void onResume() {
        super.onResume();
        resetDates();
        appContext.surfSpots.updateCurrentConditions();
        baliMap.resume();
    }
    @Override
    protected void onStop() {
        appContext.usageStat.save();
        appContext.metarProvider.save();
        super.onStop();
        baliMap.stop();
    }


    Calendar c = null;
    private void resetDates() {
        Calendar c = new GregorianCalendar();
        if (this.c != null && this.c.get(Calendar.DATE) == c.get(Calendar.DATE)) return;

        this.c = new GregorianCalendar();
        for (OneDayConditionsSmallView v : smallViews) {
            v.setDate(c);
            c.add(Calendar.DATE, 1);
        }

        updateSurfConditionsImages();
        forecast.redrawTide();
    }


//    private boolean olclWorked = false;
    private int w, h;
    class OLCL implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
//            if (olclWorked) {
//                Log.i(TAG, "OLCL:  if (olclWorked) {");
//                return;
//            }
//            else olclWorked = true;
            if (w == mainLayout.getWidth() && h == mainLayout.getHeight()) return;
            w = mainLayout.getWidth();
            h = mainLayout.getHeight();
            Log.i(TAG, "onGlobalLayout: " + w + "x" + h);

            final RelativeLayout spotsRL = (RelativeLayout)findViewById(R.id.top);

            dh = Math.max(mainLayout.getWidth(), mainLayout.getHeight()) - Math.min(mainLayout.getWidth(), mainLayout.getHeight())*14/15;
            dh = (int)(dh / 10.1);

            int minDH = (int)(Math.min(mainLayout.getWidth(), mainLayout.getHeight()) / 7 / 2);
            int maxDH = (int)(Math.min(mainLayout.getWidth(), mainLayout.getHeight()) / 6.5 / 2);
            dh = Math.min(maxDH, Math.max(minDH, dh));

            appContext.metricsAndPaints = new MetricsAndPaints(density, dh);

            forecast.setDH(dh);

            ViewGroup.LayoutParams mParams = spotsRL.getLayoutParams();

            int h = (int)(dh * 10.1);
            mParams.height = mainLayout.getHeight() - h;
            spotsRL.setLayoutParams(mParams);

            mParams = MainActivity.this.forecast.getLayoutParams();
            mParams.height = h;
            MainActivity.this.forecast.setLayoutParams(mParams);

            spotsRL.invalidate();
            MainActivity.this.forecast.invalidate();
            mainLayout.invalidate();

            int daysBottom = (int)(dh*0.75);

            RelativeLayout.LayoutParams vllDaysLayoutParams = (RelativeLayout.LayoutParams) rlDays.getLayoutParams();
            vllDaysLayoutParams.height = (int)(dh*3 + daysBottom);

            View rlDaysScroller = findViewById(R.id.rlDaysScroller);
            RelativeLayout.LayoutParams rlDaysScrollerLayoutParams = (RelativeLayout.LayoutParams)rlDaysScroller.getLayoutParams();
            rlDaysScrollerLayoutParams.bottomMargin = (int)(daysBottom - density);
            rlDaysScroller.setLayoutParams(rlDaysScrollerLayoutParams);

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(dh*2, (int)(dh*3));
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            //lp.addRule(RelativeLayout.LEFT_OF, R.id.id_to_be_left_of);
            //btnMenu.setPadding(dh, 0, dh, 0);
            btnMenu.setLayoutParams(lp);
            //btnMenu.setTextSize(TypedValue.COMPLEX_UNIT_PX, dh);

            int i = 2;
            for (OneDayConditionsSmallView scv : smallViews) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)scv.getLayoutParams();
                params.width = (int)(dh * 1.333);
                if (i == 2) {
                    params.setMargins(dh / 2, 0, 0, daysBottom+dh/2);
                    params.width = dh * 2;
                }
                else if (i == 1) {
                    params.setMargins(0, 0, 0, daysBottom+dh/2);
                    params.width = dh * 2;
                }
                else params.setMargins(0, 0, 0, daysBottom+dh/2);

                scv.setLayoutParams(params);
                scv.setTextSize(dh / 2);
                i--;
            }
            baliMap.setDh(dh);

            initListSpots();
            listSpots.updatePadding(mainLayout.getHeight() - h);

            forecast.post(() -> {
                forecast.showDay(0);
            });

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//            }
        }
    }


    private void initListSpots() {
        List<View> views = new ArrayList<>();
        View selected = null;
        int i = 0;
        SurfSpots surfSpots = appContext.surfSpots;
        int ssi = surfSpots.selectedSpotI;

        MetricsAndPaints metricsAndPaints = appContext.metricsAndPaints;

        for (com.avaa.surfforecast.data.SurfSpot SurfSpot : surfSpots.getAll()) {
            if (surfSpots.categories.containsKey(i)) {
                TextView textView = new TextView(this);
                FrameLayout.LayoutParams lparams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dh*2);
                lparams.setMargins(0, 0, 0, 0);
                textView.setLayoutParams(lparams);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.font);
                textView.setTextColor(0x99000000 | colorTextSpotNames);
                textView.setText(surfSpots.categories.get(i));
                textView.setAlpha(0);
                textView.setVisibility(View.INVISIBLE);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                views.add(textView);
            }

            TextView textView = new TextView(this);
            FrameLayout.LayoutParams lparams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dh*2);
            lparams.setMargins(0,0,0,0);
            textView.setLayoutParams(lparams);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, metricsAndPaints.fontHeader);
            textView.setTextColor(0xff000000 | colorTextSpotNames);
            textView.setText(SurfSpot.name + (SurfSpot.favorite ? "   " + "\u2605" : "")); //2605");
            textView.setAlpha(i == ssi ? 1 : 0);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setVisibility(i == ssi ? View.VISIBLE : View.INVISIBLE);

            if (i == ssi) selected = textView;

            spotsTV.put(i, textView);
            views.add(textView);

            i++;
        }

        listSpots.setDH(dh);
        listSpots.setViews(views);
        listSpots.onSelected = surfSpots::setSelectedSpotI;

        final View finalSelected = selected;
        listSpots.post(() -> listSpots.select(finalSelected));
        forecast.post(() -> {
            Log.i(TAG, "forecast.post 2");
            forecast.showDay(0);
        });
    }


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


    private void updateConditionsSmallViews() {
        SurfConditionsProvider conditionsProvider = appContext.surfSpots.selectedSpot().conditionsProvider;
        for (int i = 0; i < smallViews.size(); i++) {
            smallViews.get(i).setConditions(conditionsProvider.get(i));
        }
    }
    private void updateSurfConditionsImages() {
        updateConditionsSmallViews();
        if (dh == 0) return;
        forecast.redrawSurfConditions();
    }


    public void performSelectSpot(SurfSpot spot, Runnable after) {
        Log.i(TAG, "performSelectSpot(" + spot.name + ")");

        int spotI = appContext.surfSpots.indexOf(spot);

        if (appContext.surfSpots.selectedSpotI != spotI) {
            View view = spotsTV.get(spotI);
            listSpots.awake(() -> listSpots.scrollTo(view, () -> listSpots.select(view, after)));
        }
        else if (after != null) after.run();
    }

    //  &@&
    //   |
    //   \

    public void performSelectDay(int day, Runnable after) {
        Log.i(TAG, "performSelectDay(" + day + ")");
        forecast.showDaySmooth(day);
    }
}
