package de.tgoll.projects.bzf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;
import com.google.gson.Gson;

public class CatalogueActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final Set<String> EMPTY_SET = new HashSet<>();

    private TextView txt_questions;
    private RadioGroup txt_answers;
    private RadioButton[] buttons;
    private SeekBar progress;
    private TextView txt_progress;
    private Button btn_next, btn_prev;

    private List<Integer> playlist;
    private List<Integer> choices;
    private SparseArray<Integer[]> answers;

    private Vibrator vibrator;
    private SharedPreferences settings;
    private Gson gson;
    private String key;
    private Catalogue cat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue);

        key = getIntent().getStringExtra("key");

        cat = new Catalogue(getApplicationContext(), key);

        txt_questions = findViewById(R.id.txt_question);
        txt_answers = findViewById(R.id.lyt_ABCD);
        txt_progress = findViewById(R.id.txt_progress);
        btn_next = findViewById(R.id.btn_next);
        btn_prev = findViewById(R.id.btn_prev);
        progress = findViewById(R.id.progress);
        progress.setOnSeekBarChangeListener(this);
        progress.setMax(cat.size() - 1);

        answers = new SparseArray<>();

        vibrator = (Vibrator)  getSystemService(Context.VIBRATOR_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        buttons = new RadioButton[] {
                findViewById(R.id.A),
                findViewById(R.id.B),
                findViewById(R.id.C),
                findViewById(R.id.D)
        };

        enableInput();

        gson = new Gson();

    }

    @Override
    protected void onStart() {
        super.onStart();

        String s = settings.getString(key + "-state", "");
        if (s.isEmpty()) resetQuestions();
        else {
            SavedState state = gson.fromJson(s, SavedState.class);
            Log.i("BZF", "Loading State from " + key + "-state");
            playlist = state.playlist;
            choices = state.choices;

            // if we contain an old state saved in app data, we clear it.
            if (playlist == null || playlist.size() != cat.size()) resetQuestions();
            else loadQuestion(state.progress);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i("BZF", "Saving Catalogue state in " + key + "-state");
        // Only save, it at least one question has been answered
        for (Integer c : choices)  if (c != -1) {
            settings.edit().putString(key + "-state", gson.toJson(new SavedState(playlist, choices, getProgress()))).apply();
            break;
        }

    }

    public void resetQuestions() {
        settings.edit().remove(key + "-state").apply();

        playlist = new ArrayList<>();
        choices = new ArrayList<>();
        for (int i = 0; i < cat.size(); i++) {
            playlist.add(i);
            choices.add(-1);    // Not set
        }
        if (settings.getBoolean(getString(R.string.settings_shuffle_questions), false))
            Collections.shuffle(playlist);

        answers.clear();

        // Send to Crashlytics, that the user started a new trial
        if (Answers.getInstance() != null) Answers.getInstance().logLevelStart(new LevelStartEvent()
            .putLevelName(key)
            .putCustomAttribute(getString(R.string.settings_shuffle_questions), ""+settings.getBoolean(getString(R.string.settings_shuffle_questions), false))
            .putCustomAttribute(getString(R.string.settings_shuffle_answers), ""+settings.getBoolean(getString(R.string.settings_shuffle_answers), false))
        );

        loadQuestion(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_restart:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.restart))
                        .setMessage(getString(R.string.restart_alert))
                        .setNegativeButton(getString(R.string.negative), null)
                        .setPositiveButton(getString(R.string.positive), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resetQuestions();
                            }
                        }).show();
                return true;

            case R.id.menu_statistics:
                startActivity(new Intent(this, StatisticsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onNextOrPref(View view) {
        switch (view.getId()) {
            case R.id.btn_next: loadQuestion(); break;
            case R.id.btn_prev: loadQuestion(getProgress()-1); break;
        }

    }

    public void updateButtons () {
        btn_next.setEnabled(isNotFinalQuestion());
        btn_prev.setEnabled(getProgress() != 0);
    }

    @Override public void onStartTrackingTouch(SeekBar s) {}
    @Override public void onStopTrackingTouch (SeekBar s) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;
        loadQuestion(progress);
    }

    public int getProgress() {
        return progress.getProgress();
    }

    public int getCurrentQuestion() { return playlist.get(getProgress()); }

    public void loadQuestion() throws IndexOutOfBoundsException {
        loadQuestion(getProgress() + 1);
    }

    private void setQuestionProgress(int i) {
        progress.setProgress(i);
        txt_progress.setText(String.format(getString(R.string.txt_progress), i + 1, cat.size()));
    }

    public void loadQuestion(int i) throws IndexOutOfBoundsException {
        setQuestionProgress(i);
        updateButtons();

        if (settings.getBoolean(getString(R.string.settings_shuffle_answers), false))
            shuffleAnswerFields(i);

        unhighlightAnswers(true);
        txt_questions.setText(cat.getQuestion(playlist.get(i)));
        int choice = choices.get(i);
        if (choice != -1) highlightCorrectAnswer();
        for (int n = 0; n < 4; n++) {
            buttons[n].setText(cat.getAnswer(playlist.get(i), n));
            if (n == choice) buttons[n].setChecked(true);
        }
    }

    public void onCheckboxSelect (View view) {
        for(int i = 0; i < 4; i++)
            if (buttons[i].getId() == view.getId())
                choices.set(getProgress(), i);
        startHighlightAnimation();

    }

    public void ignoreInput() {
        for(RadioButton button : buttons) button.setClickable(false);
    }
    public void enableInput() {
        for(RadioButton button : buttons) button.setClickable(true);
    }

    public void startHighlightAnimation() {
        ignoreInput();
        boolean isVibrationGloballyEnabled = settings.getBoolean(getString(R.string.settings_vibrate), true);
        boolean isVibrationOnFalseEnabled = settings.getBoolean(getString(R.string.settings_vibrate_false), true);
        boolean isVibrationOnTrueEnabled = settings.getBoolean(getString(R.string.settings_vibrate_correct), true);

        if (highlightCorrectAnswer()) {
            if (isVibrationGloballyEnabled && isVibrationOnTrueEnabled)
                vibrator.vibrate(200);                          // Correct
        }
        else {
            if (isVibrationGloballyEnabled && isVibrationOnFalseEnabled)
                vibrator.vibrate(new long[]{0, 100, 100, 200}, -1); // Wrong

        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                enableInput();
                if (allQuestionsAnswered()) showResultDialog();
                else if (isNotFinalQuestion()) loadQuestion();

            }
        }, (long) (Double.parseDouble(settings.getString(getString(R.string.settings_delay), "1")) * 1000));

    }

    public boolean allQuestionsAnswered() {
        for (Integer choice : choices) if (choice == -1) return false;
        return true;
    }

    public void showResultDialog() {
        SharedPreferences.Editor e = settings.edit();
        Trial trial = new Trial(key, cat, playlist, choices);
        Set<String> history = new HashSet<>(settings.getStringSet(key + "-history", EMPTY_SET));
        history.add(gson.toJson(trial));
        e.remove(key + "-history");
        e.putStringSet(key + "-history", history);
        e.apply();
        boolean success = trial.getSuccessRate() > 0.75;

        new AlertDialog.Builder(CatalogueActivity.this)
                .setTitle(getString(success ? R.string.msg_finish_pass : R.string.msg_finish_fail))
                .setMessage(String.format(getString(R.string.msg_finish), Math.round(trial.getSuccessRate() * 100)))
                .setIcon(success ? R.drawable.like : R.drawable.dislike)
                .setPositiveButton(getString(R.string.back_to_title), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetQuestions();
                        finish();
                    }
                }).show();

        // Send to Crashlytics, that the user has successfully finished the catalogue
        if (Answers.getInstance() != null) Answers.getInstance().logLevelEnd(new LevelEndEvent()
            .putLevelName(key)
            .putScore(trial.getSuccessRate())
            .putSuccess(success));

    }

    public boolean isNotFinalQuestion() {
        return getProgress() != cat.size()-1;
    }

    public void unhighlightAnswers(boolean alsoClearCheck) {
        for(RadioButton button : buttons) {
            button.setTypeface(Typeface.DEFAULT);
            button.setTextColor(getResources().getColor(R.color.colorDefaultText));
        }
        if (alsoClearCheck) txt_answers.clearCheck();
    }

    /** Highlights the correct answer of the four possible options
     * @return true if the selected answer was correct, false otherwise
     */
    public boolean highlightCorrectAnswer() {
        unhighlightAnswers(false);
        RadioButton option = buttons[cat.getSolution(getCurrentQuestion())];
        option.setTypeface(Typeface.DEFAULT_BOLD);
        option.setTextColor(getResources().getColor(R.color.colorHighlight));
        return option.isChecked();
    }

    public void shuffleAnswerFields(int question) {
        Integer[] idx = answers.get(question);
        if (idx == null) {
            List<Integer> list = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(list);
            answers.put(question, (Integer[]) list.toArray());
        }
        txt_answers.removeAllViews();
        for(int i : answers.get(question))
            txt_answers.addView(buttons[i]);
    }


    public class SavedState {
        private final List<Integer> playlist;
        private final List<Integer> choices;
        private final int progress;

        public SavedState () {
            this.playlist = new ArrayList<>();
            this.choices = new ArrayList<>();
            this.progress = 0;
        }

        public SavedState(List<Integer> playlist, List<Integer> choices, int progress) {
            this.playlist = playlist;
            this.choices = choices;
            this.progress = progress;
        }
    }
}
