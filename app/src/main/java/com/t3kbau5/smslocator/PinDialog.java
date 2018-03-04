package com.t3kbau5.smslocator;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.t3kbau5.smslocator.Utils;

public class PinDialog{

	Context context;
	private AlertDialog.Builder adb;
	
	private String message;
	private Boolean hidden = false;
	
	private String input = "";
	
	public PinDialog(Context context, String message) {
		this.context = context;
		this.message = message;
		
		adb = new AlertDialog.Builder(context);
		adb.setTitle(getStr(R.string.dialog_enterpin));
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		adb.setView(inflater.inflate(R.layout.pin_layout, null));
		
	}
	
	public PinDialog(Context context, String message, String title) {
		this.context = context;
		this.message = message;
		
		adb = new AlertDialog.Builder(context);
		adb.setTitle(title);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		adb.setView(inflater.inflate(R.layout.pin_layout, null));
		
	}
	
	public Builder getBuilder(){
		return adb;
	}
	
	public void setBuilder(Builder builder){
		adb = builder;
	}
	
	public void setPositiveButton(String text, OnClickListener listener){
		adb.setPositiveButton(text, listener);
	}
	
	public void setNeutralButton(String text, OnClickListener listener){
		adb.setNeutralButton(text, listener);
	}
	
	public void setNegativeButton(String text, OnClickListener listener){
		adb.setNegativeButton(text, listener);
	}
	
	public void setCancelable(Boolean cancelable){
		adb.setCancelable(cancelable);
	}
	
	public void setHidden(Boolean hidden){
		this.hidden = hidden;
	}
	
	public String getPin(){
		return input;
	}
	public String getEncryptedPin() throws NoSuchAlgorithmException, UnsupportedEncodingException{
		return Utils.SHA1(input);
	}
	
	public Dialog show(){
		Dialog d = adb.show();
		
		TextView tv = (TextView) d.findViewById(R.id.pinLayoutMessage);
		tv.setText(message);
		
		final TextView output = (TextView) d.findViewById(R.id.pinLayoutOutput);
		
		
		Button keys[] = new Button[11];
		
		keys[0] = (Button) d.findViewById(R.id.keypad_0);
		keys[1] = (Button) d.findViewById(R.id.keypad_1);
		keys[2] = (Button) d.findViewById(R.id.keypad_2);
		keys[3] = (Button) d.findViewById(R.id.keypad_3);
		keys[4] = (Button) d.findViewById(R.id.keypad_4);
		keys[5] = (Button) d.findViewById(R.id.keypad_5);
		keys[6] = (Button) d.findViewById(R.id.keypad_6);
		keys[7] = (Button) d.findViewById(R.id.keypad_7);
		keys[8] = (Button) d.findViewById(R.id.keypad_8);
		keys[9] = (Button) d.findViewById(R.id.keypad_9);
		keys[10] = (Button) d.findViewById(R.id.keypad_back);
		
		for(int i=0; i<=9; i++){
			keys[i].setOnClickListener(new Button.OnClickListener(){

				@Override
				public void onClick(View v) {
					input = input + ((Button)v).getText();
					if(hidden){
						String display = "";
						for(int j=0; j<input.length(); j++){
							display = display + "•";
						}
						output.setText(display);
					}else{
						output.setText(input);
					}
					
				}
				
			});
		}
		keys[10].setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(input.length() == 0) return;
				input = input.substring(0, input.length()-1);
				if(hidden){
					String display = "";
					for(int j=0; j<input.length(); j++){
						display = display + "•";
					}
					output.setText(display);
				}else{
					output.setText(input);
				}
			}
			
		});
		return d;
	}
	
	public String getStr(int id){
    	return context.getResources().getString(id);
    }

}
