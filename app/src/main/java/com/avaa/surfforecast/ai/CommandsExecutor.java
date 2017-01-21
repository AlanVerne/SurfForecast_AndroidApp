package com.avaa.surfforecast.ai;

import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfConditionsOneDay;
import com.avaa.surfforecast.data.SurfSpot;
import com.avaa.surfforecast.data.TideData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            "where to surf - where to go,where's the seraph,where to serve,where are to surf" + "\n" +
            "hide ai";

    private static final String SOUND_LIKE_TIME_OF_DAY =
            "now - " + "\n" +
            "sunset - sun set" + "\n" +
            "sunrise - sonrise,sun rise" + "\n" +
            "midday - " + "\n" +
            "afternoon - " + "\n" +
            "morning - " + "\n" +
            "evening - " + "\n" +
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
            "Serangan - sorry i'm gone,surround gun,sauron gun,birmingham,shotgun,sharingan,should i come,set an alarm,7 gun,sean gunn,set alarm,should i run,savannah,sheeran,710,cnn" + "\n" +
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
    public final Map<String, Integer> sToDay = new LinkedHashMap<>();

    public final Map<String, Integer> sToTime = new LinkedHashMap<>();

    private final AppContext appContext;
    private final Answers answers;

    public CommandsExecutor(AppContext c) {
        this.appContext = c;
        answers = new Answers(c);
        initSToDay();
        initSToTime();
        initSToSpot();
        initSoundLikeMap(SOUND_LIKE_KEYWORD, soundLikeKeyword);
        initSoundLikeMap(SOUND_LIKE_TIME_OF_DAY, soundLikeTimeOfDay);

        c.commandsExecutor = this;
    }


    private void initSToTime() {
        sToTime.put(" now ", -1);

        sToTime.put(" sunrise ", 6*60);
        sToTime.put(" morning ", 9*60);
        sToTime.put(" noon ", 12*60);
        sToTime.put(" midday ", 12*60);
        sToTime.put(" afternoon ", 15*60);
        sToTime.put(" sunset ",  19*60);
        sToTime.put(" evening ",  19*60);
    }
    private void initSToDay() {
        sToDay.put(" now ", 0);

        String today = DateUtils.getRelativeTimeSpanString(
                0, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String tomorrow = DateUtils.getRelativeTimeSpanString(
                1000 * 60 * 60 * 24, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String afterTomorrow = DateUtils.getRelativeTimeSpanString(
                2 * 1000 * 60 * 60 * 24, 0, DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        sToDay.put(" " + today + " ", 0);
        sToDay.put(" " + tomorrow + " ", 1);
        sToDay.put(" " + afterTomorrow + " ", 2);

        Calendar c = Common.getCalendarToday(Common.TIME_ZONE);
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            if (i > 1 && dayOfWeek == Calendar.SATURDAY) sToDay.put(" weekend ", i);
            String s = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).toLowerCase();
            sToDay.put(" " + s + " ", i);
            c.add(Calendar.DATE, 1);
        }

        initSoundLikeMap(SOUND_LIKE_DAY, soundLikeDay);
    }

    private void initSToSpot() {
        for (SurfSpot spot : appContext.surfSpots.getAll()) {
            sToSpot.put(" " + spot.name.toLowerCase() + " ", spot);
            if (spot.altNames != null) {
                for (String altName : spot.altNames) {
                    sToSpot.put(" " + altName.toLowerCase() + " ", spot);
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
            map.put(" " + key + " ", key);

            if (sj.length > 1) {
                String soundLikeKey = sj[1];
                for (String sk : soundLikeKey.split(",")) {
                    map.put(" " + sk + " ", key);
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
        List<String> timehits = new ArrayList<>();
        List<Map.Entry<String, String>> slTimeHits = new ArrayList<>();

        List<Map.Entry<String, String>> slKeywordHits = new ArrayList<>();

        for (String s : strings) {
            String ss = " " + s.toLowerCase().replace(",", " ,").replace("?", " ?") + " ";

            for (String sj : sToDay.keySet()) {
                if (ss.contains(sj)) {
                    dayhits.add(sj.trim());
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeDay.entrySet()) {
                if (ss.contains(e.getKey())) slDayhits.add(e);
            }

            for (String sj : sToTime.keySet()) {
                if (ss.contains(sj)) {
                    timehits.add(sj.trim());
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeTimeOfDay.entrySet()) {
                if (ss.contains(e.getKey())) slTimeHits.add(e);
            }

            for (String sj : sToSpot.keySet()) {
                if (ss.contains(sj)) {
                    spothits.add(sj.trim());
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeSpot.entrySet()) {
                if (ss.contains(e.getKey())) slSpothits.add(e);
            }

            for (Map.Entry<String, String> e : soundLikeKeyword.entrySet()) {
                if (ss.contains(e.getKey())) slKeywordHits.add(e);
            }
        }

        String s = null;

        if (!spothits.isEmpty()) s = spothits.get(0);
        else if (!slSpothits.isEmpty()) s = slSpothits.get(0).getValue();

        if (!slKeywordHits.isEmpty()) s = s == null ? slKeywordHits.get(0).getValue() : s + " " + slKeywordHits.get(0).getValue();

        if (!dayhits.isEmpty()) s = s == null ? dayhits.get(0) : s + " " + dayhits.get(0);
        else if (!slDayhits.isEmpty()) s = s == null ? slDayhits.get(0).getValue() : s + " " + slDayhits.get(0).getValue();

        if (!timehits.isEmpty()) s = s == null ? timehits.get(0) : s + " " + timehits.get(0);
        else if (!slTimeHits.isEmpty()) s = s == null ? slTimeHits.get(0).getValue() : s + " " + slTimeHits.get(0).getValue();

        if (s != null ) {
            Log.i(TAG, "                    | spot = " + spothits);
            Log.i(TAG, "                    | spot = " + slSpothits);
            Log.i(TAG, "                    | keyw = " + slKeywordHits);
            Log.i(TAG, "                    | day  = " + dayhits);
            Log.i(TAG, "                    | day  = " + slDayhits);
            Log.i(TAG, "                    | time = " + timehits);
            Log.i(TAG, "                    | time = " + slTimeHits);
        }
        Log.i(TAG, "rRToStringCommand() | command = '" + s + "'");

        if (s != null && s.length() > 1) s = s.substring(0, 1).toUpperCase() + s.substring(1);

        return s;
    }


    public Command stringCommandToCommand(String s) {
        if (s == null) return null;

        Integer day = null;
        SurfSpot spot = null;
        Integer time = null;
        List<String> keywords = new ArrayList<>();
        List<String> timeOfDay = new ArrayList<>();

        String ss = " " + s.toLowerCase().replace(",", " ,").replace("?", " ?") + " ";

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
            if (ss.contains(si) && !keywords.contains(si)) keywords.add(si);
        }
        for (Map.Entry<String, Integer> e : sToTime.entrySet()) {
            if (ss.contains(e.getKey())) {
                time = e.getValue();
                break;
            }
        }
        for (String si : soundLikeTimeOfDay.values()) {
            if (ss.contains(si) && !timeOfDay.contains(si)) timeOfDay.add(si);
        }

        return new Command(day, spot, keywords.isEmpty() ? null : keywords, time, timeOfDay.isEmpty() ? null : timeOfDay);
    }


    // --


    public String intDayToNL(int day) {
        for (Map.Entry<String, Integer> e : AppContext.instance.commandsExecutor.sToDay.entrySet()) {
            if (day == e.getValue()) return e.getKey().trim();
        }
        return null;
    }
    public String intTimeToNL(int time) {
        for (Map.Entry<String, Integer> e : AppContext.instance.commandsExecutor.sToTime.entrySet()) {
            if (time == e.getValue()) return e.getKey().trim();
        }
        return null;
    }


    // --


    public String[] getDefaultOpts() {
        SurfSpot selectedSpot = appContext.surfSpots.selectedSpot();
        String name = selectedSpot.getShortName();

        int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);
        if (nowTimeInt > 18*60) ;

        String day = nowTimeInt > 18 * 60 ? "tomorrow" : "today";

        return new String[]{
                "Canggu".equals(name) ? "[spot]Serangan " + day : "[spot]Canggu " + day,
//                "[spot]Serangan " + day,
                selectedSpot.urlCam == null ? "[cam]Camera" : "[cam]Camera for " + name,
                "[cond]Conditions",
                "[cond]Where to surf?",
                "[date]Tomorrow"};
    }


    // --


    List<String> commandsHistory = new ArrayList<>();
    Answer lastAnswer = null;


    public Answer performCommand(String s) {
        Log.i(TAG, "performCommand(String '" + s + "')");
        return performCommand(stringCommandToCommand(s), s);
    }
    public Answer performCommand(Command c, String sc) {
        Log.i(TAG, "performCommand(" + c + ")");

        if (c.keywords != null && (c.keywords.contains("cancel") || c.keywords.contains("hide ai")) && c.spot == null && c.day == null) {
            lastAnswer = null;
            return null;
        }

        if (lastAnswer != null && lastAnswer.replyInterpreters != null) {
            for (String replyInterpreter : lastAnswer.replyInterpreters) {
                String[] split = replyInterpreter.split(" - ");
                if (c.fits(split[0])) {
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

            int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);

            if (keywords.contains("where to surf")) {
                int r = 0;

                float best = -1000;
                Integer bestTime = 0;
                SurfSpot bestSpot = null;

                int plusDays;
                if (c.time == null) {
                    if (c.day == null) {
                        if (nowTimeInt > 18*60) plusDays = 1;
                        else plusDays = 0;
                    }
                    else plusDays = c.day;

                    TideData tideData = appContext.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
                    for (SurfSpot surfSpot : appContext.surfSpots.getFavoriteSurfSpotsList()) {
                        SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);
                        for (Map.Entry<Integer, SurfConditions> entry : surfConditionsOneDay.entrySet()) {
                            Integer time = entry.getKey();
                            if ((plusDays == 0 && time < nowTimeInt) || time < 5*60 || time > 19*60) continue;
                            float rate = entry.getValue().rate(surfSpot, tideData, plusDays, time);
                            if (rate > best) {
                                best = rate;
                                bestTime = time;
                                bestSpot = surfSpot;
                            }
                        }
                    }

                    if (bestSpot == null) lastAnswer = new Answer("IDKN");
                    else {
                        lastAnswer = new Answer(
                                "In " + bestSpot.getShortName() + " at " + bestTime / 60 + " o'clock",
                                "In " + bestSpot.getShortName() + " at " + bestTime / 60 + " o'clock"
                        );
                        lastAnswer.replyVariants = new String[]{
                                bestSpot.getShortName() + " " + intDayToNL(plusDays),
                                "-Ok, thanks"
                        };
                    }

                    if (c.day == null) lastAnswer.clarification = intDayToNL(plusDays);
                }
                else if ((c.day == null || c.day == 0) && c.time == -1) {
                    TideData tideData = appContext.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
                    for (SurfSpot surfSpot : appContext.surfSpots.getFavoriteSurfSpotsList()) {
                        SurfConditions surfConditions = surfSpot.conditionsProvider.getNow();
                        float rate = surfConditions.rate(surfSpot, tideData, 0, nowTimeInt);
                        if (rate > best) {
                            best = rate;
                            bestSpot = surfSpot;
                        }
                    }

                    if (bestSpot == null) lastAnswer = new Answer("IDKN");
                    else {
                        lastAnswer = new Answer(
                                "In " + bestSpot.getShortName(),
                                "In " + bestSpot.getShortName()
                        );
                        lastAnswer.replyVariants = new String[]{
                                bestSpot.getShortName() + " " + intDayToNL(0),
                                bestSpot.getShortName() + " " + "now",
                                "-Ok, thanks"
                        };
                    }
                }
                else {
                    if (c.day == null) {
                        if (nowTimeInt > 18*60) plusDays = 1;
                        else plusDays = 0;
                    }
                    else plusDays = c.day;

                    TideData tideData = appContext.tideDataProvider.getTideData(Common.BENOA_PORT_ID);
                    for (SurfSpot surfSpot : appContext.surfSpots.getFavoriteSurfSpotsList()) {
                        SurfConditions surfConditions = surfSpot.conditionsProvider.get(plusDays).get((int)(c.time));
                        float rate = surfConditions.rate(surfSpot, tideData, 0, nowTimeInt);
                        if (rate > best) {
                            best = rate;
                            bestSpot = surfSpot;
                        }
                    }

                    if (bestSpot == null) lastAnswer = new Answer("IDKN");
                    else {
                        lastAnswer = new Answer(
                                "In " + bestSpot.getShortName(),
                                "In " + bestSpot.getShortName()
                        );
                        lastAnswer.replyVariants = new String[]{
                                bestSpot.getShortName() + " " + intDayToNL(plusDays),
                                bestSpot.getShortName() + " " + intDayToNL(plusDays) + " " + intTimeToNL(c.time),
                                "-Ok, thanks"
                        };
                    }
                }
            }
            else if ("camera".equals(keywords.get(0))) {
                if (c.spot != null) {
                    if (c.spot.urlCam != null && !c.spot.urlCam.isEmpty()) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.spot.urlCam));
                        appContext.mainActivity.startActivity(browserIntent);
                    }
                    else {
                        lastAnswer = new Answer("No camera for " + c.spot.name + ". Choose spot:", "For what spot?", true, new String[]{"[spot]Pererenan", "[spot]Old man's", "[spot]Bingin", "[spot]Uluwatu", "[spot]Keramas", "-[close]Cancel"},
                                new String[]{"spot - Camera for [spot]", " - Camera"});
                    }
                }
                else {
                    lastAnswer = new Answer("Choose spot:", "For what spot?", true, new String[]{"[spot]Pererenan", "[spot]Old man's", "[spot]Bingin", "[spot]Uluwatu", "[spot]Keramas", "-[close]Cancel"},
                            new String[]{"spot - Camera for [spot]", " - Camera"});
                }
            }
            else if (keywords.contains("conditions") || keywords.contains("waves") || keywords.contains("swell") || keywords.contains("wind") || keywords.contains("tide")) {
                boolean forNowTime = c.time != null && c.time == -1;

                Integer time = c.time;
                if (time == null && (c.day == null || c.day == 0)) {
                    time = nowTimeInt;
                    forNowTime = true;

                    lastAnswer.clarification = "Now";
                    lastAnswer.add(new Answer(
                            null,
                            "Right now"
                    ), true);
                }

                int plusDays = c.day == null ? 0 : c.day;

                SurfSpot surfSpot = c.spot;
                if (surfSpot == null) {
                    surfSpot = appContext.surfSpots.selectedSpot();

                    String shortName = surfSpot.getShortName();
                    lastAnswer.clarification = (lastAnswer.clarification == null ? "In " : lastAnswer.clarification + " in ") + shortName;
                    lastAnswer.add(new Answer(
                            null,
                            "in " + shortName
                    ), true);
                }

                if (time != null) {
                    if ("conditions".equals(keywords.get(0)) || "waves".equals(keywords.get(0))) {
                        keywords.add("swell");
                        keywords.add("tide");
                        keywords.add("wind");
                    }
                    if (keywords.contains("swell"))
                        lastAnswer.add(forNowTime ? answers.swellNow() : answers.swell(plusDays, time));
                    if (keywords.contains("tide"))
                        lastAnswer.add(forNowTime ? answers.tideNow() : answers.tide(plusDays, time));
                    if (keywords.contains("wind"))
                        lastAnswer.add(forNowTime ? answers.windNow() : answers.wind(plusDays, time));

                    if (!lastAnswer.isEmpty() && lastAnswer.toSay != null) {
                        lastAnswer.replyVariants = new String[]{
                                "[cond]Where to surf now?",
                                (forNowTime ? "[cond]Conditions tomorrow sunrise" : "[cond]Conditions now") + " for " + surfSpot.getShortName(),
                                "[cond]Where to surf tomorrow?",
                                "-[ok]Ok, thanks"};
                        lastAnswer.replyInterpreters = new String[]{
                                "kw:ok - Hide ai",
                                "day,time,spot - Conditions [day] [time] for [spot]",
                                "day,time - Conditions [day] [time]"
                        };
                    }
                }
                else {
                    // TODO ans for unspecified time
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

        if (lastAnswer != null) Log.i(TAG, "performCommand() | answering with " + lastAnswer);

        return lastAnswer;
    }
}
