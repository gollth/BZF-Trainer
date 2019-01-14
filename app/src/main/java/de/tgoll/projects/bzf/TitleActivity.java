package de.tgoll.projects.bzf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class TitleActivity extends AppCompatActivity {


    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        root = findViewById(R.id.title_lyt_root);

        ChangeLog changelog = new ChangeLog(this);
        if (changelog.firstRun())
            changelog.getFullLogDialog().show();
    }

    public void onButtonClick (View v) {


        switch(v.getId()) {
            case R.id.title_btn_main:
                startActivity(new Intent(this, CatalogueActivity.class));
                break;
            case R.id.title_btn_sim:
                startActivity(new Intent(this, SimulatorActivity.class));
                break;
            case R.id.title_btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.title_btn_feedback:
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(LayoutInflater.from(this).inflate(R.layout.dialog_feedback, null))
                        .setCancelable(true)
                        .setTitle(R.string.feedback)
                        .setNegativeButton(R.string.cancel, null)   // simple dismiss
                        .setPositiveButton(R.string.send, null)
                        .create();
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Spinner act = dialog.findViewById(R.id.cbx_feedback_activity);
                        Spinner type = dialog.findViewById(R.id.cbx_feedback_type);
                        EditText comment = dialog.findViewById(R.id.txt_feedback_comment);
                        Answers.getInstance().logCustom(new CustomEvent(getString(R.string.feedback))
                            .putCustomAttribute("Activity", act.getSelectedItem().toString())
                            .putCustomAttribute("Type", type.getSelectedItem().toString())
                            .putCustomAttribute(getString(R.string.feedback_comment), comment.getText().toString())
                        );
                        Snackbar.make(root, R.string.feedback_sent, Snackbar.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });
                break;
        }

    }
}
