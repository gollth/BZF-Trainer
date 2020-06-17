package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class QuestionFilter implements View.OnTouchListener {

    public interface OnSubmitListener {
        void onPlaylistGenerated(List<Integer> playlist);
    }


    private final TextView label;
    private final TextView total;
    private final Context context;
    private final BarChart chart;
    private final @ColorInt int color;
    private final int maximumCorrectAnswers;
    private final List<Integer> playlist;
    private Pair<BarDataSet, List<Integer>> data;

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    QuestionFilter(@NonNull Context context, String key, OnSubmitListener callback) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_questionfilter, null);
        this.chart = view.findViewById(R.id.qf_chart);
        TextView name = view.findViewById(R.id.qf_lbl_chart);
        name.setText(key.toUpperCase());

        this.label = view.findViewById(R.id.qf_lbl_slider);
        this.total = view.findViewById(R.id.qf_lbl_total);

        Gson gson = new Gson();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inflater.getContext());

        List<Trial> list = Util.getTrials(settings, gson, key);
        this.playlist = new ArrayList<>();

        setupBarChart(chart);
        color = Util.lookupColor(context, key.equals("azf") ? R.attr.colorSecondaryVariant : R.attr.colorPrimary);
        data = Util.createQuestionHistogram(list, color);
        if (data == null) {
            Log.w("BZF", "WARNING: Attempting to show Question Filter despite no questions for " + key +" have yet been answered");
            maximumCorrectAnswers = 0;
            return;
        }

        chart.getXAxis().setValueFormatter(new StatisticsFragment.ObjectValueFormatter<>(data.second));
        chart.getAxisLeft().setAxisMaximum(1);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.setData(new BarData(data.first) {{ setBarWidth(0.99f); }});

        chart.setOnTouchListener(this);

        chart.notifyDataSetChanged();
        chart.invalidate();

        // use one less, such that you cannot select "no questions"
        this.maximumCorrectAnswers = Util.getMaxCorrectAnswered(data.first, list.size()) - 1;
        onValueChange(1); // Set "minimum 1 time incorrect" as default

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
            .setTitle(context.getResources().getString(R.string.questionfilter_tooltip))
            .setView(view)
            .setCancelable(true)
            .setPositiveButton(
                context.getResources().getString(R.string.questionfilter_btn_ok),
                (d, btn) -> callback.onPlaylistGenerated(this.playlist)
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
        barchart.getXAxis().setEnabled(false);
        barchart.getXAxis().setTextColor(Util.lookupColor(context, R.attr.colorOnBackground));
        barchart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barchart.getAxisLeft().setEnabled(true);
        barchart.getAxisLeft().setDrawGridLines(false);
        barchart.getAxisLeft().setDrawLabels(false);
        barchart.getAxisLeft().setDrawAxisLine(false);
        barchart.getAxisLeft().setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        barchart.getAxisRight().setEnabled(false);
        barchart.getXAxis().setGranularity(1f);
        barchart.setTouchEnabled(true);
        barchart.setDoubleTapToZoomEnabled(false);
        barchart.setHighlightPerTapEnabled(false);
        barchart.setHighlightPerDragEnabled(false);
    }

    private void renderLimit(int n) {
        this.chart.getAxisLeft().removeAllLimitLines();
        this.chart.getAxisLeft().addLimitLine(new LimitLine(1f - 1f / (maximumCorrectAnswers+1) * n) {{
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
        for (int i = 0; i < data.first.getEntryCount(); i++) {
            boolean selected = Math.round(data.first.getEntryForIndex(i).getSumBelow(0) * (maximumCorrectAnswers+1)) >= n;
            colors.add(selected ? color : ignoredColor);
            colors.add(Color.TRANSPARENT);
        }
        data.first.setColors(colors);
    }


    private void onValueChange(float value) {
        int limit = Math.round(Util.saturate(value, 0, maximumCorrectAnswers));
        label.setText(String.format(context.getString(R.string.questionfilter_lbl_slider), limit));
        playlist.clear();
        for(int i = 0; i < data.first.getEntryCount(); i++) {
            if (Util.getValue(data.first, i, maximumCorrectAnswers) > maximumCorrectAnswers - limit) continue;
            int x = data.second.get(i);   // X label to bar entry aka question index
            playlist.add(x);
        }
        total.setText(String.format(context.getString(R.string.questionfilter_lbl_summary), playlist.size()));
        renderLimit(limit);
        updateBarColors(limit);
        chart.invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent me) {
        float value;
        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                value = me.getY() / chart.getHeight();
                onValueChange(value * maximumCorrectAnswers);
                break;

                //Highlight highlight = chart.getHighlightByTouchPoint(me.getX(), me.getY());
                //value = 1-Util.getValue(data, highlight.getX());
                //onValueChange(slider, (slider.getValueTo()+1) * value);
                //v.performClick();
                //break;
        }
        return false;
    }
}
