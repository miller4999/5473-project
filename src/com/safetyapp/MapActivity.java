package com.safetyapp;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
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

public class MapActivity extends Activity {
	
	private GoogleMap mMap;
	private String name;
	private float lat,lon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		// Show the Up button in the action bar.
		setupActionBar();
		
		setTitle("Safety Alert Map");
		
		//Set private variables based on push notification payload
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    name = extras.getString("name");
		    Log.i("SafetyApp","Sender Name: "+name);
		    
		    lat = Float.parseFloat(extras.getString("lat"));
		    Log.i("SafetyApp", "Sender Lat: "+lat);
		    
		    lon = Float.parseFloat(extras.getString("lon"));
		    Log.i("SafetyApp", "Sender Lon: "+lon);
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
		    .title(name+" needs help!")
		    .snippet(fmt.format(new Date())));
		
		//Move map camera to alert location
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 17);
	    mMap.animateCamera(cameraUpdate);
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

}
