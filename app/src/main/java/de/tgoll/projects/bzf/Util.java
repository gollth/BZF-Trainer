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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (trials == null || trials.size() == 0) return null;

        // Map from #question to fraction (#correct / #total)
        Map<Integer, Fraction> questions = new HashMap<>();
        for (Trial trial : trials) {
            for (int i = 0; i < trial.size(); i++) {
                int question = trial.getQuestion(i);
                Fraction fraction = new Fraction(questions.get(question));
                if (trial.isCorrect(i)) {
                    fraction.denominator += 1;
                }
                fraction.nominator += 1;
                questions.put(trial.getQuestion(i), fraction);
            }
        }

        // Convert the data to BarChart entries
        List<Pair<Integer, Float>> values = new ArrayList<>();

        for (int i = 0; i < questions.size(); i ++) {
            Fraction fraction = questions.get(i);
            if (fraction == null) continue;
            values.add(new Pair<>(i+1, fraction.toRational()));
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
    static float getValue(BarDataSet set, int index) {
        BarEntry entry = set.getEntryForIndex(index);
        float[] values = entry.getYVals();
        return values[0];
    }
    static float getValue(BarDataSet set, float x) {
        return getValue(set, set.getEntryIndex(x, Float.NaN, null));
    }

    /**
     * Get the number of most wrong answered questions in the whole history
     * @param set the dataset containing the sorted values of the history of a catalogue
     * @param trialsAmount the amount of trials which were conducted
     * @return a number between 0 and `trialsAmount`
     */
    static int getMaxWrongAnswered(BarDataSet set, int trialsAmount) {
        return Math.round(getValue(set, 0) * trialsAmount);
    }

    /**
     * Get the number of most correct answered questions in the whole history
     * @param set the dataset containing the sorted values of the history of a catalogue
     * @param trialsAmount the amount of trials which were conducted
     * @return a number between 0 and `trialsAmount`
     */
    static int getMaxCorrectAnswered(BarDataSet set, int trialsAmount) {
        return Math.round(getValue(set, set.getEntryCount()-1) *  trialsAmount);
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
