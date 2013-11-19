package com.safetyapp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends Activity {
	
	private GoogleMap mMap;
	private String name;
	private double lat,lon;
	private String directions = "";
	private boolean wait = true;
	private double lastLat;
    private double lastLon;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		// Show the Up button in the action bar.
		setupActionBar();
		
		setTitle("Safety Alert Map");
		
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
		
		
		//Set private variables based on push notification payload
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    name = extras.getString("name");
		    Log.i("SafetyApp","SafetyApp - Sender Name: "+name);
		    
		    lat = Double.parseDouble(extras.getString("lat"));
		    Log.i("SafetyApp", "SafetyApp - Sender Lat: "+lat);
		    
		    lon = Double.parseDouble(extras.getString("lon"));
		    Log.i("SafetyApp", "SafetyApp - Sender Lon: "+lon);
		}else{
			Log.i("SafetyApp"," SafetyApp - bundle is empty");
			
			
		}
		
		//Get map fragment
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mMap.setMyLocationEnabled(true);
		
		//Clear any old markers
		mMap.clear();
		
		SimpleDateFormat fmt = new SimpleDateFormat("h:mm aa M/d/yy");
		
		//Add new marker from push notification
		mMap.addMarker(new MarkerOptions()
	    .position(new LatLng(lat, lon))
	    .title(name +" needs help here!")
	    .snippet(fmt.format(new Date()))).showInfoWindow();
		
		//Move map camera to alert location
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLon), 17);
	    mMap.animateCamera(cameraUpdate);
	    
	    Log.i("SafetyApp"," SafetyApp - map is ready");
    	DownloadWebPageTask task = new DownloadWebPageTask();
    	String origin = lastLat +"," + lastLon;
    	String dest = lat + "," +lon;
    	
    	mMap.addMarker(new MarkerOptions()
	    .position(new LatLng(lastLat, lastLon))
	    .title("You are here")
	    ).showInfoWindow();
    	
    	String url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + dest + "&sensor=false";
    	Log.i("SafetyApp"," SafetyApp - task started");
    	
    	task.execute(url);
    	while (wait);
    	Log.i("SafetyApp"," SafetyApp - web results "+ directions);
    	
    	
    	PolylineOptions path = new PolylineOptions().width(5).color(Color.RED);
    	
    	
    	List<LatLng> pts = null;
    	path.add(new LatLng(lastLat, lastLon));
    	Log.i("SafetyApp"," SafetyApp - get points");
		pts = getDirections(directions);
    	Log.i("SafetyApp"," SafetyApp - points =  "+ pts);
    	
    	path.addAll(pts);
    	Polyline polyline = mMap.addPolyline(path);
	    
	    
//	    double latf = 40.05297;
//	    double lonf = -82.98658;
//	    
//	    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr="+lat+","+lon+"&daddr="+latf+","+lonf));
//	     startActivity(intent);
	    
	    
	    
	}
	
	public List<LatLng> getDirections(String json) {
		List<LatLng> points = new ArrayList<LatLng>();
		double lat1 = 0, lon1 = 0, lat2 = 0, lon2 = 0;
		Log.i("SafetyApp", " SafetyApp - start reading");
		String[] jsonArray = json.split(" ");
		boolean start = false;
		for (int i = 0; i < jsonArray.length; i++) {
			if(jsonArray[i].replace('"', ' ').trim().equals("steps")){
				start = true;
			}
			if (jsonArray[i].length() > 0 && start) {
				if (jsonArray[i].replace('"', ' ').trim().equals("lat")) {
					if(lat1 == 0){
						lat1 = Double.parseDouble(jsonArray[i + 2].replace(',', ' ').trim());
					}else{
						lat2 = Double.parseDouble(jsonArray[i + 2].replace(',', ' ').trim());
					}
				} else if (jsonArray[i].replace('"', ' ').trim().equals("lng")) {
					if(lon1 == 0){
						lon1 = Double.parseDouble(jsonArray[i + 2].replace(',', ' ').trim());
					}else{
						lon2 = Double.parseDouble(jsonArray[i + 2].replace(',', ' ').trim());
					}
				}
				if (lat1 != 0 && lon1 != 0 && lat2 != 0 && lon2 != 0) {
					points.add(new LatLng(lat2, lon2));
					points.add(new LatLng(lat1, lon1));
					lat1 = 0;
					lat2 = 0;
					lon1 = 0;
					lon2 = 0;
				}
			}

		}
		return points;

	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
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
		}
		return super.onOptionsItemSelected(item);
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
              Log.i("SafetyApp"," SafetyApp - task finished");
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
          directions = response;    
          wait = false;
          return response;
        }

        @Override
        protected void onPostExecute(String result) {

        }
      }

}
