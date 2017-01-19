package com.avaa.surfforecast.data;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.Log;

import com.avaa.surfforecast.UsageStat;

import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String SPKEY_SELECTED_SPOT = "selectedSpot";

    public static final String WADD = "WADD";


    public static SurfSpots getInstance() {
        return instance;
    }
    public static SurfSpots getInstance(SharedPreferences sp, BusyStateListener bsl) {
        if (instance == null) instance = new SurfSpots(sp, bsl);
        return instance;
    }


    public int selectedSpotI = -1;
    public void setSelectedSpotI(int i) {
        if (selectedSpotI == i) return;
        //Log.i("SurfSpots", "setSelectedSpotI() 1");
        selectedSpotI = i;
        updateCurrentConditions(false);
        //Log.i("SurfSpots", "setSelectedSpotI() 2");
        UsageStat.getInstance().incrementSpotsShownCount();
        fireChanged(new HashSet<Change>(){{add(Change.SELECTED_SPOT);add(Change.CONDITIONS);add(Change.CURRENT_CONDITIONS);}});
        sp.edit().putInt(SPKEY_SELECTED_SPOT, selectedSpotI).apply();
    }
    public SurfSpot selectedSpot() {
        if (selectedSpotI == -1 || selectedSpotI >= list.size()) return null;
        return list.get(selectedSpotI);
    }


    public SurfSpot get(int i) {
        return list.get(i);
    }
    public List<SurfSpot> getAll() {
        return list;
    }


    public int indexOf(SurfSpot surfSpot) {
        return list.indexOf(surfSpot);
    }


//    public void updateAll() {
//        for (SurfSpot surfSpot : list) {
//            surfSpot.conditionsProvider.update();
//        }
//    }


    public SurfConditions currentConditions = null;
    public METAR currentMETAR = null;
