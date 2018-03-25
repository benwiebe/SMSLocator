package com.t3kbau5.smslocator;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BCR extends BroadcastReceiver {

    Context context;
    SharedPreferences prefs;
    String keyPhrase;
    String savedPin;

    Runnable rn;

    Boolean hasPremium = false;

    private DataHandler dh;

    public void onReceive(Context context, Intent intent) {
        this.context = context;

        dh = new DataHandler(context, null, null, 1);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("smsenabled", false)) return; //if we have the service disabled

        String action = intent.getAction();

        if (action.equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
            Log.d("BCR", "sms rec");
            keyPhrase = prefs.getString("keyPhrase", "TMWMPI");
            savedPin = prefs.getString("pin", "1234");

            Bundle pdusBundle = intent.getExtras();
            Object[] pdus = (Object[]) pdusBundle.get("pdus");
            if(pdus == null)
                return;
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);


            String msgBody = message.getMessageBody();
            String sender = message.getOriginatingAddress();
            String[] words = msgBody.split(" ");

            if (msgBody == null || msgBody.equals("") || msgBody.equals(null) || words.length < 1)
                return; //error prevention

            String pass = words[0];
            String cmd;
            String pin;

            if (pass.equals(keyPhrase)) {
                abortBroadcast();

                if (prefs.getBoolean("enableRestriction", false)) {
                    String serialized = prefs.getString("pnumbers", "");
                    String[] nums = serialized.split(",");
                    List<String> numbers = new ArrayList<String>(Arrays.asList(nums));
                    Boolean isOK = false;
                    for (int i = 0; i < numbers.size(); i++) {
                        if (numbers.get(i).equals(sender)) {
                            isOK = true;
                            break;
                        }
                    }

                    if (!isOK) {
                        addInteraction(sender, msgBody, getStr(R.string.comment_unauthorized_restriction));
                        //reply(getStr(R.string.sms_notauthed), sender);
                        return;
                    }
                }


                try {
                    cmd = words[2].toLowerCase(Locale.getDefault());
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    cmd = "";
                }

                try {
                    pin = words[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    pin = "";
                }
	            
	            /*if(pin.equals("") || pin.equals(null)){
	            	pin = cmd;
	            	cmd = "";
	            }*/

                Boolean isCorrectPin = false;
                try {
                    isCorrectPin = Utils.compareToSHA1(pin, savedPin);
                } catch (NoSuchAlgorithmException e) {
                    reply(getStr(R.string.sms_pinerror), sender);
                    addInteraction(sender, cmd, getStr(R.string.sms_pinerror));
                    e.printStackTrace();
                    return;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    reply(getStr(R.string.sms_pinerror), sender);
                    return;
                }

                if (!isCorrectPin) {
                    addInteraction(sender, cmd, getStr(R.string.sms_badpin));
                    reply(getStr(R.string.sms_badpin), sender);
                    return;
                }

                hasPremium = prefs.getBoolean("premium", false);
                boolean freemiumExpired = false;
                if(hasPremium && prefs.getBoolean("premium_is_freemium", false) && prefs.getLong("freemium_expiry", 0L) < Calendar.getInstance().getTimeInMillis()) {
                    prefs.edit().putBoolean("premium", false).apply();
                    freemiumExpired = true;
                    hasPremium = false;
                }

                if (cmd != null && !cmd.equals("") && !hasPremium) {
                    if(freemiumExpired) {
                        addInteraction(sender, cmd, getStr(R.string.sms_freemium_expired));
                        reply(getStr(R.string.sms_freemium_expired), sender);
                    }else{
                        addInteraction(sender, cmd, getStr(R.string.sms_nopremium));
                        reply(getStr(R.string.sms_nopremium), sender);
                    }
                    return;
                }

                DevicePolicyManager DPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                String resp = "";
                if (cmd.equals(null) || cmd.equals("")) {
                    resp = sendLocation(sender);
                } else if (cmd.equals(getStr(R.string.command_lock))) {
                    DPM.lockNow();
                    reply(getStr(R.string.sms_locked), sender);
                    resp = getStr(R.string.sms_locked);
                } else if (cmd.equals(getStr(R.string.command_reset))) {
                    if (!prefs.getBoolean("passChange", false)) {
                        addInteraction(sender, cmd, getStr(R.string.sms_nopasschange));
                        reply(getStr(R.string.sms_nopasschange), sender);
                        addInteraction(sender, cmd, getStr(R.string.sms_nopasschange));
                        return;
                    }
                    if (words.length != 4) return;
                    if (words[3].equals(getStr(R.string.command_reset_random))) {
                        int random1 = (int) (Math.random() * 9);
                        int random2 = (int) (Math.random() * 9);
                        int random3 = (int) (Math.random() * 9);
                        int random4 = (int) (Math.random() * 9);
                        String newCode = "" + random1 + random2 + random3 + random4;
                        DPM.resetPassword(newCode, 0);
                        reply(getStr(R.string.smstemp_passchange) + newCode, sender);
                        resp = getStr(R.string.smstemp_passchange) + newCode;
                    } else {
                        DPM.resetPassword(words[3], 0);
                        reply(getStr(R.string.smstemp_passchange) + words[3], sender);
                        resp = getStr(R.string.smstemp_passchange) + words[3];
                    }
                } else if (cmd.equals(getStr(R.string.command_sound))) {
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    resp = getStr(R.string.sms_audio);

                    if (prefs.getBoolean("dndcontrol", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                            resp += " " + getStr(R.string.sms_nonotifpolicy);
                        } else {
                            resp += " " + getStr(R.string.sms_undnd);
                            unDnd(mNotificationManager);
                        }
                    }

                    reply(resp, sender);
                } else if (cmd.equals(getStr(R.string.command_ring))) {
                    playSound();
                    reply(getStr(R.string.sms_ringing), sender);
                    resp = getStr(R.string.sms_ringing);
                }/*else if(cmd.equals(getStr(R.string.command_lost)) && isCorrectPin){
	            	
	            	if(prefs.getBoolean("lostMode", false)){
	            		exitLostMode(sender);
	            	}else{
	            		enterLostMode(sender);
	            	}
	            	
	            }*/ else {
                    addInteraction(sender, cmd, getStr(R.string.sms_invalidcommand));
                    reply(getStr(R.string.sms_invalidcommand), sender);
                    return;
                }
                addInteraction(sender, cmd, resp);
            }
        } else if (action.equalsIgnoreCase("android.intent.action.ACTION_BATTERY_LOW")) {
            if (prefs.getBoolean("lostMode", false)) {

                String dest = prefs.getString("lostNumber", "");

                if (dest.equals("") || dest.equals(null)) return;

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int pct = level / scale;
                reply(getStr(R.string.smstemp_lowbat) + pct, dest);
            } else {
                return;
            }
        }


    }

    private String sendLocation(String destinationAddress) {
        if (destinationAddress.equals("") || destinationAddress == null) return ""; //if we didn't get a valid address, return

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            reply(getStr(R.string.sms_permissionmissing) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_missingloc), destinationAddress);
            return getStr(R.string.sms_permissionmissing) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_missingloc);
        }

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Location loc;
        Boolean notAccurate;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            notAccurate = false;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            notAccurate = true;
		}else{
			reply(getStr(R.string.sms_noprovider) + " "+ getStr(R.string.sms_errorcode) + getStr(R.string.code_noprovider_initial), destinationAddress);
			return getStr(R.string.sms_noprovider) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_noprovider_initial);
		}

		if(loc == null){
			//reply(getStr(R.string.sms_nullloc), destinationAddress);
			sendUpdatedLocation(destinationAddress);
			return "[Sent updated location]";
		}

		final int refreshTime = Integer.parseInt(prefs.getString("refresh_time", "300000"));

		if(System.currentTimeMillis() - loc.getTime() > refreshTime){
			sendUpdatedLocation(destinationAddress);
			return "[Sent updated location]";
		}

		String resp;

		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		double acc = loc.getAccuracy();
		if(notAccurate){
			resp = getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc + getStr(R.string.smstemp_notacc);
			reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc + getStr(R.string.smstemp_notacc), destinationAddress);
		}else{
			resp = getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc;
			reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc, destinationAddress);
		}
		if(hasPremium){
			resp += "\n" + getStr(R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15";
			reply(getStr(R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15", destinationAddress);
		}
		return resp;
	}

	public void sendUpdatedLocation(final String destinationAddress){

		Log.d("BCR", "sendUpdatedLocation");

		final LocationManager lm  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		final Handler handler = new Handler();


    	final LocationListener ll = new LocationListener(){

    		int i=0;

			@Override
			public void onLocationChanged(Location loc) {

				if(i < 5 && loc.getAccuracy() > 8){
					i ++;
					return;
				}
				handler.removeCallbacks(rn);
				Log.d("OLC", String.valueOf(loc.getAccuracy()));
				double lat = loc.getLatitude();
				double lon = loc.getLongitude();
				double acc = loc.getAccuracy();
				if(loc.getProvider().equals(LocationManager.NETWORK_PROVIDER)){
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc + getStr(R.string.smstemp_notacc), destinationAddress);
				}else{
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc, destinationAddress);
				}
				if(hasPremium){
					reply(getStr(R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15", destinationAddress);
				}

                try {
                    lm.removeUpdates(this);
                }catch (SecurityException e){
                    e.printStackTrace();
                }

			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}
    		
    	};
    	
    	rn = new Runnable() {
			  @Override
			  public void run() {

              if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                  reply(getStr(R.string.sms_permissionmissing) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_missingloc), destinationAddress);
                  return;
              }
			    lm.removeUpdates(ll);
			    Location loc;
			    Boolean notAccurate;
			    if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
					loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					notAccurate = false;
				}else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
					loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					notAccurate = true;
				}else{
					reply(getStr(R.string.sms_nullloc) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_noprovider_update), destinationAddress);
					return;
				}
			    
			    if(loc == null){
			    	reply(getStr(R.string.sms_nullloc) + " " + getStr(R.string.sms_errorcode) + getStr(R.string.code_oldlocationnull), destinationAddress);
			    	return;
			    }
			    
			    double lat = loc.getLatitude();
				double lon = loc.getLongitude();
				double acc = loc.getAccuracy();
				if(notAccurate){
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc + getStr(R.string.smstemp_notacc), destinationAddress);
				}else{
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc, destinationAddress);
				}
				if(hasPremium){
					reply(getStr(R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15", destinationAddress);
				}
			    
			  }
			};
		
		int waitTime = Integer.parseInt(prefs.getString("gps_wait", "20000"));
		Log.d("BCR-WT", prefs.getString("gps_wait", "20000")); //load the wait time (default 20 sec)
		String provider = getProvider(lm);
		handler.postDelayed(rn, waitTime); //if we can't get a new location after waitTime ms, just send the old one
		lm.requestLocationUpdates(provider, 0, 0, ll);
	}
	
	private void reply(String message, String destination){
		Utils.sendSMS(message, destination);
		if(prefs.getBoolean("preference_notify", true)){
			/*Intent intent = new Intent(context, MessageInfo.class);
			intent.putExtra("message", message);
			intent.putExtra("destination", destination);
			Utils.showNotif(context, getStr(R.string.notif_sent_title), getStr(R.string.notif_sent_body) + destination, R.drawable.icon_notif, R.drawable.ic_launcher, intent, 0);*/
			Utils.showMessageNotif(context, destination, message); //TODO update this to work with interactions
		}
		
	}
	
	private void playSound(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		Ringtone r = RingtoneManager.getRingtone(context, notification);
		r.play();
	}

	@TargetApi(Build.VERSION_CODES.M)
    private void unDnd(NotificationManager nm){
        //see http://stackoverflow.com/a/35324211/1896516
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        //nm.setNotificationPolicy(NotificationManager.Policy.CR);
	}
	
	private String getProvider(LocationManager lm){
		if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			return LocationManager.GPS_PROVIDER;
		}else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			return LocationManager.NETWORK_PROVIDER;
		}else{
			return null;
		}
	}
	
	private void addInteraction(String number, String request, String response){
		
		Interaction iaction = new Interaction(-1, number, request, response, System.currentTimeMillis());
		
		dh.addInteraction(iaction);
	}
	
	public String getStr(int id){
    	return context.getResources().getString(id);
    }
	        
}
