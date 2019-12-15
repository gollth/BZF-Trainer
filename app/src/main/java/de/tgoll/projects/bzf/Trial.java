package de.tgoll.projects.bzf;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class Trial implements Comparable<Trial> {

    Date getTimestamp() {
        return timestamp;
    }

    private final Date timestamp;
    private Integer[] choices;
    final String key;
    private float success;

    public Trial(String key, Catalogue cat, List<Integer> playlist, List<Integer> choices) {
        this.key = key;
        this.timestamp = new Date();
        this.choices = new Integer[playlist.size()];
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for(int i = 0; i < playlist.size(); i++) map.put(playlist.get(i), choices.get(i));
        map.values().toArray(this.choices);
        success = 0;
        for(int i = 0; i < choices.size(); i++)
            if (cat.isCorrect(i, getChoice(i))) success += 1;
        success /= choices.size();
    }
    int getChoice(int i) { return this.choices[i];}

    double getSuccessRate() {
        return success;
    }

    @NonNull
    @Override
    public String toString() {
        return "Trial-" + timestamp.toString();
    }

    @Override
    public int compareTo(Trial another) {
        Date asr = another.getTimestamp();
        if (asr.before(timestamp)) return -1;
        if (asr.after(timestamp)) return 1;
        return 0;

    }
}
