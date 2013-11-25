package com.safetyapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;



public class MainActivity extends Activity {
	
	
	public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_NICKNAME = "nickname";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    //This is the project number from the API Console
    String SENDER_ID = "571234493980";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "SafetyApp";
    String groupId;
    
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;
    
    private double lastLat;
    private double lastLon;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
            else {
            	Log.i(TAG,"GCM Reg ID = "+regid);
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
        
        //Setup Location Tracking
        
     // Acquire a reference to the system Location Manager
     		LocationManager locationManager = (LocationManager) this.getApplication().getSystemService(Context.LOCATION_SERVICE);

     		List<String> providers = locationManager.getProviders(true);

            /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
            Location l = null;
            
            for (int i=providers.size()-1; i>=0; i--) {
                    l = locationManager.getLastKnownLocation(providers.get(i));
                    if (l != null) break;
            }

            if (l != null) {
                    lastLat = l.getLatitude();
                    lastLon = l.getLongitude();
            }
            
            
     		
     		// Define a listener that responds to location updates
     		LocationListener locationListener = new LocationListener() {
     			
     			public void onLocationChanged(Location location) {
     				// Called when a new location is found by the location provider.
     				lastLat = location.getLatitude();
     				lastLon = location.getLongitude();
     				Log.i("SA", "Location Updated to: "+lastLat +", "+lastLon);
     				updateLocationOnServer();
     			}
     			
     			public void onStatusChanged(String provider, int status,
    					Bundle extras) {
    			}

    			public void onProviderEnabled(String provider) {
    			}

    			public void onProviderDisabled(String provider) {
    			}
     		};
     		
     	// Register the listener with the Location Manager to receive location
    		if (locationManager.getAllProviders().contains(
    				LocationManager.NETWORK_PROVIDER)) {
    			locationManager.requestLocationUpdates(
    					LocationManager.NETWORK_PROVIDER, 30000, 10, locationListener);
    		}
    		if (locationManager.getAllProviders().contains(
    				LocationManager.GPS_PROVIDER)) {
    			locationManager.requestLocationUpdates(
    					LocationManager.GPS_PROVIDER, 30000, 10, locationListener);
    		}
    		
    		DownloadWebPageTask task = new DownloadWebPageTask();
    		String url = "http://cse5473.web44.net/getInviteCode.php?id=" + regid;
    		
    		try {
				groupId = task.execute(url).get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			groupId = groupId.replaceAll("\\<.*?>"," ");
    		Log.i("SafetyApp"," SafetyApp - group num created " + groupId);
    }
    
    @Override
    protected void onResume() {
    	
        super.onResume();
        checkPlayServices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void showMap(View view) {
    	Log.i("SafetyApp"," SafetyApp - Enter ShowMap" );
        Intent intent = new Intent(this, MapActivity.class);
        Bundle b = new Bundle();
        final SharedPreferences prefs = getGCMPreferences(context);
        String name = (String) prefs.getAll().get(PROPERTY_NICKNAME);
        b.putCharSequence("name",name);
        b.putCharSequence("lat", "" + lastLat);
        b.putCharSequence("lon", "" + lastLon);
        intent.putExtras(b);
        startActivity(intent);
    }
    
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    
    
    /**
     * Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
    	return 1;
        /*try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }*/
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i(TAG,msg + "\n");
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
      // Your implementation here.
    }
    
    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        Log.i(TAG, "ID: "+regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
    
    public void storeNickName(String name) {

    	Log.i("SafetyApp"," SafetyApp - storeName");
    	final SharedPreferences prefs = getGCMPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_NICKNAME, name);
        editor.commit();
    }
    
    private String getNickName() {
        final SharedPreferences prefs = getGCMPreferences(context);
        String name = prefs.getString(PROPERTY_NICKNAME, "");
        if (name.isEmpty()) {
            Log.i(TAG, "Nickname not found.");
            return "";
        }
        return name;
    }
    
    
    public void setName1(String name){
    	Log.i("SafetyApp"," SafetyApp - setname1");
    	String rID = getRegistrationId(context);
    	this.storeNickName(name);
    	
    	Log.i("SafetyApp"," SafetyApp - storeName2 " + rID);
    	
    	DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute(new String[] { "http://cse5473.web44.net/updateName.php?id="+rID+"&name="+name });
        Log.i(TAG,"http://cse5473.web44.net/updateName.php?id="+rID+"&name="+name);
        Log.i("SafetyApp"," SafetyApp - storeName3 " );
        
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE); 
        Log.i("SafetyApp"," SafetyApp - storeName4 " );
        //inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
          //         InputMethodManager.HIDE_NOT_ALWAYS);
        Log.i("SafetyApp"," SafetyApp - storeName5 " );
        Toast.makeText(getApplicationContext(), "Display Name Changed to "+name,
        		   Toast.LENGTH_LONG).show();
        Log.i("SafetyApp"," SafetyApp - storeName6 " );
    }
    
    public void sendAlert(View view) {
    	DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute(new String[] { "http://cse5473.web44.net/recievedAlert.php?name="+getNickName()+"&lat="+lastLat+"&long="+lastLon + "&id=" + regid });
        Log.i(TAG,"http://cse5473.web44.net/recievedAlert.php?name="+getNickName()+"&lat="+lastLat+"&long="+lastLon);
    }
    
    public void gotoOptions(View view){
        Intent intent = new Intent(this, Options.class);
        Bundle b = new Bundle();
        final SharedPreferences prefs = getGCMPreferences(context);
        String name = (String) prefs.getAll().get(PROPERTY_NICKNAME);
        b.putCharSequence("name",name);
        b.putCharSequence("regid", regid);
        b.putCharSequence("groupId", groupId);
        intent.putExtras(b);
        startActivityForResult(intent,1);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i("SafetyApp"," SafetyApp - result code");
    	  if (requestCode == 1) {

    	     if(resultCode == RESULT_OK){      
    	         String result = data.getStringExtra("result");
    	         Log.i("SafetyApp"," SafetyApp - result code = " + result);
    	         setName1(result);
    	         Log.i("SafetyApp"," SafetyApp - done!!");
    	         
    	         
    	     }
    	     if (resultCode == RESULT_CANCELED) {    
    	         //Write your code if there's no result
    	     }
    	  }
    	}
    
    public void updateLocationOnServer() {
    	String rID = getRegistrationId(context);
    	if(!rID.equals("")) {
    		DownloadWebPageTask task = new DownloadWebPageTask();
            task.execute(new String[] { "http://cse5473.web44.net/updateLocations.php?id="+rID+"&lat="+lastLat+"&long="+lastLon });
            Log.i(TAG,"http://cse5473.web44.net/updateLocations.php?id="+rID+"&lat="+lastLat+"&long="+lastLon);
    	}
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
