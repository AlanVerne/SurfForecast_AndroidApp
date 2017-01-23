package com.avaa.surfforecast.ai;

import com.avaa.surfforecast.AppContext;
import com.avaa.surfforecast.data.Common;
import com.avaa.surfforecast.data.SurfSpot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Alan on 20 Jan 2017.
 */

public class Command {
    public final Integer day;
    public final SurfSpot spot;
    public final List<String> keywords;
    public Integer time;
    public final List<String> timeOfDay;

    public Command(Integer day, SurfSpot spot, List<String> keywords, Integer time, List<String> timeOfDay) {
        this.day = day;
        this.spot = spot;
        this.keywords = keywords;
        this.time = time;
        this.timeOfDay = timeOfDay;
    }

    public boolean fits(String s) {
        if (spot != null && !s.contains("spot")) return false;
        if (day  != null && !s.contains("day"))  return false;
        if (time != null && !s.contains("time")) return false;
        if (keywords != null) {
            for (String keyword : keywords) {
                if (!s.contains("kw:"+keyword)) return false;
            }
        }

        String[] split = s.split(",");

        for (String si : split) {
            if ("spot".equals(si)) { if (spot == null) return false; }
            else if ("day".equals(si)) { if (day == null) return false; }
            else if ("time".equals(si)) { if (time == null) return false; }
            else if (si.startsWith("kw:")) { if (keywords == null || keywords.contains(si.substring(2)) == false) return false; }
        }

        return true;
    }
    public String fill(String s) {
        if (spot != null) s = s.replace("[spot]", spot.getShortName());

        if (isTimeNow()) {
            if (s.contains("[time]")) {
                s = s.replace("[time]",  "now");
                s = s.replace(" [day] ",  " ");
            }
            else s = s.replace("[day]",  "today");
        }
        else {
            if (day != null) {
                s = s.replace("[day]", AppContext.instance.commandsExecutor.intDayToNL(day));
            }
//            if (timeOfDay != null) s = s.replace("[time]", timeOfDay.get(0));
            if (time != null) {
                s = s.replace("[time]", AppContext.instance.commandsExecutor.intTimeToNL(time));
            }
        }
        return s;
    }


    public boolean isTimeNow() {
        return day != null && day == 0 && time != null && (time == -1 || Math.abs(time - Common.getNowTimeInt(Common.TIME_ZONE)) < 10);
    }


    @Override
    public String toString() {
        return "Command{" +
                "day=" + day +
                ", spot=" + (spot == null ? "null" : spot.name) +
                ", keywords=" + keywords +
                ", time=" + time +
                ", timeOfDay=" + timeOfDay +
                '}';
    }
}
