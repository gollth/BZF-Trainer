package de.tgoll.projects.bzf;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Trial implements Comparable<Trial> {

    Date getTimestamp() {
        return timestamp;
    }

    private final Date timestamp;
    private Integer[] choices;
    private final Integer[] playlist;
    final String key;
    private float success;

    // Don't serialize this in Gson
    private transient Catalogue cat;


    public Trial(String key, float success) {
        this(key, null);
        this.success = success;
    }
    public Trial(String key, Catalogue cat) {
        this(key, cat, new ArrayList<>(), new ArrayList<>());
    }
    public Trial(String key, Catalogue cat, List<Integer> playlist, List<Integer> choices) {
        this.key = key;
        this.cat = cat;
        this.timestamp = new Date();
        this.choices = new Integer[playlist.size()];
        this.playlist = new Integer[playlist.size()];
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for(int i = 0; i < playlist.size(); i++) map.put(playlist.get(i), choices.get(i));
        map.values().toArray(this.choices);
        for(int i = 0; i < choices.size(); i++) {
            this.playlist[i] = playlist.get(i);
        }
    }

    public void setCatalogue(Catalogue cat) {
        this.cat = cat;
    }

    int size() { return this.choices.length; }
    int getQuestion(int i) {
        // If no playlist was set in shared preferences, we assume a complete trial
        if (this.playlist == null || this.playlist.length == 0) return i;
        return this.playlist[i];
    }
    private int getChoice(int question) {
        if (playlist == null || this.playlist.length == 0) {
            return this.choices[question];
        }

        Map<Integer, Integer> map = new HashMap<>();
        List<Integer> sortedPlaylist = Arrays.asList(Arrays.copyOf(playlist, playlist.length));
        Collections.sort(sortedPlaylist);
        for(int j = 0; j < sortedPlaylist.size(); j++) {
            map.put(sortedPlaylist.get(j), this.choices[j]);
        }

        return map.get(question);
    }
    boolean isCorrect(int i) {
        int question = this.getQuestion(i);
        int choice = this.getChoice(question);
        return cat.isCorrect(question, choice);
    }
    boolean wasAnswered(int i) { return this.getChoice(getQuestion(i)) != -1; }

    int getSuccessful() {
        int success = 0;
        for(int i = 0; i < size(); i++) {
            if (isCorrect(i)) {
                success += 1;
            }
        }
        return success;
    }

    double getSuccessRate() {
        if (this.cat == null) return this.success;
        return (double)this.getSuccessful() / size();
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
