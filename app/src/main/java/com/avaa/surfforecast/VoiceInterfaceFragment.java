package com.avaa.surfforecast;

import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class VoiceInterfaceFragment extends Fragment {
    private static final String TAG = "VoiceIntFr";

    private SpeechRecognizer speech;

    View btnMic;
    View btnMicImage;

    MainActivity ma = null;


    public VoiceInterfaceFragment() { }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        speech = SpeechRecognizer.createSpeechRecognizer(getContext());
        speech.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { }
            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.i(TAG, "rms" + rmsdB);
                btnMicImage.setAlpha(0.3f + 0.7f*Math.max(0f, Math.min(1f, rmsdB/20f)));
            }

            @Override
            public void onBufferReceived(byte[] buffer) { }
            @Override
            public void onEndOfSpeech() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button_transparent));
                    btnMicImage.setAlpha(0.2f);
                }
            }
            @Override
            public void onError(int error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button_transparent));
                    btnMicImage.setAlpha(0.2f);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                Map<Float, String> res = new TreeMap<Float, String>();

                String text = "";
                for (int i = 0; i < matches.size(); i++) {
                    text += (int)(scores[i]*100) + " " + matches.get(i) + "\n";
                }
                Log.i(TAG, "onResults()\n" + text);

                if (matches != null) recognize(matches);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) { }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_voice_interface, container, false);

        btnMic = inflate.findViewById(R.id.btnMic);
        btnMic.setOnClickListener(this::mic);

        btnMicImage = inflate.findViewById(R.id.btnMicImage);

        return inflate;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speech.destroy();
    }

    public void mic(View view) {
        startListening();
    }


    Map<String, Integer> sToDay = new HashMap<>();
    private void initSToDay() {
        String today = DateUtils.getRelativeTimeSpanString(
                0, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String tomorrow = DateUtils.getRelativeTimeSpanString(
                1000*60*60*24, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String afterTomorrow = DateUtils.getRelativeTimeSpanString(
                2*1000*60*60*24, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        sToDay.put(today, 0);
        sToDay.put(tomorrow, 1);
        sToDay.put(afterTomorrow, 2);

        Calendar c = Common.getCalendarToday(Common.TIME_ZONE);
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            if (i > 1 && dayOfWeek == Calendar.SATURDAY) sToDay.put("weekend", i);
            String s = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).toLowerCase();
            sToDay.put(s, i);
            c.add(Calendar.DATE, 1);
        }
        sToDay.put("friendly", sToDay.get("friday"));
//        sToDay.put("friendly", sToDay.get("friday"));
//        sToDay.put("friendly", sToDay.get("friday"));
//        sToDay.put("friendly", sToDay.get("friday"));
    }


    Map<String, SurfSpot> sToSpot = new HashMap<>();
    SurfSpots surfSpots = null;
    private void initSToSpot() {
        for (SurfSpot spot : surfSpots.getAll()) {
            sToSpot.put(spot.name.toLowerCase(), spot);
            if (spot.altNames != null) {
                for (String altName : spot.altNames) {
                    sToSpot.put(altName, spot);
                }
            }
        }
    }


    public void startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button));
            btnMicImage.setAlpha(0.8f);
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
//        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{new Locale("ru", "RU").toString()});
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, false);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);

        speech.startListening(intent);
    }


    public void recognize(List<String> strings) {
        Log.i(TAG, "recognize()");

        Integer  day = null;
        SurfSpot spot = null;
        for (String s : strings) {
            s = s.toLowerCase();

            for (Map.Entry<String, Integer> e : sToDay.entrySet()) {
                if (s.contains(e.getKey())) {
                    day = e.getValue();
                    s = s.replace(e.getKey(), "").trim();
                    break;
                }
            }
            for (Map.Entry<String, SurfSpot> e : sToSpot.entrySet()) {
                if (s.contains(e.getKey())) {
                    spot = e.getValue();
                    s = s.replace(e.getKey(), "").trim();
                    break;
                }
            }

            if (day != null && spot != null) break;
        }

        Runnable rDay = null;
        if (day != null) {
            Log.i(TAG, "recognize() | day = " + day);
            Integer finalDay = day;
            rDay = () -> ma.performSelectDay(finalDay, null);
        }

        if (spot != null) {
            ma.performSelectSpot(spot, rDay);
        }
        else {
            if (rDay != null) rDay.run();
        }
    }

    public void init() {
        initSToDay();
        initSToSpot();
    }
}
