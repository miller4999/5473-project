package com.safetyapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
    List<Map<String,String>> dir = new ArrayList<Map<String,String>>();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("SafetyApp"," SafetyApp - Enter Map" );
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
		Marker marker2 = mMap.addMarker(new MarkerOptions()
	    .position(new LatLng(lastLat, lastLon))
	    .title("You are here"));
		
		Marker marker1 = mMap.addMarker(new MarkerOptions()
	    .position(new LatLng(lat, lon))
	    .title(name +" needs help here!")
	    .snippet(fmt.format(new Date())));
	    marker1.showInfoWindow();
		
		
		
		//Move map camera to alert location
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng((lastLat + lat)/2, (lastLon + lon)/2), 14);
	    mMap.animateCamera(cameraUpdate);
	    
	    Log.i("SafetyApp"," SafetyApp - map is ready");
    	DownloadWebPageTask task = new DownloadWebPageTask();
    	String origin = lastLat +"," + lastLon;
    	String dest = lat + "," +lon;
    	
    	
    	String url = "http://maps.googleapis.com/maps/api/directions/xml?origin=" + origin + "&destination=" + dest + "&sensor=false";
    	Log.i("SafetyApp"," SafetyApp - task started");
    	
    	task.execute(url);
    	while (wait);
    	//Log.i("SafetyApp"," SafetyApp - web results "+ directions);
    	
    	
    	PolylineOptions path = new PolylineOptions().width(5).color(Color.RED);
    	
    	
    	List<LatLng> pts = null;
    	//path.add(new LatLng(lastLat, lastLon));
    	Log.i("SafetyApp"," SafetyApp - get points");
		try {
			pts = getDirections(directions);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Log.i("SafetyApp"," SafetyApp - points =  "+ pts);
    	
    	path.addAll(pts);
    	Polyline polyline = mMap.addPolyline(path);
    	ListView lv = (ListView) findViewById(R.id.listView1);
    	Log.i("SafetyApp"," SafetyApp - set list");
    	SimpleAdapter simpleAdpt = new SimpleAdapter(this, dir, android.R.layout.simple_list_item_1, new String[] {"directions"}, new int[] {android.R.id.text1});
    	Log.i("SafetyApp"," SafetyApp - set list1");
    	lv.setAdapter(simpleAdpt);
    	Log.i("SafetyApp"," SafetyApp - set list2");

	    
//	    double latf = 40.05297;
//	    double lonf = -82.98658;
//	    
//	    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr="+lat+","+lon+"&daddr="+latf+","+lonf));
//	     startActivity(intent);
	    
	    
	    
	}
	
	public List<LatLng> getDirections(String json) throws XmlPullParserException, IOException {
		List<LatLng> points = new ArrayList<LatLng>();
		double lat1 = 0, lon1 = 0, lat2 = 0, lon2 = 0;
		boolean run = false;
		Log.i("SafetyApp", " SafetyApp - start reading");
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput( new StringReader ( json ) );

		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			
			if(eventType == XmlPullParser.START_TAG) {
				String tag = xpp.getName();
				if(tag.equals("lat") && run){
					eventType = xpp.next();
					String num = xpp.getText();
					lat1 = Double.parseDouble(num);
					

				}else if(tag.equals("lng") && run){
					eventType = xpp.next();
					String num = xpp.getText();
					lon1 = Double.parseDouble(num);
					
				}else if(tag.equals("step")){
					Log.i("SafetyApp", " SafetyApp - run = true");
					run = true;
				}else if(tag.equals("html_instructions") && run){
					eventType = xpp.next();
					String temp = xpp.getText();
					temp = temp.replaceAll("\\<.*?>"," ");
					Log.i("SafetyApp", " SafetyApp - directions = " + temp);
					Map <String, String> entry = new HashMap<String, String>();
					entry.put("directions", temp);
					dir.add(entry);
					
				}
			} else if(eventType == XmlPullParser.END_TAG) {
				
				String tag = xpp.getName();
				if(tag.equals("step")){
					Log.i("SafetyApp", " SafetyApp - run = false");
					run = false;
				}
			}
			if (lat1 != 0 && lon1 != 0 ) {
				points.add(new LatLng(lat1, lon1));
				lat1 = 0;
				lon1 = 0;
			}
			eventType = xpp.next();
		}
		System.out.println("End document");
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
