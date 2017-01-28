package com.avaa.surfforecast.data;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeMap;

import static com.avaa.surfforecast.data.Common.TIME_ZONE;

/**
 * Created by Alan on 10 Nov 2016.
 */

public class SurfConditionsRetriever extends AsyncTask<String, Void, TreeMap<Long, SurfConditions>> {
    private static final String TAG = "SurfConditionsRetriever";

    private static final String LINE_START  = "</div></div><div id=\"contdiv\"><div class=\"flag-sprites\" id=\"breadcrumbs\">";
    private static final String TABLE_START = "<table class=\"forecasts js-forecast-table\" id=\"target-for-range-tabs\">";
    private static final String TABLE_END   = "addon-tabs-cont table-end"; //"</table>";
    private static final String TR = "<tr";
    private static final String TD = "<td";
    private static final String TD_END = "</td>";

    private final SurfConditionsProvider surfConditionsProvider;

    private final String url;
    private final boolean forWeek;


    public SurfConditionsRetriever(SurfConditionsProvider surfConditionsProvider, boolean forWeek) {
        this.surfConditionsProvider = surfConditionsProvider;
        this.forWeek = forWeek;
        url = forWeek ? surfConditionsProvider.urlfullweek : surfConditionsProvider.urlfull;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "onPreExecute() | " + surfConditionsProvider.url);
        surfConditionsProvider.bsl.busyStateChanged(true);
    }

    protected TreeMap<Long, SurfConditions> doInBackground(String... addr) {
        Log.i(TAG, "doInBackground() | " + url);
        TreeMap<Long, SurfConditions> newConditions = null;
        BufferedReader reader = null;

        try {
            java.net.URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(15 * 1000);
            connection.connect();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            newConditions = readPage(reader);
        } catch (Exception e) {
            Log.i(TAG, "doInBackground() | forecast update failed");
            //e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    Log.i(TAG, "doInBackground() | forecast update failed");
                    //ioe.printStackTrace();
                }
            }
        }

        return newConditions;
    }


    protected void onPostExecute(TreeMap<Long, SurfConditions> newConditions) {
        surfConditionsProvider.setConditions(newConditions);
        surfConditionsProvider.bsl.busyStateChanged(false);
    }


    private TreeMap<Long, SurfConditions> readPage(BufferedReader reader) throws IOException {
        String line;

        String toParce = null;
        boolean startFound = false;
        while ((line = reader.readLine()) != null) {
            if (toParce == null) {
                if (line.startsWith(LINE_START)) toParce = line;
            }
            else {
                toParce += line;
            }
            if (toParce != null) {
                if (startFound && toParce.indexOf(TABLE_END) != -1) break;
                else {
                    int start = toParce.indexOf(TABLE_START);
                    if (start != -1) {
                        toParce = toParce.substring(start + TABLE_START.length());
                        startFound = true;
                    }
                }
            }
        }
        Log.i(TAG, "readPage() | " + toParce);

        if (toParce == null) return null;
        line = toParce;

        int i = line.indexOf(TR);
        line = line.substring(i+TR.length());

        int trn = 0;

        int tri = 0;
        int tdi = 0;

        Log.i(TAG, "readPage() | " + line);

        Calendar calendar = new GregorianCalendar(TIME_ZONE);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SurfConditions sc[] = new SurfConditions[18];

        while (true) {
            trn++;
            if (trn == 4 && !forWeek) trn = 5;
            if (trn >= 10) break;

            tri = line.indexOf(TR);
            if (tri == -1) break;

            String row = line.substring(0, tri);
            line = line.substring(tri + TR.length());

            i = line.indexOf(TD);
            line = line.substring(i);

            List<String> tds = new ArrayList<>();
            while (true) {
                int tdEnd = row.indexOf(TD_END);
                if (tdEnd == -1) break;

                String td = row.substring(0, tdEnd);
                tds.add(td);
                row = row.substring(tdEnd + TD_END.length());
            }

            tdi = 0;
            if (trn == 1) {
                if (forWeek) {
                    String td = tds.get(0);

                    int i1 = td.indexOf("colspan=") + 9;

                    int n = Integer.valueOf(td.substring(i1, i1 + 1));

                    //int day;

                    int today = calendar.get(Calendar.DAY_OF_MONTH);
                    calendar.add(Calendar.DATE, -1);
                    int est = calendar.get(Calendar.DAY_OF_MONTH);
                    calendar.add(Calendar.DATE, 2);
                    int tmr = calendar.get(Calendar.DAY_OF_MONTH);
                    calendar.add(Calendar.DATE, -1);
                    //calendar.add(Calendar.DATE, 1);

                    td = tds.get(1);
                    int tmrday = Integer.valueOf(td.substring(td.length() - 2, td.length()));

                    if (tmrday == today) calendar.add(Calendar.DATE, -1);
                    else if (tmrday == est) calendar.add(Calendar.DATE, -2);
                        //else if (tmrday == tmr) calendar.add(Calendar.DATE, 0);
                    else if (tmrday != tmr) calendar.add(Calendar.DATE, 1);
                    //calendar.add(Calendar.DATE, 1);

                    int h = n == 3 ? 11 : n == 2 ? 17 : 2;
                    if (n == 1) calendar.add(Calendar.DATE, 1);

                    calendar.set(Calendar.HOUR_OF_DAY, h);
                }
                else {
                    String td = tds.get(0);

                    int i1 = td.indexOf("colspan=") + 9;

                    int n = Integer.valueOf(td.substring(i1, i1 + 1));
                    int h = 2 + 3 * (8 - n);

                    int day;
                    boolean minusOneDay = false;
                    try {
                        day = Integer.valueOf(td.substring(td.length() - 2, td.length()));
                    }
                    catch (Exception e) {
                        td = tds.get(1);
                        day = Integer.valueOf(td.substring(td.length() - 2, td.length()));
                        minusOneDay = true;
                    }

                    int today = calendar.get(Calendar.DAY_OF_MONTH);
                    calendar.add(Calendar.DATE, -1);
                    int est = calendar.get(Calendar.DAY_OF_MONTH);

                    if (day == today) calendar.add(Calendar.DATE, 1);
                    else if (day != est) calendar.add(Calendar.DATE, -1);

                    if (minusOneDay) calendar.add(Calendar.DATE, -1);

                    calendar.set(Calendar.HOUR_OF_DAY, h);

                    //Log.i(TAG, calendar.get(Calendar.DAY_OF_MONTH)+"");
                }
            }
            else if (trn == 5) {
                if (tds.size() != 18) sc = new SurfConditions[tds.size()];
                for (int j = 0; j < sc.length; j++) {
                    sc[j] = new SurfConditions();
                }

                for (String td : tds) {
                    int iSwell = td.indexOf("/swell.");
                    if (iSwell > 0) {
                        int i1 = iSwell + "/swell.".length();
                        int i2 = td.indexOf(".", i1);
                        Direction dir = Direction.valueOf(td.substring(i1, i2));
                        i2++;
                        String height = td.substring(i2, td.indexOf(".", i2));

                        sc[tdi].waveAngle = Direction.directionToAngle(dir);
                        sc[tdi++].waveHeight = Integer.valueOf(height);
                    }
                    else {
                        sc[tdi].waveAngle = 0;
                        sc[tdi++].waveHeight = 0;
                    }
                }
            }
            else if (trn == 6) for (String td : tds) {
                String period = td.substring(td.indexOf(">") + 1);
                try {
                    sc[tdi].wavePeriod = Integer.valueOf(period);
                }
                catch (NumberFormatException e) {
                    sc[tdi].wavePeriod = 0;
                }
                tdi++;
            }
            else if (trn == 8) {
                for (String td : tds) {
                    sc[tdi++].waveEnergy = Integer.valueOf(td.substring(td.lastIndexOf(">", td.length()-4) + 1, td.length()-4));
                }
            }
            else if (trn == 9) {
                for (String td : tds) {
                    int i1 = td.indexOf("alt=") + 5;
                    int i2 = td.indexOf(" ", i1);
                    sc[tdi].windSpeed = Integer.valueOf(td.substring(i1, i2));
                    sc[tdi++].windAngle = Direction.directionToAngle(Direction.valueOf(td.substring(i2+1, td.indexOf("\"", i1))));
                }
            }
        }

        TreeMap<Long, SurfConditions> newSC = new TreeMap<>();

        if (forWeek) {
            Log.i(TAG, "readPage() | week for " + surfConditionsProvider.url);
            for (SurfConditions surfConditions : sc) {
                Log.i(TAG, calendar.getTime().toString() + " " + surfConditions.toString());
                newSC.put(calendar.getTime().getTime(), surfConditions);
                int hour = calendar.get(Calendar.HOUR);
                calendar.add(Calendar.HOUR, hour == 2 ? 9 : hour == 11 ? 6 : 9);
            }
        }
        else {
            Log.i(TAG, "readPage() | latest for " + surfConditionsProvider.url);
            for (SurfConditions surfConditions : sc) {
                Log.i(TAG, calendar.getTime().toString() + " " + surfConditions.toString());
                newSC.put(calendar.getTime().getTime(), surfConditions);
                calendar.add(Calendar.HOUR, 3);
            }
        }

        return newSC;
    }
}
