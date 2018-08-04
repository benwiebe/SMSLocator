package com.t3kbau5.smslocator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.florent37.expansionpanel.ExpansionLayout;
import com.github.florent37.expansionpanel.viewgroup.ExpansionLayoutCollection;
import com.github.florent37.runtimepermission.RuntimePermission;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class EnableActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1203;

    private Activity _this = this;
    private DevicePolicyManager DPM;
    private SharedPreferences prefs;
    private ExpansionLayout el_s1, el_s2, el_s3, el_s4, el_s5, el_s6;
    private TextView tv_s1Label, tv_s2Label, tv_s3Label, tv_s4Label, tv_s5Label, tv_s6Label;

    private boolean issuesDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable);
        DPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /* Get Views */
        TextView tv_s1content = findViewById(R.id.s1Content);
        TextView tv_s3content = findViewById(R.id.s3Content);
        TextView tv_s4content = findViewById(R.id.s4Content);
        Button bt_accept = findViewById(R.id.bt_accept);
        Button bt_grant_perm = findViewById(R.id.bt_grant_perm);
        Button bt_grant_admin = findViewById(R.id.bt_grant_admin);
        Button bt_set_pin = findViewById(R.id.bt_set_pin);
        Button bt_set_keyword = findViewById(R.id.bt_set_keyword);

        el_s1 = findViewById(R.id.step1Expansion);
        el_s2 = findViewById(R.id.step2Expansion);
        el_s3 = findViewById(R.id.step3Expansion);
        el_s4 = findViewById(R.id.step4Expansion);
        el_s5 = findViewById(R.id.step5Expansion);
        el_s6 = findViewById(R.id.step6Expansion);
        tv_s1Label = findViewById(R.id.step1Label);
        tv_s2Label = findViewById(R.id.step2Label);
        tv_s3Label = findViewById(R.id.step3Label);
        tv_s4Label = findViewById(R.id.step4Label);
        tv_s5Label = findViewById(R.id.step5Label);
        tv_s6Label = findViewById(R.id.step6Label);

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
        expansionLayoutCollection.add(el_s6);

        expansionLayoutCollection.openOnlyOne(true);

        /* Setup Listeners */
        bt_accept.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                el_s2.setEnabled(true);
                el_s2.expand(true);
                tv_s1Label.setTextColor(getResources().getColor(R.color.primary));
            }
        });

        bt_grant_perm.setOnClickListener(new OnClickListener() {
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

        bt_grant_admin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAdmin();
            }
        });

        bt_set_pin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setPin();
            }
        });

        bt_set_keyword.setOnClickListener(new OnClickListener() {
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
        el_s6.setEnabled(false);
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
    public void onResume() {
        super.onResume();
        if(el_s5.isExpanded()) {
            if(prefs.getString("keyPhrase", "").equals("")) {
                CustomToast.makeText(this, getStr(R.string.error_nokeyword), Toast.LENGTH_LONG, 1).show();
            }else{
                el_s6.setEnabled(true);
                el_s6.expand(true);
                tv_s5Label.setTextColor(getResources().getColor(R.color.primary));
                performSystemTest();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        adb.setCancelable(true);
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
                            if(prefs.getString("keyPhrase", "").equals("")) {
                                el_s5.expand(true);
                            }else{
                                tv_s5Label.setTextColor(getResources().getColor(R.color.primary));
                                el_s6.setEnabled(true);
                                el_s6.expand(true);
                                performSystemTest();
                            }
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

    @SuppressLint("MissingPermission")
    private void performSystemTest() {
        Button bt_done = findViewById(R.id.bt_done);

        TextView tv_cached = findViewById(R.id.tv_test_cached);
        TextView tv_newloc = findViewById(R.id.tv_test_newloc);
        tv_cached.getCompoundDrawables()[0].setColorFilter(getResources().getColor(R.color.tint_neutral_blue), PorterDuff.Mode.SRC_ATOP);
        tv_newloc.getCompoundDrawables()[0].setColorFilter(getResources().getColor(R.color.tint_neutral_blue), PorterDuff.Mode.SRC_ATOP);

        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;

        bt_done.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View view) {
                prefs.edit().putBoolean("smsenabled", true).commit();
                finish();
            }
        });


        if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

        setTestStatus(tv_cached, loc != null);

        String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        if(provider == null){
            provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null;
        }

        if(provider != null) {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    setTestStatus(tv_newloc, location != null);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            };

            Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    lm.removeUpdates(locationListener);
                    setTestStatus(tv_newloc, false);
                }
            };

            int waitTime = Integer.parseInt(prefs.getString("gps_wait", "20000"));
            new Handler().postDelayed(timeoutRunnable, waitTime);

            lm.requestSingleUpdate(provider, locationListener, this.getMainLooper());
        }else{
            setTestStatus(tv_newloc, false);
        }

    }

    private void setTestStatus(TextView testView, boolean status) {
        int drawable = status ? R.drawable.ic_sentiment_very_satisfied_black_24dp : R.drawable.ic_sentiment_very_dissatisfied_black_24dp;
        int color = status ? R.color.primary : R.color.accent;
        testView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawable), null, null, null);
        testView.getCompoundDrawables()[0].setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);

        if(!status) {
            TextView tv_warning = findViewById(R.id.tv_warning_errors);
            tv_warning.setVisibility(View.VISIBLE);
            issuesDetected = true;
        }
        tv_s6Label.setTextColor(getResources().getColor(issuesDetected ? R.color.accent : R.color.primary));
    }

    public String getStr(int id){
        return getResources().getString(id);
    }
}
