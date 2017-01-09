package com.avaa.surfforecast.drawers;

import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

/**
 * Created by Alan on 5 Oct 2016.
 */

public class SVGPathToAndroidPath {
    public static Path convert(String data, Point size) {
        Path p = new Path();
        try {
            String[] tokens = data.split("[ ,\n]");

            int i = 0;

            size.set(Integer.valueOf(tokens[i++]), Integer.valueOf(tokens[i++]));

            while (i < tokens.length) {
                String token = tokens[i].toLowerCase();
                //Log.i("Con", tokens[i]);
                if (token.length() > 1) {
                    tokens[i] = token.substring(1);
                    token = token.substring(0, 1);
                    //Log.i("Con/", token + "  " + tokens[i]);
                }
                else i++;
                switch (token) {
                    case "m": {
                        float x = Float.valueOf(tokens[i++]);
                        float y = Float.valueOf(tokens[i++]);
                        p.moveTo(x, y);
                        break;
                    }
                    case "l": {
                        float x = Float.valueOf(tokens[i++]);
                        float y = Float.valueOf(tokens[i++]);
                        p.rLineTo(x, y);
                        break;
                    }
                    case "c":
                        float x1 = Float.valueOf(tokens[i++]);
                        float y1 = Float.valueOf(tokens[i++]);
                        float x2 = Float.valueOf(tokens[i++]);
                        float y2 = Float.valueOf(tokens[i++]);
                        float x3 = Float.valueOf(tokens[i++]);
                        float y3 = Float.valueOf(tokens[i++]);
                        p.rCubicTo(x1, y1, x2, y2, x3, y3);
                        break;
                    case "z":
                        p.close();
                        break;
                    default:
                        //throw new RuntimeException("unknown command [" + token + "]");
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("bad data ", e);
        }

        return p;
    }
}
