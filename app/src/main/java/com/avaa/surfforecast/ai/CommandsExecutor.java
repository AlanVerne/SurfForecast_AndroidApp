package com.avaa.surfforecast.ai;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.avaa.surfforecast.MainModel;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.METAR;
import com.avaa.surfforecast.data.RatedConditions;
import com.avaa.surfforecast.data.SurfConditions;
import com.avaa.surfforecast.data.SurfSpot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Alan on 18 Jan 2017.
 */

public class CommandsExecutor {
    private static final String TAG = "CommandsExecutor";

    public final Map<String, SurfSpot> sToSpot = new HashMap<>();
    public final Map<String, Integer> sToDay = new LinkedHashMap<>();
    public final Map<String, Integer> sToTime = new LinkedHashMap<>();

    private final Collection<String> keywords;
    private final Collection<String> timesOfDay;

    private final MainModel mainModel;
    private final Answers answers;


    public CommandsExecutor(MainModel m) {
        this.mainModel = m;
        answers = new Answers(m);
        initSToDay();
        initSToTime();
        initSToSpot();

        m.commandsExecutor = this;
        m.voiceRecognitionHelper = new VoiceRecognitionHelper(this);

        keywords = m.voiceRecognitionHelper.soundLikeKeyword.values();
        timesOfDay = m.voiceRecognitionHelper.soundLikeTimeOfDay.values();
    }


    private void initSToTime() {
        sToTime.put(" now ", -1);

        sToTime.put(" sunrise ", 6 * 60);
        sToTime.put(" morning ", 9 * 60);
        sToTime.put(" noon ", 12 * 60);
        sToTime.put(" midday ", 12 * 60);
        sToTime.put(" afternoon ", 15 * 60);
        sToTime.put(" sunset ", 18 * 60);
        sToTime.put(" evening ", 18 * 60);
    }

    private void initSToDay() {
        String today = "today";
//                DateUtils.getRelativeTimeSpanString(
//                0, 0, DateUtils.DAY_IN_MILLIS,
//                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String tomorrow = "tomorrow";
//                DateUtils.getRelativeTimeSpanString(
//                DateUtils.DAY_IN_MILLIS, 0, DateUtils.DAY_IN_MILLIS,
//                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        String afterTomorrow = "after tomorrow";
//                DateUtils.getRelativeTimeSpanString(
//                2 * DateUtils.DAY_IN_MILLIS, 0, DateUtils.DAY_IN_MILLIS,
//                DateUtils.FORMAT_SHOW_WEEKDAY).toString().toLowerCase();

        sToDay.put(" " + today + " ", 0);
        sToDay.put(" " + tomorrow + " ", 1);

        sToDay.put(" now ", 0);

        Calendar c = Common.getCalendarToday(Common.TIME_ZONE);
        for (int i = 0; i < 7; i++) {
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            String s = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toLowerCase();
            sToDay.put(" " + s + " ", i);
            if (i > 1 && dayOfWeek == Calendar.SATURDAY) sToDay.put(" weekend ", i);
            c.add(Calendar.DATE, 1);
        }

        sToDay.put(" " + afterTomorrow + " ", 2);
    }

    private void initSToSpot() {
        for (SurfSpot spot : mainModel.surfSpots.getAll()) {
            sToSpot.put(" " + spot.name.toLowerCase() + " ", spot);
            if (spot.altNames != null) {
                for (String altName : spot.altNames) {
                    sToSpot.put(" " + altName.toLowerCase() + " ", spot);
                }
            }
        }
    }


    // --


    public static String capitalize(String s) {
        if (s != null && s.length() > 1) s = s.substring(0, 1).toUpperCase() + s.substring(1);
        return s;
    }


    public Command toCommand(String s) {
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
        for (String si : this.keywords) {
            if (ss.contains(si) && !keywords.contains(si)) keywords.add(si);
        }
        for (Map.Entry<String, Integer> e : sToTime.entrySet()) {
            if (ss.contains(e.getKey())) {
                time = e.getValue();
                break;
            }
        }
        for (String si : timesOfDay) {
            if (ss.contains(si) && !timeOfDay.contains(si)) timeOfDay.add(si);
        }

        return new Command(day, spot, keywords.isEmpty() ? null : keywords, time, timeOfDay.isEmpty() ? null : timeOfDay);
    }


