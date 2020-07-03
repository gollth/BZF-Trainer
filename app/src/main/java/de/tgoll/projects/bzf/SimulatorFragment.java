package de.tgoll.projects.bzf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tgoll.projects.bzf.databinding.SimChatMessageAtcBinding;
import de.tgoll.projects.bzf.databinding.SimChatMessageYouBinding;

import static com.google.firebase.analytics.FirebaseAnalytics.Event.LEVEL_END;
import static com.google.firebase.analytics.FirebaseAnalytics.Event.LEVEL_START;
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
    private Spinner cbx_departure, cbx_arrival;
    private LinearLayout lyt_chat;
    private ScrollView lyt_scroller;
    private @Nullable SimChatMessageYouBinding firstChatMessage;

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
    private FirebaseAnalytics firebase;

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
    @SuppressWarnings("RegExpRedundantEscape")
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
        Phrase.Params.CALLSIGN = txt_callsign.getText().toString().toUpperCase().replace("-", "");
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
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.LEVEL_NAME, getString(R.string.simulator));
        bundle.putInt(FirebaseAnalytics.Param.SCORE, correct);
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, correct > 50);
        firebase.logEvent(LEVEL_END, bundle);

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
                .setNegativeButton(R.string.statistics, (d, which) -> {
                    d.dismiss();
                    try {
                        TitleActivity activity = (TitleActivity) getActivity();
                        if (activity == null) return;
                        activity.showFragment(getString(R.string.statistics), true);
                    } catch (ClassCastException cce){
                        String error = "SimulatorFragment: Error casting getActivity() to TitleActivity" + cce.getMessage();
                        Log.e("BZF", error);
                        FirebaseCrashlytics.getInstance().log(error);
                    }
                })
                .setPositiveButton(getString(R.string.restart), (d, which) -> {
                    d.dismiss();
                    restart();
                })
                .setTitle(getString(R.string.msg_finish_sim, correct))
                .setIcon(correct >= 50 ? R.drawable.like : R.drawable.dislike).create();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebase = FirebaseAnalytics.getInstance(requireContext());
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    @SuppressLint("ClickableViewAccessibility")
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
        lyt_scroller = view.findViewById(R.id.sim_lyt_table_help);
        lyt_chat = view.findViewById(R.id.sim_lyt_table_help_chat);

        if (TitleActivity.isDarkMode(requireContext())) {
            // Dark Mode
            ImageView arrival = view.findViewById(R.id.icon_arrival);
            ImageView departure = view.findViewById(R.id.icon_departure);
            arrival.setImageDrawable(activity.getDrawable(R.drawable.arrival_dark));
            departure.setImageDrawable(activity.getDrawable(R.drawable.departure_dark));
        }

        // Remove views from debugging layout
        lyt_chat.removeAllViews();
        btn_record = view.findViewById(R.id.btn_record);
        btn_record.setOnTouchListener(this);

        answers = new ArrayList<>();
        progress = 0;
        generateNewParameters();

        // Update the Help panel
        lyt_scroller.setVisibility(
            settings.getBoolean(getString(R.string.settings_sim_help), false)
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
            FirebaseCrashlytics.getInstance().recordException(ioe);
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
                getString(R.string.sim_default_callsign)
        ).toUpperCase());

        // Update the Aircraft Type from preferences
        txt_aircraft.setText(settings.getString(
                getString(R.string.settings_sim_aircraft),
                getString(R.string.sim_default_aircraft)
        ));

        // After all texts have been setup correctly, we parse the values to new phrase params
        generateNewParameters();
        createChatMessage(inflater, phrases.get(0));

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

        atc = new TextToSpeech(getContext(), status -> {
            if(status != TextToSpeech.SUCCESS) return;
            // Configure ATC
            atc.setLanguage(english ? Locale.US : Locale.GERMANY);
            atc.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    enableButton();
                    // Update the help "next phrase from pilot" text view
                    if (progress >= phrases.size()) return;
                    requireActivity().runOnUiThread(() -> createChatMessage(inflater, phrases.get(progress)));
                }

                @Override
                public void onError(String utteranceId) {
                    enableButton();
                }
            });

            if (wifi != null && wifi.isWifiEnabled()) return;

            Activity act = getActivity();
            if (act == null) return;
            new MaterialAlertDialogBuilder(act)
                .setMessage(getString(R.string.sim_init))
                .setNegativeButton(getString(R.string.negative), null)
                .setNeutralButton(getString(R.string.btn_info), (dialog, which) -> {
                    Activity a = getActivity();
                    if (a == null) return;
                    new MaterialAlertDialogBuilder(act)
                            .setMessage(getString(R.string.sim_info))
                            .setPositiveButton("Roger", null)
                            .show();
                })
                .setPositiveButton(getString(R.string.positive), (dialog, which) -> {
                    if (wifi != null) wifi.setWifiEnabled(true);
                    dialog.dismiss();

                })
                .show();
        });

        return view;
    }

    private void restart() {
        requireActivity()
            .getSupportFragmentManager()
            .beginTransaction()
            .detach(SimulatorFragment.this)
            .attach(SimulatorFragment.this)
            .commit();
    }

    private void enableButton() {
        requireActivity().runOnUiThread(() -> btn_record.setEnabled(true));
    }

    private void scrollFullDown() {
        lyt_scroller.post(() -> lyt_scroller.fullScroll(View.FOCUS_DOWN));
    }

    private void createChatMessage(LayoutInflater inflater, Phrase phrase) {
        createChatMessage(inflater, phrase.isFromATC(), phrase.toString());
    }
    private void createChatMessage(LayoutInflater inflater, boolean fromAtc, String message) {
        if (fromAtc) {
            SimChatMessageAtcBinding binding = DataBindingUtil.inflate(inflater, R.layout.sim_chat_message_atc, lyt_chat, true);
            if (!settings.getBoolean(getString(R.string.settings_sim_help_atc), true)) {
                message = getString(R.string.sim_chat_message_hidden);
            }
            binding.setMessage(message);
            scrollFullDown();
        }
        else {
            SimChatMessageYouBinding binding = DataBindingUtil.inflate(inflater, R.layout.sim_chat_message_you, lyt_chat, true);
            if (!settings.getBoolean(getString(R.string.settings_sim_help_you), true)) {
                message = getString(R.string.sim_chat_message_hidden);
            }
            binding.setMessage(message);

            // Cache the first message, since its message can change, when the comboboxes change
            if (firstChatMessage == null) firstChatMessage = binding;
        }
        scrollFullDown();
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
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.LEVEL_NAME, getString(R.string.simulator));
                    bundle.putString(getString(R.string.language), getString(english ? R.string.settings_sim_language_en : R.string.settings_sim_language_de));
                    bundle.putString(getString(R.string.settings_sim_level), getDifficultyName());
                    firebase.logEvent(LEVEL_START, bundle);
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
                } else onHeard("");    // if speech recognition is turned off, simulate nothing said
                break;
        }
        return false;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Update the AIRPORT parameter
        Phrase.Params.AIRPORT = (showedDepartureFinishMessage ? cbx_arrival : cbx_departure).getSelectedItem().toString();
        if (firstChatMessage == null) return;
        firstChatMessage.setMessage(phrases.get(0).toString());
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private boolean isDepartureFinished() {
        return progress >= departurePhraseSize;
    }
    private boolean isArrivalFinished() {
        return progress >= phrases.size();
    }

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
            Spanned comparison = phrase.compareWith(msg);
            totalSuccessRates += phrase.getSuccessRate();
            answeredFromYou++;
            answers.add((Spanned) TextUtils.concat(Html.fromHtml(getString(R.string.sim_lbl_you)), comparison));

            // If you said the last departure phrase, show message
            if (isDepartureFinished() && !showedDepartureFinishMessage) {
                generateNewParameters();
                Snackbar.make(requireView(), getString(R.string.sim_msg_departure_complete, Phrase.Params.AIRPORT), Snackbar.LENGTH_LONG).show();
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
            createChatMessage(inflater, next);
            return;
        }


        // When next phrase is from ATC, permit the talking of the user ...
        btn_record.setEnabled(false);

        // Do the talking delayed
        new Handler().postDelayed(() -> {
           // Let ATC talk the next Phrase
           speak(next);
           progress++;

           // Update the help, what is being said by ATC
           createChatMessage(inflater, next);

           // If ATC said the last departure  message, show the message
           if (isDepartureFinished() && !showedDepartureFinishMessage) {
                generateNewParameters();
                Snackbar.make(requireView(), getString(R.string.sim_msg_departure_complete, Phrase.Params.AIRPORT), Snackbar.LENGTH_LONG).show();
                showedDepartureFinishMessage = true;
           }
           // If ATC said the last arrival message, show resulting dialog
           if (isArrivalFinished()) {
               btn_record.setEnabled(false);
               createResultDialog().show();
           }
       }, 1000);

    }
    private void speak(Phrase phrase) {
        Log.i("Simulator", "ATC: " + phrase.toString());
        answers.add(Html.fromHtml("<i>ATC:</i> " + phrase));
        atc.speak(phrase.makePronounceable(), TextToSpeech.QUEUE_ADD, null, "Simulator");
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
            .setPositiveButton(R.string.positive, (dialog, which) -> {
                restart();
                dialog.dismiss();
            })
            .show();
        return true;
    }

}
