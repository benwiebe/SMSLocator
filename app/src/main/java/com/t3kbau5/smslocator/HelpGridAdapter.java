package com.t3kbau5.smslocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

public class HelpGridAdapter extends BaseAdapter{

	private int count = 0;
	private Context context;
	private List<String> topics;
	
	public HelpGridAdapter(Context context){
		this.context = context;
		String[] t = context.getResources().getStringArray(R.array.help_topics);
		topics = new ArrayList<String>(Arrays.asList(t));
		count = topics.size();
	}
	
	@Override
	public int getCount() {
		return count;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Button b = null;
		if(convertView != null){
			b = (Button) convertView;
		}else{
			b = new Button(context);
		}
		
		final String topic = topics.get(position);
		
		b.setText(topic);
		//b.setBackgroundResource(R.drawable.main_button);
		b.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, HelpDisplay.class);
				intent.putExtra("topic", topic);
				context.startActivity(intent);
			}
			
		});
		
		return b;
	}

}
