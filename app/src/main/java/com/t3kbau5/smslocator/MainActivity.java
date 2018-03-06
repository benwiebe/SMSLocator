package com.t3kbau5.smslocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private int REQUEST_CODE_ENABLE_ADMIN = 1203;
    private int REQUEST_CODE_PERMISSIONS = 1703;
	private SharedPreferences prefs;
	
	private Context _this;
	ToggleButton enableSMS;
	Boolean smsenabled = false;
	Button gotoSetKeyword;
	CompoundButton passSMS;
	Button gotoRestriction;
	CompoundButton toggleRestriction;
	DevicePolicyManager DPM;
	CompoundButton toggleDnd;
	Boolean dndPending = false;
    Boolean permissionsDuringEnable = false;
	
	BillingUtil2 bu;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		_this = this;
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		DPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

		bu = new BillingUtil2(this);
		
		enableSMS = (ToggleButton) findViewById(R.id.enableSMS);

		enableSMS.setOnClickListener(new ToggleButton.OnClickListener(){

			@Override
			public void onClick(View v) {
				Boolean s = enableSMS.isChecked();
				
				if(s){
					showTermsDialog();
				}else{
					final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmdisable));
					adb.setCancelable(false);
					adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener(){
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Boolean isCorrectPin = false;
							try {
								isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
							} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
								CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
								enableSMS.setChecked(true);
								e.printStackTrace();
								return;
							}
							if(isCorrectPin){
								ComponentName cm = new ComponentName(_this, DevAdmin.class);
								DPM.removeActiveAdmin(cm);
								prefs.edit().putBoolean("smsenabled", false).apply();
								updateStates();
							}else{
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
		gotoSetKeyword = (Button) findViewById(R.id.gotoSetPass);
		gotoSetKeyword.setEnabled(smsenabled);
		/*if(!prefs.getString("keyPhrase", "TMWMPI").equals("TMWMPI")){
			gotoSetKeyword.setBackgroundResource(R.drawable.complete_button);
		}else{
			gotoSetKeyword.setBackgroundResource(R.drawable.incomplete_button);
		}*/ //TODO: update this
		gotoSetKeyword.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(_this, SetKeyword.class);
				startActivity(intent);
			}
		});
		
		passSMS = (CompoundButton) findViewById(R.id.togglePassChange);
		passSMS.setOnClickListener(new CompoundButton.OnClickListener(){

			@Override
			public void onClick(View v) {
				final Boolean isChecked = passSMS.isChecked();
				final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmpin));
				adb.setCancelable(false);
				adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						Boolean isCorrectPin = false;
						try {
							isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
						} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
							CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
							enableSMS.setChecked(!isChecked);
							e.printStackTrace();
							return;
						}
						
						if(isCorrectPin){
							prefs.edit().putBoolean("passChange", isChecked).apply();
						}else{
							passSMS.setChecked(!isChecked);
							CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();
							
						}
					}
					
				});
				adb.setHidden(true);
				adb.show();
			}
			
		});
		
		toggleRestriction = (CompoundButton) findViewById(R.id.toggleRestriction);
		toggleRestriction.setOnClickListener(new CompoundButton.OnClickListener(){

			@Override
			public void onClick(View v) {
				final Boolean isChecked = toggleRestriction.isChecked();
				final PinDialog adb = new PinDialog(_this, getStr(R.string.message_confirmpin));
				adb.setCancelable(false);
				adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						Boolean isCorrectPin = false;
						try {
							isCorrectPin = Utils.compareToSHA1(adb.getPin(), prefs.getString("pin", ""));
						} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
							CustomToast.makeText(_this, getStr(R.string.error_decoding), Toast.LENGTH_LONG, 1).show();
							toggleRestriction.setChecked(!isChecked);
							e.printStackTrace();
							return;
						}
						
						if(isCorrectPin){
							prefs.edit().putBoolean("enableRestriction", isChecked).apply();
						}else{
							toggleRestriction.setChecked(!isChecked);
							CustomToast.makeText(_this, getStr(R.string.error_badpin), Toast.LENGTH_LONG, 1).show();
							
						}
					}
					
				});
				adb.setHidden(true);
				adb.show();
			}
			
		});
		
		gotoRestriction = (Button) findViewById(R.id.gotoRestrictNumbers);
		gotoRestriction.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent i = new Intent(_this, RestrictNumbers.class);
            	startActivity(i);
			}
			
		});

        toggleDnd = (CompoundButton) findViewById(R.id.toggleDnd);
        if(toggleDnd != null) {
            toggleDnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!toggleDnd.isChecked()) {
                        prefs.edit().putBoolean("dndcontrol", false).apply();
                    } else {
                        //from http://stackoverflow.com/a/36162332/1896516
                        NotificationManager mNotificationManager = (NotificationManager) _this.getSystemService(Context.NOTIFICATION_SERVICE);

                        // Check if the notification policy access has been granted for the app.
                        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
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
		
		if(prefs.getBoolean("firstRun", true)){
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
		
		if(!prefs.getBoolean("premium", false)){
			showAd(); //if the user isn't premium, show an ad
		}


		//Support for the new permissions system
		if (smsenabled && !Utils.checkPermissionsGranted(this).getBoolean("allGranted")){
            permissionsDuringEnable = false;
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSIONS);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		if(prefs.getBoolean("premium", false)) menu.findItem(R.id.menu_unlock).setVisible(false);
		return true;
	}
	
	@Override
	public void onResume(){
		updateStates();
		
		super.onResume();
	}
	
	@Override
	public void onPause(){
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

                if(Utils.internetConnected(this) && bu.isConnected()) {

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
                                bu.buyPremium();
                            }
                        }
                    });
                    adb.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    adb.setNeutralButton(getStr(R.string.dialog_watchad), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            final ProgressDialog pd = ProgressDialog.show(_act, Utils.getStr(_act, R.string.dialog_pleaseWait), "");
                            final RewardedVideoAd mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(_act);
                            mRewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
                                @Override
                                public void onRewardedVideoAdLoaded() {
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
                                    Calendar expiry = Calendar.getInstance();
                                    expiry.add(Calendar.DAY_OF_MONTH, 7);
                                    String expiryString = expiry.get(Calendar.DAY_OF_MONTH) + "-" + (expiry.get(Calendar.MONTH) + 1) + "-" + expiry.get(Calendar.YEAR);

                                    prefs.edit().putLong("freemium_expiry", expiry.getTimeInMillis()).putBoolean("premium", true).apply();
                                    updateStates();
                                    AlertDialog.Builder adb = new AlertDialog.Builder(_this)
                                            .setTitle(Utils.getStr(_this, R.string.dialog_premium))
                                            .setMessage(Utils.getStr(_this, R.string.dialog_freemium_success) + " " + expiryString)
                                            .setPositiveButton(Utils.getStr(_this, R.string.dialog_continue), new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    adb.show();
                                }

                                @Override
                                public void onRewardedVideoAdLeftApplication() {

                                }

                                @Override
                                public void onRewardedVideoAdFailedToLoad(int i) {

                                }
                            });
                            AdRequest adreq = new AdRequest.Builder().addTestDevice("0CA205FF0785B1495463D2F5D77BEBF7")
                                    .addTestDevice("A030DF014385BBC02B04E68B65A8F7D4").build();

                            mRewardedVideoAd.loadAd("ca-app-pub-3534916998867938/5345514381", adreq);
                            //mRewardedVideoAd.loadAd("ca-app-pub-3940256099942544/5224354917", adreq);
                        }
                    });
                    adb.show();
                }else{ //billing not connected, device likely offline
                    AlertDialog.Builder adb = new AlertDialog.Builder(_this)
                                            .setTitle(Utils.getStr(_this, R.string.dialog_tryagain))
                                            .setMessage(Utils.getStr(_this, R.string.dialog_premium_offline))
                                            .setNegativeButton(Utils.getStr(_this, R.string.dialog_close), new DialogInterface.OnClickListener(){

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
            case R.id.menu_makeSystem:
				try {
					if(!Utils.canRunRootCommands()){
						Log.e("SMSL", "Can't run root commands!");
						return true;
					}
					Process sup = Runtime.getRuntime().exec("su");
					 DataOutputStream os = new DataOutputStream(sup.getOutputStream());
			         DataInputStream osRes = new DataInputStream(sup.getInputStream());
			         os.writeBytes("mount -o remount,rw /system /system\n");
			         os.flush();
			         os.writeBytes("pm path com.t3kbau5.smslocator\n");
		             os.flush();
		             
		             String path = osRes.readLine();
		             path = path.substring(9);
		             path = "/" + path;
		             
		             os.writeBytes("cp " + path + " /system/app/" + path.substring(9) + "\n");
		             os.flush();
		             android.os.Process.killProcess(android.os.Process.myPid());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void showAd(){

        if(Utils.isTestDevice(this))
            return;

		if(Utils.checkAdBlock()){
			
			//disable the services
			ComponentName cm = new ComponentName(_this, DevAdmin.class);
			DPM.removeActiveAdmin(cm);
			prefs.edit().putBoolean("smsenabled", false).apply();
			updateStates();
			
			//show adblock warning dialog
			final Activity _act = this;
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
												 .addTestDevice("A030DF014385BBC02B04E68B65A8F7D4").build();
		
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.mainLayout);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		mAdView.setLayoutParams(lp);
		//layout.setLayoutParams(lp);
		layout.addView(mAdView);
		mAdView.loadAd(adreq);
	}
	
	private Dialog showAdminDialog(){
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle(getStr(R.string.dialog_notice));
		adb.setMessage(getStr(R.string.message_noadmin));
		adb.setNegativeButton(getStr(R.string.dialog_close), new OnClickListener(){

				@Override
				public void onClick(DialogInterface di, int arg1) {
					di.dismiss();
				}
			   
		});
		adb.setPositiveButton(getStr(R.string.dialog_tryagain), new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				requestAdmin();
				
			}
			
		});
		return adb.show();
	}
	
	protected void requestAdmin(){
		
		if(prefs.getBoolean("admin", false)) return;
		
		ComponentName comp = new ComponentName(_this, DevAdmin.class);
		
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.admin_details);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	}
	
	private void setPin(){
		final PinDialog adb = new PinDialog(_this, getStr(R.string.message_choosepin), getStr(R.string.dialog_setpin));
		adb.setCancelable(false);
		adb.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener(){
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String pin1 = adb.getPin();
				
				if(pin1.equals("") || pin1 == null || pin1.length() < 4){
					ComponentName cm = new ComponentName(_this, DevAdmin.class);
					DPM.removeActiveAdmin(cm);
					prefs.edit().putBoolean("admin", true).apply();
					enableSMS.setChecked(false);
					CustomToast.makeText(_this, getStr(R.string.error_setpin), Toast.LENGTH_LONG, 1).show();
					return;
				}
				
				final PinDialog adbc = new PinDialog(_this, getStr(R.string.message_confirmpin), "Set Pin");
				adbc.setCancelable(false);
				adbc.setPositiveButton(getStr(R.string.dialog_done), new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(pin1.equals(adbc.getPin()) && !adbc.getPin().equals("")){
							String pin = "";
							try {
								pin = Utils.SHA1(adbc.getPin());
							} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
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
						}else{
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
	        }else{
	        	enableSMS.setChecked(false);
	        	showAdminDialog();
	        	updateStates();
	        }
	    }
	}

    @Override
    public void onRequestPermissionsResult(int resultCode, @NonNull String permissions[], @NonNull int[] grantResults){
        if(resultCode == REQUEST_CODE_PERMISSIONS){

            boolean permissionsGranted = true;
			for (int grantResult : grantResults) {
				if (grantResult != PackageManager.PERMISSION_GRANTED) {
					permissionsGranted = false;
				}
			}

            if(permissionsGranted){
                if(permissionsDuringEnable) {
                    if(!prefs.getBoolean("premium", false) || prefs.getBoolean("admin", false)){
                        setPin();
                    }else{
                        requestAdmin();
                    }
                }
            }else{
                CustomToast.makeText(this, getStr(R.string.error_permissions), Toast.LENGTH_LONG).show();
                if(permissionsDuringEnable) {
                    enableSMS.setChecked(false);
                    updateStates();
                }else {
                    finish();
                }
            }
        }
    }
	
	protected void updateStates(){
		Boolean current = smsenabled;
		smsenabled = prefs.getBoolean("smsenabled", false);
		if(current!= smsenabled){
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
            if(prefs.getBoolean("dndcontrol", false) || dndPending){
				dndPending = false;
                NotificationManager mNotificationManager = (NotificationManager) _this.getSystemService(Context.NOTIFICATION_SERVICE);

                // Check if the notification policy access has been granted for the app.
                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                    toggleDnd.setChecked(false);
                    prefs.edit().putBoolean("dndcontrol", false).apply();
                }else{
                    toggleDnd.setChecked(true);
                }
            }else{
                toggleDnd.setChecked(false);
            }

            toggleDnd.setEnabled(smsenabled);
		}else{
            //device doesn't support DND control, so hide the option if it exists
            if(toggleDnd != null) {
                toggleDnd.setVisibility(View.GONE);
                findViewById(R.id.toggledndtext).setVisibility(View.GONE);
            }
		}
	}
	
	private void showTermsDialog(){
		final Activity _this = this;
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle(getStr(R.string.dialog_terms))
			.setMessage(Utils.formatAndSpan(getStr(R.string.app_terms) + "[br][br]" + getStr(R.string.admin_details)))
			.setPositiveButton(getStr(R.string.dialog_agree), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					if (!Utils.checkPermissionsGranted(_this).getBoolean("allGranted")) {
                        permissionsDuringEnable = true;
                        ActivityCompat.requestPermissions(_this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSIONS);
                    }
				}
			})
			.setNegativeButton(getStr(R.string.dialog_donotagree), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					enableSMS.setChecked(false);
					updateStates();
					dialog.cancel();
				}
			});
		
		Dialog d = adb.show();
		((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	public void showPostSetup(){
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
	
	public String getStr(int id){
    	return getResources().getString(id);
    }
}
