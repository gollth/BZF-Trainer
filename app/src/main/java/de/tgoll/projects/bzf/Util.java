package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

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


    static @ColorInt int lookupColor(@NonNull Context context, @AttrRes int id) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(id, typedValue, true);
        return typedValue.data;
    }
}
