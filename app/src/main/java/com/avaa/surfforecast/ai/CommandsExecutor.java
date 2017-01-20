package com.avaa.surfforecast.ai;

import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.Direction;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.SurfSpots;
import com.avaa.surfforecast.data.TideData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.avaa.surfforecast.ai.ToNL.*;

/**
 * Created by Alan on 18 Jan 2017.
 */

public class CommandsExecutor {
    private static final String TAG = "CommandsExecutor";


    private static final String SOUND_LIKE_DAY =
            "Friday - friendly";

    private static final String SOUND_LIKE_KEYWORD =
            "wind - winged" + "\n" +
            "swell - swag,swallow,12" + "\n" +
            "tide - dyed,guide,died,diet,dyed" + "\n" +
            "conditions - can you show us" + "\n" +
            "waves" + "\n" +
            "camera - cam,canara,canada,kamera" + "\n" +
            "in meters - in metres" + "\n" +
            "cancel - forget it,exit,close" + "\n" +
            "ok - good" + "\n" +
            "hide ai";

    private static final String SOUND_LIKE_TIME_OF_DAY =
            "now - " + "\n" +
            "sunset - sun set" + "\n" +
            "sunrise - sonrise,sun rise" + "\n" +
            "midday - " + "\n" +
            "afternoon - " + "\n" +
            "[number] am - " + "\n" +
            "[number] pm - " + "\n";

    private static final String SOUND_LIKE_SPOT = "" +
            "Medewi - mandiri,madeira,man baby,man daily,mondavi,big daddy,men daily,andretti,when daddy" + "\n" +
            "Balian - aaliyah,banana,benihana,money on,minion,banyan,valium,bali on,violin" + "\n" +
            "Canggu - chattanooga,jingle,jungle,canggu,django,jango,can you move,can you do,can you go,demi moore,jamie,china gold,tangled,tangle,triangle,chinese food,kangol,kangal,congo" + "\n" +
            "Old mans - old mans,old man's,oldman,old men,old man,goldman,almonds,ahlmans,harmons,holman's" + "\n" +
            "Batu bolong - how to draw,autozone,let alone,methylone,atenolol" + "\n" +
            "Batu - batoon,bo2,butter,bathroom" + "\n" +
            "Berawa - bravo,brava,java,driver,arava,brower,browa,brother,there are" + "\n" +
            "Seminyak - singing app,selenium" + "\n" +
            "Kuta - puta,cota,guta" + "\n" +
            "Balangan - bellingham,malanga,mehlingen,milan gun" + "\n" +
            "Dreamland - greenland,dream land,keeneland,finland,timland" + "\n" +
            "Bingin - didn't get,bending,binging,ben king,ninjin,engine,bingen,benjen,bingeon,binging" + "\n" +
            "Impossibles - impossible,imposibles,kim possible,kim possible's" + "\n" +
            "Padang-Padang - padang padang,pandanan,adam adam,adam,badung,banana banana,badonkadonk,padam padam,button button" + "\n" +
            "Uluwatu - palo alto,hello weather,all weather,all the weather,blue waffle,hello motto,aluminum,uidaho,all idaho,old idaho,idaho,lovato,all lotto,or lotto,whole lotta,holy water" + "\n" +
            "Nyang-Nyang - nyang nyang,yum yum,yum-yum,young young,yin yang,yang yang,young,yung" + "\n" +
            "Green Ball - green bow,green bowl,rainbow,greenbo,greenbow,green bay,greenville" + "\n" +
            "Nusa Dua - methadone,north ottawa,north andover,los angela,rosangela,massage oil,what's a doula,missoula,north abdullah,i said dora,wilson duo,minnesota,methadone,mistletoe,miss angela" + "\n" +
            "Sri Lanka - srilanka,shri lanka,lanka,blanca,blanka" + "\n" +
            "Serangan - birmingham,shotgun,sharingan,should i come,set an alarm,7 gun,sean gunn,set alarm,should i run,savannah,sheeran,710,cnn" + "\n" +
            "Tandjung left's - tandjung,tanjung,dungeon,on june" + "\n" +
            "Sanur Reef - summer,son of" + "\n" +
            "Keramas - chatham mass,can i mass,can i mas,can i must" + "\n" +
            "Padangbai - padang bay,button bay,badung bay,cabana Bay,banana bay" + "\n" +
            "Shipwrecks - shipwrecked,ship wrecks,suprax,cheap hats,chipwrecked" + "\n" +
            "Lacerations - playstations,playstation's,restorations,destinations,nice relations,need directions" + "\n" +
            "Playgrounds - playground,backgrounds,baker island,baker islands" + "\n" +
            "Ceningan - changing them,cannon gun,h&m,canyonlands,canyonland,canyon gun,canyon land,chain gun,kenyon gun,kenyan gun";

