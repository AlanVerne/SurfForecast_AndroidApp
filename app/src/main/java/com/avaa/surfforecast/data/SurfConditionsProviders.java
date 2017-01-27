package com.avaa.surfforecast.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alan on 27 Jan 2017.
 */

public class SurfConditionsProviders {
    private static Map<String, SurfConditionsProvider> map = new HashMap<>();

    public static SurfConditionsProvider get(String name) {
        SurfConditionsProvider surfConditionsProvider = map.get(name);
        if (surfConditionsProvider == null) {
            surfConditionsProvider = new SurfConditionsProvider(name);
            map.put(name, surfConditionsProvider);
        }
        return surfConditionsProvider;
    }
}
