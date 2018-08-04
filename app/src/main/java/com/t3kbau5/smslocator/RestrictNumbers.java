package com.t3kbau5.smslocator;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.Objects;

public class RestrictNumbers extends AppCompatActivity {

    private final Context _this = this;
    private Button add;
    private ListView lv;
    private SharedPreferences prefs;
    private NumberAdapter na;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restrict_numbers);
        // Show the Up button in the action bar.
        //setupActionBar();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        add = findViewById(R.id.addNumber);
        lv = findViewById(R.id.numberList);
        na = new NumberAdapter(this);

        lv.setAdapter(na);

        add.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder adb = new AlertDialog.Builder(_this);
                adb.setTitle(getStr(R.string.dialog_addnum));

                final EditText in = new EditText(_this);
                in.setInputType(InputType.TYPE_CLASS_PHONE);
                adb.setView(in);

                adb.setPositiveButton(getStr(R.string.dialog_add), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //prefs.edit().putString("pnumbers", "," + prefs.getString("pnumbers", "") + in.getText().toString()).commit(); //long line that adds the phone # to the list :P
                        na.add(in.getText().toString());
                        //na.notifyDataSetChanged();
                        //lv.invalidate();
                        na.updateData();

                    }
                });
                adb.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                adb.show();
            }

        });

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.restrict_numbers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
            case R.id.menu_rn_help:
                Intent intent = new Intent(this, HelpDisplay.class);
                intent.putExtra("topic", getResources().getStringArray(R.array.help_topics)[3]);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getStr(int id) {
        return getResources().getString(id);
    }

}
