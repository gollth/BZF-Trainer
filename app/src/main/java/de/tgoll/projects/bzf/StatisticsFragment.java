package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

        Gson gson = new Gson();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inflater.getContext());

        boolean noTrialsYet = true;
        Map<String, List<Trial>> trials = new HashMap<>();
        for (String key : Arrays.asList("bzf", "azf", "sim")) {
            Set<String> history = settings.getStringSet(key + "-history", CatalogueFragment.EMPTY_SET);
            List<Trial> list = new ArrayList<>();
            noTrialsYet &= history.isEmpty();

            for (String json : history) list.add(gson.fromJson(json, Trial.class));
            trials.put(key, list);
        }

        if (noTrialsYet) {
           return createWelcomeView(inflater, container);
        }

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        LineChart history = view.findViewById(R.id.st_chart_history);
        setupHistoryChart(history);
        fillHistory(history, trials);

        boolean landscape = requireContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        BarChart barazf = view.findViewById(R.id.st_chart_answers_azf);
        BarChart barbzf = view.findViewById(R.id.st_chart_answers_bzf);
        setupBarChart(barazf, landscape);
        setupBarChart(barbzf, landscape);

        fillBarChart(barazf, trials.get("azf"), "azf", Util.lookupColor(requireContext(), R.attr.colorSecondaryVariant));
        fillBarChart(barbzf, trials.get("bzf"), "bzf", Util.lookupColor(requireContext(), R.attr.colorPrimary));

        return view;
    }

    @SuppressLint("InflateParams")
    private View createWelcomeView(LayoutInflater inflater, @Nullable ViewGroup container) {
        TitleActivity title = (TitleActivity) requireActivity();
        View view = inflater.inflate(R.layout.fragment_statistics_empty, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TextView txt = view.findViewById(R.id.statistics_welcome_message);
            txt.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            txt.setGravity(Gravity.START);
        }

        Button bzf = view.findViewById(R.id.statistics_btn_start_bzf);
        bzf.setOnClickListener(v -> title.showFragment(getString(R.string.bzf), true));

        Button azf = view.findViewById(R.id.statistics_btn_start_azf);
        azf.setOnClickListener(v -> title.showFragment(getString(R.string.azf), true));
        return view;
    }

    private void setupHistoryChart(LineChart history) {
        int color = Util.lookupColor(requireContext(), R.attr.colorOnBackground);
        history.setTouchEnabled(true);
        history.setDragEnabled(true);
        history.setPinchZoom(true);
        history.getAxisRight().setEnabled(true);
        history.getAxisRight().setTextColor(color);
        history.getAxisRight().setValueFormatter(new PercentFormatter());
        history.getAxisLeft().setEnabled(false);
        history.getAxisLeft().setAxisMinimum(0);
        history.getAxisLeft().setAxisMaximum(1.0001f);  // to include 100% still in the label
        history.getXAxis().setEnabled(true);
        history.getXAxis().setTextColor(color);
        history.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        history.getXAxis().setGranularity(1f);
        history.getLegend().setEnabled(true);
        history.getLegend().setTextColor(color);
        history.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        history.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        history.getAxisRight().setGranularity(0.2f);
        history.getAxisRight().setAxisMinimum(0);
        history.getAxisRight().setAxisMaximum(1.0001f);
        history.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        history.getLegend().setDrawInside(true);
        history.getDescription().setEnabled(false);
    }
    private void setupBarChart(BarChart barchart, boolean landscape) {
        barchart.getDescription().setEnabled(false);
        barchart.getLegend().setEnabled(false);
        barchart.setDrawGridBackground(false);
        barchart.setFitBars(true);
        barchart.getXAxis().setDrawAxisLine(false);
        barchart.getXAxis().setDrawGridLines(false);
        barchart.getXAxis().setEnabled(true);
        barchart.getXAxis().setTextColor(Util.lookupColor(requireContext(), R.attr.colorOnBackground));
        barchart.getXAxis().setPosition(landscape ? XAxis.XAxisPosition.TOP : XAxis.XAxisPosition.BOTTOM);
        barchart.getAxisLeft().setEnabled(false);
        barchart.getAxisRight().setEnabled(false);
        barchart.getXAxis().setGranularity(1f);
        barchart.setHighlightFullBarEnabled(true);
    }

    private void fillBarChart(BarChart chart, List<Trial> trials, String key, int color) {
        Pair<BarDataSet, List<Integer>> pair;
        pair = Util.createQuestionHistogram(trials, color);
        if (pair == null) return;
        chart.setData(new BarData(pair.first) {{ setBarWidth(0.99f); }});
        chart.getXAxis().setValueFormatter(new ObjectValueFormatter<>(pair.second));
        chart.getAxisLeft().setAxisMaximum(1);
        chart.getAxisLeft().setAxisMinimum(0);

        chart.setOnChartValueSelectedListener(
            new QuestionTooltipOnChartValueSelectedListener(
                requireContext(),
                chart,
                key,
                pair.second
            )
        );
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
            Collections.sort(
                entry.getValue(),
                (a, b) -> createDateOnly(a.getTimestamp()).compareTo(createDateOnly(b.getTimestamp()))
            );

            for (Trial trial : entry.getValue()) {
                DateTime stamp = createDateOnly(trial.getTimestamp());
                if (dates.contains(stamp)) continue;
                dates.add(stamp);
            }
            entries.put(entry.getKey(), new ArrayList<>());
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
            line.setCircleColor(Color.BLACK);
            line.setValueFormatter(new NoneValueFormatter());
            int color;
            switch (entry.getKey()) {
                case "azf":
                    color = Util.lookupColor(requireContext(), R.attr.colorSecondaryVariant);
                    line.setFillColor(color);
                    line.setColor(color);
                    line.setFillAlpha(100);
                    break;
                case "bzf":
                    color = Util.lookupColor(requireContext(), R.attr.colorPrimary);
                    line.setFillColor(color);
                    line.setColor(color);
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

    public static class NoneValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return "";
        }
    }

    public static class PercentFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.GERMAN, "%.0f%%", value * 100);
        }
    }

    public static class ObjectValueFormatter<T> extends ValueFormatter {

        private final List<T> values;

        ObjectValueFormatter(List<T> values) {
            this.values = values;
        }

        @Override
        public String getFormattedValue(float value) {
            if (value < 0 || value >= values.size()) return "";
            return values.get((int)value).toString();
        }
    }

    public static class DateAxisFormatter extends ValueFormatter {


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
