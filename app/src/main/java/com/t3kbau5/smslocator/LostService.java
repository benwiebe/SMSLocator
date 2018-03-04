package com.t3kbau5.smslocator;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class LostService extends IntentService{

	LocationManager lm;
	SharedPreferences prefs;
	Context context;

	boolean hasPremium = false;


	LocationListener lostModeListener;


	public LostService() {
		super("LostService");
		Log.d("LS", "LostService()");

		lostModeListener = new LocationListener(){

			boolean firstChange = true;

			@Override
			public void onLocationChanged(Location loc) {
				String destinationAddress = prefs.getString("lostNumber", "");
				Log.d("OLC:LOST", String.valueOf(loc.getAccuracy()));
				double lat = loc.getLatitude();
				double lon = loc.getLongitude();
				double acc = loc.getAccuracy();
				if(firstChange){
					firstChange = false;
					prefs.edit().putFloat("lost_lat", ((float) lat)).putFloat("lost_lon", ((float) lon)).apply();
					return;
				}

				Location dest = new Location(LocationManager.PASSIVE_PROVIDER);
				dest.setLatitude(((double) prefs.getFloat("lost_lat", 0)));
				dest.setLongitude(((double) prefs.getFloat("lost_lon", 0)));
				if(loc.distanceTo(dest) < 50) return;

				prefs.edit().putFloat("lost_lat", ((float) lat)).putFloat("lost_lon", ((float) lon)).apply();

				reply(getStr(R.string.sms_movementalert), destinationAddress);
				if(loc.getProvider().equals(LocationManager.NETWORK_PROVIDER)){
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc + getStr(R.string.smstemp_notacc), destinationAddress);
				}else{
					reply(getStr(R.string.smstemp_pos1) + getStr(R.string.smstemp_lat) + lat + getStr(R.string.smstemp_lon) + lon + getStr(R.string.smstemp_acc) + acc, destinationAddress);
				}
				if(hasPremium){
					reply(getStr(R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15", destinationAddress);
				}

			}

			@Override
			public void onProviderDisabled(String provider) {
				Log.d("OLC:LOST", "provider disabled");
			}

			@Override
			public void onProviderEnabled(String provider) {
				Log.d("OLC:LOST", "provider enabled");
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				Log.d("OLC:LOST", "status changed");

			}

		};

	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("LS", "onHandleIntent");
		context = getApplicationContext();
		hasPremium = intent.getExtras().getBoolean("hasPremium");

		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		lm  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(getProvider(lm), 0, 0, lostModeListener); //TODO: look into this before implementing full service
	}
	
	@Override
	public void onDestroy(){
		final LocationManager lm  = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(lostModeListener);
	}


	private void reply(String message, String destination){
		Utils.sendSMS(message, destination);
		if(prefs.getBoolean("preference_notify", true)){
			/*Intent intent = new Intent(context, MessageInfo.class);
			intent.putExtra("message", message);
			intent.putExtra("destination", destination);
			Utils.showNotif(context, getStr(R.string.notif_sent_title), getStr(R.string.notif_sent_body) + destination, R.drawable.icon_notif, R.drawable.ic_launcher, intent, 0);*/
			Utils.showMessageNotif(context, destination, message);
		}
		
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
	
	public String getStr(int id){
    	return context.getResources().getString(id);
    }
	
}
