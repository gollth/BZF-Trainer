package de.tgoll.projects.bzf;

import android.graphics.Color;

import androidx.annotation.ColorInt;

class Util {

    static @ColorInt int desaturate(@ColorInt int color) {
        return desaturate(color, 0);
    }
    static @ColorInt int desaturate(@ColorInt int color, float saturation) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = saturation;
        return Color.HSVToColor(hsv);
    }


}
