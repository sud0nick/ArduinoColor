
package com.trevorshp.arduinocolor;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BTListActivity extends ListActivity {
	
	public final static String EXTRA_DEVICE_KEY = "com.puffycode.android.bluetooth_devices";
	public final static String EXTRA_DEVICE_SELECTION = "com.puffycode.android.selected_device";
	private ArrayList<String> mDeviceList;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);
		
		// Retrieve device list from MainActivity
		mDeviceList = getIntent().getStringArrayListExtra(EXTRA_DEVICE_KEY);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDeviceList));
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String[] selection = ((String)l.getItemAtPosition(position)).split("\n");
		
		// Pack up the selection in a new intent and send it back to MainActivity
		Intent i = new Intent();
		i.putExtra(EXTRA_DEVICE_SELECTION, selection[1]);
		setResult(RESULT_OK, i);
		finish();
	}
}
