package com.t3kbau5.smslocator;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class DevAdmin extends DeviceAdminReceiver {

	Context context;
	
    @Override
    public void onEnabled(Context context, Intent intent) {
    	Log.d("DevAdmin", "onEnabled");
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	prefs.edit().putBoolean("admin", true).commit();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
    	this.context = context;
    	Log.d("DevAdmin", "onDisableRequested");
    	return getStr(R.string.message_admindisable);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
    	this.context = context;
    	Log.d("DevAdmin", "onDisabled");
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	prefs.edit().putBoolean("admin", false).commit();
    	prefs.edit().putBoolean("smsenabled", false).commit();
    	CustomToast.makeText(context, getStr(R.string.message_smslocatordisabled), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
    	Log.d("DevAdmin", "onPasswordChanged");
    }

    public String getStr(int id){
    	return context.getResources().getString(id);
    }
    
}
