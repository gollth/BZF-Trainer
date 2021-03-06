package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

class QuestionTooltipOnChartValueSelectedListener implements OnChartValueSelectedListener,
        DialogInterface.OnDismissListener {

    private final List<Integer> questions;
    private final Context context;
    private final BarChart chart;
    private final Catalogue catalogue;
    private final String key;
    private final AlertDialog popup;
    private final TextView txt_number;
    private final TextView txt_question;
    private final RadioButton[] buttons;
    private final TextView txt_stat;
    private List<Trial> trials;

    @SuppressLint("InflateParams")
    QuestionTooltipOnChartValueSelectedListener(@NonNull Context context, @NonNull BarChart chart, String key, List<Integer> questions) {
        this.context = context;
        this.chart = chart;
        this.catalogue = new Catalogue(context, key);
        this.key = key;
        this.questions = questions;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_catalogue, null);
        this.txt_number = view.findViewById(R.id.txt_number);
        this.txt_question = view.findViewById(R.id.txt_question);
        this.txt_stat = view.findViewById(R.id.txt_stat);
        this.buttons = new RadioButton[]{
                view.findViewById(R.id.A),
                view.findViewById(R.id.B),
                view.findViewById(R.id.C),
                view.findViewById(R.id.D)
        };
        view.findViewById(R.id.lyt_ABCD).setClickable(false);
        this.popup = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setOnDismissListener(this)
                .create();

        trials = Util.getTrials(catalogue, PreferenceManager.getDefaultSharedPreferences(context), new Gson(), key);
    }

    private void updateQuestionStatistics(int number) {
        if (txt_stat.getVisibility() != View.VISIBLE) return;
        Pair<Integer, Integer> ratio = Util.calculateQuestionRatio(trials, number);
        txt_stat.setText(String.format(context.getString(R.string.catalogue_stat), ratio.first, ratio.second));
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        int number = questions.get((int) e.getX());

        String question = catalogue.getQuestion(number-1);
        String[] parts = question.split("\\d{1,4}.\\s", 2);
        String format = key.equals("azf") ? "Question %d" : "Frage %d";

        updateQuestionStatistics(number-1);
        txt_number.setText(String.format(Locale.getDefault(), format, number));
        txt_question.setText(parts[1]);

        int solution = catalogue.getSolution(number-1);
        int normal = Util.lookupColor(context, R.attr.colorOnBackground);
        int highlight = Util.lookupColor(context, R.attr.colorControlHighlight);

        for (int n = 0; n < 4; n++) {
            boolean correct = n == solution;
            buttons[n].setEnabled(false);
            buttons[n].setText(catalogue.getAnswer(number-1, n));
            buttons[n].setTypeface(correct ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            buttons[n].setTextColor(correct
                    ? highlight
                    : normal
            );
            if (correct) buttons[n].setChecked(true);
        }

        popup.show();
    }

    @Override
    public void onNothingSelected() {
        popup.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        chart.highlightValues(null); // clear
    }
}
