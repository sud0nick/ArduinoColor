package com.trevorshp.arduinocolor;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


 
public class MainActivity extends FragmentActivity implements OnTouchListener, OnSeekBarChangeListener {
	public final static String TAG = "AndroidColor";
	public final static String EXTRA_DEVICE_KEY = "com.puffycode.android.bluetooth_devices";
	public final static String EXTRA_DEVICE_SELECTION = "com.puffycode.android.selected_device";
	public ColorPickerView colorPicker;
	private TextView text1;
	private static final int blueStart = 100;
	protected static final int REQUEST_ENABLE_BT = 0;
	
	private BluetoothAdapter BTAdapter;
	private BluetoothDevice BTDevice;
	private BluetoothSocket BTSocket;
	private OutputStream BTOutputStream;
	private final ArrayList<String> list = new ArrayList<String>();
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Do something with the returned data from BTListActivity
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == 0) {
				// Get the selected device MAC address
				String deviceAddr = data.getStringExtra(EXTRA_DEVICE_SELECTION);
				Log.d("BT_Bluetooth", "Using device: " + deviceAddr);
				
				// Get the Bluetooth device by using the MAC address . get a bluetooth socket
				BTDevice = BTAdapter.getRemoteDevice(deviceAddr);
				
				// Get a Bluetooth Socket
				try {
					Log.d("BT_Bluetooth", "Getting a socket...");
					BTSocket = BTDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Connect the socket
				try {
					Log.d("BT_Bluetooth", "Attempting to connect...");
					BTSocket.connect();
					BTOutputStream = BTSocket.getOutputStream();
				} catch (IOException e) {
					Log.d("BT_Bluetooth", "Failed to connect/write message");
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        
        // Get the Bluetooth Adapter
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
    	
        // Check if Bluetooth is supported by the device
    	if (BTAdapter == null) {
    		// Device does not support Bluetooth
    		alertUser("Fatal Error", "Bluetooth is required to run this application.  Unfortunately your device does not support Bluetooth.",
    				new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int which) {
            					finish();
            			}
            		}
    		);
    	}
    	
    	// Check if Bluetooth is enabled on the device
    	if (!BTAdapter.isEnabled()) {
    		// Device does not support Bluetooth
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth Disabled")
            .setMessage("Before using this application please enable Bluetooth on your device and connect to a Bluetooth module.")
            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
            AlertDialog alert = builder.create();
            alert.show();
    	}
    	
    	// Search for connected devices
    	Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
    	if (pairedDevices.size() > 0) {
    		for (BluetoothDevice device : pairedDevices) {
    			list.add(device.getName() + "\n" + device.getAddress());
    		}
    		// After the list of devices is built send it to the BTListActivity
    		Intent i = new Intent(this, BTListActivity.class);
    		i.putExtra(EXTRA_DEVICE_KEY, list);
    		startActivityForResult(i, 0);
    	}
    	
        LinearLayout layout = (LinearLayout) findViewById(R.id.color_picker_layout);
        final int width = layout.getWidth();
        //get the display density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        colorPicker = new ColorPickerView(this,blueStart,metrics.densityDpi);
        layout.setMinimumHeight(width);
        layout.addView(colorPicker);
        layout.setOnTouchListener(this);
        
        text1 = (TextView) findViewById(R.id.result1_textview);
	    text1.setText("Tap a color!");
                
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setProgress(blueStart);
		seek.setMax(255);
		seek.setOnSeekBarChangeListener(this);
    }
	    
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (BTSocket != null) {
			try {
				BTSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// sends color data to a Serial device as {R, G, B, 0x0A}
	private void sendToArduino(int color) {
		String dataToSend = String.format("%02X", Color.red(color)) + String.format("%02X", Color.green(color)) + String.format("%02X", Color.blue(color));
		Log.d("BT_Bluetooth", dataToSend.toString());
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
    // sets the text boxes' text and color background.
	private void updateTextAreas(int col) {
		int[] colBits = {Color.red(col),Color.green(col),Color.blue(col)};
		//set the text & color backgrounds
		text1.setText("You picked #" + String.format("%02X", Color.red(col)) + String.format("%02X", Color.green(col)) + String.format("%02X", Color.blue(col)));
		text1.setBackgroundColor(col);
		
		if (isDarkColor(colBits)) {
			text1.setTextColor(Color.WHITE);
		} else {
			text1.setTextColor(Color.BLACK);
		}
	}
	
	// returns true if the color is dark.  useful for picking a font color.
    public boolean isDarkColor(int[] color) {
    	if (color[0]*.3 + color[1]*.59 + color[2]*.11 > 150) return false;
    	return true;
    }
    
    @Override
    //called when the user touches the color palette
	public boolean onTouch(View view, MotionEvent event) {
    	int color = 0;
		color = colorPicker.getColor(event.getX(),event.getY(),true);
		colorPicker.invalidate();
		//re-draw the selected colors text
		updateTextAreas(color);
		//send data to arduino
		sendToArduino(color);
		return true;
	}
	
    @Override
	public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
		int amt = seek.getProgress();
		int col = colorPicker.updateShade(amt);
		updateTextAreas(col);
		sendToArduino(col);
		colorPicker.invalidate();
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		
	}
	
	// Methods for turning lights on and off
	public void turnLightsOn(View v) {
		String dataToSend = "xxonxx";
		Log.d("BT_Bluetooth", dataToSend);
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void turnLightsOff(View v) {
		String dataToSend = "xxoffx";
		Log.d("BT_Bluetooth", dataToSend);
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void colorShift(View v) {
		String dataToSend = "xshift";
		Log.d("BT_Bluetooth", dataToSend);
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void colorPulse(View v) {
		String dataToSend = "xpulse";
		Log.d("BT_Bluetooth", dataToSend);
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void colorParty(View v) {
		String dataToSend = "xparty";
		Log.d("BT_Bluetooth", dataToSend);
		if (BTOutputStream != null) {
			try {
				BTOutputStream.write(dataToSend.getBytes("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void alertUser(String title, String msg, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
        .setMessage(msg)
        .setCancelable(false)
        .setNegativeButton("Ok", listener);
        AlertDialog alert = builder.create();
        alert.show();
	}
}
