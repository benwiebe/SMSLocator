package com.t3kbau5.smslocator;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

/**
 * Created by Ben on 2018-03-24.
 */

public class LostMode extends BroadcastReceiver {
    private static LostMode singleton = null;

    private static String FENCE_ID = "03232018";
    private static int FENCE_RAD = 50;
    private static int FENCE_CAST_ID = 158926; //random lol
    private static String FENCE_CAST_NAME = "com.t3kbau5.smslocator.DEVICE_MOVED";

    private LostMode() {
        super();
    }

    public static LostMode getInstance() {
        if(singleton == null)
            singleton = new LostMode();
        return singleton;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (action.equalsIgnoreCase("android.intent.action.ACTION_BATTERY_LOW")) {
            if (prefs.getBoolean("lostMode", false)) {

                String dest = prefs.getString("lostNumber", "");

                if (dest.equals("") || dest.equals(null)) return;

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int pct = level / scale;
                reply(context, getStr(context, R.string.smstemp_lowbat) + pct, dest);
                addInteraction(context, dest, "[EVENT]", getStr(context, R.string.smstemp_lowbat) + pct);
            } else {
                return;
            }
        }else if (action.equalsIgnoreCase(FENCE_CAST_NAME)) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            Location loc = geofencingEvent.getTriggeringLocation();
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();

            String destinationAddress = prefs.getString("lostNumber", "");
            reply(context, getStr(context, R.string.sms_movementalert), destinationAddress);
            addInteraction(context, destinationAddress, "[EVENT]", getStr(context, R.string.sms_movementalert));
            String locmsg = getStr(context, R.string.smstemp_pos1) + getStr(context, R.string.smstemp_lat) + lat + getStr(context, R.string.smstemp_lon) + lon + getStr(context, R.string.smstemp_acc) + loc.getAccuracy();
            reply(context, locmsg, destinationAddress);
            addInteraction(context, destinationAddress, "[EVENT]", locmsg);

            if(prefs.getBoolean("premium", false)){
                reply(context, getStr(context, R.string.smstemp_gml) + "http://maps.google.com/maps?q=" + lat + "," + lon + "&ll=" + lat + "," + lon + "&z=15", destinationAddress);
            }
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String resp = getStr(context, R.string.sms_lostNoPerm);
                addInteraction(context, destinationAddress, "[EVENT]", resp);
                reply(context, resp, destinationAddress);
            }else {
                setupGeofence(context, loc);
            }
        }
    }

    public static void enterLostMode(final Context context, final String destination, final String req) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        FusedLocationProviderClient flc = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String resp = getStr(context, R.string.sms_lostNoPerm);
            addInteraction(context, destination, req, resp);
            reply(context, resp, destination);
        }else{
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
            filter.addAction(FENCE_CAST_NAME);
            context.getApplicationContext().registerReceiver(getInstance(), filter);
            flc.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    setupGeofence(context, location);
                    reply(context, getStr(context, R.string.sms_lostOn), destination);
                    addInteraction(context, destination, req, getStr(context, R.string.sms_lostOn));
                    prefs.edit().putBoolean("lostMode", true).putString("lostNumber", destination).apply();
                }
            });
        }
    }

    public static void exitLostMode(Context context, String sender, String req){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final GeofencingClient gc = LocationServices.getGeofencingClient(context);
        ArrayList<String> reqs = new ArrayList<>();
        reqs.add(FENCE_ID);
        gc.removeGeofences(reqs);
        context.getApplicationContext().unregisterReceiver(getInstance());

        prefs.edit().putBoolean("lostMode", false).putString("lostNumber", "").apply();
        reply(context, getStr(context, R.string.sms_lostOff), sender);
        addInteraction(context, sender, req, getStr(context, R.string.sms_lostOff));
    }

    private static String getStr(Context context, int id){
        return context.getResources().getString(id);
    }

    private static void reply(Context context, String message, String destination){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Utils.sendSMS(message, destination);
        if(prefs.getBoolean("preference_notify", true)){
			Utils.showMessageNotif(context, destination, message); //TODO update this to work with interactions
        }
    }

    private static void setupGeofence(Context context, Location location) throws SecurityException{
        final GeofencingClient gc = LocationServices.getGeofencingClient(context);
        Geofence geofence = new Geofence.Builder()
                .setCircularRegion(location.getLatitude(), location.getLongitude(), FENCE_RAD)
                .setRequestId(FENCE_ID)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        GeofencingRequest gr = new GeofencingRequest.Builder().addGeofence(geofence).build();
        Intent intent = new Intent();
        intent.setAction(FENCE_CAST_NAME);

        PendingIntent pi = PendingIntent.getBroadcast(context, FENCE_CAST_ID, intent, PendingIntent.FLAG_ONE_SHOT);
        gc.addGeofences(gr, pi);
    }

    private static void addInteraction(Context context, String number, String request, String response){
        DataHandler dh = new DataHandler(context, null, null, 1);
        Interaction iaction = new Interaction(-1, number, request, response, System.currentTimeMillis());
        dh.addInteraction(iaction);
        dh.close();
    }
}