//    public TideData currentTideData = null; // unsupported
    public void updateCurrentConditions() {
        updateCurrentConditions(true);
    }
    public void updateCurrentConditions(boolean fire) {
        SurfSpot spot = selectedSpot();

        if (spot == null) return;

        SurfConditions newCC = spot.conditionsProvider.getNow();
        METAR newMETAR = METARProvider.getInstance().get(spot.metarName);

        if (newCC == currentConditions && newMETAR == currentMETAR) return;

        currentConditions = newCC;
        currentMETAR = newMETAR;

        Log.i(TAG, newMETAR == null ? "null" : newMETAR.toString());

        if (fire) fireChanged(new HashSet<Change>(){{add(Change.CURRENT_CONDITIONS);}});
    }


    public interface ChangeListener {
        void onChange(Set<Change> changes);
    }
    public enum Change { SELECTED_SPOT, CONDITIONS, CURRENT_CONDITIONS }
    private Map<ChangeListener, Set<Change>> cls = new HashMap<>();
    public void addChangeListener(ChangeListener l) {
        cls.put(l, null);
    }
    public void addChangeListener(ChangeListener l, Set<Change> changes) {
        cls.put(l, changes);
    }


    public Set<String> getFavorite() {
        Set<String> favSpots = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).favorite) favSpots.add(String.valueOf(i));
        }
        return favSpots;
    }
    public void setFavorite(int i, boolean b) {
        list.get(i).favorite = b;
        sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
    }
    public void setFavorite(SurfSpot surfSpot, boolean b) {
        surfSpot.favorite = b;
        sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
    }
    public void swapFavorite(int i) {
        setFavorite(i, !list.get(i).favorite);
    }
    public void swapFavorite(SurfSpot surfSpot) {
        setFavorite(surfSpot, !surfSpot.favorite);
    }


    private static SurfSpots instance = null;
    private List<SurfSpot> list = new ArrayList<>();
    public Map<Integer, String> categories = new TreeMap<>();

    private final SharedPreferences sp;
    private final BusyStateListener bsl;

    private SurfSpots(SharedPreferences sp, BusyStateListener bsl) {
        this.sp = sp;
        this.bsl = bsl;

        categories.put(list.size(), "Miles and miles north-west");
        addSpot("Medewi", new SurfConditionsProvider(sp, "Medewi"), new PointF(484, 460), new PointF(0, 0), Direction.NNE, 1, 1+2+4, 2, 10, "http://magicseaweed.com/Medewi-Surf-Report/1135/", "", -8.421528, 114.805771);
        addSpot("Balian", new SurfConditionsProvider(sp, "Balian"), new PointF(677, 568), new PointF(0, 0), Direction.NE, 0, 1+2+4, 2, 12, "http://magicseaweed.com/Balian-Surf-Report/4009/", "", -8.503239, 114.965390);

        categories.put(list.size(), "Canggu");
        //addSpot("Pererenan", new SurfConditionsProvider(sp, "Pererenan"), new PointF(833, 722), new PointF(0, 0), Direction.NE);
        addSpot("Canggu / Echo", new String[]{"Canggu", "Echo"}, new SurfConditionsProvider(sp, "Canggu"), new PointF(840, 726), new PointF(0, 0), Direction.NE, 0, 1+2+4, 3, 10, "http://magicseaweed.com/Canggu-Surf-Report/935/", "http://balibelly.com/canggu", -8.654989, 115.125030);
        addSpot("Old man's / BB", new String[]{"Old mans", "Old man's", "Oldman", "Old men", "Old man", "Batu Bolong", "Batu"}, new SurfConditionsProvider(sp, "Canggu"), new PointF(843, 730), new PointF(0, 0), Direction.NE, 0, 1+2+4, 2, 10, "http://magicseaweed.com/Old-Mans-Batu-Bolong-Surf-Report/2305/", "http://oldmans.net/#surfcam-popup", -8.659556, 115.130200);
        addSpot("Berawa", new String[]{"Brava"}, new SurfConditionsProvider(sp, "Canggu"), new PointF(845, 734), new PointF(0, 0), Direction.NE, 2, 1+2+4, 2, 8, "http://magicseaweed.com/Berawa-Beach-Surf-Report/1293/", "", -8.667381, 115.139262);

        categories.put(list.size(), "Seminyak - Kuta");
        //addSpot("Blue Ocean", new SurfConditionsProvider(sp, "Blue-Ocean"), new PointF(856, 742), new PointF(0, 0), Direction.ENE);
        addSpot("Padma", new SurfConditionsProvider(sp, "Padma"), new PointF(867, 758), new PointF(0, 0), Direction.NE, 0, -1, -1, -1, "http://magicseaweed.com/Padma-Surf-Report/4005/", "", -8.705259, 115.165101);
        addSpot("Seminyak", new SurfConditionsProvider(sp, "Halfway"), new PointF(887, 784), new PointF(0, 0), Direction.NE, 2, 1+2+4, 1, 8, "http://magicseaweed.com/Seminyak-Surf-Report/1294/", "", -8.692632, 115.158116);
        //addSpot("Halfway", new SurfConditionsProvider(sp, "Halfway"), new PointF(887, 784), new PointF(0, 0), Direction.E, 1, 1+2+4, 1, 8);
        addSpot("Kuta Beach", new String[]{"Kuta"}, new SurfConditionsProvider(sp, "Kuta-Beach"), new PointF(887, 798), new PointF(0, 0), Direction.ENE, 2, 1+2+4, 1, 8, "http://magicseaweed.com/Kuta-Beach-Surf-Report/566/", "http://balibelly.com/kuta", -8.717815, 115.168486);
        addSpot("Kuta Reef", new SurfConditionsProvider(sp, "Kuta-Reef"), new PointF(864, 825), new PointF(0, 0), Direction.ENE, 1, 2+4, 3, 6, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.731291, 115.157751);
        //addSpot("Legian Beach", new SurfConditionsProvider(sp, "Legian-Beach"), new PointF(872, 826), new PointF(0, 0), Direction.ENE);
        addSpot("Airport left's", new String[]{"Airport left", "Airport"}, new SurfConditionsProvider(sp, "Airport-Lefts"), new PointF(874, 836), new PointF(0, 0), Direction.E, 1, 2+4, 4, 6, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.740112, 115.150799);
        addSpot("Airport right", new String[]{"Airport right's"}, new SurfConditionsProvider(sp, "Airport-Rights_2"), new PointF(878, 849), new PointF(0, 0), Direction.E, -1, 2+4, 3, 6, "http://magicseaweed.com/Airport-Reef-Surf-Report/2309/", "", -8.757235, 115.154246);

        categories.put(list.size(), "Bukit west");
        addSpot("Balangan", new SurfConditionsProvider(sp, "Balangan"), new PointF(840, 894), new PointF(0, 0), Direction.SE, 1, 1+2+4, 4, 15, "http://magicseaweed.com/Balangan-Surf-Report/2304/", "", -8.792450, 115.120989);
        addSpot("Dreamland", new String[]{"Dream Land"}, new SurfConditionsProvider(sp, "Dreamland"), new PointF(830, 901), new PointF(0, 0), Direction.SE, 0, 1+2, 2, 10, "http://magicseaweed.com/Dreamland-Surf-Report/2301/", "", -8.799007, 115.116690);
        addSpot("Bingin", new SurfConditionsProvider(sp, "Bingin"), new PointF(822, 908), new PointF(0, 0), Direction.SE, 1, 2, 3, 8, "http://magicseaweed.com/Bingin-Surf-Report/878/", "http://balibelly.com/bingin", -8.805410, 115.111312);
        addSpot("Impossibles", new SurfConditionsProvider(sp, "Bingin"), new PointF(818, 913), new PointF(0, 0), Direction.SE, 1, 1+2+4, 4, 12, "http://magicseaweed.com/Impossibles-Surf-Report/2302/", "", -8.808615, 115.104613);
        addSpot("Padang-Padang", new String[]{"Padang Padang", "Padang"}, new SurfConditionsProvider(sp, "Bingin"), new PointF(809, 917), new PointF(0, 0), Direction.SE, 1, 2+4, 6, 12, "http://magicseaweed.com/Padang-Padang-Surf-Report/1121/", "", -8.809937, 115.100767);
        addSpot("Uluwatu", new SurfConditionsProvider(sp, "Uluwatu"), new PointF(795, 922), new PointF(0, 0), Direction.SE, 1, 1+2+4, 2, 15, "http://magicseaweed.com/Uluwatu-Surf-Report/565/", "http://balibelly.com/uluwatu", -8.815899, 115.086159);
        addSpot("Nyang-Nyang", new String[]{"Nyang Nyang", "Nyang"}, new SurfConditionsProvider(sp, "Uluwatu"), new PointF(802, 952), new PointF(0, 0), Direction.NE, -1, 2+4, 2, 6, "http://magicseaweed.com/Nyang-Nyang-Surf-Report/2315/", "", -8.841612, 115.094292);

        categories.put(list.size(), "Bukit east");
        addSpot("Green Ball", new String[]{"Green bowl"}, new SurfConditionsProvider(sp, "Green-Ball"), new PointF(898, 962), new PointF(0, 0), Direction.N, -1, 2, 2, 8, "http://magicseaweed.com/Green-Ball-Surf-Report/2320/", "", -8.849996, 115.171464);
        addSpot("Nusa Dua", new SurfConditionsProvider(sp, "Nusadua"), new PointF(981, 911), new PointF(0, 0), Direction.NW, 0, 1+2+4, 3, 20, "http://magicseaweed.com/Nusa-Dua-Surf-Report/564/", "", -8.818622, 115.231821);
        addSpot("Sri Lanka", new String[]{"lanka"}, new SurfConditionsProvider(sp, "Sri-Lanka"), new PointF(965, 881), new PointF(0, 0), Direction.SW, -1, 1+2, 4, 12, "http://magicseaweed.com/Sri-Lanka-Surf-Report/2312/", "", -8.788045, 115.233243);

        categories.put(list.size(), "Sanur");
        addSpot("Serangan", new SurfConditionsProvider(sp, "Sanur-Reef"), new PointF(985, 828), new PointF(0, 0), Direction.NW, 0, 1+2+4, 2, 15, "http://magicseaweed.com/Serangan-Surf-Report/2319/", "", -8.743074, 115.243055); // !!
        //addSpot("Hyatt Reef", new SurfConditionsProvider(sp, "Sanur-Grand-Hyatt"), new PointF(1011, 793), new PointF(0, 0), Direction.W);
        //addSpot("Ketewel", new SurfConditionsProvider(sp, "Ketewel"), new PointF(1011, 793), new PointF(0, 0), Direction.NNW, "http://magicseaweed.com/Ketewel-Surf-Report/4008/", "");
        addSpot("Tandjung left's", new String[]{"Tandjung", "Tanjung"}, new SurfConditionsProvider(sp, "Tandjung-Lefts"), new PointF(1014, 769), new PointF(0, 0), Direction.NW, 1, 1+2+4, 3, 10, "http://magicseaweed.com/Tanjung-Sari-Surf-Report/2313/", "", -8.698470, 115.270707);
        addSpot("Tandjung right's", new SurfConditionsProvider(sp, "Tandjung-Rights"), new PointF(1014, 769), new PointF(0, 0), Direction.NW, -1, 1+2+4, 3, 10, "http://magicseaweed.com/Tanjung-Sari-Surf-Report/2313/", "", -8.691786, 115.270863);
        addSpot("Sanur Reef", new String[]{"Sanur"}, new SurfConditionsProvider(sp, "Sanur-Reef"), new PointF(1009, 749), new PointF(0, 0), Direction.W, -1, 2+4, 6, 15, "http://magicseaweed.com/Sanur-Surf-Report/1272/", "", -8.672768, 115.266042);

        categories.put(list.size(), "East");
//        addSpot("Padang Galak", new SurfConditionsProvider(sp, "Ketewel"), new PointF(1040, 712), new PointF(0, 0), Direction.W);
        //addSpot("Ketewel", new SurfConditionsProvider(sp, "Ketewel"), new PointF(1078, 683), new PointF(0, 0), Direction.NNW); //!! it is on MSW
//        addSpot("Lebih", new SurfConditionsProvider(sp, "Ketewel"), new PointF(1150, 637), new PointF(0, 0), Direction.W);
        addSpot("Keramas", new SurfConditionsProvider(sp, "Keramas-Beach"), new PointF(1116, 650), new PointF(0, 0), Direction.NW, -1, 1+2+4, 2, 12, "http://magicseaweed.com/Keramas-Surf-Report/909/", "http://balibelly.com/keramas", -8.587816, 115.351592);
        addSpot("Padangbai", new String[]{"Padang Bay"}, new SurfConditionsProvider(sp, "Padangbai"), new PointF(1306, 585), new PointF(0, 0), Direction.W, -1, 2, 3, 6, "http://magicseaweed.com/Padangbai-Surf-Report/4010/", "", -8.535793, 115.511652);

        categories.put(list.size(), "Lembongan");
        addSpot("Shipwrecks", new String[]{"Ship Wrecks"}, new SurfConditionsProvider(sp, "Shipwrecks"), new PointF(1245, 735), new PointF(0, 0), Direction.E, -1, 2+4, 3, 10, "http://magicseaweed.com/Shipwrecks-Lembongan-Surf-Report/1088/", "", -8.664195, 115.442830);
        addSpot("Lacerations", new SurfConditionsProvider(sp, "Lacerations"), new PointF(1234, 735), new PointF(0, 0), Direction.E, -1, 2+4, 3, 10, "http://magicseaweed.com/Lacerations-Surf-Report/1090/", "", -8.671239, 115.441876);
        addSpot("Playgrounds", new String[]{"Playground"}, new SurfConditionsProvider(sp, "Playgrounds"), new PointF(1227, 746), new PointF(0, 0), Direction.E, 0, 1+2+4, 2, 8, "http://magicseaweed.com/Playgrounds-Surf-Report/1089/", "", -8.675822, 115.440853);
        addSpot("Ceningan", new SurfConditionsProvider(sp, "Ceningan-Point"), new PointF(1216, 786), new PointF(0, 0), Direction.E, 1, 1+2+4, 3, 10, "http://magicseaweed.com/Ceningan-Surf-Report/2311/", "", -8.702750, 115.437421);

        for (int i = 2; i < list.size()-6; i++) {
            list.get(i).metarName = WADD;
        }

        Set<String> favSpots = sp.getStringSet(SPKEY_FAV_SPOTS, null);
        if (favSpots != null) {
            for (String favSpot : favSpots) {
                Integer integer = Integer.decode(favSpot);
                Log.i(TAG, favSpot + " " + integer);
                if (integer != null) {
                    list.get(integer).favorite = true;
                    list.get(integer).conditionsProvider.updateIfNeed();
                }
            }
        }
        else {
            list.get(3).favorite = true;
            list.get(7).favorite = true;
            list.get(16).favorite = true;
            sp.edit().putStringSet(SPKEY_FAV_SPOTS, getFavorite()).apply();
        }

        setSelectedSpotI(sp.getInt(SPKEY_SELECTED_SPOT, 3));

        selectedSpot().conditionsProvider.updateIfNeed();

        METARProvider.getInstance().addUpdateListener((name, metar) -> {
            SurfSpot selectedSpot = selectedSpot();
            if (selectedSpot == null) return;
            if (name.equals(selectedSpot.metarName) && currentMETAR != metar) {
                currentMETAR = metar;
                fireChanged(new HashSet<Change>() {{add(Change.CURRENT_CONDITIONS);}});
            }
        });
    }


    private static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        for (T ai : a) {
            for (T bi : b) {
                if (ai == bi) return true;
            }
        }
        return false;
    }
    private void fireChanged(Set<Change> changes) {
        for (Map.Entry<ChangeListener, Set<Change>> e : cls.entrySet()) {
            if (e.getValue() == null || hasIntersection(e.getValue(), changes)) e.getKey().onChange(changes);
        }
    }


    private final SurfConditionsProvider.UpdateListener scpul = surfConditionsProvider -> {
        if (list.get(selectedSpotI).conditionsProvider == surfConditionsProvider) {
            currentConditions = surfConditionsProvider.getNow();
            fireChanged(new HashSet<Change>(){{add(Change.CONDITIONS);add(Change.CURRENT_CONDITIONS);}});
        }
    };
    private void addSpot(SurfSpot surfSpot) {
        list.add(surfSpot);
        surfSpot.conditionsProvider.setBsl(bsl);
        surfSpot.conditionsProvider.addUpdateListener(scpul);
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
    private void addSpot(String name, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String msw, String sf, double la, double lo) {
        addSpot(new SurfSpot(name, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell, msw, sf, la, lo));
    }
    private void addSpot(String name, String[] alt, SurfConditionsProvider conditionsProvider, PointF pointOnSVG, PointF pointEarth, Direction waveDirection, int leftright, int tides, int minSwell, int maxSwell, String msw, String sf, double la, double lo) {
        addSpot(new SurfSpot(name, alt, conditionsProvider, pointOnSVG, pointEarth, waveDirection, leftright, tides, minSwell, maxSwell, msw, sf, la, lo));
    }
}
