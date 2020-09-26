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
    private float maxLimitLine;
    private float minLimitLine;
    private final @ColorInt int color;
    private final List<Integer> playlist;
    private Pair<BarDataSet, List<Integer>> data;

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    QuestionFilter(@NonNull Context context, String key, OnSubmitListener callback) throws NoTrialsYetExcpetion {
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
        Catalogue cat = new Catalogue(context, key);

        List<Trial> list = Util.getTrials(cat, settings, gson, key);
        this.playlist = new ArrayList<>();

        setupBarChart(chart);
        color = Util.lookupColor(context, key.equals("azf") ? R.attr.colorSecondaryVariant : R.attr.colorPrimary);

        data = Util.createQuestionHistogram(list, color);
        if (data == null) {
            Log.w("BZF", "WARNING: Attempting to show Question Filter despite no questions for " + key +" have yet been answered");
            throw new NoTrialsYetExcpetion();
        }

        maxLimitLine = Util.getMaxCorrectAnswered(data.first, list.size()) * 1f / list.size();
        minLimitLine = Util.getValue(data.first, 1);  // Not the lowest, but the 2nd lowest

        chart.getXAxis().setValueFormatter(new StatisticsFragment.ObjectValueFormatter<>(data.second));
        chart.getAxisLeft().setAxisMaximum(1);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.setData(new BarData(data.first) {{ setBarWidth(0.99f); }});

        chart.setOnTouchListener(this);

        chart.notifyDataSetChanged();
        chart.invalidate();

        onValueChange(0.9f); // Set "minimum 10% incorrect" as default

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

    private void renderLimit(float limit) {
        this.chart.getAxisLeft().removeAllLimitLines();
        this.chart.getAxisLeft().addLimitLine(new LimitLine(limit) {{
            setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
            if (limit >= maxLimitLine) {
                setLabel(context.getResources().getString(R.string.questionfilter_limit_line_all));
            } else if (limit <= minLimitLine) {
                setLabel(context.getResources().getString(R.string.questionfilter_limit_line_none));
            } else {
                setLabel(context.getResources().getString(R.string.questionfilter_limit_line, Math.round(limit * 100)));
            }

            if (limit <= 0.12f) {
                // If the limit is close the the bottom axis, put its label on top
                setLabelPosition(LimitLabelPosition.RIGHT_TOP);
            }
            setTextSize(10);
            setTextColor(Util.lookupColor(context, R.attr.colorOnSurface));
            setLineColor(color);
        }});
    }

    private void updateBarColors(float limit) {
        List<Integer> colors = new ArrayList<>();
        @ColorInt int ignoredColor = Util.lookupColor(context, R.attr.colorOnSecondary);
        for (int i = 0; i < data.first.getEntryCount(); i++) {
            boolean selected = Util.getValue(data.first, i) <= limit;
            colors.add(selected ? color : ignoredColor);
            colors.add(Color.TRANSPARENT);
        }
        data.first.setColors(colors);
    }


    private void onValueChange(float value) {
        label.setText(String.format(context.getString(R.string.questionfilter_lbl_slider), Math.round(value * 100)));
        playlist.clear();
        for(int i = 0; i < data.first.getEntryCount(); i++) {
            if (Util.getValue(data.first, i) > value) continue;
            int x = data.second.get(i);   // X label to bar entry aka question index
            playlist.add(x);
        }
        int catalogueSize = playlist.size();
        total.setText(catalogueSize > 1
            ? String.format(context.getString(R.string.questionfilter_lbl_summary), playlist.size())
            : context.getString(R.string.questionfilter_lbl_summary_single)
        );
        renderLimit(value);
        updateBarColors(value);
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
                onValueChange(Util.saturate(1f-value, 0, 1));
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
