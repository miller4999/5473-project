package com.safetyapp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Options extends Activity {


    private static final String PROPERTY_NICKNAME = "nickname";
    Context context;
	String SENDER_ID = "571234493980";
	String regid;
	String name;
	String groupId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_options);
		Log.i("SafetyApp","SafetyApp - entered options");
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    name = extras.getString("name");
		    Log.i("SafetyApp","SafetyApp - Sender Name: "+name);
		    regid = extras.getString("regid");
		    
		    Log.i("SafetyApp","SafetyApp - Sender id: "+ regid);
		    groupId = extras.getString("groupId");
		    
		}else{
			Log.i("SafetyApp"," SafetyApp - bundle is empty");
		}
		
		TextView t1 = (TextView) findViewById(R.id.currenName);
		t1.setText("Your current name is " + name);
		
		TextView t2 = (TextView) findViewById(R.id.currentGroupId);
		t2.setText("Your current group Id is " + groupId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options, menu);
		return true;
	}
	

	public void setName(View view) {
		Log.i("SafetyApp"," SafetyApp - setName function");
		EditText txt = (EditText) findViewById(R.id.changeName);
		String name = txt.getText().toString();
    	name = name.replace(" ", "");
    	if(name.length() > 0){
    		Intent returnIntent = new Intent();
    		returnIntent.putExtra("result",name);
    		setResult(RESULT_OK,returnIntent);   
    		finish();
    	}else{
    		 Toast.makeText(getApplicationContext(), "Please enter a non-empty name.",
          		   Toast.LENGTH_LONG).show();
    	}
	}
	 
	public void sendEmail(View view){
		Log.i("SafetyApp"," SafetyApp - sendEmail");
		EditText txt = (EditText) findViewById(R.id.emailAddress);
		String to = txt.getText().toString();
		String subject = "Group invitation from " + name + " for the safetyApp";
		String message = "The group code = " + groupId + ". Enter this in the options page to join their group.";
		
		//email
		Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_TEXT, message);
        // need this to prompts email client only
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client"));
	}
	
	public void addGroup(View view){
		EditText txt = (EditText) findViewById(R.id.groupId);
		String code = txt.getText().toString();
		DownloadWebPageTask task = new DownloadWebPageTask();
		String url = "http://cse5473.web44.net/subscribeToUser.php?id=" + regid + "&invite=" + code;
		task.execute(url);
		Toast.makeText(getApplicationContext(), "You have joined group " + code,
       		   Toast.LENGTH_LONG).show();
	}
	
	public void sendText(View view){
		Log.i("SafetyApp"," SafetyApp - sendEmail");
		EditText txt = (EditText) findViewById(R.id.phoneNum);
		String to = txt.getText().toString();
		String subject = "Group invitation from " + name + " for the safetyApp";
		String message = "The group code = " + groupId + ". Enter this in the options page to join their group.";
		//text
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage(to, null, message, null, null);
	}
	
    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
          String response = "";
          for (String url : urls) {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            try {
              HttpResponse execute = client.execute(httpGet);
              InputStream content = execute.getEntity().getContent();

              BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
              String s = "";
              while ((s = buffer.readLine()) != null) {
                response += s;
              }

            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          return response;
        }

        @Override
        protected void onPostExecute(String result) {

        }
      }
}
