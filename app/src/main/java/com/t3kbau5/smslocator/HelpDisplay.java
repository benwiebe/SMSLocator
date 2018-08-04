package com.t3kbau5.smslocator;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Objects;

public class HelpDisplay extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help_display);
		// Show the Up button in the action bar.
		Intent intent = this.getIntent();
		String topic = intent.getStringExtra("topic");
		
		this.setTitle(getStr(R.string.title_activity_help_display) + " " + topic);

		TextView tv = findViewById(R.id.helpDisplayTV);
		tv.setText(Html.fromHtml(getDisplayText(topic)));
		
		//setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help_display, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			//NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private String getDisplayText(String topic){
		String finalText = getStr(R.string.error_loadinghelp);
		Resources res = getResources();
		
		String[] topics = res.getStringArray(R.array.help_topics);
		
		if(topic.equals(topics[2])){
			finalText = getStr(R.string.help_commands) + "<br /> <br />";
			String[] commands = res.getStringArray(R.array.help_commands);
			String[] desc = res.getStringArray(R.array.help_commands_desc);
			
			for(int i=0; i<commands.length; i++){
				finalText = finalText + "<b>" + commands[i] + "</b> - " + desc[i] + "<br /><br />";
			}
			
		}else if(topic.equals(topics[0])){
			finalText = formatHtml(res.getString(R.string.help_pin));
		}else if(topic.equals(topics[1])){
			finalText = formatHtml(res.getString(R.string.help_keyword));
		}else if(topic.equals(topics[3])){
			finalText = formatHtml(res.getString(R.string.help_restriction));
		}else if(topic.equals(topics[4])){
			finalText = formatHtml(res.getString(R.string.help_uninstall));
		}else if(topic.equals(topics[5])) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://t3kbau5.com/support/"));
            startActivity(i);
            finish();
		}
		
		return finalText;
	}
	
	private String formatHtml(String coded){
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

	private String getStr(int id){
    	return getResources().getString(id);
    }
	
}
