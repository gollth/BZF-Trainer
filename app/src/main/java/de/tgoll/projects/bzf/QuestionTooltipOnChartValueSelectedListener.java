package de.tgoll.projects.bzf;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

class QuestionTooltipOnChartValueSelectedListener implements OnChartValueSelectedListener,
        DialogInterface.OnDismissListener {

    private final Context context;
    private final BarChart chart;
    private final Catalogue catalogue;
    private final String key;
    private final AlertDialog popup;
    private final TextView txt_number;
    private final TextView txt_question;
    private final RadioButton[] buttons;

    QuestionTooltipOnChartValueSelectedListener(@NonNull Context context, @NonNull LayoutInflater inflater, @NonNull BarChart chart, String key) {
        this.context = context;
        this.chart = chart;
        this.catalogue = new Catalogue(context, key);
        this.key = key;
        View view = inflater.inflate(R.layout.dialog_catalogue, null);
        this.txt_number = view.findViewById(R.id.txt_number);
        this.txt_question = view.findViewById(R.id.txt_question);
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
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        int number = (int) e.getX() - 1;


        String question = catalogue.getQuestion(number);
        String[] parts = question.split("\\d{1,4}.\\s", 2);
        String format = key.equals("azf") ? "Question %d" : "Frage %d";

        txt_number.setText(String.format(Locale.getDefault(), format, number + 1));
        txt_question.setText(parts[1]);

        int solution = catalogue.getSolution(number);
        for (int n = 0; n < 4; n++) {
            boolean correct = n == solution;
            buttons[n].setEnabled(false);
            buttons[n].setText(catalogue.getAnswer(number, n));
            buttons[n].setTypeface(correct ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            buttons[n].setTextColor(correct
                    ? context.getResources().getColor(R.color.colorHighlight)
                    : context.getResources().getColor(R.color.black)
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
