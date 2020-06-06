package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QuestionFilter implements Slider.OnChangeListener {


    private final TextView label;
    private final TextView total;
    private final Context context;
    private final Slider slider;
    private final BarChart chart;
    private final @ColorInt int color;
    private BarDataSet data;

    @SuppressLint("InflateParams")
    QuestionFilter(@NonNull Context context, String key, Catalogue cat) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_questionfilter, null);
        this.slider = view.findViewById(R.id.qf_slider);
        this.chart = view.findViewById(R.id.qf_chart);
        TextView name = view.findViewById(R.id.qf_lbl_chart);
        name.setText(key.toUpperCase());

        this.label = view.findViewById(R.id.qf_lbl_slider);
        this.total = view.findViewById(R.id.qf_lbl_total);

        Gson gson = new Gson();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inflater.getContext());

        Set<String> history = settings.getStringSet(key + "-history", CatalogueFragment.EMPTY_SET);
        List<Trial> list = new ArrayList<>();

        //noTrialsYet &= history.isEmpty();
        for (String json : history) list.add(gson.fromJson(json, Trial.class));

        setupBarChart(chart);
        color = Util.lookupColor(context, key.equals("azf") ? R.attr.colorSecondaryVariant : R.attr.colorPrimary);
        fillBarChart(chart, list, key);


        // TODO max size to amount of corrects...
        int maximumCorrectAnswer = list.size();
        slider.setValueTo(maximumCorrectAnswer - 1);  // use one less, such that you cannot select "no questions"
        slider.setOnChangeListener(this);
        slider.setValue(1); // Set "minimum 1 time incorrect" as default

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
            .setTitle(context.getResources().getString(R.string.questionfilter_tooltip))
            .setView(view)
            .setCancelable(true)
            .setPositiveButton(
                context.getResources().getString(R.string.questionfilter_btn_ok),
                (d, btn) -> doStuff()
            )
            .create();
        dialog.show();
    }

    private void setupBarChart(BarChart barchart) {
        barchart.getDescription().setEnabled(false);
        barchart.getLegend().setEnabled(false);
        barchart.setDrawGridBackground(false);
        barchart.setFitBars(true);
        barchart.getXAxis().setDrawAxisLine(false);
        barchart.getXAxis().setDrawGridLines(false);
        barchart.getXAxis().setEnabled(true);
        barchart.getXAxis().setTextColor(Util.lookupColor(context, R.attr.colorOnBackground));
        barchart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barchart.getAxisLeft().setEnabled(true);
        barchart.getAxisLeft().setDrawGridLines(false);
        barchart.getAxisLeft().setDrawLabels(false);
        barchart.getAxisLeft().setDrawAxisLine(false);
        barchart.getAxisLeft().setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        barchart.getAxisRight().setEnabled(false);
        barchart.getXAxis().setGranularity(1f);
        barchart.setHighlightFullBarEnabled(true);
    }

    private void fillBarChart(BarChart chart, List<Trial> trials, String key) {
        // TODO Extract this and StatisticsFragement into separate Class
        if (trials == null) return;

        // Create a list capable of holding all choices
        int max = 0;
        for (Trial trial : trials) {
            if (trial.size() > max) max = trial.size();
        }
        List<Float> questions = new ArrayList<>(Collections.nCopies(max, 0f));

        // Fill the list with data
        for (Trial trial : trials) {
            for (int i = 0; i < trial.size(); i++) {
                if (!trial.isCorrect(i)) continue;
                questions.set(i, questions.get(i) + 1f);
            }
        }

        // Convert the data to BarChart entries
        List<Pair<Integer, Float>> values = new ArrayList<>();

        for (int i = 0; i < questions.size(); i ++) {
            values.add(new Pair<>(i+1, questions.get(i) / trials.size()));
        }

        Collections.sort(values, (a, b) -> Float.compare(a.second, b.second));

        List<Integer> keys = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            Pair<Integer, Float> pair = values.get(i);
            keys.add(pair.first);
            entries.add(new BarEntry(i, new float[]{ pair.second, 1f-pair.second }));
        }

        int background = Color.TRANSPARENT;
        int[] colors = new int[]{ color, background };

        data = new BarDataSet(entries, "Antworten");
        data.setValueFormatter(new StatisticsFragment.NoneValueFormatter());
        chart.getXAxis().setValueFormatter(new StatisticsFragment.ObjectValueFormatter<>(keys));
        data.setColors(colors);
        chart.getAxisLeft().setAxisMaximum(1);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.setData(new BarData(data) {{ setBarWidth(0.99f); }});

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void doStuff() {

    }

    private void renderLimit(int n) {
        this.chart.getAxisLeft().removeAllLimitLines();
        this.chart.getAxisLeft().addLimitLine(new LimitLine(1f - 1f / (slider.getValueTo()+1) * n) {{
            setLabel(context.getResources().getString(R.string.questionfilter_limit_line, n));
            setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
            setTextSize(10);
            setTextColor(Util.lookupColor(context, R.attr.colorOnSurface));
            setLineColor(color);
        }});
    }

    private void updateBarColors(int n) {
        List<Integer> colors = new ArrayList<>();
        @ColorInt int ignoredColor = Util.lookupColor(context, R.attr.colorOnSecondary);
        for (int i = 0; i < data.getEntryCount(); i++) {
            boolean selected = Math.round(data.getEntryForIndex(i).getSumBelow(0) * (slider.getValueTo()+1)) >= n;
            colors.add(selected ? color : ignoredColor);
            colors.add(Color.TRANSPARENT);
        }
        data.setColors(colors);
    }


    @Override
    public void onValueChange(Slider slider, float value) {
        int limit = (int)value;
        label.setText(String.format(context.getString(R.string.questionfilter_lbl_slider), limit));
        total.setText(String.format(context.getString(R.string.questionfilter_lbl_summary), limit));
        renderLimit(limit);
        updateBarColors(limit);
        chart.invalidate();

    }
}
