package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

public class QuestionFilter implements Slider.OnChangeListener {


    private final TextView label;
    private final TextView total;
    private final Context context;

    @SuppressLint("InflateParams")
    QuestionFilter(@NonNull Context context, Catalogue cat) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_questionfilter, null);
        Slider slider = view.findViewById(R.id.qf_slider);

        this.label = view.findViewById(R.id.qf_lbl_slider);
        this.total = view.findViewById(R.id.qf_lbl_total);

        // TODO max size to amount of corrects...
        slider.setValueTo(5);
        slider.setOnChangeListener(this);


        // Update text once initially
        onValueChange(slider, 5);

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

    void doStuff() {

    }


    @Override
    public void onValueChange(Slider slider, float value) {
        label.setText(String.format(context.getString(R.string.questionfilter_lbl_slider), (int)value));
        total.setText(String.format(context.getString(R.string.questionfilter_lbl_summary), (int)value));
    }
}
