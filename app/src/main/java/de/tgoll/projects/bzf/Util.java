package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Pair;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    /**
     * Generate a ordered histogram bar chart of the questions of the trials of a catalogue.
     *
     * The bar chart is ordered by frequency, where always wrongly answered questions are far left
     * and always correctly answered questions are far right, the rest in between. The labels (X values)
     * of the bars show the question number.
     *
     * @param trials A list of all trials of a catalogue to analyze
     * @param color The primary color of the catalogue to tint the bars
     * @return Null if trials is empty. Otherwise a tuple with the bar data set as first element and
     * a list of question numbers according to the order, which can be assigned to X axis labels of the chart
     */
    static @Nullable Pair<BarDataSet, List<Integer>> createQuestionHistogram(List<Trial> trials, @ColorInt int color) {
        if (trials == null) return null;

        // Create a list capable of holding all choices
        int max = 0;
        for (Trial trial : trials) {
            if (trial.size() > max) max = trial.size();
        }
        List<Float> questions = new ArrayList<>(Collections.nCopies(max, 0f));

        // Fill the list with data
        for (Trial trial : trials) {
            for (int i = 0; i < trial.size(); i++) {
                if (!trial.isCorrect(i)) continue;
                questions.set(i, questions.get(i) + 1f);
            }
        }

        // Convert the data to BarChart entries
        List<Pair<Integer, Float>> values = new ArrayList<>();

        for (int i = 0; i < questions.size(); i ++) {
            values.add(new Pair<>(i+1, questions.get(i) / trials.size()));
        }

        Collections.sort(values, (a, b) -> Float.compare(a.second, b.second));

        List<Integer> keys = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            Pair<Integer, Float> pair = values.get(i);
            keys.add(pair.first);
            entries.add(new BarEntry(i, new float[]{ pair.second, 1f-pair.second }));
        }

        int background = Color.TRANSPARENT;
        int[] colors = new int[]{ color, background };

        return new Pair<>(new BarDataSet(entries, "Fragen") {{
            setColors(colors);
            setValueFormatter(new StatisticsFragment.NoneValueFormatter());
        }}, keys);
    }

    /**
     * Get the lower value of a stacked bar entry
     * @param set the dataset contianing the bar entry
     * @param index the index of the bar in the dataset
     * @return the value of the lower stack in the bar entry
     */
    static int getValue(BarDataSet set, int index, int maximum) {
        BarEntry entry = set.getEntryForIndex(index);
        float[] values = entry.getYVals();
        return Math.round(values[0] * maximum);
    }
    static int getValue(BarDataSet set, float x, int maximum) {
        return getValue(set, set.getEntryIndex(x, Float.NaN, null), maximum);
    }

    /**
     * Get the number of most wrong answered questions in the whole history
     * @param set the dataset containing the sorted values of the history of a catalogue
     * @param trialsAmount the amount of trials which were conducted
     * @return a number between 0 and `trialsAmount`
     */
    static int getMaxWrongAnswered(BarDataSet set, int trialsAmount) {
        return getValue(set, 0, trialsAmount);
    }

    /**
     * Get the number of most correct answered questions in the whole history
     * @param set the dataset containing the sorted values of the history of a catalogue
     * @param trialsAmount the amount of trials which were conducted
     * @return a number between 0 and `trialsAmount`
     */
    static int getMaxCorrectAnswered(BarDataSet set, int trialsAmount) {
        return getValue(set, set.getEntryCount()-1, trialsAmount);
    }

    /**
     * Get the list of all trials in the history of a catalogue
     * @param settings the shared preferences where the data is stored
     * @param gson a Gson parser instance
     * @param key either "azf" or "bzf"
     * @return the list with all the trials for `key` in the history
     */
    static List<Trial> getTrials(SharedPreferences settings, Gson gson, String key) {
        Set<String> history = settings.getStringSet(key + "-history", CatalogueFragment.EMPTY_SET);
        List<Trial> list = new ArrayList<>();
        for (String json : history) list.add(gson.fromJson(json, Trial.class));
        return list;
    }

    /** Clamp a value between two bounds
     * @param value to clamp
     * @param min   the lower bound (inclusive)
     * @param max   the upper bound (inclusive)
     * @return min <= value <= max
     */
    static float saturate(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }
}