package com.t3kbau5.smslocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class NumberAdapter extends BaseAdapter{

	private Context context;
	private SharedPreferences prefs;
	private List<String> numbers = new ArrayList<String>();
	private int vid1, vid2;
	
	public NumberAdapter(Context c){
		context = c;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			vid1 = View.generateViewId();
			vid2 = View.generateViewId();
		}else{
			vid1 = 1;
			vid2 = 2;
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		loadData();
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return numbers.size();
	}

	@Override
	public Object getItem(int position) {
		return numbers.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View v;
		TextView tv;
		ImageView iv;
		if(convertView != null){
			v = convertView;
			tv = (TextView) v.findViewById(vid1);
			iv = (ImageView) v.findViewById(vid2);
		}else{
			v = new View(context);
			tv = new TextView(context);
			tv.setId(vid1);
			iv = new ImageView(context);
			iv.setId(vid2);
		}
		
		tv.setText(numbers.get(position));
		tv.setTextSize(25);
		
		tv.setOnClickListener(new TextView.OnClickListener(){

			@Override
			public void onClick(View v) {
				AlertDialog.Builder adb = new AlertDialog.Builder(context);
				adb.setTitle(getStr(R.string.dialog_numopt))
					.setMessage(getStr(R.string.dialog_numbody) + numbers.get(position) + "?")
					.setPositiveButton(getStr(R.string.dialog_remove), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							numbers.remove(position);
							saveData();
							updateData(false);
						}
					})
					.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					})
					.setNeutralButton(getStr(R.string.dialog_edit), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							AlertDialog.Builder eadb = new AlertDialog.Builder(context);
							eadb.setTitle(getStr(R.string.dialog_editnum));
							
							final EditText in = new EditText(context);
							in.setInputType(InputType.TYPE_CLASS_PHONE);
							in.setText(numbers.get(position));
							eadb.setView(in);
							
							eadb.setPositiveButton(getStr(R.string.dialog_done), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									numbers.set(position, in.getText().toString());
									saveData();
									updateData(false);
								}
							})
							.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							eadb.show();
						}
					});
				adb.show();
				
			}
			
		});
		
		return tv;
	}
	
	private void loadData(){
		String serialized = prefs.getString("pnumbers", "");
		String[] nums = serialized.split(",");
		if(nums[0].equals("")) numbers = new ArrayList<String>();
		else numbers = new ArrayList<String>(Arrays.asList(nums));
	}
	
	private void saveData(){
		String fs = "";
		for(int i=0; i<numbers.size()-1; i++){
			fs = fs + numbers.get(i) + ",";
		}
		if(numbers.size() > 0){
			fs = fs + numbers.get(numbers.size()-1);
		}
		Log.d("NumberAdapter", fs);
		prefs.edit().putString("pnumbers", fs).apply();
	}
	
	public void updateData(){
		loadData();
		this.notifyDataSetInvalidated();
		this.notifyDataSetChanged();
	}
	
	public void updateData(Boolean load){
		if(load){
			loadData();
		}
		this.notifyDataSetInvalidated();
		this.notifyDataSetChanged();
	}
	
	public void add(String number){
		numbers.add(number);
		saveData();
	}
	
	public String getStr(int id){
    	return context.getResources().getString(id);
    }

}
