package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.google.firebase.analytics.FirebaseAnalytics.Event.LEVEL_END;
import static com.google.firebase.analytics.FirebaseAnalytics.Event.LEVEL_START;

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
    private int sliderLastQuestion;
    private FirebaseAnalytics analytics;

    static CatalogueFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString("key", key);
        CatalogueFragment fragment = new CatalogueFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analytics = FirebaseAnalytics.getInstance(requireContext());
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        if (args == null) {
            throw new InvalidParameterException("CatalogueFragment is missing the required \"key\" parameter");
        }
        this.key = args.getString("key");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_catalogue, container, false);

        gson = new Gson();
        answers = new SparseArray<>();

        cat = new Catalogue(view.getContext(), key);

        txt_number = view.findViewById(R.id.txt_number);
        txt_questions = view.findViewById(R.id.txt_question);
        txt_answers = view.findViewById(R.id.lyt_ABCD);
        txt_progress = view.findViewById(R.id.txt_progress);
        btn_next = view.findViewById(R.id.btn_next);
        btn_prev = view.findViewById(R.id.btn_prev);
        progress = view.findViewById(R.id.progress);

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_restart:
                new MaterialAlertDialogBuilder(view.getContext())
                        .setTitle(R.string.restart)
                        .setMessage(R.string.restart_alert)
                        .setNegativeButton(R.string.negative, null)
                        .setPositiveButton(R.string.positive, (dialog, which) -> {
                            resetQuestions();
                            dialog.dismiss();
                        }).show();
                return true;

            case R.id.menu_filter:
                new QuestionFilter(requireContext(), key, this::resetQuestions);
                return true;

            default:
                return false;
        }
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
        if (s.isEmpty()) {
            Log.i("BZF", "Could not find previous saved stated, resetting playlist");
            resetQuestions();
        }
        else {
            SavedState state = gson.fromJson(s, SavedState.class);
            Log.i("BZF", "Loading State from " + key + "-state");
            playlist = state.playlist;
            choices = state.choices;

            // if we contain an old state saved in app data, we clear it.
            if (playlist == null) resetQuestions();
            else loadQuestion(state.progress);
        }

        // Set the label formatter after the choices have been initialized
        progress.setLabelFormatter(new CompletedFormatter(choices));
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
        resetQuestions(null);
    }
    private void resetQuestions(List<Integer> filter) {
        settings.edit().remove(key + "-state").apply();

        boolean customPlaylist = filter != null;
        playlist = new ArrayList<>();
        choices = new ArrayList<>();
        for (int i = 0; i < cat.size(); i++) {
            if (customPlaylist && !filter.contains(i+1)) continue;
            playlist.add(i);
            choices.add(-1);    // Not set
        }
        if (settings.getBoolean(getString(R.string.settings_shuffle_questions), false))
            Collections.shuffle(playlist);

        answers.clear();

        progress.setLabelFormatter(new CompletedFormatter(choices));

        // Send to Firebase, that the user started a new trial
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.LEVEL_NAME, key);
        bundle.putString(getString(R.string.settings_shuffle_questions), ""+settings.getBoolean(getString(R.string.settings_shuffle_questions), false));
        bundle.putString(getString(R.string.settings_shuffle_answers), ""+settings.getBoolean(getString(R.string.settings_shuffle_answers), false));
        analytics.logEvent(LEVEL_START, bundle);

        progress.setValueTo(playlist.size());
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
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        // Check if the value changed, if not, slider probably at a bound
        if ((int)value - 1 == sliderLastQuestion) return;

        int question = getProgress();
        if (hasAnsweredOutlierEdgeBetween(sliderLastQuestion, question)) {
            vibrateTick();
        }
        sliderLastQuestion = question;
        loadQuestion(question);
    }

    private void vibrateTick() {
        if (Build.VERSION.SDK_INT < 29) vibrator.vibrate(50);
        else vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
    }
    private void vibrateClick() {
        if (Build.VERSION.SDK_INT < 29) vibrator.vibrate(100);
        else vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
    }
    private void vibrateDoubleClick() {
        if (Build.VERSION.SDK_INT < 29) vibrator.vibrate(new long[]{0, 100, 100, 200}, -1);
        else vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
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
        progress.setValueTo(playlist.size());
        progress.removeOnChangeListener(this);
        progress.setValue(i+1);
        progress.addOnChangeListener(this);
        txt_progress.setText(String.format(getString(R.string.txt_progress), i+1, playlist.size()));
    }

    private void loadQuestion(int i) {
        if (i < 0) i = 0;
        if (i >= playlist.size()) i = playlist.size()-1;

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
        int choice = choices.get(i);
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
                vibrateClick();       // Correct
        }
        else {
            if (isVibrationGloballyEnabled && isVibrationOnFalseEnabled)
                vibrateDoubleClick(); // Wrong

        }
        new Handler().postDelayed(() -> {
            enableInput();
            if (allQuestionsAnswered()) showResultDialog();
            else if (isNotFinalQuestion()) loadQuestion(getProgress()+1);  // load next

        }, (long) (Double.parseDouble(settings.getString(getString(R.string.settings_delay), "1")) * 1000));

    }

    private boolean allQuestionsAnswered() {
        for (Integer choice : choices) if (choice == -1) return false;
        return true;
    }

    private boolean hasAnsweredOutlierEdgeBetween(int a, int b) {
        for (int i = Math.min(a,b); i < Math.max(a,b); i++) {
            try {
                boolean answered = choices.get(i) >= 0;
                boolean previousAnswered = choices.get(i - 1) >= 0;
                if (previousAnswered ^ answered) return true;

            } catch (IndexOutOfBoundsException ignore) {
                // This only happens, when the `lower` limit zero
                // that usually happens on the first time the slider is touched
                // because sliderLastQuestion is not setup properly yet.
                // In this rare case, we just skip this loop iteration and check the next one
            }
        }
        return false;
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

        new MaterialAlertDialogBuilder(view.getContext())
                .setTitle(getString(success ? R.string.msg_finish_pass : R.string.msg_finish_fail))
                .setMessage(String.format(getString(R.string.msg_finish), Math.round(trial.getSuccessRate() * 100)))
                .setIcon(success ? R.drawable.like : R.drawable.dislike)
                .setNegativeButton(R.string.statistics, (dialog, which) -> {
                    dialog.dismiss();
                    try {
                        TitleActivity activity = (TitleActivity) getActivity();
                        if (activity == null) return;
                        activity.showFragment(getString(R.string.statistics), true);
                    } catch (ClassCastException cce){
                        String error = "CatalogueFragment: Error casting getActivity() to TitleActivity" + cce.getMessage();
                        Log.e("BZF", error);
                        FirebaseCrashlytics.getInstance().log(error);
                    }
                })
                .setPositiveButton(R.string.restart, (dialog, which) -> {
                    resetQuestions();
                    dialog.dismiss();
                }).show();

        // Send to Firebase, that the user has finished the catalogue
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.LEVEL_NAME, key);
        bundle.putDouble(FirebaseAnalytics.Param.SCORE, trial.getSuccessRate());
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, success);
        analytics.logEvent(LEVEL_END, bundle);
    }

    private boolean isNotFinalQuestion() {
        return getProgress() != playlist.size()-1;
    }

    private void unhighlightAnswers(boolean alsoClearCheck) {
        int color = Util.lookupColor(requireContext(), R.attr.colorOnBackground);
        for(RadioButton button : buttons) {
            button.setTypeface(Typeface.DEFAULT);
            button.setTextColor(color);
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
        option.setTextColor(Util.lookupColor(requireContext(), R.attr.colorControlHighlight));
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

    public static class SavedState {
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

    public static final class CompletedFormatter implements Slider.LabelFormatter {

        private final List<Integer> choices;

        CompletedFormatter(List<Integer> choices) {
            this.choices = choices;
        }

        @NonNull
        @Override
        public String getFormattedValue(float value) {
            return (choices.get((int) value-1) >= 0) ? "☑" : "☐";
        }
    }
}
