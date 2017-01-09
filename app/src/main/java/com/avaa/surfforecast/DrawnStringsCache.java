package com.avaa.surfforecast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Alan on 3 Aug 2016.
 */

@Deprecated
public class DrawnStringsCache {
    private final Map<Integer, Bitmap> cache = new TreeMap<>();

    private final Paint paint;
    private final int h, ws, wb;


    public DrawnStringsCache(Paint paint) {
        this.paint = paint;

        Rect bounds = new Rect();
        paint.getTextBounds("0", 0, 1, bounds);
        h = bounds.height();
        ws = bounds.width();
        paint.getTextBounds("00", 0, 2, bounds);
        wb = bounds.width();
    }

    public Bitmap get(int i) {
        Bitmap bitmap = cache.get(i);
        if (bitmap == null) {
            int w = i < 10 ? ws : wb;
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            c.drawText(String.valueOf(i), w/2, h, paint);
            cache.put(i, bitmap);
        }
        return bitmap;
    }
}