    // --


    public static String intDayToNL(int day) {
        for (Map.Entry<String, Integer> e : MainModel.instance.commandsExecutor.sToDay.entrySet()) {
            if (day == e.getValue()) return e.getKey().trim();
        }
        return null;
    }


    public static String intTimeToNL(int time) {
        for (Map.Entry<String, Integer> e : MainModel.instance.commandsExecutor.sToTime.entrySet()) {
            if (time == e.getValue()) {
                if (time == -1) return e.getKey().trim();
                else return "at " + e.getKey().trim();
            }
        }
        return "at " + (time / 60) + " o'clock";
    }

    public static String intTimeToNL(int time, boolean at) {
        if (at) return intTimeToNL(time);
        for (Map.Entry<String, Integer> e : MainModel.instance.commandsExecutor.sToTime.entrySet()) {
            if (time == e.getValue()) {
                if (time == -1) return e.getKey().trim();
                else return e.getKey().trim();
            }
        }
        return (time / 60) + " o'clock";
    }


    // --


    public String[] getDefaultOpts() {
        SurfSpot selectedSpot = mainModel.getSelectedSpot();
        String name = selectedSpot.getShortName();

        int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);
        if (nowTimeInt > 18 * 60) ;

        String day = nowTimeInt > 18 * 60 ? "tomorrow" : "today";

