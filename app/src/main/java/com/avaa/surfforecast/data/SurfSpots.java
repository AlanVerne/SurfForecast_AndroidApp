package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Handler;

import com.avaa.surfforecast.MainModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Created by Alan on 12 Jul 2016.
 */


public class SurfSpots {
    private static final String TAG = "SurfSpots";

    private static final String SPKEY_FAV_SPOTS = "favSpots";

    public static final String WADD = "WADD";


    public SurfSpot get(int i) {
        return list.get(i);
    }

    public List<SurfSpot> getAll() {
        return list;
    }


    public Set<SurfSpot> getFavoriteSurfSpotsList() {
        Set<SurfSpot> favSpots = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).favorite) favSpots.add(list.get(i));
        }
        return favSpots;
    }


    public int indexOf(SurfSpot surfSpot) {
        return list.indexOf(surfSpot);
    }


    public Set<String> getFavorite() {
        Set<String> favSpots = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).favorite) favSpots.add(String.valueOf(i));
        }
        return favSpots;
    }

    public void setFavorite(int i, boolean b) {
        SharedPreferences sp = MainModel.instance.sharedPreferences;
        list.get(i).favorite = b;
        sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
    }

    public void setFavorite(SurfSpot surfSpot, boolean b) {
        SharedPreferences sp = MainModel.instance.sharedPreferences;
        surfSpot.favorite = b;
        sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
    }

    public void swapFavorite(int i) {
        setFavorite(i, !list.get(i).favorite);
    }

    public void swapFavorite(SurfSpot surfSpot) {
        setFavorite(surfSpot, !surfSpot.favorite);
    }


    private List<SurfSpot> list = new ArrayList<>();
    public Map<Integer, SpotsArea> areas = new TreeMap<>();


    public SurfSpots() {
        initSpots();
    }

    public void initSpots() {
        areas.put(list.size(), new SpotsArea("Miles and miles north-west", new PointF(577, 510)));
        addSpot("Medewi", "Medewi", new PointF(480, 463), new PointF(0, 0),
                Direction.NNE, 1, 1 + 2 + 4, 2, 7, "http://magicseaweed.com/Medewi-Surf-Report/1135/", "", -8.421528, 114.805771);
        addSpot("Balian", "Balian", new PointF(675, 569), new PointF(0, 0),
                Direction.NE, 0, 1 + 2 + 4, 2, 7, "http://magicseaweed.com/Balian-Surf-Report/4009/", "", -8.503239, 114.965390);

        areas.put(list.size(), new SpotsArea("Canggu", new PointF(843, 730)));
        //addSpot("Pererenan", "Pererenan"), new PointF(833, 722), new PointF(0, 0), Direction.NE);
        addSpot("Echo / Pererenan", new String[]{"Canggu", "Echo", "Pererenan"}, "Canggu", new PointF(837, 726), new PointF(0, 0),
                Direction.NE, 0, 1 + 2 + 4, 3, 8, "http://magicseaweed.com/Canggu-Surf-Report/935/", "http://balibelly.com/canggu", -8.654989, 115.125030);
        lastSpot().labelLeft = true;
        addSpot("Old man's / BB", new String[]{"Old mans", "Old man's", "Oldman", "Old men", "Old man", "Batu Bolong", "Batu"}, "Canggu", new PointF(841, 730), new PointF(0, 0),
                Direction.NE, 0, 1 + 2 + 4, 2, 7, "http://magicseaweed.com/Old-Mans-Batu-Bolong-Surf-Report/2305/", "http://oldmans.net/#surfcam-popup", -8.659556, 115.130200);
        addSpot("Berawa", new String[]{"Brava"}, "Canggu", new PointF(853, 740), new PointF(0, 0),
                Direction.NE, 2, 1 + 2 + 4, 2, 6, "http://magicseaweed.com/Berawa-Beach-Surf-Report/1293/", "", -8.667381, 115.139262);
        lastSpot().labelLeft = true;

        areas.put(list.size(), new SpotsArea("Seminyak - Kuta", new PointF(870, 800)));
        //addSpot("Blue Ocean", "Blue-Ocean"), new PointF(856, 742), new PointF(0, 0), Direction.ENE);
        addSpot("Padma", "Padma", new PointF(869, 758), new PointF(0, 0),
                Direction.NE, 0, 1 + 2 + 4, 1, 6, "http://magicseaweed.com/Padma-Surf-Report/4005/", "", -8.705259, 115.165101);
        addSpot("Seminyak", "Halfway", new PointF(886, 784), new PointF(0, 0),
                Direction.NE, 2, 1 + 2 + 4, 1, 6, "http://magicseaweed.com/Seminyak-Surf-Report/1294/", "", -8.692632, 115.158116);
        lastSpot().labelLeft = true;
        //addSpot("Halfway", "Halfway"), new PointF(887, 784), new PointF(0, 0), Direction.E, 1, 1+2+4, 1, 8);
        addSpot("Kuta Beach", new String[]{"Kuta"}, "Kuta-Beach", new PointF(891, 798), new PointF(0, 0),
                Direction.ENE, 2, 1 + 2 + 4, 1, 6, "http://magicseaweed.com/Kuta-Beach-Surf-Report/566/", "http://balibelly.com/kuta", -8.717815, 115.168486);
        addSpot("Kuta Reef", "Kuta-Reef", new PointF(878, 825), new PointF(0, 0), Direction.ENE, 1, 2 + 4, 3, 6, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.731291, 115.157751);
        //addSpot("Legian Beach", "Legian-Beach"), new PointF(872, 826), new PointF(0, 0), Direction.ENE);
        addSpot("Airport left's", new String[]{"Airport", "Airport left"}, "Airport-Lefts", new PointF(876, 835), new PointF(0, 0),
                Direction.E, 1, 2 + 4, 3, 8, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.740112, 115.150799);
        lastSpot().labelLeft = true;
        addSpot("Airport right", new String[]{"Airport right's"}, "Airport-Rights_2", new PointF(877, 847), new PointF(0, 0),
                Direction.E, -1, 2 + 4, 3, 7, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.757235, 115.154246);

        areas.put(list.size(), new SpotsArea("Bukit west", new PointF(820, 930)));
        addSpot("Balangan", "Balangan", new PointF(838, 894), new PointF(0, 0),
                Direction.SE, 1, 1 + 2 + 4, 4, 8, "http://magicseaweed.com/Balangan-Surf-Report/2304/", "", -8.792450, 115.120989);
        lastSpot().labelLeft = true;
        addSpot("Dreamland", new String[]{"Dream Land"}, "Dreamland", new PointF(833, 901), new PointF(0, 0),
                Direction.SE, 0, 1 + 2, 2, 8, "http://magicseaweed.com/Dreamland-Surf-Report/2301/", "", -8.799007, 115.116690);
        addSpot("Bingin", "Bingin", new PointF(831, 907), new PointF(0, 0),
                Direction.SE, 1, 2, 3, 9, "http://magicseaweed.com/Bingin-Surf-Report/878/", "http://balibelly.com/bingin", -8.805410, 115.111312);
        lastSpot().labelLeft = true;
        addSpot("Impossibles", "Bingin", new PointF(819, 913), new PointF(0, 0),
                Direction.SE, 1, 1 + 2 + 4, 4, 8, "http://magicseaweed.com/Impossibles-Surf-Report/2302/", "", -8.808615, 115.104613);
        addSpot("Padang-Padang", new String[]{"Padang Padang", "Padang"}, "Bingin", new PointF(815, 915), new PointF(0, 0),
                Direction.SE, 1, 2 + 4, 5, 9, "http://magicseaweed.com/Padang-Padang-Surf-Report/1121/", "", -8.809937, 115.100767);
        lastSpot().labelLeft = true;
        addSpot("Uluwatu", new String[]{"Blue Point"}, "Uluwatu", new PointF(796, 923), new PointF(0, 0),
                Direction.SE, 1, 1 + 2 + 4, 2, 9, "http://magicseaweed.com/Uluwatu-Surf-Report/565/", "http://balibelly.com/uluwatu", -8.815899, 115.086159);
        lastSpot().labelLeft = true;
        addSpot("Nyang-Nyang", new String[]{"Nyang Nyang", "Nyang"}, "Uluwatu", new PointF(802, 953), new PointF(0, 0),
                Direction.NE, -1, 2 + 4, 2, 6, "http://magicseaweed.com/Nyang-Nyang-Surf-Report/2315/", "", -8.841612, 115.094292);

        areas.put(list.size(), new SpotsArea("Bukit east", new PointF(920, 915)));
        addSpot("Green Ball", new String[]{"Green bowl"}, "Green-Ball", new PointF(898, 965), new PointF(0, 0),
                Direction.N, -1, 2 + 4, 2, 6, "http://magicseaweed.com/Green-Ball-Surf-Report/2320/", "", -8.849996, 115.171464);
        addSpot("Nusa Dua", "Nusadua", new PointF(970, 925), new PointF(0, 0),
                Direction.NW, 0, 1 + 2 + 4, 3, 9, "http://magicseaweed.com/Nusa-Dua-Surf-Report/564/", "", -8.818622, 115.231821);
        addSpot("Sri Lanka", new String[]{"Sri Lanka", "lanka"}, "Sri-Lanka", new PointF(967, 881), new PointF(0, 0),
                Direction.SW, -1, 2 + 4, 4, 8, "http://magicseaweed.com/Sri-Lanka-Surf-Report/2312/", "", -8.788045, 115.233243);

        areas.put(list.size(), new SpotsArea("Sanur", new PointF(995, 790)));
        addSpot("Serangan", "Sanur-Reef", new PointF(990, 828), new PointF(0, 0),
                Direction.NW, 0, 1 + 2 + 4, 2, 9, "http://magicseaweed.com/Serangan-Surf-Report/2319/", "", -8.743074, 115.243055); // !!
        //addSpot("Hyatt Reef", "Sanur-Grand-Hyatt"), new PointF(1011, 793), new PointF(0, 0), Direction.W);
        //addSpot("Ketewel", "Ketewel"), new PointF(1011, 793), new PointF(0, 0), Direction.NNW, "http://magicseaweed.com/Ketewel-Surf-Report/4008/", "");
        addSpot("Tandjung right's", "Tandjung-Rights", new PointF(1015, 770), new PointF(0, 0),
                Direction.NW, -1, 1 + 2 + 4, 3, 8, "http://magicseaweed.com/Tanjung-Sari-Surf-Report/2313/", "", -8.691786, 115.270863);
        addSpot("Tandjung left's", new String[]{"Tandjung", "Tanjung"}, "Tandjung-Lefts", new PointF(1012, 760), new PointF(0, 0),
                Direction.NW, 1, 1 + 2 + 4, 3, 8, "http://magicseaweed.com/Tanjung-Sari-Surf-Report/2313/", "", -8.698470, 115.270707);
        lastSpot().labelLeft = true;
        addSpot("Sanur Reef", new String[]{"Sanur"}, "Sanur-Reef", new PointF(1011, 749), new PointF(0, 0),
                Direction.W, -1, 2 + 4, 4, 9, "http://magicseaweed.com/Sanur-Surf-Report/1272/", "", -8.672768, 115.266042);

        areas.put(list.size(), new SpotsArea("East", new PointF(1200, 615)));
//        addSpot("Padang Galak", "Ketewel"), new PointF(1040, 712), new PointF(0, 0), Direction.W);
        //addSpot("Ketewel", "Ketewel"), new PointF(1078, 683), new PointF(0, 0), Direction.NNW); //!! it is on MSW
//        addSpot("Lebih", "Ketewel"), new PointF(1150, 637), new PointF(0, 0), Direction.W);
        addSpot("Keramas", "Keramas-Beach", new PointF(1116, 650), new PointF(0, 0),
                Direction.NW, -1, 1 + 2 + 4, 2, 9, "http://magicseaweed.com/Keramas-Surf-Report/909/", "http://balibelly.com/keramas", -8.587816, 115.351592);
        addSpot("Padangbai", new String[]{"Padang Bay"}, "Padangbai", new PointF(1306, 585), new PointF(0, 0),
                Direction.W, -1, 2, 3, 6, "http://magicseaweed.com/Padangbai-Surf-Report/4010/", "", -8.535793, 115.511652);

        areas.put(list.size(), new SpotsArea("Lembongan", new PointF(1228, 752)));
        addSpot("Shipwrecks", new String[]{"Ship Wrecks"}, "Shipwrecks", new PointF(1226, 740), new PointF(0, 0),
                Direction.E, -1, 2 + 4, 3, 8, "http://magicseaweed.com/Shipwrecks-Lembongan-Surf-Report/1088/", "", -8.664195, 115.442830);
        addSpot("Lacerations", "Lacerations", new PointF(1224, 750), new PointF(0, 0),
                Direction.E, -1, 2 + 4, 3, 8, "http://magicseaweed.com/Lacerations-Surf-Report/1090/", "", -8.671239, 115.441876);
        lastSpot().labelLeft = true;
        addSpot("Playgrounds", new String[]{"Playground"}, "Playgrounds", new PointF(1225, 754), new PointF(0, 0),
                Direction.E, 0, 1 + 2 + 4, 2, 7, "http://magicseaweed.com/Playgrounds-Surf-Report/1089/", "", -8.675822, 115.440853);
        addSpot("Ceningan", "Ceningan-Point", new PointF(1217, 786), new PointF(0, 0),
                Direction.E, 1, 1 + 2 + 4, 3, 8, "http://magicseaweed.com/Ceningan-Surf-Report/2311/", "", -8.702750, 115.437421);
        lastSpot().labelLeft = true;

        for (int i = 2; i < list.size() - 6; i++) {
            list.get(i).metarName = WADD;
        }
        for (int i = 25; i < list.size(); i++) {
            list.get(i).tidePortID = Common.SANUR_PORT_ID;
        }
    }

    public void updateFavorite() {
        for (SurfSpot surfSpot : getFavoriteSurfSpotsList()) {
            surfSpot.conditionsProvider.updateIfNeed();
        }
    }


    public SpotsArea getArea(int spotI) {
        int k = 0;
        for (Integer i : areas.keySet()) {
            if (i < spotI) {
                k = i;
            } else {
                break;
            }
        }
        return areas.get(k);
    }


    public void init() {
        final MainModel mainModel = MainModel.instance;
        final SharedPreferences sp = mainModel.sharedPreferences;

        Set<String> favSpots = sp.getStringSet(SPKEY_FAV_SPOTS, null);
        if (favSpots != null) {
            for (String favSpot : favSpots) {
                Integer integer = Integer.decode(favSpot);
//                Log.i(TAG, favSpot + " " + integer);
                if (integer != null) list.get(integer).favorite = true;
            }
        } else {
            list.get(3).favorite = true;
            list.get(7).favorite = true;
            list.get(16).favorite = true;
            list.get(19).favorite = true;
            list.get(21).favorite = true;
            sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
        }

        final Handler handler = new Handler();
        handler.postDelayed(this::updateFavorite, 10000);
    }


    private void addSpot(SurfSpot surfSpot) {
        list.add(surfSpot);
    }

//    private void addSpot(String name, SurfConditionsProvider provider, PointF onSVG, PointF onMap, Direction direction) {
//        addSpot(new SurfSpot(name, provider, onSVG, onMap, direction));
//    }
//    private void addSpot(String name, SurfConditionsProvider provider, PointF onSVG, PointF onMap, Direction direction, int lr) {
//        addSpot(new SurfSpot(name, provider, onSVG, onMap, direction, lr));
//    }
//    private void addSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell) {
//        addSpot(new SurfSpot(name, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell));
//    }
//    private void addSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String msw, String sf) {
//        addSpot(new SurfSpot(name, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell, msw, sf, 0, 0));
//    }

    private void addSpot(String name, String conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String msw, String urlCam, double la, double lo) {
        addSpot(new SurfSpot(name, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell, msw, urlCam, la, lo));
    }

    private void addSpot(String name, String[] alt, String conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String msw, String urlCam, double la, double lo) {
        addSpot(new SurfSpot(name, alt, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell, msw, urlCam, la, lo));
    }
    
    private SurfSpot lastSpot() {
        return list.get(list.size()-1);
    }
}
