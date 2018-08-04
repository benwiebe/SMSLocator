package com.t3kbau5.smslocator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PinDialog extends AlertDialog.Builder{

	Context context;
	
	private String message;
	private Boolean hidden = true;
	
	private String input = "";

	public PinDialog(Context context) {
		this(context, "");
	}

	public PinDialog(Context context, String message) {
		this(context, message, context.getResources().getString(R.string.dialog_enterpin));
	}
	
	public PinDialog(Context context, String message, String title) {
		super(context);
		this.context = context;
		this.message = message;

		this.setTitle(title);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.setView(inflater.inflate(R.layout.pin_layout, null));

		super.setNegativeButton(getStr(R.string.dialog_cancel), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		});
		
	}
	
	public void setHidden(Boolean hidden){
		this.hidden = hidden;
	}
	
	public String getPin(){
		return input;
	}

	@Override
	@Deprecated
	public AlertDialog.Builder setNegativeButton(CharSequence title, DialogInterface.OnClickListener listener) {
		return this;
	}

	@Override
	@Deprecated
	public AlertDialog.Builder setNegativeButton(int resId, DialogInterface.OnClickListener listener) {
		return this;
	}

	@Override
	public AlertDialog.Builder setCancelable(boolean cancelable) {
		super.setCancelable(cancelable);
		super.setNegativeButton("", null);
		return this;
	}

	public AlertDialog show(){
		AlertDialog d = super.show();
		
		TextView tv = d.findViewById(R.id.pinLayoutMessage);
		tv.setText(message);
		
		final TextView output = d.findViewById(R.id.pinLayoutOutput);

		Button keys[] = new Button[11];
		
		keys[0] = d.findViewById(R.id.keypad_0);
		keys[1] = d.findViewById(R.id.keypad_1);
		keys[2] = d.findViewById(R.id.keypad_2);
		keys[3] = d.findViewById(R.id.keypad_3);
		keys[4] = d.findViewById(R.id.keypad_4);
		keys[5] = d.findViewById(R.id.keypad_5);
		keys[6] = d.findViewById(R.id.keypad_6);
		keys[7] = d.findViewById(R.id.keypad_7);
		keys[8] = d.findViewById(R.id.keypad_8);
		keys[9] = d.findViewById(R.id.keypad_9);
		keys[10] = d.findViewById(R.id.keypad_back);
		
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
