package com.avaa.surfforecast.ai;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alan on 18 Jan 2017.
 */

public class VoiceRecognitionHelper {
    private static final String TAG = "VoiceRecognitionHelper";


    private static final String SOUND_LIKE_DAY =
            "Friday - friendly";

    private static final String SOUND_LIKE_KEYWORD =
            "wind - winged" + "\n" +
                    "swell - swag,swallow,12" + "\n" +
                    "tide - dyed,guide,died,diet,dyed" + "\n" +
                    "conditions - can you show us,weather,auditions" + "\n" +
                    "waves" + "\n" +
                    "camera - cam,canara,canada,kamera" + "\n" +
                    "in meters - in metres" + "\n" +
                    "cancel - forget it,exit,close" + "\n" +
                    "ok - good" + "\n" +
                    "where to surf - go to surf,where's the surf,where's the service,why the surf,where to go,where's the seraph,where to serve,where are to surf,what to serve" + "\n" +
                    "best spot - mess for,best for,best book,best butt,best sport,best sports,baysport,passport at" + "\n" +
//            "best waves"  + "\n" +
                    "what's up here - what's up there,what's a panera,what's on there,what's out there" + "\n" +
                    "repeat" + "\n" +
//            "don't want to surf - don't want to go to" + "\n" +
//            "suggest something else" + "\n" +
//                    "never suggest" + "\n" +

//                    "no - noup" + "\n" +
//                    "yes - yep,yeah" + "\n" +
//
//                    "newbie" + "\n" +
//                    "beginner" + "\n" +
//                    "intermediate" + "\n" + //skilled
//                    "experienced" + "\n" + //advanced
//                    "professional" + "\n" +

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
            "Bingin - in-game,in gimp,mingin,minghin,mean king,new king,eugene,didn't get,bending,binging,ben king,ninjin,engine,bingen,benjen,bingeon,binging" + "\n" +
            "Impossibles - impossible,imposibles,kim possible,kim possible's" + "\n" +
            "Padang-Padang - padang padang,pandanan,adam adam,adam,badung,banana banana,badonkadonk,padam padam,button button" + "\n" +
            "Uluwatu - hello otto,hello auto,palo alto,hello weather,all weather,all the weather,blue waffle,hello motto,aluminum,uidaho,all idaho,old idaho,idaho,lovato,all lotto,or lotto,whole lotta,holy water" + "\n" +
            "Nyang-Nyang - nyang nyang,yum yum,yum-yum,young young,yin yang,yang yang,young,yung" + "\n" +
            "Green Ball - green bow,green bowl,rainbow,greenbo,greenbow,green bay,greenville" + "\n" +
            "Nusa Dua - methadone,north ottawa,north andover,los angela,rosangela,massage oil,what's a doula,missoula,north abdullah,i said dora,wilson duo,minnesota,methadone,mistletoe,miss angela" + "\n" +
            "Sri Lanka - srilanka,shri lanka,lanka,blanca,blanka" + "\n" +
            "Serangan - sarin gas,sharon van,sarin gun,theron gun,saron gun,sarin gun,seren gun,so i'm done,saran gun,sorry i'm gone,surround gun,sauron gun,birmingham,shotgun,sharingan,should i come,set an alarm,7 gun,sean gunn,set alarm,should i run,savannah,sheeran,710,cnn" + "\n" +
            "Tandjung left's - tandjung,tanjung,dungeon,on june" + "\n" +
            "Sanur Reef - summer,son of" + "\n" +
            "Keramas - chatham mass,can i mass,can i mas,can i must" + "\n" +
            "Padangbai - padang bay,button bay,badung bay,cabana Bay,banana bay" + "\n" +
            "Shipwrecks - shipwrecked,ship wrecks,suprax,cheap hats,chipwrecked" + "\n" +
            "Lacerations - playstations,playstation's,restorations,destinations,nice relations,need directions" + "\n" +
            "Playgrounds - playground,backgrounds,baker island,baker islands" + "\n" +
            "Ceningan - changing them,cannon gun,h&m,canyonlands,canyonland,canyon gun,canyon land,chain gun,kenyon gun,kenyan gun";


