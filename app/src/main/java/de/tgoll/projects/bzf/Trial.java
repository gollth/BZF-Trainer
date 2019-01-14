package de.tgoll.projects.bzf;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class Trial implements Comparable<Trial> {

    Date getTimestamp() {
        return timestamp;
    }

    private final Date timestamp;
        private Integer[] choices;

    public Trial(List<Integer> playlist, List<Integer> choices) {
        this.timestamp = new Date();
        this.choices = new Integer[playlist.size()];
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for(int i = 0; i < playlist.size(); i++) map.put(playlist.get(i), choices.get(i));
        map.values().toArray(this.choices);
    }
    int getChoice(int i) { return this.choices[i];}

    boolean wasQuestionCorrect (int i) {
        return Catalogue.isCorrect(i,choices[i]);
    }

    double getSuccessRate() {
        double rate = 0;
        for(int i = 0; i < choices.length; i++)
            if (wasQuestionCorrect(i))
                rate += 1;
        rate /= choices.length;
        return rate;
    }

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
