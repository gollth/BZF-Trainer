package de.tgoll.projects.bzf;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tgoll.projects.bzf.CatalogueFragment.EMPTY_SET;

public class SimulatorFragment extends Fragment
                                implements View.OnTouchListener,
                                AdapterView.OnItemSelectedListener {

    private static final int PERMISSION_RECORD_AUDIO = 19;
    private static final String key = "sim";

    private static final int EASY = -1;
    private static final int MEDIOCRE = 0;
    private static final int HARD = 1;

    // Array handling
    private List<Phrase> phrases;
    private int progress, departurePhraseSize;
    private boolean showedDepartureFinishMessage = false;

    // Views
    private TextView txt_callsign, txt_aircraft, txt_atis;
    private TextView txt_you, txt_atc, lbl_atc;
    private Spinner cbx_departure, cbx_arrival;

    // Record- and TTS- Stuff
    private TextToSpeech atc;
    private SpeechRecognizer recorder;
    private Intent recordIntent;
    private ImageButton btn_record;
    private AudioManager audio;
    private long startTime;
    private boolean english;

    // Success rate calculation
    private List<Spanned> answers;
    private float totalSuccessRates = 0;
    private int answeredFromYou = 0;

    // Misc
    private Gson gson;
    private LayoutInflater inflater;
    private Random rng = new Random();
    private SharedPreferences settings;
    private boolean loggedLevelStart = false;

    private String getDifficultyName () {
        switch (getDifficulty()) {
            case EASY:      return "EASY";
            case MEDIOCRE:  return "MEDIOCRE";
            case HARD:      return "HARD";
            default:        return "UNKNOWN";
        }
    }
    private int getDifficulty () {
        String[] levels = getResources().getStringArray(R.array.settings_sim_levels_indices);
        String value = settings.getString(getString(R.string.settings_sim_level), levels[0]);
        for(int i = 0; i < levels.length; i++) {
            if (levels[i].equals(value)) return i-1;
        }
        Log.w ("Simulator", "Could not find Difficulty " + value + " in R.array.settings_sim_levels, assuming it is MEDIOCRE");
        return MEDIOCRE;   // Might never happen because of default value
    }

    // Creation functions
    private List<Phrase> parseConversation (int id, boolean english) throws IOException {
        InputStream stream = getResources().openRawResource(id);
        byte[] data = new byte[stream.available()];
        int bytes = stream.read(data);
        if (bytes == 0) return new ArrayList<>();

        String file = new String(data);

        int level = getDifficulty();
        Pattern pattern = null;
        switch (level) {
            case EASY:      pattern = Pattern.compile("\\[.*?]|\\{.*?\\}", Pattern.MULTILINE); break;
            case MEDIOCRE:  pattern = Pattern.compile("\\{.*?\\}|\\[|]", Pattern.MULTILINE); break;
            case HARD:      pattern = Pattern.compile("[\\[\\]{}]", Pattern.MULTILINE); break;
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(file);
            file = matcher.replaceAll("");
        }

        List<Phrase> phrases = new ArrayList<>();
        for (String line : file.split("\r?\n")) {
            if (line.isEmpty()) continue;
            phrases.add(new Phrase(line, english));
        }
        return phrases;

    }
    private void generateNewParameters() {
        int diff = getDifficulty();

        int rw = rng.nextInt(36)+1;         // ranges from [01 ... 36]
        int rw2 = rw;
        while (rw2 == rw) rw2 = rng.nextInt(36)+1;

        StringBuilder taxi = new StringBuilder();
        taxi.append(Phrase.getRandomLetter(rng));
        if (diff >= MEDIOCRE){
            taxi.append(" ").append(Phrase.getRandomLetter(rng));
            if (diff >= HARD) taxi.append(" ").append(Phrase.getRandomLetter(rng));
            String and = english ? " and " : " und ";
            taxi.append(and).append(Phrase.getRandomLetter(rng));
        }

        Phrase.Params.AIRCRAFT = txt_aircraft.getText().toString();
        Phrase.Params.AIRPORT = showedDepartureFinishMessage ? cbx_departure.getSelectedItem().toString()
                                               : cbx_arrival.getSelectedItem().toString();
        Phrase.Params.CALLSIGN = txt_callsign.getText().toString();
        Phrase.Params.ATIS = "" + Phrase.getRandomLetter(rng);
        Phrase.Params.FIXPOINT = Phrase.getRandomFixpoint(rng);
        Phrase.Params.SQUAWK = String.format(Locale.GERMAN, "%04d", rng.nextInt(10000));
        Phrase.Params.FREQ = Phrase.getRandomFreq(rng);
        Phrase.Params.QNH = "" + (rng.nextInt(31) + 995);  // ranges from [995...1025]
        Phrase.Params.TAXI_ROUTE = taxi.toString();
        Phrase.Params.RUNWAY2 = String.format(Locale.GERMAN, "%02d", rw2);
        Phrase.Params.RUNWAY = String.format(Locale.GERMAN, "%02d", rw);
        Phrase.Params.WIND_DIR = String.format(Locale.GERMAN, "%3d",(rng.nextInt(36)+1)*10);
        Phrase.Params.WIND_KN = String.format(Locale.GERMAN, "%d", rng.nextInt(11)+5);

        // Update the ATIS letter now
        txt_atis.setVisibility(diff > EASY ? View.VISIBLE : View.GONE);
        txt_atis.setText(getString(R.string.sim_txt_atis, Phrase.Params.ATIS));

    }
    private Dialog createResultDialog() {
        Point size = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);

        ViewGroup root = requireActivity().findViewById(R.id.fragment);
        View dialog = inflater.inflate(R.layout.dialog_sim_results, root, false);
        ScrollView scroller = dialog.findViewById(R.id.scroll_sim_diag);
        scroller.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, (int) (size.y * 0.8f)));
        LinearLayout l = dialog.findViewById(R.id.lyt_sim_diag_results);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        LayoutParams line1  = new LayoutParams(LayoutParams.MATCH_PARENT,1);
        params.setMargins(0, 0, 0, 5); line1.setMargins(0, 2, 0, 2);
        for (int i = 0; i < answers.size(); i++) {
            l.addView(new LineView(getContext()), line1);
            if (i == departurePhraseSize) l.addView(new LineView(getContext()), line1);
            TextView t = new TextView(getContext());
            t.setText(answers.get(i));
            l.addView(t, params);
        }
        int correct = Math.round(totalSuccessRates * 100 / answeredFromYou);

        // Send to Crashlytics, that the user completed the Simulator
        if (Answers.getInstance() != null) Answers.getInstance().logLevelEnd(new LevelEndEvent()
                .putLevelName(getString(R.string.simulator))
                .putScore(correct)
                .putSuccess(correct > 50));

        // Add the trial to the statistics
        Trial trial = new Trial(key, correct * .01f);
        Set<String> history = new HashSet<>(settings.getStringSet(key + "-history", EMPTY_SET));
        history.add(gson.toJson(trial));
        settings.edit()
                .remove(key + "-history")
                .putStringSet(key + "-history", history)
                .apply();

        return new MaterialAlertDialogBuilder(requireContext())
                .setCancelable(false)
                .setView(dialog)
                .setNegativeButton(R.string.statistics, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        try {
                            TitleActivity activity = (TitleActivity) getActivity();
                            if (activity == null) return;
                            activity.showFragment(getString(R.string.statistics), true);
                        } catch (ClassCastException cce){
                            String error = "SimulatorFragment: Error casting getActivity() to TitleActivity" + cce.getMessage();
                            Log.e("BZF", error);
                            Crashlytics.log(error);
                        }
                    }
                })
                .setPositiveButton(getString(R.string.restart), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        restart();
                    }
                })
                .setTitle(getString(R.string.msg_finish_sim, correct))
                .setIcon(correct >= 50 ? R.drawable.like : R.drawable.dislike).create();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.fragment_simulator, container, false);

        gson = new Gson();
        Activity activity = requireActivity();

        settings = PreferenceManager.getDefaultSharedPreferences(activity);
        english = settings.getBoolean(getString(R.string.language), true);

        Phrase.initialize(activity, english);

        txt_aircraft = view.findViewById(R.id.sim_txt_aircraft);
        txt_callsign = view.findViewById(R.id.sim_txt_callsign);
        txt_atis = view.findViewById(R.id.sim_txt_atis);
        cbx_departure = view.findViewById(R.id.sim_cbx_dep);
        cbx_arrival = view.findViewById(R.id.sim_cbx_arr);

        txt_atc = view.findViewById(R.id.sim_txt_atc);
        txt_you = view.findViewById(R.id.sim_txt_you);
        lbl_atc = view.findViewById(R.id.sim_txt_atc_lbl);

        btn_record = view.findViewById(R.id.btn_record);
        btn_record.setOnTouchListener(this);

        answers = new ArrayList<>();
        progress = 0;
        generateNewParameters();

        // Update the Help panel
        TableLayout helpPanel = view.findViewById(R.id.sim_lyt_table_help);
        helpPanel.setVisibility(
            settings.getBoolean(getString(R.string.settings_sim_help), false)
            ? View.VISIBLE : View.GONE
        );
        helpPanel.findViewById(R.id.sim_atc).setVisibility(
            settings.getBoolean(getString(R.string.settings_sim_help_atc), true)
            ? View.VISIBLE : View.GONE
        );
        helpPanel.findViewById(R.id.sim_you).setVisibility(
            settings.getBoolean(getString(R.string.settings_sim_help_you), true)
            ? View.VISIBLE : View.GONE
        );

        audio = (AudioManager) activity
                .getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);

        cbx_arrival.setOnItemSelectedListener(this);
        cbx_departure.setOnItemSelectedListener(this);

        try {
            int dep = english ? R.raw.sim_dep_en : R.raw.sim_dep_de;
            int arr = english ? R.raw.sim_arr_en : R.raw.sim_arr_de;
            phrases = new ArrayList<>(parseConversation(dep, english));
            departurePhraseSize = phrases.size();
            phrases.addAll (parseConversation(arr,english));
        } catch (IOException ioe) {
            Log.e("Simulator", "" + ioe.getLocalizedMessage());
            Crashlytics.logException(ioe);
            Toast.makeText(
                    activity,
                    "Upps, da ging leider etwas schief...",
                    Toast.LENGTH_LONG
            ).show();
            activity.finish();
        }

        updateComboBoxes();

        // Update the Callsign from preferences
        txt_callsign.setText(settings.getString(
                getString(R.string.settings_sim_callsign),
                getString(R.string.sim_default_callsign)));

        // Update the Aircraft Type from preferences
        txt_aircraft.setText(settings.getString(
                getString(R.string.settings_sim_aircraft),
                getString(R.string.sim_default_aircraft)
        ));

        // After all texts have been setup correctly, we parse the values to new phrase params
        generateNewParameters();
        txt_you.setText(phrases.get(0).toString());
        highlightHelp(false, true);

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{ Manifest.permission.RECORD_AUDIO },
                    PERMISSION_RECORD_AUDIO
            );
        }

        final WifiManager wifi = (WifiManager) activity
                    .getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

        recorder = SpeechRecognizer.createSpeechRecognizer(getContext());
        recorder.destroy(); // old one
        recorder.setRecognitionListener(new VoiceRecognizer(this, view));
        recordIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.getPackageName())
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, english ? "en-US" : "de-DE");

        atc = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    // Configure ATC
                    atc.setLanguage(english ? Locale.US : Locale.GERMANY);
                    atc.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            enableButton();
                        }

                        @Override
                        public void onError(String utteranceId) {
                            enableButton();
                        }

                        private void enableButton() {
                            Activity act = getActivity();
                            if (act == null) return;
                            act.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn_record.setEnabled(true);
                                    highlightHelp(false, true);
                                }
                            });
                        }
                    });

                    if (wifi != null && wifi.isWifiEnabled()) return;

                    Activity act = getActivity();
                    if (act == null) return;
                    new MaterialAlertDialogBuilder(act)
                            .setMessage(getString(R.string.sim_init))
                            .setNegativeButton(getString(R.string.negative), null)
                            .setNeutralButton(getString(R.string.btn_info), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, int which) {
                                    Activity act = getActivity();
                                    if (act == null) return;
                                    new MaterialAlertDialogBuilder(act)
                                            .setMessage(getString(R.string.sim_info))
                                            .setPositiveButton("Roger", null)
                                            .show();
                                }
                            })
                            .setPositiveButton(getString(R.string.positive), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which){
                                    if (wifi != null) wifi.setWifiEnabled(true);
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }
        });

        return view;
    }

    private void restart() {
        FragmentManager manager = getFragmentManager();
        if (manager == null) return;
        manager.beginTransaction()
                .detach(SimulatorFragment.this)
                .attach(SimulatorFragment.this)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        atc.shutdown();
        try {
            if (recorder != null) recorder.destroy();
        } catch (IllegalArgumentException e) {
            // if on some devices random error occurs, try just to cancel
            recorder.cancel();
        }
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If the record button got pushed the first time, send it to Crashlytics
                if (!loggedLevelStart) {
                    if (Answers.getInstance() != null) Answers.getInstance().logLevelStart(new LevelStartEvent()
                            .putLevelName(getString(R.string.simulator))
                            .putCustomAttribute(getString(R.string.language), getString(english ? R.string.settings_sim_language_en : R.string.settings_sim_language_de))
                            .putCustomAttribute(getString(R.string.settings_sim_level), getDifficultyName()));
                    loggedLevelStart = true;
                }
                startTime = new Date().getTime();
                if (!settings.getBoolean(getString(R.string.settings_sim_recorder), false))
                    recorder.startListening(recordIntent);
                break;

            case MotionEvent.ACTION_UP:
                if (!settings.getBoolean(getString(R.string.settings_sim_recorder), false)) {
                    if (new Date().getTime() - startTime < 500) {
                        Snackbar.make(txt_aircraft, R.string.msg_record_explanation, Snackbar.LENGTH_SHORT).show();
                    }
                    recorder.stopListening();
                } else onHeard("");    // if speechrecognition is turned off, simulate nothing said
                break;
        }
        return false;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Update the AIRPORT parameter
        Phrase.Params.AIRPORT = (showedDepartureFinishMessage ? cbx_arrival : cbx_departure).getSelectedItem().toString();
        Phrase phrase = phrases.get(progress);
        (phrase.isFromATC() ? txt_atc : txt_you).setText(phrase.toString());
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void highlightHelp(boolean atc, boolean you) {
        txt_atc.setTypeface(null, atc ? Typeface.BOLD : Typeface.NORMAL);
        txt_you.setTypeface(null, you ? Typeface.BOLD : Typeface.NORMAL);
        txt_atc.setTextColor(getResources().getColor(atc ? R.color.colorPrimaryDark : R.color.black));
        txt_you.setTextColor(getResources().getColor(you ? R.color.colorPrimaryDark : R.color.black));
    }
    private boolean isDepartureFinished() { return progress >= departurePhraseSize;}
    private boolean isArrivalFinished() {return progress >= phrases.size();}

    private void updateComboBoxes() {
        cbx_departure.setEnabled(progress == 0);
        cbx_arrival.setEnabled(!isDepartureFinished());
    }

    private boolean shallRepeat(String msg) {
        String m = msg.toLowerCase();
        return m.contains("say again") || m.contains("wiederholen");
    }

    void onHeard(String msg) {
        // On heard can only be called, if button if active, if arrival is not yet complete...

        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) < .1)
            Snackbar.make(btn_record, R.string.sim_info_volume, Snackbar.LENGTH_LONG).show();

        // Check if the user has understood the last message, if not we do nothing
        // and let ATC repeat its last message, if there is a last one
        if (shallRepeat(msg) && progress > 0) {
            progress--;
        }
        else {
            Phrase phrase = phrases.get(progress);
            progress++;
            updateComboBoxes();

            // Compare the next phrase with the said message
            Spanned comparison = phrase.compareWith(msg, Color.GREEN);
            totalSuccessRates += phrase.getSuccessRate();
            answeredFromYou++;
            answers.add((Spanned) TextUtils.concat(Html.fromHtml(getString(R.string.sim_lbl_you)), comparison));

            // If you said the last departure phrase, show message
            if (isDepartureFinished() && !showedDepartureFinishMessage) {
                generateNewParameters();
                Snackbar.make(txt_you, getString(R.string.sim_msg_departure_complete, Phrase.Params.AIRPORT), Snackbar.LENGTH_LONG).show();
                showedDepartureFinishMessage = true;
            }
            // If you said the last arrival phrase, show result dialog and don't continue
            if (isArrivalFinished()) {
                btn_record.setEnabled(false);
                createResultDialog().show();
                return;
            }
        }

        // If not yet finished, only continue when the next phrase is from ATC
        final Phrase next = phrases.get(progress);
        if (!next.isFromATC()) {
            // Update the help "next phrase from pilot" text view
            txt_you.setText(next.toString());
            lbl_atc.setText("");
            txt_atc.setText("");
            highlightHelp(false, true);
            return;
        }


        // When next phrase is from ATC, permit the talking of the user ...
        btn_record.setEnabled(false);

        // Do the talking delayed
        new Handler().postDelayed(new Runnable() {
           @Override
           public void run() {
               // Let ATC talk the next Phrase
               speak(next);
               progress++;

               // Update the help, what is being said by ATC
               String help = Phrase.Params.AIRPORT + " " + next.getSender() + ": ";
               lbl_atc.setText(help);
               txt_atc.setText(next.toString());
               highlightHelp(true, false);


               // If ATC said the last departure  message, show the message
               if (isDepartureFinished() && !showedDepartureFinishMessage) {
                    generateNewParameters();
                    Snackbar.make(txt_you, getString(R.string.sim_msg_departure_complete, Phrase.Params.AIRPORT), Snackbar.LENGTH_LONG).show();
                    showedDepartureFinishMessage = true;
               }
               // If ATC said the last arrival message, show resulting dialog
               if (isArrivalFinished()) {
                   btn_record.setEnabled(false);
                   createResultDialog().show();
                   return;
               }

               // Update the help "next phrase from pilot" text view
               txt_you.setText(phrases.get(progress).toString());

           }
       }, 1000);

    }
    private void speak(Phrase phrase) {
        Log.i("Simulator", "ATC: " + phrase.toString());
        answers.add(Html.fromHtml("<i>ATC:</i> " + phrase));
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Simulator");
        atc.speak(phrase.makePronounceable(), TextToSpeech.QUEUE_ADD, params);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() != R.id.menu_restart) return false;
        Activity activity = getActivity();
        if (activity == null) return false;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.restart)
                .setMessage(R.string.restart_alert)
                .setNegativeButton(R.string.negative, null)
                .setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restart();
                        dialog.dismiss();
                    }
                }).show();
        return true;
    }

}
