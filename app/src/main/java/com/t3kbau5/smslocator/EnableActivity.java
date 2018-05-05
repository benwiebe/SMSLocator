package com.t3kbau5.smslocator;

import android.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.florent37.expansionpanel.ExpansionLayout;
import com.github.florent37.expansionpanel.viewgroup.ExpansionLayoutCollection;
import com.github.florent37.runtimepermission.RuntimePermission;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static com.github.florent37.runtimepermission.RuntimePermission.askPermission;

public class EnableActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1203;

    private Activity _this = this;
    private DevicePolicyManager DPM;
    private SharedPreferences prefs;
    private ExpansionLayout el_s1, el_s2, el_s3, el_s4, el_s5;
    private TextView tv_s1Label, tv_s2Label, tv_s3Label, tv_s4Label, tv_s5Label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable);
        DPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /* Get Views */
        TextView tv_s1content = (TextView) findViewById(R.id.s1Content);
        TextView tv_s3content = (TextView) findViewById(R.id.s3Content);
        TextView tv_s4content = (TextView) findViewById(R.id.s4Content);
        Button bt_accept = (Button) findViewById(R.id.bt_accept);
        Button bt_grant_perm = (Button) findViewById(R.id.bt_grant_perm);
        Button bt_grant_admin = (Button) findViewById(R.id.bt_grant_admin);
        Button bt_set_pin = (Button) findViewById(R.id.bt_set_pin);
        Button bt_set_keyword = (Button) findViewById(R.id.bt_set_keyword);

        el_s1 = (ExpansionLayout) findViewById(R.id.step1Expansion);
        el_s2 = (ExpansionLayout) findViewById(R.id.step2Expansion);
        el_s3 = (ExpansionLayout) findViewById(R.id.step3Expansion);
        el_s4 = (ExpansionLayout) findViewById(R.id.step4Expansion);
        el_s5 = (ExpansionLayout) findViewById(R.id.step5Expansion);
        tv_s1Label = (TextView) findViewById(R.id.step1Label);
        tv_s2Label = (TextView) findViewById(R.id.step2Label);
        tv_s3Label = (TextView) findViewById(R.id.step3Label);
        tv_s4Label = (TextView) findViewById(R.id.step4Label);
        tv_s5Label = (TextView) findViewById(R.id.step5Label);

        /* Setup Expander Contents */
        tv_s1content.setText(Utils.formatAndSpan(getStr(R.string.app_terms)));
        tv_s3content.setText(Utils.formatAndSpan(getStr(R.string.admin_details)));
        tv_s4content.setText(getStr(R.string.message_choosepin));

        /* Setup Expansion Collection */
        final ExpansionLayoutCollection expansionLayoutCollection = new ExpansionLayoutCollection();
        expansionLayoutCollection.add(el_s1);
        expansionLayoutCollection.add(el_s2);
        expansionLayoutCollection.add(el_s3);
        expansionLayoutCollection.add(el_s4);
        expansionLayoutCollection.add(el_s5);

        expansionLayoutCollection.openOnlyOne(true);

        /* Setup Listeners */
        bt_accept.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                el_s2.setEnabled(true);
                el_s2.expand(true);
                tv_s1Label.setTextColor(getResources().getColor(R.color.primary));
            }
        });

        bt_grant_perm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RuntimePermission((FragmentActivity) _this).request(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.SEND_SMS, android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                        .onAccepted((result) -> {
                            if(prefs.getBoolean("premium", false)) {
                                el_s3.setEnabled(true);
                                el_s3.expand(true);
                            }else{
                                el_s4.setEnabled(true);
                                el_s4.expand(true);
                                tv_s3Label.setTextColor(getResources().getColor(R.color.primary));
                            }
                            tv_s2Label.setTextColor(getResources().getColor(R.color.primary));
                        })
                        .onDenied((result) -> {
                            CustomToast.makeText(_this, getStr(R.string.error_permissions), Toast.LENGTH_LONG, 1).show();
                        })
                        .onForeverDenied((result) -> {
                            //TODO: this
                            //the list of forever denied permissions, user has check 'never ask again'
                            for (String permission : result.getForeverDenied()) {

                            }
                            // you need to open setting manually if you really need it
                            //result.goToSettings();
                        })
                        .ask();
            }
        });

        bt_grant_admin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAdmin();
            }
        });

        bt_set_pin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPin();
            }
        });

        bt_set_keyword.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(_this, SetKeyword.class);
                startActivity(intent);
            }
        });

        /* Default Setup */
        el_s2.setEnabled(false);
        el_s3.setEnabled(false);
        el_s4.setEnabled(false);
        el_s5.setEnabled(false);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                prefs.edit().putBoolean("admin", true).apply();
                el_s4.setEnabled(true);
                el_s4.expand(true);
                tv_s3Label.setTextColor(getResources().getColor(R.color.primary));
            }else{
                showAdminDialog();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_enable, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!prefs.getBoolean("smsenabled", false)) {
            ComponentName cm = new ComponentName(_this, DevAdmin.class);
            DPM.removeActiveAdmin(cm);
            prefs.edit().putBoolean("admin", false).apply();
        }
    }

    protected void requestAdmin(){

        if(prefs.getBoolean("admin", false)) return;

        ComponentName comp = new ComponentName(_this, DevAdmin.class);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.admin_details);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    private void showAdminDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getStr(R.string.dialog_notice));
        adb.setMessage(getStr(R.string.message_noadmin));
        adb.setNegativeButton(getStr(R.string.dialog_close), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface di, int arg1) {
                di.dismiss();
            }
        });
        adb.setPositiveButton(getStr(R.string.dialog_tryagain), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestAdmin();
            }
        });
        adb.show();
    }

    private void setPin(){
        final PinDialog adb = new PinDialog(_this, getStr(R.string.message_choosepin), getStr(R.string.dialog_setpin));
        adb.setCancelable(false);
        adb.setPositiveButton(getStr(R.string.dialog_done), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String pin1 = adb.getPin();

                if(pin1.equals("") || pin1 == null || pin1.length() < 4){
                    CustomToast.makeText(_this, getStr(R.string.error_setpin), Toast.LENGTH_LONG, 1).show();
                    setPin();
                }

                final PinDialog adbc = new PinDialog(_this, getStr(R.string.message_confirmpin), "Set Pin");
                adbc.setCancelable(false);
                adbc.setPositiveButton(getStr(R.string.dialog_done), new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(pin1.equals(adbc.getPin()) && !adbc.getPin().equals("")){
                            String pin = "";
                            try {
                                pin = Utils.SHA1(adbc.getPin());
                            } catch (NoSuchAlgorithmException |UnsupportedEncodingException e) {
                                CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                                e.printStackTrace();
                                return;
                            }

                            prefs.edit().putString("pin", pin).apply();
                            tv_s4Label.setTextColor(getResources().getColor(R.color.primary));
                            el_s5.setEnabled(true);
                            el_s5.expand(true);
                            //CustomToast.makeText(getBaseContext(), getStr(R.string.message_pinset), Toast.LENGTH_LONG, 2).show();
                        }else{
                            CustomToast.makeText(getBaseContext(), getStr(R.string.error_pinmatch), Toast.LENGTH_LONG, 1).show();
                        }
                    }
                });
                adbc.setHidden(true);
                adbc.show();
            }
        });
        adb.setHidden(true);
        adb.show();
    }

    public String getStr(int id){
        return getResources().getString(id);
    }
}
