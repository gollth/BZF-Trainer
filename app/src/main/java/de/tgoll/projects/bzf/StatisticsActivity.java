package de.tgoll.projects.bzf;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StatisticsActivity extends AppCompatActivity {

    Random rng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        rng = new Random();

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();

        Map<String, List<Trial>> trials = new HashMap<>();
        for (String key : Arrays.asList("bzf", "azf")) {
            Set<String> history = settings.getStringSet(key + "-history", CatalogueActivity.EMPTY_SET);
            List<Trial> list = new ArrayList<>();
            for (String json : history) list.add(gson.fromJson(json, Trial.class));
            trials.put(key, list);
        }

        LineChart history = findViewById(R.id.st_chart_history);
        setupHistoryChart(history);
        fillHistory(history, trials);

        HorizontalBarChart barazf = findViewById(R.id.st_chart_answers_azf);
        HorizontalBarChart barbzf = findViewById(R.id.st_chart_answers_bzf);
        setupBarChart(barazf, XAxis.XAxisPosition.TOP);
        setupBarChart(barbzf, XAxis.XAxisPosition.BOTTOM);

        fillBarChart(barazf, trials.get("azf"), R.color.colorStatAZF);
        fillBarChart(barbzf, trials.get("bzf"), R.color.colorStatBZF);

    }

    void setupHistoryChart(LineChart history) {
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
        history.getAxisRight().setAxisMaximum(1.01f);
        history.getAxisRight().setAxisMinimum(0);
        history.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        history.getLegend().setDrawInside(true);
        history.getDescription().setEnabled(false);
    }
    void setupBarChart(HorizontalBarChart barchart, XAxis.XAxisPosition position) {
        barchart.getDescription().setEnabled(false);
        barchart.getLegend().setEnabled(false);
        barchart.setDrawGridBackground(false);
        barchart.setFitBars(true);
        barchart.getXAxis().setDrawAxisLine(false);
        barchart.getXAxis().setDrawGridLines(false);
        barchart.getXAxis().setEnabled(true);
        barchart.getXAxis().setPosition(position);
        barchart.getAxisLeft().setEnabled(false);
        barchart.getAxisRight().setEnabled(false);
        barchart.getXAxis().setGranularity(1f);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    public void fillBarChart(BarChart chart, List<Trial> trials, int color) {
        if (trials == null) return;

        // Create a list capable of holding all choices
        int max = 0;
        for (Trial trial : trials) {
            if (trial.size() > max) max = trial.size();
        }
        List<Float> questions = new ArrayList<Float>(Collections.nCopies(max, 0f));

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


    private boolean isSameDay(DateTime c1, DateTime c2) {
        return c1.getYear() == c2.getYear()
                && c1.getDayOfYear() == c2.getDayOfYear();
    }

    public void fillHistory(LineChart history, Map<String, List<Trial>> trials) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        Map<String, List<Entry>> entries = new HashMap<>();

        for (Map.Entry<String,List<Trial>> entry : trials.entrySet()) {
            for (Trial trial : entry.getValue()) {
                if (dates.contains(trial.getTimestamp())) continue;
                dates.add(trial.getTimestamp());
            }
            entries.put(entry.getKey(), new ArrayList<Entry>());
        }

        Collections.sort(dates);

        for (List<Trial> ts : trials.values()) {
            for (Trial trial : ts) {
                for (int i = 0; i < dates.size(); i++) {
                    if (!isSameDay(new DateTime(trial.getTimestamp()), new DateTime(dates.get(i)))) continue;
                    entries.get(trial.key).add(new Entry(i, (float) trial.getSuccessRate()));
                }
            }

        }

        for (Map.Entry<String,List<Entry>> entry : entries.entrySet()) {
            LineDataSet line = new LineDataSet(entry.getValue(), entry.getKey().toUpperCase());
            line.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            line.setDrawFilled(true);
            line.setFillAlpha(170);
            line.setCircleRadius(3);
            line.setCircleColor(getResources().getColor(R.color.colorDefaultText));
            line.setValueFormatter(new NoneValueFormatter());
            switch (entry.getKey()) {
                case "azf":
                    line.setFillColor(getResources().getColor(R.color.colorStatAZF));
                    line.setColor(getResources().getColor(R.color.colorStatAZF));
                    break;
                case "bzf":
                    line.setFillColor(getResources().getColor(R.color.colorStatBZF));
                    line.setColor(getResources().getColor(R.color.colorStatBZF));
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

    public class IntegerFormatter extends ValueFormatter {
        private final String format;
        public IntegerFormatter() {
            this("%.0f");
        }
        public IntegerFormatter(String format) { this.format = format; }
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.GERMAN, this.format , value);
        }
    }

    public class PercentFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.GERMAN, "%.0f%%", value * 100);
        }
    }

    public class DateAxisFormatter extends ValueFormatter {


        private final List<Date> dates;
        private final DateFormat formatter;

        DateAxisFormatter(List<Date> dates) {
            this.dates = dates;
            this.formatter = new SimpleDateFormat("dd.MMM", Locale.getDefault());
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            if (value < 0 || value >= dates.size()) return "";
            return formatter.format(dates.get((int)value));
        }
    }
}