    private final CommandsExecutor commandsExecutor;

    private final Map<String, String> soundLikeSpot = new HashMap<>();

    public final Map<String, String> soundLikeKeyword = new HashMap<>();

    private final Map<String, String> soundLikeDay = new HashMap<>();
    public final Map<String, String> soundLikeTimeOfDay = new HashMap<>();


    public VoiceRecognitionHelper(CommandsExecutor commandsExecutor) {
        this.commandsExecutor = commandsExecutor;

        initSoundLikeMap(SOUND_LIKE_SPOT, soundLikeSpot);
        initSoundLikeMap(SOUND_LIKE_KEYWORD, soundLikeKeyword);
        initSoundLikeMap(SOUND_LIKE_DAY, soundLikeDay);
        initSoundLikeMap(SOUND_LIKE_TIME_OF_DAY, soundLikeTimeOfDay);
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


    public String toStringCommand(Collection<String> strings) {
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

            for (String sj : commandsExecutor.sToSpot.keySet()) {
                if (ss.contains(sj)) {
                    spothits.add(sj.trim());
                    ss = ss.replace(sj, " ");
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeSpot.entrySet()) {
                if (ss.contains(e.getKey())) {
                    slSpothits.add(e);
                    ss = ss.replace(e.getKey(), " ");
                    break;
                }
            }

            for (String sj : commandsExecutor.sToDay.keySet()) {
                if (ss.contains(sj)) {
                    dayhits.add(sj.trim());
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeDay.entrySet()) {
                if (ss.contains(e.getKey())) slDayhits.add(e);
            }

            for (String sj : commandsExecutor.sToTime.keySet()) {
                if (ss.contains(sj)) {
                    timehits.add(sj.trim());
                    break;
                }
            }
            for (Map.Entry<String, String> e : soundLikeTimeOfDay.entrySet()) {
                if (ss.contains(e.getKey())) slTimeHits.add(e);
            }

            for (Map.Entry<String, String> e : soundLikeKeyword.entrySet()) {
                if (ss.contains(e.getKey())) slKeywordHits.add(e);
            }
        }

        String s = null;

        if (!spothits.isEmpty()) s = spothits.get(0);
        else if (!slSpothits.isEmpty()) s = slSpothits.get(0).getValue();

        if (!slKeywordHits.isEmpty())
            s = s == null ? slKeywordHits.get(0).getValue() : s + " " + slKeywordHits.get(0).getValue();

        if (!dayhits.isEmpty()) s = s == null ? dayhits.get(0) : s + " " + dayhits.get(0);
        else if (!slDayhits.isEmpty())
            s = s == null ? slDayhits.get(0).getValue() : s + " " + slDayhits.get(0).getValue();

        if (!(!dayhits.isEmpty() && dayhits.contains("now") || dayhits.isEmpty() && !slDayhits.isEmpty() && "now".equals(slDayhits.get(0)))) {
            if (!timehits.isEmpty()) s = s == null ? timehits.get(0) : s + " at " + timehits.get(0);
            else if (!slTimeHits.isEmpty())
                s = s == null ? slTimeHits.get(0).getValue() : s + " at " + slTimeHits.get(0).getValue();
        }

        if (s != null) {
            Log.i(TAG, "                    | spot = " + spothits);
            Log.i(TAG, "                    | spot = " + slSpothits);
            Log.i(TAG, "                    | keyw = " + slKeywordHits);
            Log.i(TAG, "                    | day  = " + dayhits);
            Log.i(TAG, "                    | day  = " + slDayhits);
            Log.i(TAG, "                    | time = " + timehits);
            Log.i(TAG, "                    | time = " + slTimeHits);
        }
        Log.i(TAG, "  toStringCommand() | command = '" + s + "'");

        return CommandsExecutor.capitalize(s);
    }
}
