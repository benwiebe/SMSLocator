package com.t3kbau5.smslocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.michaelflisar.gdprdialog.GDPR;
import com.michaelflisar.gdprdialog.GDPRConsent;
import com.michaelflisar.gdprdialog.GDPRConsentState;
import com.michaelflisar.gdprdialog.GDPRDefinitions;
import com.michaelflisar.gdprdialog.GDPRLocation;
import com.michaelflisar.gdprdialog.GDPRSetup;
import com.michaelflisar.gdprdialog.helper.GDPRPreperationData;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import io.fabric.sdk.android.Fabric;
import io.github.tonnyl.whatsnew.WhatsNew;
import io.github.tonnyl.whatsnew.item.WhatsNewItem;

public class MainActivity extends AppCompatActivity implements GDPR.IGDPRCallback {

    private final int REQUEST_CODE_ENABLE_ADMIN = 1203;
    private final int REQUEST_CODE_PERMISSIONS = 1703;
    BillingUtil2 bu;
    private SharedPreferences prefs;
    private Context _this;
    private ToggleButton enableSMS;
    private Boolean smsenabled = false;
    private Button gotoSetKeyword;
    private CompoundButton passSMS;
    private Button gotoRestriction;
    private CompoundButton toggleRestriction;
    private DevicePolicyManager DPM;
    private CompoundButton toggleDnd;
    private Boolean dndPending = false;
    private Boolean permissionsDuringEnable = false;
    private GDPRSetup gdprSetup;
    private FirebaseAnalytics mFirebaseAnalytics;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_main);

        _this = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        DPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        bu = new BillingUtil2(this, mFirebaseAnalytics);

        /*
         * Code for showing WhatsNew dialog for each new release
         * todo: update XML for each release
         */

        TypedArray wndrawables = getResources().obtainTypedArray(R.array.whatsnew_drawables);
        String wntitles[] = getResources().getStringArray(R.array.whatsnew_titles);
        String wncontents[] = getResources().getStringArray(R.array.whatsnew_contents);
        WhatsNewItem[] wnitems = new WhatsNewItem[wndrawables.length()];
        for (int i = 0; i < wndrawables.length(); i++) {
            wnitems[i] = new WhatsNewItem(wntitles[i], wncontents[i], wndrawables.getResourceId(i, -1));
        }
        wndrawables.recycle();

        WhatsNew wn = WhatsNew.newInstance(wnitems);
        wn.setButtonBackground(getResources().getColor(R.color.primary_dark));
        wn.setButtonTextColor(getResources().getColor(android.R.color.white));
        wn.setTitleColor(getResources().getColor(R.color.primary));
        wn.setButtonText(getStr(R.string.whatsnew_button));
        wn.setTitleText(getStr(R.string.whatsnew_title));

        /*if(BuildConfig.DEBUG)
            wn.setPresentationOption(PresentationOption.DEBUG); //always show for debug builds*/
        wn.presentAutomatically(this);

        /* end WhatsNew code */

        enableSMS = findViewById(R.id.enableSMS);

        enableSMS.setOnClickListener(new ToggleButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                Boolean s = enableSMS.isChecked();

                if (s) {
                    //showTermsDialog();
                    enableSMS.setChecked(false);
                    Intent intent = new Intent(_this, EnableActivity.class);
                    startActivity(intent);

                } else {
                    final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmdisable));
                    adb.setCancelable(false);
                    adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Boolean isCorrectPin;
                            try {
                                isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
                            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                                CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                                enableSMS.setChecked(true);
                                e.printStackTrace();
                                return;
                            }
                            if (isCorrectPin) {
                                ComponentName cm = new ComponentName(_this, DevAdmin.class);
                                DPM.removeActiveAdmin(cm);
                                prefs.edit().putBoolean("smsenabled", false).apply();
                                updateStates();
                            } else {
                                enableSMS.setChecked(true);
                                CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();
                            }
                        }

                    });
                    adb.setHidden(true);
                    adb.show();
                }
            }
        });
        gotoSetKeyword = findViewById(R.id.gotoSetPass);
        gotoSetKeyword.setEnabled(smsenabled);
		/*if(!prefs.getString("keyPhrase", "TMWMPI").equals("TMWMPI")){
			gotoSetKeyword.setBackgroundResource(R.drawable.complete_button);
		}else{
			gotoSetKeyword.setBackgroundResource(R.drawable.incomplete_button);
		}*/ //TODO: update this
        gotoSetKeyword.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(_this, SetKeyword.class);
                startActivity(intent);
            }
        });

        passSMS = findViewById(R.id.togglePassChange);
        passSMS.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Boolean isChecked = passSMS.isChecked();
                final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmpin));
                adb.setCancelable(false);
                adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Boolean isCorrectPin;
                        try {
                            isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
                        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                            CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                            enableSMS.setChecked(!isChecked);
                            e.printStackTrace();
                            return;
                        }

                        if (isCorrectPin) {
                            prefs.edit().putBoolean("passChange", isChecked).apply();
                        } else {
                            passSMS.setChecked(!isChecked);
                            CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();

                        }
                    }

                });
                adb.setHidden(true);
                adb.show();
            }

        });

        toggleRestriction = findViewById(R.id.toggleRestriction);
        toggleRestriction.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Boolean isChecked = toggleRestriction.isChecked();
                final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmpin));
                adb.setCancelable(false);
                adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Boolean isCorrectPin;
                        try {
                            isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
                        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                            CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                            toggleRestriction.setChecked(!isChecked);
                            e.printStackTrace();
                            return;
                        }

                        if (isCorrectPin) {
                            prefs.edit().putBoolean("enableRestriction", isChecked).apply();
                        } else {
                            toggleRestriction.setChecked(!isChecked);
                            CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();

                        }
                    }

                });
                adb.setHidden(true);
                adb.show();
            }

        });

        gotoRestriction = findViewById(R.id.gotoRestrictNumbers);
        gotoRestriction.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(_this, RestrictNumbers.class);
                startActivity(i);
            }

        });

        toggleDnd = findViewById(R.id.toggleDnd);
        if (toggleDnd != null) {
            toggleDnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!toggleDnd.isChecked()) {
                        prefs.edit().putBoolean("dndcontrol", false).apply();
                    } else {
                        //from http://stackoverflow.com/a/36162332/1896516
                        NotificationManager mNotificationManager = (NotificationManager) _this.getSystemService(Context.NOTIFICATION_SERVICE);

                        // Check if the notification policy access has been granted for the app.
                        if (!Objects.requireNonNull(mNotificationManager).isNotificationPolicyAccessGranted()) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            startActivity(intent);
                            CustomToast.makeText(_this, getStr(R.string.message_activatedndaccess), Toast.LENGTH_LONG, 0).show();
                            dndPending = true; //this is used in updateStates to check if the permission was granted
                        } else {
                            prefs.edit().putBoolean("dndcontrol", true).apply();
                        }
                    }
                }
            });
        }

        updateStates();

        if (prefs.getBoolean("firstRun", true)) {
            prefs.edit().putBoolean("firstRun", false).apply();
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(getStr(R.string.dialog_welcome));
            adb.setMessage(getStr(R.string.dialog_firstrun));
            adb.setPositiveButton(getStr(R.string.dialog_close), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            adb.show();
        }

        GDPR.getInstance().init(this);
        gdprSetup = new GDPRSetup(GDPRDefinitions.ADMOB, GDPRDefinitions.FABRIC_CRASHLYTICS); // add all networks you use to the constructor
        //gdprSetup.withPaidVersion(true);
        gdprSetup.withExplicitAgeConfirmation(true);
        gdprSetup.withPrivacyPolicy("https://t3kbau5.com/app-policy.php?app=SMSLocator");
        GDPR.getInstance().checkIfNeedsToBeShown(this, gdprSetup);

        //Support for the new permissions system
        if (smsenabled && !Utils.checkPermissionsGranted(this).getBoolean("allGranted")) {
            permissionsDuringEnable = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.MODIFY_AUDIO_SETTINGS}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (prefs.getBoolean("premium", false)) menu.findItem(R.id.menu_unlock).setVisible(false);
        return true;
    }

    @Override
    public void onResume() {
        updateStates();

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_help:
                intent = new Intent(this, AppHelp.class);
                startActivity(intent);
                return true;
            case R.id.menu_settings:
                intent = new Intent(this, MoreSettings.class);
                startActivity(intent);
                return true;
            case R.id.menu_unlock:
                final Activity _act = this;
                mFirebaseAnalytics.logEvent("open_premium_dialog", null);
                if (Utils.internetConnected(this) && bu.isConnected()) {

                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    adb.setTitle(getStr(R.string.dialog_premium));
                    adb.setMessage(getStr(R.string.dialog_upgrade));
                    adb.setPositiveButton(getStr(R.string.dialog_continue), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            if (bu.hasPremium()) {
                                CustomToast.makeText(getBaseContext(), getStr(R.string.message_restored), Toast.LENGTH_LONG, 2).show();
                            } else {
                                Bundle event = new Bundle();
                                event.putString(FirebaseAnalytics.Param.ITEM_ID, "premium");
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, event);
                                bu.buyPremium();
                            }
                        }
                    });
                    adb.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mFirebaseAnalytics.logEvent("cancel_premium_dialog", null);
                            dialog.dismiss();
                        }
                    });
                    adb.setNeutralButton(getStr(R.string.dialog_watchad), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mFirebaseAnalytics.logEvent("freemium_ad_request", null);

                            final ProgressDialog pd = ProgressDialog.show(_act, Utils.getStr(_act, R.string.dialog_pleaseWait), "");
                            final RewardedVideoAd mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(_act);
                            mRewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
                                @Override
                                public void onRewardedVideoAdLoaded() {
                                    mFirebaseAnalytics.logEvent("freemium_ad_start", null);
                                    pd.dismiss();
                                    mRewardedVideoAd.show();
                                }

                                @Override
                                public void onRewardedVideoAdOpened() {

                                }

                                @Override
                                public void onRewardedVideoStarted() {

                                }

                                @Override
                                public void onRewardedVideoAdClosed() {

                                }

                                @Override
                                public void onRewarded(RewardItem rewardItem) {
                                    mFirebaseAnalytics.logEvent("freemium_ad_complete", null);
                                    Calendar expiry = Calendar.getInstance();
                                    expiry.add(Calendar.DAY_OF_MONTH, 7);
                                    String expiryString = expiry.get(Calendar.DAY_OF_MONTH) + "-" + (expiry.get(Calendar.MONTH) + 1) + "-" + expiry.get(Calendar.YEAR);

                                    prefs.edit().putLong("freemium_expiry", expiry.getTimeInMillis()).putBoolean("premium", true).putBoolean("premium_is_freemium", true).apply();
                                    updateStates();
                                    AlertDialog.Builder adb = new AlertDialog.Builder(_this)
                                            .setTitle(Utils.getStr(_this, R.string.dialog_premium))
                                            .setMessage(Utils.getStr(_this, R.string.dialog_freemium_success) + " " + expiryString)
                                            .setPositiveButton(Utils.getStr(_this, R.string.dialog_continue), new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    finish();
                                                    startActivity(getIntent());
                                                }
                                            });
                                    adb.show();
                                }

                                @Override
                                public void onRewardedVideoAdLeftApplication() {

                                }

                                @Override
                                public void onRewardedVideoAdFailedToLoad(int i) {
                                    mFirebaseAnalytics.logEvent("freemium_ad_load_fail", null);
                                    pd.dismiss();
                                    CustomToast.makeText(_this, getStr(R.string.error_novideo), Toast.LENGTH_LONG, 1).show();
                                }
                            });
                            AdRequest adreq = new AdRequest.Builder().addTestDevice("0CA205FF0785B1495463D2F5D77BEBF7")
                                    .addTestDevice("A030DF014385BBC02B04E68B65A8F7D4").build();

                            mRewardedVideoAd.loadAd("ca-app-pub-3534916998867938/5345514381", adreq);
                            //mRewardedVideoAd.loadAd("ca-app-pub-3940256099942544/5224354917", adreq);
                        }
                    });
                    adb.show();
                } else { //billing not connected, device likely offline
                    AlertDialog.Builder adb = new AlertDialog.Builder(_this)
                            .setTitle(Utils.getStr(_this, R.string.dialog_tryagain))
                            .setMessage(Utils.getStr(_this, R.string.dialog_premium_offline))
                            .setNegativeButton(Utils.getStr(_this, R.string.dialog_close), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    adb.show();
                }

                return true;
            case R.id.menu_interactions:
                intent = new Intent(this, Interactions.class);
                this.startActivity(intent);
                return true;
            case R.id.menu_updateGDPR:
                GDPR.getInstance().showDialog(this, gdprSetup, GDPRLocation.UNDEFINED);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAd() {

        if (Utils.isTestDevice(this))
            return;

        if (Utils.checkAdBlock()) {

            //disable the services
            ComponentName cm = new ComponentName(_this, DevAdmin.class);
            DPM.removeActiveAdmin(cm);
            prefs.edit().putBoolean("smsenabled", false).apply();
            updateStates();

            //show adblock warning dialog
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(getStr(R.string.dialog_adblock_title));
            adb.setMessage(getStr(R.string.dialog_adblock_msg));
            adb.setPositiveButton(getStr(R.string.dialog_purchase), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    bu.buyPremium();
                }
            });
            adb.setNegativeButton(getStr(R.string.dialog_close), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();

                }
            });
            adb.show();

        }

        AdView mAdView = new AdView(this);

        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId("ca-app-pub-3534916998867938/8188948008");

        AdRequest adreq = new AdRequest.Builder().addTestDevice("0CA205FF0785B1495463D2F5D77BEBF7")
                .addTestDevice("A030DF014385BBC02B04E68B65A8F7D4")
                .addNetworkExtrasBundle(AdMobAdapter.class, Utils.personalAdBundle(GDPR.getInstance().canCollectPersonalInformation()))
                .build();

        RelativeLayout layout = findViewById(R.id.mainLayout);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mAdView.setLayoutParams(lp);
        //layout.setLayoutParams(lp);
        layout.addView(mAdView);
        mAdView.loadAd(adreq);
    }

    private void showAdminDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getStr(R.string.dialog_notice));
        adb.setMessage(getStr(R.string.message_noadmin));
        adb.setNegativeButton(getStr(R.string.dialog_close), new OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int arg1) {
                di.dismiss();
            }

        });
        adb.setPositiveButton(getStr(R.string.dialog_tryagain), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestAdmin();

            }

        });
        adb.show();
    }

    void requestAdmin() {

        if (prefs.getBoolean("admin", false)) return;

        ComponentName comp = new ComponentName(_this, DevAdmin.class);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.admin_details);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    private void setPin() {
        final PinDialog adb = new PinDialog(_this, getStr(R.string.message_choosepin), getStr(R.string.dialog_setpin));
        adb.setCancelable(false);
        adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String pin1 = adb.getPin();

                if (pin1.equals("") || pin1.length() < 4) {
                    ComponentName cm = new ComponentName(_this, DevAdmin.class);
                    DPM.removeActiveAdmin(cm);
                    prefs.edit().putBoolean("admin", true).apply();
                    enableSMS.setChecked(false);
                    CustomToast.makeText(_this, getStr(R.string.error_setpin), Toast.LENGTH_LONG, 1).show();
                    return;
                }

                final PinDialog adbc = new PinDialog(_this, getStr(R.string.message_confirmpin), "Set Pin");
                adbc.setCancelable(false);
                adbc.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (pin1.equals(adbc.getPin()) && !adbc.getPin().equals("")) {
                            String pin;
                            try {
                                pin = Utils.SHA1(adbc.getPin());
                            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                                CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
                                enableSMS.setChecked(false);
                                updateStates();
                                e.printStackTrace();
                                return;
                            }

                            prefs.edit().putBoolean("smsenabled", true).putString("pin", pin).apply();
                            updateStates();
                            CustomToast.makeText(getBaseContext(), getStr(R.string.message_pinset), Toast.LENGTH_LONG, 2).show();
                            showPostSetup();
                        } else {
                            ComponentName cm = new ComponentName(_this, DevAdmin.class);
                            DPM.removeActiveAdmin(cm);
                            prefs.edit().putBoolean("admin", true).apply();
                            enableSMS.setChecked(false);
                            CustomToast.makeText(getBaseContext(), getStr(R.string.error_pinmatch), Toast.LENGTH_LONG, 1).show();
                            updateStates();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                prefs.edit().putBoolean("admin", true).apply();
                setPin();
            } else {
                enableSMS.setChecked(false);
                showAdminDialog();
                updateStates();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (resultCode == REQUEST_CODE_PERMISSIONS) {

            boolean permissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                }
            }

            if (permissionsGranted) {
                if (permissionsDuringEnable) {
                    if (!prefs.getBoolean("premium", false) || prefs.getBoolean("admin", false)) {
                        setPin();
                    } else {
                        requestAdmin();
                    }
                }
            } else {
                CustomToast.makeText(this, getStr(R.string.error_permissions), Toast.LENGTH_LONG).show();
                if (permissionsDuringEnable) {
                    enableSMS.setChecked(false);
                    updateStates();
                } else {
                    finish();
                }
            }
        }
    }

    private void updateStates() {
        Boolean current = smsenabled;
        smsenabled = prefs.getBoolean("smsenabled", false);
        if (current != smsenabled) {
            enableSMS.setChecked(smsenabled);

        }
        gotoSetKeyword.setEnabled(smsenabled);
		/*if(!prefs.getString("keyPhrase", "TMWMPI").equals("TMWMPI")){
			gotoSetKeyword.setBackgroundResource(R.drawable.complete_button);
		}else{
			gotoSetKeyword.setBackgroundResource(R.drawable.incomplete_button);
		}*///TODO: update this

        passSMS.setChecked(prefs.getBoolean("passChange", false));
        passSMS.setEnabled(smsenabled);

        toggleRestriction.setChecked(prefs.getBoolean("enableRestriction", false));
        toggleRestriction.setEnabled(smsenabled);

        gotoRestriction.setEnabled(smsenabled);

        // Check if we're at a high enough API to control DND
        //LG Phones are broken currently, see http://mobile.developer.lge.com/support/forums/general/?pageMode=Detail&tID=10000362
        //add extra check for the required activity
        Intent dndIntent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        List<ResolveInfo> dndlist = getPackageManager().queryIntentActivities(dndIntent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && dndlist.size() > 0 && !Build.BRAND.toLowerCase().equals("lg")) {
            if (prefs.getBoolean("dndcontrol", false) || dndPending) {
                dndPending = false;
                NotificationManager mNotificationManager = (NotificationManager) _this.getSystemService(Context.NOTIFICATION_SERVICE);

                // Check if the notification policy access has been granted for the app.
                if (!Objects.requireNonNull(mNotificationManager).isNotificationPolicyAccessGranted()) {
                    toggleDnd.setChecked(false);
                    prefs.edit().putBoolean("dndcontrol", false).apply();
                } else {
                    toggleDnd.setChecked(true);
                }
            } else {
                toggleDnd.setChecked(false);
            }

            toggleDnd.setEnabled(smsenabled);
        } else {
            //device doesn't support DND control, so hide the option if it exists
            if (toggleDnd != null) {
                toggleDnd.setVisibility(View.GONE);
                findViewById(R.id.toggledndtext).setVisibility(View.GONE);
            }
        }
    }

    private void showPostSetup() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getStr(R.string.dialog_finishSetupTitle));
        adb.setMessage(getStr(R.string.dialog_finishSetupMessage));
        adb.setPositiveButton(getStr(R.string.dialog_gotoKeyword), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(_this, SetKeyword.class);
                _this.startActivity(intent);
            }
        });
        adb.setNegativeButton(getStr(R.string.dialog_close), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        adb.show();
    }

    private String getStr(int id) {
        return getResources().getString(id);
    }

    @Override
    public void onConsentNeedsToBeRequested(GDPRPreperationData gdprPreperationData) {
        GDPR.getInstance().showDialog(this, gdprSetup, gdprPreperationData.getLocation());
    }

    @Override
    public void onConsentInfoUpdate(GDPRConsentState gdprConsentState, boolean isNewState) {
        if (isNewState) {
            // user just selected this consent, do whatever you want...
            switch (gdprConsentState.getConsent()) {
                case UNKNOWN:
                    // never happens!
                    break;
                case NO_CONSENT:
                    if (!bu.hasPremium())
                        bu.buyPremium();
                    break;
                case NON_PERSONAL_CONSENT_ONLY:
                case PERSONAL_CONSENT:
                    onConsentKnown(gdprConsentState.getConsent() == GDPRConsent.PERSONAL_CONSENT);
                    break;
            }
        } else {
            switch (gdprConsentState.getConsent()) {
                case UNKNOWN:
                    // never happens!
                    break;
                case NO_CONSENT:
                    // with the default setup, the dialog will shown in this case again anyways!
                    break;
                case NON_PERSONAL_CONSENT_ONLY:
                case PERSONAL_CONSENT:
                    // user restarted activity and consent was already given...
                    onConsentKnown(gdprConsentState.getConsent() == GDPRConsent.PERSONAL_CONSENT);
                    break;
            }
        }
    }

    private void onConsentKnown(boolean personal) {
        if (!prefs.getBoolean("premium", false)) {
            showAd(); //if the user isn't premium, show an ad
        }
        setupCrashlytics(personal);
    }

    private void setupCrashlytics(boolean gdprConsent) {
        boolean crashDisabled = !gdprConsent || BuildConfig.DEBUG;
        CrashlyticsCore cc = new CrashlyticsCore.Builder().disabled(crashDisabled).build();
        Fabric.with(this, new Crashlytics.Builder().core(cc).build());
    }

    /*
    @Override
    public final void startActivity(Intent intent) {
	    if(Build.VERSION.SDK_INT >= 21) {
	        super.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }else{
	        super.startActivity(intent);
        }
    }
    */
}
