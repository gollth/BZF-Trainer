package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class Catalogue {

    private String[] questions;
    private String[][] answers;
    private int[] solutions;

    private int idStringArray(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "array", context.getPackageName());
        if (id == 0) Log.e("BZF", "Resource " + name + " not found.");
        return id;
    }

    Catalogue(Context c, String key) {
        questions = c.getResources().getStringArray(idStringArray(c,key + "_questions"));
        answers = new String[questions.length][];
        solutions = c.getResources().getIntArray(idStringArray(c,key + "_solutions"));

        String[] tmp = c.getResources().getStringArray(idStringArray(c,key + "_answers"));
        if (questions.length != tmp.length) {
            throw new Resources.NotFoundException("The amount of questions (" + questions.length +
                    ") does not match with the amount of answers (" + tmp.length + ")");
        }

        for (int i = 0; i < questions.length; i++) {
            answers[i] = tmp[i].split(c.getString(R.string.answer_separator));
            for (int j = 0; j < answers[i].length; j++) answers[i][j] = answers[i][j].trim();
            if (answers[i].length < 4) Log.e("Question " + (i+1), "Missing ;");
            if (answers[i].length > 4) Log.e("Question " + (i+1), "To much ;");
        }
    }

    boolean isCorrect(int question, int answer) {
        try {
            return solutions[question] == answer;
        } catch (Exception e) {
            String error = "Error occurred in asking \"isCorrect\" of question " + question + " and answer " + answer + ": " + e.getMessage();
            Log.e("BZF", error);
            FirebaseCrashlytics.getInstance().log(error);
            return false;
        }
    }

    int size() {
        return questions.length;
    }

    String getQuestion(int i) {
        try {
            return questions[i];
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("Error occurred in asking \"getQuestion\" of question " + i + ": " + e.getMessage());
            return "Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen";
        }
    }

    int getSolution(int question) {
        try {
            return solutions[question];
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("Error occurred in asking \"getSolution\" of question " + question + ": " + e.getMessage());
            return 0;
        }
    }
    String getAnswer(int question, int answer) {
        try {
            return answers[question][answer];
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("Error occurred in asking \"getAnswer\" of question " + question + " and answer " + answer + ": " + e.getMessage());
            return "Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen";
        }
    }
    @SuppressWarnings("unused")
    String[] getAnswers(int question) {
        try {
            return answers[question];
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("Error occurred in asking \"getAnswers\" of question " + question + ": " + e.getMessage());
            return new String[]{"Ups, es ist leider ein Fehler aufgetreten =(. Bitte mit der nächsten Frage weitermachen", "", "", ""};
        }
    }

    @SuppressWarnings("unused")
    <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
        List<Map.Entry<K, V>> list = new LinkedList<>( map.entrySet() );
        Collections.sort(list, (a, b) -> a.getValue().compareTo(b.getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }
}
