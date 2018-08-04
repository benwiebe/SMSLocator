package com.t3kbau5.smslocator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class SetKeyword extends AppCompatActivity {

    private Context _this;
    private SharedPreferences prefs;

    private EditText passIn;
    private EditText passConf;
    private Button setPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_keyword);
        // Show the Up button in the action bar.
        //setupActionBar();

        _this = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        passIn = findViewById(R.id.passwordInput);
        passConf = findViewById(R.id.passwordConfirm);
        setPass = findViewById(R.id.setPassword);

        setPass.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {

                final String p1 = passIn.getText().toString();
                String p2 = passConf.getText().toString();

                if (p1.equals(p2) && !p1.equals("") && !p1.contains(" ")) {
                    final PinDialog pd = new PinDialog(_this, getStr(R.string.message_confirmpasschange));
                    pd.setCancelable(true);
                    pd.setPositiveButton(getStr(R.string.dialog_done), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            String pin = pd.getPin();
                            Boolean isCorrectPin;

                            try {
                                isCorrectPin = Utils.compareToSHA1(pin, prefs.getString("pin", ""));
                            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                                CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                                e.printStackTrace();
                                return;
                            }

                            if (isCorrectPin) {
                                prefs.edit().putString("keyPhrase", p1).apply();
                                CustomToast.makeText(_this, getStr(R.string.message_passchanged), Toast.LENGTH_SHORT, 2).show();
                                ((Activity) _this).finish();
                            } else {
                                CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();
                            }
                        }

                    });
                    pd.setHidden(true);
                    pd.show();
                } else if (p1.equals("")) {
                    CustomToast.makeText(_this, getStr(R.string.error_keyblank), Toast.LENGTH_LONG, 1).show();
                } else if (p1.contains(" ")) {
                    CustomToast.makeText(_this, getStr(R.string.error_keyspaces), Toast.LENGTH_LONG, 1).show();
                } else {
                    CustomToast.makeText(_this, getStr(R.string.error_keymatch), Toast.LENGTH_LONG, 1).show();
                }
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
        getMenuInflater().inflate(R.menu.set_password, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_sp_help:
                Intent intent = new Intent(this, HelpDisplay.class);
                intent.putExtra("topic", getResources().getStringArray(R.array.help_topics)[1]);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getStr(int id) {
        return getResources().getString(id);
    }

}
