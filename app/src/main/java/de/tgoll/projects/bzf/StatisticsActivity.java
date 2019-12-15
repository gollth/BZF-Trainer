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
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StatisticsActivity extends AppCompatActivity {

    private LineChart history;
    private BarChart barchart;

    private Map<String, List<Trial>> trials;

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

        trials = new HashMap<>();
        for (String key : Arrays.asList("bzf", "azf")) {
            Set<String> history = settings.getStringSet(key + "-history", CatalogueActivity.EMPTY_SET);
            List<Trial> list = new ArrayList<>();
            for (String json : history) list.add(gson.fromJson(json, Trial.class));
            trials.put(key, list);
        }

        history = findViewById(R.id.st_chart_history);
        history.setTouchEnabled(true);
        history.setDragEnabled(true);
        history.setPinchZoom(true);
        history.getAxisRight().setEnabled(true);
        history.getAxisRight().setValueFormatter(new LabelFormatter());
        history.getAxisLeft().setEnabled(false);
        history.getXAxis().setEnabled(true);
        history.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        history.getLegend().setEnabled(true);
        history.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        history.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        history.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);
        history.getLegend().setDrawInside(true);
        history.setDescription(new Description(){{ setText(""); }});

        fillHistory();

        barchart = (HorizontalBarChart) findViewById(R.id.st_chart_bar);
        barchart.getLegend().setEnabled(false);
        barchart.getXAxis().setEnabled(true);
        barchart.getAxisRight().setEnabled(true);
        barchart.getAxisLeft().setEnabled(false);
        barchart.getAxisLeft().setValueFormatter(new LabelFormatter());
        barchart.getAxisLeft().setAxisMinimum(0);
        barchart.getAxisLeft().setAxisMaximum(100);
        barchart.setTouchEnabled(false);
        barchart.setScaleYEnabled(true);
        barchart.setScaleXEnabled(false);
        barchart.setDrawGridBackground(false);

        //fillBarChart(i, Catalogue.getAnswers(i));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    public void fillBarChart(int question, String[] answers) {
        List<BarEntry> values = new ArrayList<>();
        int[] choices = new int[4];
        String[] labels = new String[4];
        //for (Map.Entry<String,List<Trial>> entry : trials.entrySet())
        //    choices[3 - trial.getChoice(question)] += 1;

        for (int c = 0; c < choices.length; c++) {
            values.add(new BarEntry(choices[c], c));
            labels[c] = String.format(Locale.GERMAN, "%d", choices[c]);
        }

        BarDataSet data = new BarDataSet(values, "answers");
        data.setValueFormatter(new NoneValueFormatter());
        int[] colors = new int[]{Color.LTGRAY, Color.LTGRAY, Color.LTGRAY, Color.LTGRAY};
        //colors[3 - Catalogue.getSolution(question)] = getResources().getColor(R.color.colorHighlight);
        data.setColors(colors);

        //barchart.setData(new BarData(labels, data));
        barchart.notifyDataSetChanged();
        barchart.invalidate();
    }


    public void fillHistory() {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        List<Trial> dates = new ArrayList<>();
        Map<String, List<Entry>> entries = new HashMap<>();
        List<String> labels = new ArrayList<>();

        for (Map.Entry<String,List<Trial>> entry : trials.entrySet()) {
            dates.addAll(entry.getValue());
            entries.put(entry.getKey(), new ArrayList<Entry>());
        }

        Collections.sort(dates);
        Collections.reverse(dates);

        for (int i = 0; i < dates.size(); i++) {
            Trial trial = dates.get(i);
            entries.get(trial.key).add(new Entry(i, 100f * (float)trial.getSuccessRate()));
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
        history.setData(data);
        history.getXAxis().setValueFormatter(new DateAxisFormatter(dates));
        history.notifyDataSetChanged();
        history.invalidate();
    }

    public class NoneValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return "";
        }
    }

    public class LabelFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.GERMAN, "%.0f%%", value);
        }
    }

    public class DateAxisFormatter extends ValueFormatter {


        private final List<Trial> trials;
        private final DateFormat formatter;

        DateAxisFormatter(List<Trial> trials) {
            this.trials = trials;
            this.formatter = new SimpleDateFormat("dd.MM.", Locale.getDefault());
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return formatter.format(trials.get((int)value).getTimestamp());
        }
    }
}
