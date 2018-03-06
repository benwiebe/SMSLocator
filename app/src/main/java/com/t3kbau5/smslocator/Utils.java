package com.t3kbau5.smslocator;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Utils {
    public static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static boolean compareToSHA1(String text, String hash) throws NoSuchAlgorithmException, UnsupportedEncodingException{
    	String tHash = SHA1(text);
    	return tHash.equals(hash);
    }

    public static Notification createNotif(Context context, String title, String message, int smallIcon, int largeIcon, Intent resultIntent, int number, int priority){

    	Bitmap largeIconB = BitmapFactory.decodeResource(context.getResources(), largeIcon);

    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(context)
    	        .setSmallIcon(smallIcon)
    	        .setContentTitle(title)
    	        .setContentText(message)
    	        .setLargeIcon(largeIconB)
    	        .setNumber(number)
    	        .setPriority(priority);



    	mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT));
    	return mBuilder.build();
    }

    public static int showNotif(Context context, String title, String message, int smallIcon, int largeIcon, Intent resultIntent, int number, int id, int priority){
    	int mId = 0;
    	if(id != -1){
    		mId = id;
    	}

    	Bitmap largeIconB = BitmapFactory.decodeResource(context.getResources(), largeIcon);

    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(context)
    	        .setSmallIcon(smallIcon)
    	        .setContentTitle(title)
    	        .setContentText(message)
    	        .setLargeIcon(largeIconB)
    	        .setNumber(number)
    	        .setPriority(priority);


    	mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT));
    	NotificationManager mNotificationManager =
    	    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	// mId allows you to update the notification later on.
    	mNotificationManager.notify(mId, mBuilder.build());
    	return mId;
    }

    public static int showPersistantNofif(Context context, String title, String message, int smallIcon, int largeIcon, Intent resultIntent, int id, int priority){
    	int mId = 0;
    	if(id != -1){
    		mId = id;
    	}

    	Bitmap largeIconB = BitmapFactory.decodeResource(context.getResources(), largeIcon);

    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(context)
    	        .setSmallIcon(smallIcon)
    	        .setContentTitle(title)
    	        .setContentText(message)
    	        .setLargeIcon(largeIconB)
    	        .setPriority(priority);


    	mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT))
    			.setOngoing(true);
    	NotificationManager mNotificationManager =
    	    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	// mId allows you to update the notification later on.
    	mNotificationManager.notify(mId, mBuilder.build());

    	return mId;
    }

    public static void showMessageNotif(Context context, String destination, String contents){
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    	int displayNum = prefs.getInt("notifDisplayNum", 0);
    	displayNum++;
    	prefs.edit().putInt("notifDisplayNum", displayNum).apply();

    	Intent intent = new Intent(context, Interactions.class);

    	Notification notif = createNotif(context, getStr(context, R.string.notif_sent_title), getStr(context, R.string.notif_sent_body) + destination, R.drawable.icon_notif, R.drawable.ic_launcher, intent, displayNum, Integer.parseInt(prefs.getString("notif_priority", "0")));

    	NotificationManager mNotificationManager =
        	    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        	// mId allows you to update the notification later on.
        	mNotificationManager.notify(0, notif);

    }

    public static void cancelNotif(Context context, int id){
    	NotificationManager mNotificationManager =
        	    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mNotificationManager.cancel(id);
    }


    public static String formatHtml(String coded){
		coded = coded.replace("[b]", "<b>");
		coded = coded.replace("[/b]", "</b>");
		coded = coded.replace("[i]", "<i>");
		coded = coded.replace("[/i]", "</i>");
		coded = coded.replace("[u]", "<u>");
		coded = coded.replace("[/u]", "</u>");
		coded = coded.replace("[br]", "<br />");
		coded = coded.replace("\n", "<br />");
		return coded;
	}

    public static Spanned formatAndSpan(String coded){
    	return Html.fromHtml(formatHtml(coded));
    }

    public static void sendSMS(String message, String destination){
    	if(destination.equals(null)) return; //if we didn't get a valid address, return
		SmsManager sm = SmsManager.getDefault();
		sm.sendTextMessage(destination, null, message, null, null);
    }

    public static String getProvider(Context context){
    	LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
    	else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return LocationManager.NETWORK_PROVIDER;
    	else return null;
    }

    public static String getProvider(LocationManager lm){
    	if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
    	else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return LocationManager.NETWORK_PROVIDER;
    	else return null;
    }

    public static String getStr(Context context, int id){
    	return context.getResources().getString(id);
    }

    public static Location getUpdatedLocation(final Context context){

    	final LocationListener ll = new LocationListener(){

    		int i=0;

			@Override
			public void onLocationChanged(Location location) {
				if(i >= 5){

				}else{
					i++;
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

		LocationManager lm  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		String provider;
		if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			provider = LocationManager.GPS_PROVIDER;
		}else if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			provider = LocationManager.NETWORK_PROVIDER;
		}else{
			return null;
		}

		//lm.requestLocationUpdates(provider, 5, 1000, ll); //TODO: this

    	
    	return null;
    }
    
    public static String randomBase64(){
    	byte[] r = new byte[64]; //Means 2048 bit
    	Random rand = new Random();
    	rand.nextBytes(r);
    	String s = Base64.encodeToString(r, Base64.DEFAULT);
    	return s;
    }
    
    
    //code from http://muzikant-android.blogspot.ca/2011/02/how-to-get-root-access-and-execute.html
    public static boolean canRunRootCommands()
    {
       boolean retval = false;
       Process suProcess;

       try
       {
          suProcess = Runtime.getRuntime().exec("su");

          DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
          DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

          if (null != os && null != osRes)
          {
             // Getting the id of the current user to check if this is root
             os.writeBytes("id\n");
             os.flush();

             String currUid = osRes.readLine();
             boolean exitSu = false;
             if (null == currUid)
             {
                retval = false;
                exitSu = false;
                Log.d("ROOT", "Can't get root access or denied by user");
             }
             else if (currUid.contains("uid=0"))
             {
                retval = true;
                exitSu = true;
                Log.d("ROOT", "Root access granted");
             }
             else
             {
                retval = false;
                exitSu = true;
                Log.d("ROOT", "Root access rejected: " + currUid);
             }

             if (exitSu)
             {
                os.writeBytes("exit\n");
                os.flush();
             }
          }
       }
       catch (Exception e)
       {
          // Can't get root !
          // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

          retval = false;
          Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
       }

       return retval;
    }
    
    public static boolean checkAdBlock(){
    	BufferedReader in = null;

            try {
				in = new BufferedReader(new InputStreamReader(
				        new FileInputStream("/etc/hosts")));
				String line;
				while ((line = in.readLine()) != null)
	            {
	                if (line.contains("admob"))
	                {
	                    return true;
	                }
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}

            return false;
    	
    }

	public static Bundle checkPermissionsGranted(Context context)
	{
		Bundle b = new Bundle();
		b.putInt(Manifest.permission.ACCESS_FINE_LOCATION, ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
        b.putInt(Manifest.permission.ACCESS_COARSE_LOCATION, ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
        b.putInt(Manifest.permission.RECEIVE_SMS, ActivityCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS));
        b.putInt(Manifest.permission.SEND_SMS, ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS));
        b.putInt(Manifest.permission.MODIFY_AUDIO_SETTINGS, ActivityCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS));
        //b.putInt(Manifest.permission.READ_PHONE_STATE, ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE));

        b.putBoolean("allGranted", b.getInt(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED);

        return b;
    }

	public static boolean isTestDevice(Activity act) {
		String testLabSetting = Settings.System.getString(act.getContentResolver(), "firebase.test.lab");
		return "true".equals(testLabSetting);
	}

	public static boolean internetConnected(Context ctx) {
		ConnectivityManager cm =
				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

}