        return new String[]{
                "Canggu".equals(name) ? "[spot]Serangan " + day : "[spot]Canggu " + day,
//                "[spot]Serangan " + day,
//                selectedSpot.urlCam == null ? "[cam]Camera" : "[cam]Camera for " + name,
                "[cam]Camera",
                "[cond]Conditions",
                "[q]Where to surf?",
                "[date]Tomorrow"};
    }


    // --


    List<String> commandsHistory = new ArrayList<>();
    LinkedList<Command> stack = new LinkedList<>();
    Answer lastAnswer = null;


    public Answer performCommand(String s) {
        Log.i(TAG, "performCommand(String '" + s + "')");
        return performCommand(toCommand(s), s);
    }

    public Answer performCommand(Command c, String sc) {
        Log.i(TAG, "performCommand(" + c + ")");

        if (c.keywords != null && (c.keywords.contains("cancel") || c.keywords.contains("hide ai")) && c.spot == null && c.day == null) {
            lastAnswer = null;
            return null;
        }

        if (c.keywords != null && c.keywords.size() == 1 && c.keywords.contains("repeat")) {
            return lastAnswer;
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
            Log.i(TAG, "performCommand() | replyInterpreters checked");
        }

        lastAnswer = new Answer();

        if (c.keywords != null) {
            List<String> keywords = c.keywords;

            if (keywords.contains("where to surf") || keywords.contains("best spot")) {
                lastAnswer = commandWhereToSurf(c);
            } else if (keywords.contains("camera")) {
                lastAnswer = commandCamera(c);
            } else if (keywords.contains("conditions")
                    || keywords.contains("waves")
                    || keywords.contains("swell")
                    || keywords.contains("wind")
                    || keywords.contains("tide")) { //&& c.time != null) {
                lastAnswer = commandConditions(c);
            }
        } else if (c.spot != null && c.time != null) {
            lastAnswer = commandConditions(c);
        }

        if (c.spot != null || c.day != null) {
            commandSelectSpotAndDay(c.spot, c.day);
        }

        if (lastAnswer != null) {
            if (lastAnswer.isEmpty()) lastAnswer = null;
            else lastAnswer.forCommand = sc;
        }

        if (lastAnswer != null) Log.i(TAG, "performCommand() | answering with " + lastAnswer);

        return lastAnswer;
    }


    private Answer commandWhereToSurf(Command c) {
//        if (MainModel.instance.userStat.surfingExperience == -1) {
//            stack.add(c);
//            stack.add();
//            return askForUsersExperience();
//        }

        int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);

        String prefix = "";
        if (c.keywords.contains("where to surf")) prefix = "In ";

        lastAnswer = new Answer();

        float bestRate = -1000;
        Integer bestTime = 0;
        SurfSpot bestSpot = null;

        int plusDays;

        if (c.day == null) {
            if (nowTimeInt > 18 * 60) plusDays = 1;
            else plusDays = 0;
        } else plusDays = c.day;

        if (c.time == null) {       //      unspecified time
//            for (SurfSpot surfSpot : mainModel.surfSpots.getFavoriteSurfSpotsList()) {
//                SurfConditionsOneDay surfConditionsOneDay = surfSpot.conditionsProvider.get(plusDays);
//                if (surfConditionsOneDay == null) continue;
//                for (Map.Entry<Integer, SurfConditions> entry : surfConditionsOneDay.entrySet()) {
//                    Integer time = entry.getKey();
//                    if ((plusDays == 0 && time < nowTimeInt - 120) || time < 5 * 60 || time > 19 * 60)
//                        continue;
//                    float rate = entry.getValue().rate(surfSpot, mainModel.tideDataProvider.getTideData(surfSpot.tidePortID), plusDays, time);
//                    if (rate > bestRate) {
//                        bestRate = rate;
//                        bestTime = time;
//                        bestSpot = surfSpot;
//                    }
//                }
//            }
            RatedConditions best = MainModel.instance.rater.getBestForDay(plusDays);
            if (best != null) {
                bestTime = best.time;
                bestRate = best.rating;
                bestSpot = best.surfSpot;
            }

            if (bestSpot == null) lastAnswer = new Answer("IDKN");
            else {
                String intDayToNL = intDayToNL(plusDays);

                bestTime += 60;

                String s = prefix + bestSpot.getShortName();
                String timeToNL = intTimeToNL(bestTime);
                s += " " + timeToNL;

                lastAnswer = new Answer(
                        s,
                        s
                );
//                lastAnswer.waitForReply = true;
                lastAnswer.replyVariants = new String[]{
                        "[spot]" + bestSpot.getShortName() + " " + intDayToNL,
                        //"[cond]" + bestSpot.getShortName() + " " + intDayToNL + " " + timeToNL,
                        "[cond]Conditions will be?",
                        //selectedTime == 6*60 ? "[i]I want to sleep at sunrise" : "[i]I can't at " + timeToNL,
                        "[q]Now?", //"[q]Where to surf now?",
                        //plusDays == 0 ? "[q]Where to surf tomorrow?" : "[q]Where to surf today?",
                        plusDays == 0 ? "[q]Tomorrow?" : "[q]Today?",
                        bestTime > 12 * 60 ? "[q]At sunrise?" : "[q]At sunset?",
                        "-[ok]Ok, thanks"
                };
                lastAnswer.replyInterpreters = new String[]{
                        "kw:conditions - Conditions in " + bestSpot.getShortName() + " " + intDayToNL + " " + timeToNL,
                        "kw:waves - Waves in " + bestSpot.getShortName() + " " + timeToNL,
                        "kw:swell - Swell in " + bestSpot.getShortName() + " " + timeToNL,
                        "kw:wind - Wind in " + bestSpot.getShortName() + " " + timeToNL,
                        "kw:tide - Tide in " + bestSpot.getShortName() + " " + timeToNL,
                        "kw:what's up here - Conditions in " + bestSpot.getShortName() + " " + timeToNL,
                        "time,day - Where to surf [day] [time]?",
                        "time - Where to surf " + intDayToNL + " [time]?",
                        "day - Where to surf [day]?",
                        //selectedTime == 6*60 ? "I want to sleep at sunrise - Where to surf " + intDayToNL(plusDays) + " except sunrise?" :
                        "I can't at " + timeToNL + " - Where to surf " + intDayToNL + " except " + timeToNL + "?",
                };
            }

            if (c.day == null) lastAnswer.addClarification(intDayToNL(plusDays));
        } else if ((c.day != null && c.day == 0) && c.time == -1) {      //      time now
            METAR currentMETAR = mainModel.selectedMETAR;

            for (SurfSpot surfSpot : mainModel.surfSpots.getFavoriteSurfSpotsList()) {
                SurfConditions surfConditions = surfSpot.conditionsProvider.getNow();
                if (surfConditions == null) continue;
                surfConditions.addMETAR(currentMETAR);
                float rate = 0;//surfConditions.rate(surfSpot, mainModel.tideDataProvider.getTideData(surfSpot.tidePortID), 0, nowTimeInt);
                if (rate > bestRate) {
                    bestRate = rate;
                    bestSpot = surfSpot;
                }
            }

            if (bestSpot == null) lastAnswer = new Answer("IDKN");
            else {
                lastAnswer = new Answer(
                        prefix + bestSpot.getShortName(),
                        prefix + bestSpot.getShortName()
                );
//                lastAnswer.waitForReply = true;
                lastAnswer.replyVariants = new String[]{
                        "[cond]Conditions are?",
                        "[spot]" + bestSpot.getShortName() + " " + intDayToNL(0),
                        "[q]Today?",
                        "[q]Tomorrow?",
                        "-[ok]Ok, thanks"
                };
                lastAnswer.replyInterpreters = new String[]{
                        "kw:conditions - Conditions in " + bestSpot.getShortName() + " now",
                        "kw:waves - Waves in " + bestSpot.getShortName() + " now",
                        "kw:swell - Swell in " + bestSpot.getShortName() + " now",
                        "kw:wind - Wind in " + bestSpot.getShortName() + " now",
                        "kw:tide - Tide in " + bestSpot.getShortName() + " now",
                        "kw:what's up there - Conditions in " + bestSpot.getShortName() + " now",
                        "time - Where to surf [time]?",
                        "day - Where to surf [day]?",
                        "time,day - Where to surf [day] [time]?"
                };
            }
        } else {  // day and time - specified. time - not now
            for (SurfSpot surfSpot : mainModel.surfSpots.getFavoriteSurfSpotsList()) {
                SurfConditions surfConditions = surfSpot.conditionsProvider.get(plusDays).get((int) (c.time));
                if (surfConditions == null) continue;
                float rate = 0;//surfConditions.rate(surfSpot, mainModel.tideDataProvider.getTideData(surfSpot.tidePortID), plusDays, c.time);
                if (rate > bestRate) {
                    bestRate = rate;
                    bestSpot = surfSpot;
                }
            }

            if (bestSpot == null) lastAnswer = new Answer("IDKN");
            else {
                lastAnswer = new Answer(
                        prefix + bestSpot.getShortName(),
                        prefix + bestSpot.getShortName()
                );
//                lastAnswer.waitForReply = true;
                lastAnswer.replyVariants = new String[]{
                        "[spot]" + bestSpot.getShortName() + " " + intDayToNL(plusDays),
                        "[cond]Conditions will be?",
                        "[q]Where to surf now?",
                        plusDays == 0 ? "[q]Tomorrow?" : "[q]Today?",
                        c.time > 12 * 60 ? "[q]At sunrise?" : "[q]At sunset?",
                        "-[ok]Ok, thanks"
                };
                lastAnswer.replyInterpreters = new String[]{
                        "kw:conditions - Conditions in " + bestSpot.getShortName() + " " + intDayToNL(plusDays) + " " + intTimeToNL(c.time),
                        "time - Where to surf " + intDayToNL(plusDays) + " [time]?",
                        "day - Where to surf [day]?",
                        "time,day - Where to surf [day] [time]?"
                };
            }
        }

        commandSelectSpotAndDay(bestSpot, plusDays);

        return lastAnswer;
    }


    private void commandSelectSpotAndDay(SurfSpot surfSpot, Integer day) {
        Runnable rDay = null;

        if (day != null) {
            Integer finalDay = day;
            rDay = () -> mainModel.mainActivity.performSelectDay(finalDay);
        }

        if (surfSpot != null) {
            mainModel.mainActivity.performSelectSpot(surfSpot, rDay);
        } else {
            if (rDay != null) rDay.run();
        }
    }


    private Answer commandCamera(Command c) {
        if (c.spot != null) {
            if (c.spot.urlCam != null && !c.spot.urlCam.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.spot.urlCam));
                mainModel.mainActivity.startActivity(browserIntent);
            } else {
                lastAnswer = new Answer(
                        "No camera for " + c.spot.name + ". Choose spot:",
                        "For what spot?",
                        true,
                        new String[]{"[spot]Pererenan", "[spot]Old man's", "[spot]Bingin", "[spot]Uluwatu", "[spot]Keramas", "-[close]Cancel"},
                        new String[]{"spot - Camera for [spot]", " - Camera"}
                );
            }
        } else {
            lastAnswer = new Answer(
                    "Choose spot:",
                    "For what spot?",
                    true,
                    new String[]{"[spot]Pererenan", "[spot]Old man's", "[spot]Bingin", "[spot]Uluwatu", "[spot]Keramas", "-[close]Cancel"},
                    new String[]{"spot - Camera for [spot]", " - Camera"}
            );
        }
        return lastAnswer;
    }


    private Answer askForUsersExperience() {
        lastAnswer = new Answer(
                "How good are you?",
                "How good are you?",
                new String[]{
                        "newbie",
                        "beginner",
                        "intermediate",
                        "experienced"}
        );
        return lastAnswer;
    }

    private Answer askForModifyFavoriteSpots() {
        lastAnswer = new Answer(
                "May I mark with a star some spots, that fits for you?",
                "May I mark with a star some spots, that fits for you?",
                new String[]{
                        "yep",
                        "no, thanks"}
        );
        return lastAnswer;
    }


    private Answer commandConditions(Command c) {
        lastAnswer = new Answer();

        List<String> keywords = c.keywords;

        int nowTimeInt = Common.getNowTimeInt(Common.TIME_ZONE);

        boolean forNowTime = c.time != null && c.time == -1;

        Integer time = c.time;
        if (time == null && (c.day == null || c.day == 0)) {
            time = nowTimeInt;
            forNowTime = true;

            lastAnswer.addClarification("Now");
            lastAnswer.add(new Answer(
                    null,
                    "Now"
            ), true);
        }

        if (keywords == null || !(keywords.contains("conditions") || keywords.contains("waves") || keywords.contains("swell") || keywords.contains("wind") || keywords.contains("tide"))) {
            lastAnswer.addClarification("conditions");
            keywords = new ArrayList<>();
            keywords.add("conditions");
        }

        int plusDays = c.day == null ? 0 : c.day;

        SurfSpot surfSpot = c.spot;

        if (surfSpot == null) {
            surfSpot = mainModel.getSelectedSpot();

            String shortName = surfSpot.getShortName();
            lastAnswer.addClarification("in " + shortName);
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
                if (nowTimeInt > 18 * 60) {
                    lastAnswer.replyVariants = new String[]{
                            "[spot]" + surfSpot.getShortName() + " " + intDayToNL(plusDays),
                            "[q]Where to surf tomorrow?",
                            "[q]Where to surf tomorrow at sunrise?",
                            "[q]Where to surf tomorrow at sunset?",
                            "-[ok]Ok, thanks"};
                } else {
                    lastAnswer.replyVariants = new String[]{
                            "[spot]" + surfSpot.getShortName() + " " + intDayToNL(plusDays),
                            "[q]Where to surf now?",
                            "[q]Where to surf today?",
                            "[q]Where to surf tomorrow?",
                            "-[ok]Ok, thanks"};
                }
                lastAnswer.replyInterpreters = new String[]{
                        "kw:ok - Hide ai",
                        "day,time,spot - Conditions [day] [time] for [spot]",
                        "day,time - Conditions [day] [time]",
                        "day - Conditions [day]",
                        "time - Conditions [time] " + intDayToNL(plusDays),
                        "kw:tide - Tide for " + surfSpot.getShortName() + " " + intTimeToNL(time) + " " + intDayToNL(plusDays),
                        "kw:swell - Swell in " + surfSpot.getShortName() + " " + intTimeToNL(time) + " " + intDayToNL(plusDays),
                        "kw:wind - Wind in " + surfSpot.getShortName() + " " + intTimeToNL(time) + " " + intDayToNL(plusDays)
                };
            }
        } else {
            // TODO ans for unspecified time
            lastAnswer = new Answer();
        }
        return lastAnswer;
    }
}
