package com.avaa.surfforecast.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Dimension;
import android.support.v4.app.Fragment;
import android.text.BoringLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.avaa.surfforecast.R;
import com.avaa.surfforecast.views.CircleAnimatedFrameLayout;
import com.avaa.surfforecast.views.CircleVoiceIndicator;
import com.avaa.surfforecast.views.TwoCirclesAnimatedFrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class VoiceInterfaceFragment extends Fragment {
    private static final String TAG = "VoiceIntFr";

    View view;

    View btnMic;
    View btnMicImage;
    CircleVoiceIndicator circleVoiceIndicator;

    TwoCirclesAnimatedFrameLayout flHeader;
    CircleAnimatedFrameLayout flHint;
    LinearLayout llHint;
    TextView tvHintTitle;
    TextView[] tvHintOpts = new TextView[5];
    TextView tvHintOptRed;
    TextView tvHintOptPrerecognized;

    public CommandsExecutor commandsExecutor = null;

    public TextToSpeech tts;

    private SpeechRecognizer speech;
    final RecognitionListener rl = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) { }
        @Override
        public void onBeginningOfSpeech() { }

        @Override
        public void onRmsChanged(float rms) {
            circleVoiceIndicator.post(() -> circleVoiceIndicator.newRms(rms));
        }

        @Override
        public void onBufferReceived(byte[] buffer) { }


        @Override
        public void onEndOfSpeech() {
            uiNotListening();
        }


        @Override
        public void onError(int error) {
            //Log.i(TAG, "rl::onError() | " + error);
            uiNotListening();
            uiHideHint();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            Map<Float, String> res = new TreeMap<Float, String>((o1, o2) -> o2.compareTo(o1));

            for (int i = 0; i < list.size(); i++) {
                float score = scores[i];
                while (res.containsKey(score)) score -= 0.0001;
                res.put(score, list.get(i));
            }

            String text = "";
            for (Map.Entry<Float, String> e : res.entrySet()) {
                text += (int)(100*e.getKey()) + " " + e.getValue() + "\n";
            }
            Log.i(TAG, "onResults()\n" + text);

            String s = commandsExecutor.recognitionResultsToStringCommand(res.values());

            if (s == null) uiHideHint();
            else {
                tvHintOptPrerecognized.setText(s);
                uiApprove(tvHintOptPrerecognized);

                Answer answer = commandsExecutor.performCommand(s);
                if (answer != null) {
                    uiShowAnswer(answer);
                    say(answer);
                }
                else {
                    uiHideHint();
                }
            }

            speech.destroy();
            speech = SpeechRecognizer.createSpeechRecognizer(getActivity().getApplicationContext());
            speech.setRecognitionListener(rl);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String result = commandsExecutor.recognitionResultsToStringCommand(list);
            if (result != null) tvHintOptPrerecognized.setText(result);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            //Log.i(TAG, "rl::onEvent() | " + eventType + params);
        }
    };

    View.OnClickListener clHintOpt = v -> {
        TextView tvHintOpt = ((TextView)v);

        speech.cancel();
        uiNotListening();

        uiApprove(tvHintOpt);

        Answer answer = commandsExecutor.performCommand(tvHintOpt.getText().toString());
        if (answer != null) {
            uiShowAnswer(answer);
        }
        else {
            uiHideHint();
        }
    };


    public VoiceInterfaceFragment() { }


    public void btnMicClicked(View view) {
        startListening();
    }


    private void uiListening() {
        btnMicImage.setAlpha(0.95f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button));
        }
        circleVoiceIndicator.setAwakened(true);
    }
    private void uiNotListening() {
        if (flHint.getState() != 1) btnMicImage.setAlpha(0.95f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnMic.setBackground(getContext().getResources().getDrawable(R.drawable.round_button_transparent));
        }
        circleVoiceIndicator.setAwakened(false);
    }


    private void uiShowHint() {
        if (flHint.getState() == 1) return;

        btnMicImage.setAlpha(0.95f);
        flHint.setVisibility(View.VISIBLE);

        tvHintOptPrerecognized.setText("...");
        tvHintOptPrerecognized.setTextColor(0x66000000);

        uiSetOpts(new String[]{"Canggu", "Serangan", "Camera", "Conditions", "Tomorrow"}); //"Where to surf tomorrow?"});

        setHintText("Say! Or tap...");

        commandsExecutor.lastAnswer = null;
    }
    private void uiApprove(TextView tv) {

    }
    private void uiHideHint() {
        if (!circleVoiceIndicator.isAwakened()) btnMicImage.setAlpha(0.2f);
        flHint.setVisibility(View.INVISIBLE);
        flHeader.setVisibility(View.INVISIBLE);
    }


    private void setHintText(String text) {
        if (text.length() > 50) {
            tvHintTitle.setTextSize(18);
            tvHintTitle.setPadding(tvHintTitle.getPaddingLeft(), 0, tvHintTitle.getPaddingRight(), 30*3);
        }
        else {
            tvHintTitle.setTextSize(24);
            tvHintTitle.setPadding(tvHintTitle.getPaddingLeft(), 0, tvHintTitle.getPaddingRight(), 18*3);
        }
        tvHintTitle.setText(text);
    }


    private void uiSetOpts(String[] opts) {
        int l = 0;

        tvHintOptRed.setVisibility(View.GONE);

        if (opts == null) opts = new String[]{"Ok, thanks"};

        if (opts != null) {
            l = opts.length;
            for (int i = 0; i < l; i++) {
                if ("Cancel".equals(opts[i])) {
                    tvHintOptRed.setText(opts[i]);
                    tvHintOptRed.setVisibility(View.VISIBLE);
                }
                else {
                    tvHintOpts[i].setText(opts[i]);
                    tvHintOpts[i].setVisibility(View.VISIBLE);
                }
            }
            if (tvHintOptRed.getVisibility() == View.VISIBLE) l--;
        }

        for (int i = l; i < 5; i++) {
            tvHintOpts[i].setVisibility(View.GONE);
        }
    }


    boolean waitingForAnswer = false;
    private void uiShowAnswer(Answer a) {
        uiShowHint();

        flHeader.setVisibility(View.VISIBLE);

        ((TextView)view.findViewById(R.id.tvHeader)).setText(a.forCommand);

        setHintText(a.toShow);

        waitingForAnswer = a.waitForReply;
        uiSetOpts(a.replyVariants);
    }


    private void say(Answer a) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id");
        tts.speak(a.toSay, TextToSpeech.QUEUE_FLUSH, map);
    }


    public void startListening() {
        if (circleVoiceIndicator.isAwakened()) {
            speech.cancel();
            uiNotListening();
            uiHideHint();
        }
        else {
            uiListening();
            uiShowHint();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
//            intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{new Locale("ru", "RU").toString()});
//            intent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, false);
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
//            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getActivity().getApplicationContext().getPackageName());

            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH); //FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);

            speech.startListening(intent);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(getActivity().getApplicationContext());

        speech = SpeechRecognizer.createSpeechRecognizer(getActivity().getApplicationContext());
        speech.setRecognitionListener(rl);

        tts = new TextToSpeech(getActivity().getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setLanguage(Locale.UK);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) { }
                    @Override
                    public void onDone(String utteranceId) {
                        flHint.post(() -> {
                            if (!waitingForAnswer) uiHideHint();
                            else startListening();
                        });
                    }
                    @Override
                    public void onError(String utteranceId) { }
                });
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_voice_interface, container, false);

        btnMic = view.findViewById(R.id.btnMic);
        btnMicImage = view.findViewById(R.id.btnMicImage);
        circleVoiceIndicator = (CircleVoiceIndicator) view.findViewById(R.id.cvi);

        flHeader = (TwoCirclesAnimatedFrameLayout)view.findViewById(R.id.flHeader);

        flHint = (CircleAnimatedFrameLayout) view.findViewById(R.id.flHint);

        llHint = (LinearLayout) view.findViewById(R.id.llHint);
        tvHintTitle = (TextView) view.findViewById(R.id.tvHintTitle);
        tvHintOpts[0] = (TextView) view.findViewById(R.id.tvHintOpt1);
        tvHintOpts[1] = (TextView) view.findViewById(R.id.tvHintOpt2);
        tvHintOpts[2] = (TextView) view.findViewById(R.id.tvHintOpt3);
        tvHintOpts[3] = (TextView) view.findViewById(R.id.tvHintOpt4);
        tvHintOpts[4] = (TextView) view.findViewById(R.id.tvHintOpt5);
        tvHintOptRed = (TextView) view.findViewById(R.id.tvHintOptRed);
        tvHintOptPrerecognized = (TextView) view.findViewById(R.id.tvPrerecognized);

        btnMic.setOnClickListener(this::btnMicClicked);

        for (TextView tvHintOpt : tvHintOpts) tvHintOpt.setOnClickListener(clHintOpt);
        tvHintOptRed.setOnClickListener(clHintOpt);

        circleVoiceIndicator.x = 15*getResources().getDisplayMetrics().density;
        circleVoiceIndicator.y = 15*getResources().getDisplayMetrics().density;

        flHint.bgClick = this::uiHideHint;

        return view;
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
}
