package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.res.Resources;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class Catalogue {

    private static String[] questions;
    private static List<List<String>> answers;
    private static int[] solutions;

    static void initialize(Context c) {
        questions = c.getResources().getStringArray(R.array.questions);
        answers = new ArrayList<>();
        solutions = new int[questions.length];

        String[] tmp = c.getResources().getStringArray(R.array.answers);
        if (questions.length != tmp.length/4) {
            throw new Resources.NotFoundException("The amount of questions (" + questions.length +
                    ") does not match with the amount of answers (" + tmp.length/4 + ")");
        }

        for (int i = 0; i < questions.length; i++) {
            int offset = i * 4;
            List<String> as = new ArrayList<>();
            for (int j = 0; j < 4; j++) as.add(tmp[offset+j]);

            // Shuffle the list of answers

            List<Integer> idx = Arrays.asList(0,1,2,3);
            Collections.shuffle(idx);
            List<String> bs = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                int id = idx.get(j);
                bs.add(as.get(id));
                if (id == 0) solutions[i] = id;
            }
            answers.add(bs);
        }
    }

    static boolean isCorrect(int question, int answer) {
        try {
            return solutions[question] == answer;
        } catch (Exception e) {
            Crashlytics.log("Error occurred in asking \"isCorrect\" of question " + question + " and answer " + answer + ": " + e.getMessage());
            return false;
        }
    }

    static int size() {
        return questions.length;
    }

    static String getQuestion(int i) {
        try {
            return questions[i];
        } catch (Exception e) {
            Crashlytics.log("Error occurred in asking \"getQuestion\" of question " + i + ": " + e.getMessage());
            return "Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen";
        }
    }

    static int getSolution(int question) {
        try {
            return solutions[question];
        } catch (Exception e) {
            Crashlytics.log("Error occurred in asking \"getSolution\" of question " + question + ": " + e.getMessage());
            return 0;
        }
    }
    static String getAnswer(int question, int answer) {
        try {
            return answers.get(question).get(answer);
        } catch (Exception e) {
            Crashlytics.log("Error occurred in asking \"getAnswer\" of question " + question + " and answer " + answer + ": " + e.getMessage());
            return "Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen";
        }
    }
    static String[] getAnswers(int question) {
        try {
            return (String[]) answers.get(question).toArray();
        } catch (Exception e) {
            Crashlytics.log("Error occurred in asking \"getAnswers\" of question " + question + ": " + e.getMessage());
            return new String[]{"Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen", "", "", ""};
        }
    }

    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
        List<Map.Entry<K, V>> list = new LinkedList<>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }
}
