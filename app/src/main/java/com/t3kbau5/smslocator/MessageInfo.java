package com.t3kbau5.smslocator;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

public class MessageInfo extends ActionBarActivity {

	JSONArray messages;
	JSONArray destinations;
	
	TextView msgView;
	TextView senderView;
	TextView numView;
	
	int msgNumber = 0;
	
	boolean jsonerror = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_info);
		// Show the Up button in the action bar.
		setupActionBar();
		
		Utils.cancelNotif(this, 0);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putString("alertUri", "").apply();
		
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl_mi);
		
		rl.setOnTouchListener(new OnSwipeTouchListener(this){
			public void onSwipeRight() {
		        try {
					previousMessage();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    public void onSwipeLeft() {
		        try {
					nextMessage();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		});
		
		//Intent intent = getIntent();
		numView = (TextView) findViewById(R.id.numView);
		msgView = (TextView) findViewById(R.id.msgView);
		senderView = (TextView) findViewById(R.id.senderView);
		
		try {
			messages = new JSONArray(prefs.getString("msgArr", "[]"));
			destinations = new JSONArray(prefs.getString("destArr", "[]"));
			msgView.setText(getStr(R.string.message_message) + messages.getString(0));
			senderView.setText(getStr(R.string.message_to) + destinations.getString(0));
		} catch (JSONException e) {
			jsonerror = true;
			e.printStackTrace();
		}
		
		prefs.edit().putString("msgArr", "[]").putString("destArr", "[]").apply();
		
		if(jsonerror){
			msgView.setText(getStr(R.string.error_msgjson));
		}else{
			numView.setText((msgNumber + 1) + " " + getStr(R.string.message_of) + " " +  messages.length());
		}
		
		if(messages.length() > 1){
			CustomToast.makeText(this, getStr(R.string.message_swipe), Toast.LENGTH_SHORT).show();
		}
		
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.message_info, menu);
		return true;
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putString("messages", messages.toString());
		savedInstanceState.putString("destinations", destinations.toString());
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		try {
			messages = new JSONArray(savedInstanceState.getString("messages"));
			destinations = new JSONArray(savedInstanceState.getString("destinations"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
	
	private void nextMessage() throws JSONException{
		if(msgNumber != messages.length()-1){
			msgNumber ++;
			msgView.setText(getStr(R.string.message_message) + messages.getString(msgNumber));
			senderView.setText(getStr(R.string.message_to) + destinations.getString(msgNumber));
			numView.setText((msgNumber + 1) + " " + getStr(R.string.message_of) + " " +  messages.length());
			
		}
	}
	
	private void previousMessage() throws JSONException{
		if(msgNumber != 0){
			msgNumber --;
			msgView.setText(getStr(R.string.message_message) + messages.getString(msgNumber));
			senderView.setText(getStr(R.string.message_to) + destinations.getString(msgNumber));
			numView.setText((msgNumber + 1) + " " + getStr(R.string.message_of) + " " +  messages.length());
			
		}
	}
	
	public String getStr(int id){
    	return getResources().getString(id);
    }

}
