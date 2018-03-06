package com.t3kbau5.smslocator;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Interactions extends AppCompatActivity {

	private final Context _this = this;
	private TableLayout tl;
	private DataHandler dh;
	private int UPTAPS = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interactions);
		// Show the Up button in the action bar.
		//setupActionBar();
		
		Utils.cancelNotif(this, 0);
		
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("notifDisplayNum", 0).apply(); //reset the notification display
		
		dh = new DataHandler(this, null, null, 1);
		
		tl = (TableLayout) findViewById(R.id.interactionsTable);
		
		final ProgressDialog pd = new ProgressDialog(_this);
		pd.setTitle(Utils.getStr(this, R.string.dialog_pleaseWait));
		pd.setMessage(Utils.getStr(this, R.string.dialog_interactionLoad));
		pd.show();
		
		new Thread(new Runnable(){
			
			List<Interaction> interactions = new ArrayList<Interaction>();
			int count = 0;
			
			@Override
			public void run() {
				interactions = dh.getInteractions();
				count = interactions.size();
				
				final TableRow trh = new TableRow(_this);
				final TextView th1 = new TextView(_this);
				final TextView th2 = new TextView(_this);
				final TextView th3 = new TextView(_this);
				final TextView th4 = new TextView(_this);
				
				th1.setPadding(5, 0, 5, 0);
				th2.setPadding(5, 0, 5, 0);
				th3.setPadding(5, 0, 5, 0);
				th4.setPadding(5, 0, 5, 0);
				
				th1.setText(Utils.getStr(_this, R.string.clabel_number));
				th2.setText(Utils.getStr(_this, R.string.clabel_date));
				th3.setText(Utils.getStr(_this, R.string.clabel_command));
				th4.setText(Utils.getStr(_this, R.string.clabel_response));
				
				trh.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				trh.addView(th1);
				trh.addView(th2);
				trh.addView(th3);
				trh.addView(th4);
				
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						tl.addView(trh, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					}
					
				});
				
				for(int i=0; i<count; i++){
					final TableRow tr = new TableRow(_this);
					final TextView tv1 = new TextView(_this);
					final TextView tv2 = new TextView(_this);
					final TextView tv3 = new TextView(_this);
					final TextView tv4 = new TextView(_this);

					tv1.setBackgroundResource(R.drawable.cellshape);
					tv2.setBackgroundResource(R.drawable.cellshape);
					tv3.setBackgroundResource(R.drawable.cellshape);
					tv4.setBackgroundResource(R.drawable.cellshape);
					
					tv1.setSingleLine(false);
					tv2.setSingleLine(false);
					tv3.setSingleLine(false);
					tv4.setSingleLine(false);
					
					tv1.setHorizontallyScrolling(false);
					tv2.setHorizontallyScrolling(false);
					tv3.setHorizontallyScrolling(false);
					tv4.setHorizontallyScrolling(false);
					
					tv1.setPadding(5, 0, 5, 0);
					tv2.setPadding(5, 0, 5, 0);
					tv3.setPadding(5, 0, 5, 0);
					tv4.setPadding(5, 0, 5, 0);
					
					Interaction iaction = interactions.get(i);
					
					tv1.setText(iaction.getNumber());
					
					long time = iaction.getDate();
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(time);
					tv2.setText(DateFormat.format("dd-MM-yyyy hh:mm", cal).toString());
					
					tv3.setText(iaction.getCommand());
					
					tv4.setText(iaction.getResponse());
					
					
					tr.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					tr.addView(tv1);
					tr.addView(tv2);
					tr.addView(tv3);
					tr.addView(tv4);
					
					
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
						}
						
					});
					
				}
				
				pd.dismiss();
				
			}
			
		}).start();
		
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
		getMenuInflater().inflate(R.menu.interactions, menu);
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
			
		case R.id.menu_clearInteractions:
			
			AlertDialog.Builder adb = new AlertDialog.Builder(this);
			
			adb.setTitle(Utils.getStr(_this, R.string.dialog_clearInteractions));
			adb.setMessage(Utils.getStr(_this, R.string.dialog_clearInteractionsMessage));
			adb.setPositiveButton(Utils.getStr(_this, R.string.dialog_clear), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dh.clearInteractions();
					
					CustomToast.makeText(_this, Utils.getStr(_this, R.string.message_interactionsCleared), Toast.LENGTH_LONG).show();
					finish();
				}
			});
			adb.setNegativeButton(Utils.getStr(_this, R.string.dialog_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					UPTAPS++;
					if(UPTAPS >= 10){
						PreferenceManager.getDefaultSharedPreferences(_this).edit().putBoolean("premium", true).apply();
						CustomToast.makeText(_this, "Premium unlocked!", CustomToast.LENGTH_LONG).show();
					}
					dialog.cancel();
				}
			});
			
			adb.show();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
