package com.avaa.surfforecast.ai;

import com.avaa.surfforecast.AppContext;
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
    public final List<String> timeOfDay;

    public Command(Integer day, SurfSpot spot, List<String> keywords, List<String> timeOfDay) {
        this.day = day;
        this.spot = spot;
        this.keywords = keywords.isEmpty() ? null : keywords;
        this.timeOfDay = timeOfDay.isEmpty() ? null : timeOfDay;
    }

    public boolean has(String s) {
        String[] split = s.split(",");
        for (String si : split) {
            if ("spot".equals(si) && spot == null) return false;
            if ("day".equals(si)  && day == null) return false;
            if (si.startsWith("kw:") && (keywords == null || keywords.contains(si.substring(2)) == false)) return false;
        }
        return true;
    }
    public String fill(String s) {
        if (spot != null) s = s.replace("[spot]", spot.name);
        if (day  != null) {
            for (Map.Entry<String, Integer> e : AppContext.instance.commandsExecutor.sToDay.entrySet()) {
                if (day.equals(e.getValue())) s = s.replace("[day]",  e.getKey());
            }
        }
        return s;
    }


    @Override
    public String toString() {
        return "Command{" +
                "day=" + day +
                ", spot=" + spot +
                ", keywords=" + keywords +
                ", timeOfDay=" + timeOfDay +
                '}';
    }
}
