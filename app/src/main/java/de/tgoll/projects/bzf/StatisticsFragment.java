package de.tgoll.projects.bzf;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StatisticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        Gson gson = new Gson();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(view.getContext());

        Map<String, List<Trial>> trials = new HashMap<>();
        for (String key : Arrays.asList("bzf", "azf", "sim")) {
            Set<String> history = settings.getStringSet(key + "-history", CatalogueFragment.EMPTY_SET);
            List<Trial> list = new ArrayList<>();
            for (String json : history) list.add(gson.fromJson(json, Trial.class));
            trials.put(key, list);
        }

        LineChart history = view.findViewById(R.id.st_chart_history);
        setupHistoryChart(history);
        fillHistory(history, trials);

        BarChart barazf = view.findViewById(R.id.st_chart_answers_azf);
        BarChart barbzf = view.findViewById(R.id.st_chart_answers_bzf);
        setupBarChart(barazf, "azf");
        setupBarChart(barbzf, "bzf");

        fillBarChart(barazf, trials.get("azf"), R.color.colorStatAZF);
        fillBarChart(barbzf, trials.get("bzf"), R.color.colorStatBZF);

        return view;
    }

    private void setupHistoryChart(LineChart history) {
        history.setTouchEnabled(true);
        history.setDragEnabled(true);
        history.setPinchZoom(true);
        history.getAxisRight().setEnabled(true);
        history.getAxisRight().setValueFormatter(new PercentFormatter());
        history.getAxisLeft().setEnabled(false);
        history.getXAxis().setEnabled(true);
        history.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        history.getXAxis().setGranularity(1f);
        history.getLegend().setEnabled(true);
        history.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        history.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        history.getAxisRight().setGranularity(0.2f);
        history.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        history.getLegend().setDrawInside(true);
        history.getDescription().setEnabled(false);
    }
    private void setupBarChart(BarChart barchart, String key) {
        barchart.getDescription().setEnabled(false);
        barchart.getLegend().setEnabled(false);
        barchart.setDrawGridBackground(false);
        barchart.setFitBars(true);
        barchart.getXAxis().setDrawAxisLine(false);
        barchart.getXAxis().setDrawGridLines(false);
        barchart.getXAxis().setEnabled(true);
        barchart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barchart.getAxisLeft().setEnabled(false);
        barchart.getAxisRight().setEnabled(false);
        barchart.getXAxis().setGranularity(1f);
        barchart.setHighlightFullBarEnabled(true);
        barchart.setOnChartValueSelectedListener(
            new QuestionTooltipOnChartValueSelectedListener(
                requireContext(),
                getLayoutInflater(),
                barchart,
                key
            )
        );
    }

    private void fillBarChart(BarChart chart, List<Trial> trials, int color) {
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
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < questions.size(); i ++) {
            float correct = questions.get(i) / trials.size();
            entries.add(new BarEntry(i+1, new float[] { correct, 1f-correct }));
        }

        int[] colors = new int[]{ getResources().getColor(color), Color.LTGRAY};

        BarDataSet data = new BarDataSet(entries, "Antworten");
        data.setValueFormatter(new NoneValueFormatter());
        data.setColors(colors);
        chart.getAxisLeft().setAxisMaximum(1);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.setData(new BarData(data) {{ setBarWidth(0.99f); }});
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private DateTime createDateOnly(Date stamp) {
        return new DateTime(stamp).withTime(0,0,0,0);
    }

    private void fillHistory(LineChart history, Map<String, List<Trial>> trials) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        List<DateTime> dates = new ArrayList<>();
        Map<String, List<Entry>> entries = new HashMap<>();

        for (Map.Entry<String,List<Trial>> entry : trials.entrySet()) {
            Collections.sort(entry.getValue(), new Comparator<Trial>() {
                @Override
                public int compare(Trial a, Trial b) {
                    return createDateOnly(a.getTimestamp()).compareTo(createDateOnly(b.getTimestamp()));
                }
            });

            for (Trial trial : entry.getValue()) {
                DateTime stamp = createDateOnly(trial.getTimestamp());
                if (dates.contains(stamp)) continue;
                dates.add(stamp);
            }
            entries.put(entry.getKey(), new ArrayList<Entry>());
        }

        Collections.sort(dates);

        for (List<Trial> ts : trials.values()) {
            for (Trial trial : ts) {
                for (int i = 0; i < dates.size(); i++) {
                    DateTime date = createDateOnly(trial.getTimestamp());
                    if (!date.equals(dates.get(i))) continue;
                    List<Entry> line = entries.get(trial.key);
                    if (line == null) continue;
                    line.add(new Entry(i, (float) trial.getSuccessRate()));
                }
            }
        }

        for (Map.Entry<String,List<Entry>> entry : entries.entrySet()) {
            LineDataSet line = new LineDataSet(entry.getValue(), entry.getKey().toUpperCase());
            line.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            line.setDrawFilled(true);
            line.setCircleRadius(3);
            line.setCircleColor(getResources().getColor(R.color.black));
            line.setValueFormatter(new NoneValueFormatter());
            switch (entry.getKey()) {
                case "azf":
                    line.setFillColor(getResources().getColor(R.color.colorStatAZF));
                    line.setColor(getResources().getColor(R.color.colorStatAZF));
                    line.setFillAlpha(100);
                    break;
                case "bzf":
                    line.setFillColor(getResources().getColor(R.color.colorStatBZF));
                    line.setColor(getResources().getColor(R.color.colorStatBZF));
                    line.setFillAlpha(100);
                    break;
                case "sim":
                    line.setFillColor(getResources().getColor(R.color.colorAccent));
                    line.setColor(getResources().getColor(R.color.colorAccent));
                    line.setFillAlpha(100);
                    break;
            }
            dataSets.add(line);
        }

        LineData data = new LineData(dataSets);
        history.getXAxis().setValueFormatter(new DateAxisFormatter(dates));
        history.setData(data);
        history.notifyDataSetChanged();
        history.invalidate();
    }

    public class NoneValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return "";
        }
    }

    public class PercentFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.GERMAN, "%.0f%%", value * 100);
        }
    }

    public class DateAxisFormatter extends ValueFormatter {


        private final List<DateTime> dates;
        private final DateTimeFormatter formatter;

        DateAxisFormatter(List<DateTime> dates) {
            this.dates = dates;
            this.formatter = DateTimeFormat.forPattern("dd.MMM");
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            if (value < 0 || value >= dates.size()) return "";
            return formatter.print(dates.get((int)value));
        }
    }
}
