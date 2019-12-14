package de.tgoll.projects.bzf;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StatisticsActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView txt_question;
    private TextView[] txt_answers;

    private LineChart history;
    private BarChart barchart;
    private DateFormat formatter;

    private List<Trial> trials;

    Random rng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        rng = new Random();

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        txt_question = findViewById(R.id.st_txt_question);
        txt_answers = new TextView[]{
                findViewById(R.id.st_txt_A),
                findViewById(R.id.st_txt_B),
                findViewById(R.id.st_txt_C),
                findViewById(R.id.st_txt_D)
        };

        formatter = new SimpleDateFormat("dd. MMM'|'hh:mm 'Uhr'", Locale.getDefault());
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();

        Set<String> h = settings.getStringSet("history", CatalogueActivity.EMPTY_SET);
        trials = new ArrayList<>();
        for (String json : h) trials.add(gson.fromJson(json, Trial.class));

        history = findViewById(R.id.st_chart_history);
        history.setTouchEnabled(true);
        history.setDragEnabled(true);
        history.setScaleXEnabled(true);
        history.getAxisLeft().setEnabled(false);
        history.getAxisRight().setEnabled(true);
        history.getAxisRight().setStartAtZero(true);
        history.getXAxis().setEnabled(false);
        history.getLegend().setEnabled(false);
        history.setDescription(getString(R.string.statistics_history));
        history.setDescriptionTextSize(16);
        fillHistory();

        barchart = (HorizontalBarChart) findViewById(R.id.st_chart_bar);
        barchart.getLegend().setEnabled(false);
        barchart.getXAxis().setEnabled(false);
        barchart.getAxisLeft().setEnabled(false);
        barchart.getAxisRight().setEnabled(true);
        barchart.getAxisRight().setStartAtZero(true);
        barchart.getAxisRight().setValueFormatter(new YAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, YAxis yAxis) {
                if (v == Math.round(v)) return String.format(Locale.GERMAN, "%.0f", v);
                else return "";
            }
        });
        barchart.setTouchEnabled(false);
        barchart.setScaleYEnabled(true);
        barchart.setScaleXEnabled(false);
        barchart.setDescription("");
        barchart.setDrawGridBackground(false);


        LinearLayout lyt = findViewById(R.id.st_lyt_ranking);
        Map<Integer, Integer> ranking = new HashMap<>();
        for (int q = 0; q < Catalogue.size(); q++) {
            int value = -1;
            for (Trial trial : trials)
                if (!trial.wasQuestionCorrect(q))
                    value++;
            if (value != -1) ranking.put(q, value);
        }
        ranking = Catalogue.sortByValue(ranking);
        for (Map.Entry<Integer, Integer> entry : ranking.entrySet()) {
            TextView label = (TextView) getLayoutInflater().inflate(R.layout.button_question, null);
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 5, 0, 0);
            label.setId(entry.getKey());
            label.setText(String.format(getString(R.string.txt_questions), entry.getKey() + 1));
            label.setOnClickListener(this);
            lyt.addView(label, params);
        }
    }


    @Override
    public void onClick(View view) {
        int i = view.getId();
        txt_question.setText(Catalogue.getQuestion(i));
        fillBarChart(i, Catalogue.getAnswers(i));

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
        for (Trial trial : trials)
            choices[3 - trial.getChoice(question)] += 1;

        for (int c = 0; c < choices.length; c++) {
            values.add(new BarEntry(choices[c], c));
            labels[c] = String.format(Locale.GERMAN, "%d", choices[c]);
        }

        BarDataSet data = new BarDataSet(values, "answers");
        data.setValueFormatter(new NoneValueFormatter());
        int[] colors = new int[]{Color.LTGRAY, Color.LTGRAY, Color.LTGRAY, Color.LTGRAY};
        colors[3 - Catalogue.getSolution(question)] = getResources().getColor(R.color.colorHighlight);
        data.setColors(colors);

        barchart.setData(new BarData(labels, data));
        barchart.notifyDataSetChanged();
        barchart.invalidate();

        for (int i = 0; i < txt_answers.length; i++) {
            txt_answers[i].setText(answers[i]);
            if (answers[i].length() > 70)
                txt_answers[i].setTextSize(10);
            else txt_answers[i].setTextSize(12);
        }
    }


    public void fillHistory() {
        List<String> dates = new ArrayList<>();
        List<Entry> rates = new ArrayList<>();

        for (int i = 0; i < trials.size(); i++) {
            Trial trial = trials.get(i);
            dates.add(formatter.format(trial.getTimestamp()).replace("|", System.lineSeparator()));
            rates.add(new Entry((float) trial.getSuccessRate() * 100f, i));
        }

        //Collections.reverse(dates);
        //Collections.reverse(rates);

        LineDataSet line = new LineDataSet(rates, getString(R.string.statistics_history));
        line.setDrawCubic(true);
        line.setDrawFilled(true);
        line.setFillAlpha(170);
        line.setCircleSize(5);
        line.setValueFormatter(new LabelFormatter(dates));
        line.setColor(getResources().getColor(R.color.colorDefaultText));
        line.setCircleColor(getResources().getColor(R.color.colorDefaultText));
        line.setFillColor(getResources().getColor(R.color.colorHighlight));
        LineData data = new LineData(dates);
        data.addDataSet(line);

        history.setData(data);
        history.notifyDataSetChanged();
        history.invalidate();
    }

    public class NoneValueFormatter implements ValueFormatter {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return "";
        }
    }

    public class LabelFormatter implements ValueFormatter {
        private List<String> labels;

        public LabelFormatter(List<String> labels) {
            this.labels = labels;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler vph) {
            return labels.get(entry.getXIndex());
        }
    }
}