    private final Map<String, String> soundLikeKeyword = new HashMap<>();
    private final Map<String, String> soundLikeTimeOfDay = new HashMap<>();

    private final Map<String, String> soundLikeSpot = new HashMap<>();
    private final Map<String, SurfSpot> sToSpot = new HashMap<>();

    private final Map<String, String> soundLikeDay = new HashMap<>();
    public final Map<String, Integer> sToDay = new HashMap<>();

    private final AppContext appContext;
    private final Answers answers;

    public CommandsExecutor(AppContext c) {
        this.appContext = c;
        answers = new Answers(c);
        initSToDay();
        initSToSpot();
        initSoundLikeMap(SOUND_LIKE_KEYWORD, soundLikeKeyword);
        initSoundLikeMap(SOUND_LIKE_TIME_OF_DAY, soundLikeTimeOfDay);

        c.commandsExecutor = this;
    }


    private void initSToDay() {
        sToDay.put("now", 0);

        String today = DateUtils.getRelativeTimeSpanString(
                0, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String tomorrow = DateUtils.getRelativeTimeSpanString(
                1000 * 60 * 60 * 24, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String afterTomorrow = DateUtils.getRelativeTimeSpanString(
                2 * 1000 * 60 * 60 * 24, 0, DateUtils.DAY_IN_MILLIS,
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

        initSoundLikeMap(SOUND_LIKE_DAY, soundLikeDay);
    }

    private void initSToSpot() {
        for (SurfSpot spot : appContext.surfSpots.getAll()) {
            sToSpot.put(spot.name.toLowerCase(), spot);
            if (spot.altNames != null) {
                for (String altName : spot.altNames) {
                    sToSpot.put(altName.toLowerCase(), spot);
                }
            }
        }

        initSoundLikeMap(SOUND_LIKE_SPOT, soundLikeSpot);
    }


    private static void initSoundLikeMap(String s, Map<String, String> map) {
        String[] sj;
        String key;

        for (String si : s.split("\n")) {
            sj = si.split(" - ");

            key = sj[0];
            map.put(key, key);

            if (sj.length > 1) {
                String soundLikeKey = sj[1];
                for (String sk : soundLikeKey.split(",")) {
                    map.put(sk, key);
                }
            }
        }
    }


    // --


    public String recognitionResultsToStringCommand(Collection<String> strings) {
        if (strings == null || strings.isEmpty()) return null;

        List<String> dayhits = new ArrayList<>();
        List<Map.Entry<String, String>> slDayhits = new ArrayList<>();
        List<String> spothits = new ArrayList<>();
        List<Map.Entry<String, String>> slSpothits = new ArrayList<>();

        List<Map.Entry<String, String>> slKeywordHits = new ArrayList<>();

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

            for (Map.Entry<String, String> e : soundLikeKeyword.entrySet()) {
                if (ss.contains(e.getKey())) {
                    slKeywordHits.add(e);
                }
            }
        }

        String s = null;

        if (!spothits.isEmpty()) s = spothits.get(0);
        else if (!slSpothits.isEmpty()) s = slSpothits.get(0).getValue();

        if (!dayhits.isEmpty()) s = s == null ? dayhits.get(0) : s + " " + dayhits.get(0);
        else if (!slDayhits.isEmpty()) s = s == null ? slDayhits.get(0).getValue() : s + " " + slDayhits.get(0).getValue();

        if (!slKeywordHits.isEmpty()) s = s == null ? slKeywordHits.get(0).getValue() : s + " " + slKeywordHits.get(0).getValue();

        Log.i(TAG, "recognitionResultsToStringCommand() | " + spothits);
        Log.i(TAG, "recognitionResultsToStringCommand() | " + slSpothits);
        Log.i(TAG, "recognitionResultsToStringCommand() | " + dayhits);
        Log.i(TAG, "recognitionResultsToStringCommand() | " + slDayhits);
        Log.i(TAG, "recognitionResultsToStringCommand() | " + slKeywordHits);

        Log.i(TAG, "recognitionResultsToStringCommand() | " + s);

        if (s != null && s.length() > 1) s = s.substring(0, 1).toUpperCase() + s.substring(1);

        return s;
    }


    public Command stringCommandToCommand(String s) {
        if (s == null) return null;

        Integer day = null;
        SurfSpot spot = null;
        List<String> keywords = new ArrayList<>();
        List<String> timeOfDay = new ArrayList<>();

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
        for (String si : soundLikeKeyword.values()) {
            if (ss.contains(si)) keywords.add(si);
        }
        for (String si : soundLikeTimeOfDay.values()) {
            if (ss.contains(si)) timeOfDay.add(si);
        }

        return new Command(day, spot, keywords, timeOfDay);
    }


    // --


    List<String> commandsHistory = new ArrayList<>();
    Answer lastAnswer = null;


    public Answer performCommand(String s) {
        Log.i(TAG, "performCommand(String " + s + ")");
        return performCommand(stringCommandToCommand(s), s);
    }
    public Answer performCommand(Command c, String sc) {
        if (c.keywords != null && (c.keywords.contains("cancel") || c.keywords.contains("hide ai"))) {
            lastAnswer = null;
            return null;
        }

        if (lastAnswer != null && lastAnswer.replyVariants != null) {
            for (String replyInterpreter : lastAnswer.replyInterpreters) {
                String[] split = replyInterpreter.split(" - ");
                if (c.has(split[0])) {
                    Log.i(TAG, "performCommand(" + c.toString() + ") | replied: '" + split[0] + "', new command: '" + c.fill(split[1]) + "'");

                    lastAnswer = null;
                    lastAnswer = performCommand(c.fill(split[1]));
                    return lastAnswer;
                }
            }
        }

        if (c.keywords != null) {
            List<String> keywords = c.keywords;

            lastAnswer = new Answer();

            if (keywords.contains("swell")) lastAnswer.add(answers.swellNow());
            if (keywords.contains("tide")) lastAnswer.add(answers.tideNow());
            if (keywords.contains("wind")) lastAnswer.add(answers.windNow());

            if ("conditions".equals(keywords.get(0)))
                lastAnswer.add(answers.swellNow()).add(answers.tideNow()).add(answers.windNow());
            if ("waves".equals(keywords.get(0)))
                lastAnswer.add(answers.swellNow()).add(answers.tideNow()).add(answers.windNow());

            if (lastAnswer != null) {
                lastAnswer.replyVariants = new String[]{"Ok, thank's", "For tomorrow"};
                lastAnswer.replyInterpreters = new String[]{"kw:ok - Hide ai", "day - Conditions [day]"};
            }

            if ("camera".equals(keywords.get(0))) {
                if (c.spot != null && c.spot.urlCam != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.spot.urlCam));
                    appContext.mainActivity.startActivity(browserIntent);
                }
                else {
                    lastAnswer = new Answer("Choose spot:", "For what spot?", new String[]{"Pererenan", "Old man's", "Uluwatu", "Bingin", "Cancel"}, new String[]{"spot - Camera for [spot]"});
                }
            }
        }
        else {
            lastAnswer = null;

            Runnable rDay = null;
            if (c.day != null) {
                Log.i(TAG, "performCommand() | day = " + c.day);
                Integer finalDay = c.day;
                rDay = () -> appContext.mainActivity.performSelectDay(finalDay, null);
            }

            if (c.spot != null) {
                appContext.mainActivity.performSelectSpot(c.spot, rDay);
            } else {
                if (rDay != null) rDay.run();
            }
        }

        if (lastAnswer != null) {
            if (lastAnswer.isEmpty()) lastAnswer = null;
            else lastAnswer.forCommand = sc;
        }

        return lastAnswer;
    }
}
