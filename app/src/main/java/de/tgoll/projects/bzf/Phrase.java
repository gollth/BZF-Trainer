package de.tgoll.projects.bzf;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phrase {

    // Inner class
    static class Params {
        static String AIRPORT, CALLSIGN, AIRCRAFT, ATIS, QNH, RUNWAY, RUNWAY2, TAXI_ROUTE,
                FREQ, FIXPOINT, SQUAWK, WIND_DIR, WIND_KN;
    }


    // Static members
    private static final String[] FIXPOINTS = new String[] {"N", "E", "S", "W" };
    private static String[] airports, airport_names;
    private static String[] numbers;
    private static Map<Character, String> dict;
    private static final String[] numbersEN = new String[] {
        "zero","one", "two", "three", "four", "five", "six","seven","eight","niner", "decimal"
    };
    private static final String[] numbersDE = new String[] {
        "null","eins","zwo", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "punkt"
    };


    // Static methods
    static void initialize(Context context, boolean english) {
        dict = new HashMap<>(26);
        dict.put('A', "ALPHA");
        dict.put('B', "BRAVO");
        dict.put('C', "CHARLIE");
        dict.put('D', "DELTA");
        dict.put('E', english ? "ECHO" : "ECKO");
        dict.put('F', "FOXTROT");
        dict.put('G', "GOLF");
        dict.put('H', "HOTEL");
        dict.put('I', "INDIA");
        dict.put('J', "JULIET");
        dict.put('K', "KILO");
        dict.put('L', "LIMA");
        dict.put('M', "MIKE");
        dict.put('N', "NOVEMBER");
        dict.put('O', "OSCAR");
        dict.put('P', "PAPA");
        dict.put('Q', english ? "QUEBEC" : "KEBECK");
        dict.put('R', "ROMEO");
        dict.put('S', "SIERRA");
        dict.put('T', "TANGO");
        dict.put('U', "UNIFORM");
        dict.put('V', "VICTOR");
        dict.put('W', english ? "WHISKEY" : "WISKI");
        dict.put('X', "X-RAY");
        dict.put('Y', "YANKEE");
        dict.put('Z', english ? "ZULU" : "SULU");
        int id = english ? R.array.airport_names_en : R.array.airport_names_de;
        airports = context.getResources().getStringArray(R.array.airports);
        airport_names = context.getResources().getStringArray(id);
        numbers = english ? numbersEN : numbersDE;
    }
    static String getRandomFixpoint(Random rng) {
        return FIXPOINTS[rng.nextInt(4)];
    }
    static String getRandomFreq(Random rng) {
        int tenth = 18 + rng.nextInt(15); // [18 .. 32]
        int decimals = rng.nextInt(20) * 5; // [00 ... 95]
        return "1" + tenth + "." + decimals;
    }
    static char getRandomLetter(Random rng) {
        return Character.toChars(rng.nextInt(26)+65)[0];
    }


    // private class members
    private final String phrase;
    private final String sender;
    private final String[] groups;
    private final static Pattern callsignFinder = Pattern.compile("\\d\\s?\\d\\s?\\d\\s?\\.(\\s?\\d){1,3}");    //Three digits, decimal, one, two or three further digits
    private final static Pattern numberFinder = Pattern.compile("[A-Z]\\s?[A-Z]\\s?[A-Z]\\s?[A-Z]\\s?[A-Z]");   //Five Letters with optional spaces in between
    private final static Pattern runwayFinder = Pattern.compile("runway\\s\\d\\s\\d");
    private final static Pattern braceFinder = Pattern.compile("\\(.*?\\)");                                    // Find everything between braces (lazy)
    private int corrects = 0;

    // Construction
    Phrase(String i, boolean english) {
        String[] ps = i.split(": ");
        sender = ps[0];
        phrase = ps[1];
        groups = phrase.split(", ");
    }

    // Converter Functions
    static String resolveParams(String s) {
        return s.replaceAll("\r", "")
                .replaceAll("#airport",     Params.AIRPORT)
                .replaceAll("#callsign",    Params.CALLSIGN)
                .replaceAll("#aircraft",    Params.AIRCRAFT)
                .replaceAll("#atis",        Params.ATIS)
                .replaceAll("#qnh",         Params.QNH)
                .replaceAll("#runway2",     Params.RUNWAY2)
                .replaceAll("#runway",      Params.RUNWAY)
                .replaceAll("#taxi_route",  Params.TAXI_ROUTE)
                .replaceAll("#freq",        Params.FREQ)
                .replaceAll("#fixpoint",    Params.FIXPOINT)
                .replaceAll("#squawk",      Params.SQUAWK)
                .replaceAll("#wind_dir",    Params.WIND_DIR)
                .replaceAll("#wind_kn", Params.WIND_KN);
    }
    static String convertABC(String abc) {
        StringBuilder answer = new StringBuilder();
        for(int i = 0; i < abc.length(); i++) {
            if (i > 0) answer.append(" ");
            String s = dict.get(abc.toUpperCase().charAt(i));
            if (s != null) answer.append(s.toLowerCase());
        }
        return answer.toString();
    }
    static String convertNumber(String number) {
        StringBuilder answer = new StringBuilder();

        // Check if number is an integer
        if (!number.contains(".")) {
            int i = Integer.parseInt(number);
            // If number is multiple of hundreds, don't convert any further
            if (i%100 == 0) return number;
        }
        for(String digit : number.split("")) {
            answer.append(" ");
            for (int d = 0; d <= 9; d++) {
               if (digit.equals("" + d)) answer.append (numbers[d]);
            }
            if (digit.equals(".")) answer.append (numbers[10]);
        }
        return answer.substring(1); // without leading space
    }
    static String convertAirport(String s) {
        int i = Arrays.asList(airports).indexOf(s);
        if (i < 0) return "";
        else return airport_names[i];
    }

    @NonNull
    @Override
    public String toString() {
        return resolveParams(phrase).replaceAll("\\(","").replaceAll("\\)","");
    }

    float getSuccessRate() { return (float)corrects / groups.length;}
    String getSender() {
        switch (sender) {
            case "P": return "Pilot";
            case "T": return "Tower";
            case "G": return "Ground";
            default: return "Unknown";
        }
    }
    @SuppressWarnings("ConstantConditions")
    Spanned compareWith(String msg) {
        String hexColor = String.format("#%06X", (0xFFFFFF & android.graphics.Color.GREEN));

        msg = msg.toLowerCase();

        // Convert "Alpha" --> "A"
        for(String letter : dict.values()) {
            msg = msg.replaceAll(letter.toLowerCase(), ""+letter.charAt(0));
        }

        // Convert "Berlin" --> "EDDB"
        for (int a = 0; a < airport_names.length; a++) {
            msg = msg.replace(airport_names[a].toLowerCase(), airports[a]);
        }

        //Convert "one one eight decimal niner five" --> 118.95
        for (int a = 0; a < numbers.length-1; a++) {    // exclude "decimal"
            msg = msg.replaceAll(numbers[a], "" + a);
        }
        msg = msg.replace("decimal",".");

        // Remove spaces between letters, e.g. "D E A B C" --> "DEABC"
        Matcher m = callsignFinder.matcher(msg);
        if(m.find()) msg = m.replaceFirst(TextUtils.join("", m.group(0).split(" ")));

        // Remove spaces between digits, e.g. 11 8. 95 --> 118.95
        m = numberFinder.matcher(msg);
        if (m.find()) msg = m.replaceFirst(TextUtils.join("", m.group(0).split(" ")));

        // Remove spaces in runways e.g. "runway 3 3" --> runway 33
        m = runwayFinder.matcher(msg);
        if (m.find()) msg = m.replaceFirst(TextUtils.join("", m.group(0).split(" ")).replace("runway","runway "));
        m = runwayFinder.matcher(msg);
        if (m.find()) msg = m.replaceFirst(TextUtils.join("", m.group(0).split(" ")).replace("runway","runway "));

        msg = msg.replace("vfr","VFR").replace("qnh", "QNH");    // Capitalize
        msg = msg.replace("general aviation terminal" , "GAT");  // Abbreviate

        Log.i("Simulator", "Pilot: " + msg);
        StringBuilder answer = new StringBuilder();

        for(String group : groups) {
            group = resolveParams(group);
            Matcher match = braceFinder.matcher(group);
            String check = match.replaceAll("");
            group = group.replaceAll("\\(","").replaceAll("\\)","");
            if (!check.isEmpty() && msg.toLowerCase().contains(check.toLowerCase())) {
                answer.append("<font color=").append(hexColor).append(">").append(group).append("</font>");
                corrects++;
            } else answer.append(group);
            answer.append(", ");
        }
        return Html.fromHtml("<b>" + answer.substring(0, answer.length() - 2) + " </b>");

    }
    boolean isFromATC() { return !sender.equalsIgnoreCase("P"); }
    String makePronounceable() {
        StringBuilder answer = new StringBuilder();
        for (String group : groups) {
            group = resolveParams(group);
            for(String word : group.split(" ")) {
                answer.append(" ");
                if (isAirport(word)) answer.append(convertAirport(word));
                else if (isABC(word)) answer.append(convertABC(word));
                else if (isNumber(word)) answer.append(convertNumber(word));
                else if (word.equalsIgnoreCase("ft")) answer.append("feet");
                else if (word.equals("GAT")) answer.append ("General Aviation Terminal");
                else if (word.equalsIgnoreCase("roger")) answer.append("rodger");
                else answer.append(word);
            }
            answer.append(",");
        }
        return answer.toString();
    }


    // Checker functions
    private boolean isAirport(String s) {
        for (String airport : airports) if (s.equals(airport)) return true;
        return false;
    }
    private boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    private boolean isABC(String s) {
        if (s.equals("QNH") || s.equals("VFR") || s.equals("GAT")) return false;
        return s.matches("[A-Z]+");
    }

}
