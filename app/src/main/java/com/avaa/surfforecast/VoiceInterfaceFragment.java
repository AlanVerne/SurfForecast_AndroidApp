package com.avaa.surfforecast;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class VoiceInterfaceFragment extends Fragment {
    private static final String TAG = "VoiceIntFr";

    private SpeechRecognizer speech;

    View btnMic;
    View btnMicImage;

    View flHint;

    TextView tvHintTitle;

    TextView[] tvHintOpts = new TextView[5];
    TextView tvHintOptPrerecognized;

    MainActivity ma = null;


    public VoiceInterfaceFragment() { }


    private void uiListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button));
            btnMicImage.setAlpha(0.8f);
        }
    }
    private void uiNotListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button_transparent));
            btnMicImage.setAlpha(0.2f);
        }
    }

    private void uiShowHint() {
        flHint.setVisibility(View.VISIBLE);
        tvHintOptPrerecognized.setText("...");
        tvHintOptPrerecognized.setTextColor(0x88000000);
        for (TextView tvHintOpt : tvHintOpts) {
            tvHintOpt.setTextColor(0x88000000);
        }
    }
    private void uiApprove(TextView tv) {
        tv.setTextColor(0xff000000);
    }
    private void uiHideHint() {
        flHint.setVisibility(View.INVISIBLE);
    }


    public void startListening() {
        uiListening();
        uiShowHint();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
//        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{new Locale("ru", "RU").toString()});
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH); //FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, false);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);

        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);

        speech.startListening(intent);
    }


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
                btnMic.post(() -> {
                    Log.i(TAG, "rms" + rmsdB);
                    btnMicImage.setAlpha(0.3f + 0.7f*Math.max(0f, Math.min(1f, rmsdB/20f)));
                });
            }

            @Override
            public void onBufferReceived(byte[] buffer) { }
            @Override
            public void onEndOfSpeech() {
                uiNotListening();
            }
            @Override
            public void onError(int error) {
                uiNotListening();
                uiHideHint();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                Map<Float, String> res = new TreeMap<Float, String>();

                String text = "";
                for (int i = 0; i < list.size(); i++) {
                    text += (int)(scores[i]*100) + " " + list.get(i) + "\n";
                }
                Log.i(TAG, "onResults()\n" + text);

                String s = processRecognitionResults(list);

                if (s == null) uiHideHint();
                else {
                    tvHintOptPrerecognized.setText(s);
                    uiApprove(tvHintOptPrerecognized);
                    uiHideHint();

                    performCommand(s);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String result = processRecognitionResults(list);
                if (result != null) tvHintOptPrerecognized.setText(result);
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

        flHint = inflate.findViewById(R.id.flHint);

        tvHintTitle = (TextView) inflate.findViewById(R.id.tvHintTitle);

        tvHintOpts[0] = (TextView) inflate.findViewById(R.id.tvHintOpt1);
        tvHintOpts[1] = (TextView) inflate.findViewById(R.id.tvHintOpt2);
        tvHintOpts[2] = (TextView) inflate.findViewById(R.id.tvHintOpt3);
        tvHintOpts[3] = (TextView) inflate.findViewById(R.id.tvHintOpt4);
        tvHintOpts[4] = (TextView) inflate.findViewById(R.id.tvHintOpt5);

        for (TextView tvHintOpt : tvHintOpts) {
            tvHintOpt.setOnClickListener(v -> {
                speech.stopListening();
                speech.cancel();
                uiNotListening();

                uiApprove(tvHintOpt);
                uiHideHint();

                performCommand(tvHintOpt.getText().toString());
            });
        }

        tvHintOptPrerecognized = (TextView) inflate.findViewById(R.id.tvPrerecognized);

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

    private final Map<String, String> soundLikeDay = new HashMap<>();
    private final Map<String, Integer> sToDay = new HashMap<>();
    private void initSToDay() {
        sToDay.put("now", 0);

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

        soundLikeDay.put("friendly", "Friday");
    }

    private static final String SOUND_LIKE_DAY = "" +
            "Friday - friendly";

    private static final String SOUND_LIKE_SPOT = "" +
            "Medewi - mandiri,madeira,man baby,man daily,mondavi,big daddy,men daily,andretti,when daddy" + "\n" +
            "Balian - banana,benihana,money on,minion,banyan,valium,bali on,violin" + "\n" +
            "Canggu - jingle,jungle,canggu,django,jango,can you move,can you do,can you go,demi moore,jamie,china gold,tangled,triangle,chinese food" + "\n" +
            "Old mans - old mans,old man's,oldman,old men,old man,goldman,almonds,ahlmans,harmons,holman's" + "\n" +
            "Batu bolong - let alone,methylone,atenolol" + "\n" +
            "Batu - batoon,bo2,butter,bathroom" + "\n" +
            "Berawa - bravo,brava,java,driver,arava,brower,browa,brother,there are" + "\n" +
            "Seminyak - singing app,selenium" + "\n" +
            "Kuta - puta,cota,guta" + "\n" +
            "Balangan - bellingham,malanga,mehlingen,milan gun" + "\n" +
            "Dreamland - greenland,dream land,keeneland,finland,timland" + "\n" +
            "Bingin - didn't get,bending,binging,ben king,ninjin,engine,bingen,benjen,bingeon,binging" + "\n" +
            "Impossibles - impossible,imposibles,kim possible,kim possible's" + "\n" +
            "Padang-Padang - padang padang,pandanan,adam adam,adam,badung,banana banana,badonkadonk,padam padam,button button" + "\n" +
            "Uluwatu - uidaho,all idaho,old idaho,idaho,lovato,all lotto,or lotto,whole lotta,holy water" + "\n" +
            "Nyang-Nyang - nyang nyang,yum yum,yum-yum,young young,yin yang,yang yang,young,yung" + "\n" +
            "Green Ball - green bow,green bowl,rainbow,greenbo,greenbow,green bay,greenville" + "\n" +
            "Nusa Dua - methadone,north ottawa,north andover,los angela,rosangela,massage oil,what's a doula,missoula,north abdullah,i said dora,wilson duo,minnesota,methadone,mistletoe,miss angela" + "\n" +
            "Sri Lanka - srilanka,shri lanka,lanka,blanca,blanka" + "\n" +
            "Serangan - shotgun,sharingan,should i come,set an alarm,7 gun,sean gunn,set alarm,should i run,savannah,sheeran,710,cnn" + "\n" +
            "Tandjung left's - tandjung,tanjung,dungeon,on june" + "\n" +
            "Sanur Reef - summer,son of" + "\n" +
            "Keramas - chatham mass,can i mass,can i mas,can i must" + "\n" +
            "Padangbai - padang bay,button bay,badung bay,cabana Bay,banana bay" + "\n" +
            "Shipwrecks - shipwrecked,ship wrecks,suprax,cheap hats,chipwrecked" + "\n" +
            "Lacerations - playstations,playstation's,restorations,destinations,nice relations,need directions" + "\n" +
            "Playgrounds - playground,backgrounds,baker island,baker islands" + "\n" +
            "Ceningan - changing them,cannon gun,h&m,canyonlands,canyonland,canyon gun,canyon land,chain gun,kenyon gun,kenyan gun";

    private final Map<String, String> soundLikeSpot = new HashMap<>();
    private final Map<String, SurfSpot> sToSpot = new HashMap<>();
    SurfSpots surfSpots = null;
    private void initSToSpot() {
        for (SurfSpot spot : surfSpots.getAll()) {
            sToSpot.put(spot.name.toLowerCase(), spot);
            if (spot.altNames != null) {
                for (String altName : spot.altNames) {
                    sToSpot.put(altName.toLowerCase(), spot);
                }
            }
        }
        for (String si : SOUND_LIKE_SPOT.split("\n")) {
            String[] sj = si.split(" - ");
            String spotName = sj[0];
            String soundLikeSpotName = sj[1];
            for (String sk : soundLikeSpotName.split(",")) {
                soundLikeSpot.put(sk, spotName);
            }
        }
    }


    private String processRecognitionResults(List<String> strings) {
        if (strings == null || strings.isEmpty()) return null;

        List<String> dayhits  = new ArrayList<>();
        List<Map.Entry<String, String>> slDayhits  = new ArrayList<>();
        List<String> spothits = new ArrayList<>();
        List<Map.Entry<String, String>> slSpothits = new ArrayList<>();

        for (String s : strings) {
            String ss = s.toLowerCase();

            for (String sj : sToDay.keySet()) {
                if (ss.contains(sj)) {
                    dayhits.add(sj);
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeDay.entrySet()) {
                if (ss.contains(e.getKey())) {
                    slDayhits.add(e);
                }
            }

            for (String sj : sToSpot.keySet()) {
                if (ss.contains(sj)) {
                    spothits.add(sj);
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeSpot.entrySet()) {
                if (ss.contains(e.getKey())) {
                    slSpothits.add(e);
                }
            }
        }

        String s = null;

        if (!spothits.isEmpty()) s = spothits.get(0);
        else if (!slSpothits.isEmpty()) s = slSpothits.get(0).getValue();

        if (!dayhits.isEmpty()) s = s == null ? dayhits.get(0) : s + " " + dayhits.get(0);
        else if (!slDayhits.isEmpty()) s = s == null ? slDayhits.get(0).getValue() : s + " " + slDayhits.get(0).getValue();

        Log.i(TAG, "processRecognitionResults() | " + spothits);
        Log.i(TAG, "processRecognitionResults() | " + slSpothits);
        Log.i(TAG, "processRecognitionResults() | " + dayhits);
        Log.i(TAG, "processRecognitionResults() | " + slDayhits);

        Log.i(TAG, "processRecognitionResults() | " + s);

        return s;
    }
    public void performCommand(String s) {
        if (s == null) return;

        Integer  day  = null;
        SurfSpot spot = null;

        String ss = s.toLowerCase();

        for (Map.Entry<String, Integer> e : sToDay.entrySet()) {
            if (ss.contains(e.getKey())) {
                day = e.getValue();
                break;
            }
        }
        for (Map.Entry<String, SurfSpot> e : sToSpot.entrySet()) {
            if (ss.contains(e.getKey())) {
                spot = e.getValue();
                break;
            }
        }

        Runnable rDay = null;
        if (day != null) {
            Log.i(TAG, "performCommand() | day = " + day);
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
