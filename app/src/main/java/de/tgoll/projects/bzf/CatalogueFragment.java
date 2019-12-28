package de.tgoll.projects.bzf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CatalogueFragment extends Fragment implements
        Slider.OnChangeListener,
        RadioButton.OnCheckedChangeListener,
        MaterialButton.OnClickListener {

    static final Set<String> EMPTY_SET = new HashSet<>();

    private View view;
    private TextView txt_number;
    private TextView txt_questions;
    private RadioGroup txt_answers;
    private RadioButton[] buttons;
    private Slider progress;
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

    public CatalogueFragment(String key) {
        this.key = key;
        gson = new Gson();
        answers = new SparseArray<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_catalogue, container, false);

        cat = new Catalogue(view.getContext(), key);

        txt_number = view.findViewById(R.id.txt_number);
        txt_questions = view.findViewById(R.id.txt_question);
        txt_answers = view.findViewById(R.id.lyt_ABCD);
        txt_progress = view.findViewById(R.id.txt_progress);
        btn_next = view.findViewById(R.id.btn_next);
        btn_prev = view.findViewById(R.id.btn_prev);
        progress = view.findViewById(R.id.progress);
        progress.setValueTo(cat.size());

        vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(view.getContext());

        buttons = new RadioButton[] {
                view.findViewById(R.id.A),
                view.findViewById(R.id.B),
                view.findViewById(R.id.C),
                view.findViewById(R.id.D)
        };

        btn_prev.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        setRadioCheckListener(this);

        enableInput();

        return view;
    }

    private void setTextSizes(float sp) {
        txt_number.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp + 10);
        txt_questions.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        for (RadioButton button : buttons) {
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        float fontSize = Float.parseFloat(settings.getString(getString(R.string.settings_text_size), "14"));
        setTextSizes(fontSize);

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
    public void onStop() {
        super.onStop();

        Log.i("BZF", "Saving Catalogue state in " + key + "-state");
        // Only save, it at least one question has been answered
        for (Integer c : choices)  if (c != -1) {
            settings
                .edit()
                .putString(
                    key + "-state",
                    gson.toJson(new SavedState(playlist, choices, getProgress())))
                .apply();
            break;
        }

    }

    private void resetQuestions() {
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_next: loadQuestion(getProgress()+1); break;
            case R.id.btn_prev: loadQuestion(getProgress()-1); break;
        }
    }

    private void updateButtons () {
        btn_next.setEnabled(isNotFinalQuestion());
        btn_prev.setEnabled(getProgress() != 0);
        boolean enabled = choices.get(getProgress()) == -1;
        for (RadioButton button : buttons) {
            button.setEnabled(enabled);
        }
    }

    @Override
    public void onValueChange(Slider slider, float value) {
        loadQuestion(getProgress());
    }

    private int getProgress() {
        return (int)progress.getValue()-1;
    }

    private int getCurrentQuestion() { return playlist.get(getProgress()); }

    private void setRadioCheckListener(RadioButton.OnCheckedChangeListener listener) {
        for (RadioButton button : buttons) {
            button.setOnCheckedChangeListener(listener);
        }
    }

    private void setQuestionProgress(int i) {
        progress.setOnChangeListener(null);
        progress.setValue(i+1);
        progress.setOnChangeListener(this);
        txt_progress.setText(String.format(getString(R.string.txt_progress), i+1, cat.size()));
    }

    private void loadQuestion(int i) throws IndexOutOfBoundsException {
        setQuestionProgress(i);
        updateButtons();

        if (settings.getBoolean(getString(R.string.settings_shuffle_answers), false))
            shuffleAnswerFields(i);

        unhighlightAnswers(true);
        int number = playlist.get(i);
        String question = cat.getQuestion(number);
        String[] parts = question.split("\\d{1,4}.\\s", 2);
        String format = key.equals("azf") ? "Question %d" : "Frage %d";
        txt_number.setText(String.format(Locale.getDefault(), format, number+1));
        txt_questions.setText(parts[1]);
        int choice = choices.get(number);
        if (choice != -1) highlightCorrectAnswer();
        for (int n = 0; n < 4; n++) {
            buttons[n].setText(cat.getAnswer(number, n));
            if (n == choice) buttons[n].setChecked(true);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) return;
        if (choices.get(getProgress()) != -1) return;
        for(int i = 0; i < 4; i++)
            if (buttons[i].getId() == buttonView.getId())
                choices.set(getProgress(), i);
        startHighlightAnimation();
    }

    private void ignoreInput() {
        for(RadioButton button : buttons) button.setClickable(false);
    }
    private void enableInput() {
        for(RadioButton button : buttons) button.setClickable(true);
    }

    private void startHighlightAnimation() {
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
                else if (isNotFinalQuestion()) loadQuestion(getProgress()+1);  // load next

            }
        }, (long) (Double.parseDouble(settings.getString(getString(R.string.settings_delay), "1")) * 1000));

    }

    private boolean allQuestionsAnswered() {
        for (Integer choice : choices) if (choice == -1) return false;
        return true;
    }

    private void showResultDialog() {
        Trial trial = new Trial(key, cat, playlist, choices);
        Set<String> history = new HashSet<>(settings.getStringSet(key + "-history", EMPTY_SET));
        history.add(gson.toJson(trial));
        settings.edit()
            .remove(key + "-history")
            .putStringSet(key + "-history", history)
            .apply();
        boolean success = trial.getSuccessRate() > 0.75;

        new AlertDialog.Builder(view.getContext())
                .setTitle(getString(success ? R.string.msg_finish_pass : R.string.msg_finish_fail))
                .setMessage(String.format(getString(R.string.msg_finish), Math.round(trial.getSuccessRate() * 100)))
                .setIcon(success ? R.drawable.like : R.drawable.dislike)
                .setNegativeButton(R.string.statistics, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        FragmentManager manager = getFragmentManager();
                        if (manager == null) return;
                        manager.beginTransaction()
                                .replace(R.id.fragment, new StatisticsFragment())
                                .commit();
                    }
                })
                .setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetQuestions();
                        dialog.dismiss();
                    }
                }).show();

        // Send to Crashlytics, that the user has successfully finished the catalogue
        if (Answers.getInstance() != null) Answers.getInstance().logLevelEnd(new LevelEndEvent()
            .putLevelName(key)
            .putScore(trial.getSuccessRate())
            .putSuccess(success));

    }

    private boolean isNotFinalQuestion() {
        return getProgress() != cat.size()-1;
    }

    private void unhighlightAnswers(boolean alsoClearCheck) {
        for(RadioButton button : buttons) {
            button.setTypeface(Typeface.DEFAULT);
            button.setTextColor(getResources().getColor(R.color.black));
        }
        if (alsoClearCheck) txt_answers.clearCheck();
    }

    /** Highlights the correct answer of the four possible options
     * @return true if the selected answer was correct, false otherwise
     */
    private boolean highlightCorrectAnswer() {
        unhighlightAnswers(false);
        RadioButton option = buttons[cat.getSolution(getCurrentQuestion())];
        option.setTypeface(Typeface.DEFAULT_BOLD);
        option.setTextColor(getResources().getColor(R.color.colorHighlight));
        return option.isChecked();
    }

    private void shuffleAnswerFields(int question) {
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
