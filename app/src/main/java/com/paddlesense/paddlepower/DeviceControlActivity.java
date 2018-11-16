/*
 * Copyright (C) 2013 The Android Open Source Project
 * This software is based on Apache-licensed code from the above.
 * 
 * Copyright (C) 2013 APUS
 *
 *     This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paddlesense.paddlepower;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;

import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.GraphicalView;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements IVisible {
	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();

	// BLE stuff
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	// Database
	private EventsDataSource datasource;
	
	// Various UI stuff
	public static boolean currentlyVisible;
	private boolean logging = false;
	private TextView mDataField;
	private String mDeviceName;
	private String mDeviceAddress;
	private ImageButton mButtonStart;
	private ImageButton mButtonStop;

	// Helpers
	private Analyzer analyzer;
	private ChartController mChartController;

	//Logging
	OutputStream output = null;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(true);
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				updateConnectionState(false);
				invalidateOptionsMenu();
				clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
				// mButtonStop.setVisibility(View.VISIBLE);
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				analyzer.processData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};

	// Handles various events fired by the Analyzer
	// STROKE_POINTS_AVAILABLE: a new stroke dataset is available for display
	private final BroadcastReceiver mStrokeDataReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG , "In mStrokeDataReceiver");
			final String action = intent.getAction();
			if (Analyzer.STROKE_POINTS_AVAILABLE.equals(action)) {
				updateDisplay();
				invalidateOptionsMenu();
			}
		}
	};

	private void clearUI() {
		// mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		mDataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(TAG, "onCreate");

		setContentView(R.layout.paddlepower);

		// Analyzer for analyzing points
		analyzer = new Analyzer(this);

		// getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Set up database connection
        datasource = new EventsDataSource(this);
        datasource.open();
        
		mDataField = (TextView) findViewById(R.id.data_value);

		mButtonStart = (ImageButton) findViewById(R.id.btnStart);
		mButtonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startLogging();
			}
		});

		mButtonStop = (ImageButton) findViewById(R.id.btnStop);
		mButtonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopLogging();
			}
		});

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		// TODO: Lars added this
		this.startService(gattServiceIntent);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		// Add the chart
		LinearLayout layout = (LinearLayout) findViewById(R.id.chart);

		if (mChartController == null) {
			mChartController = new ChartController();
			GraphicalView mChart = mChartController.initChart(this);
			layout.addView(mChart);
		} else {
			mChartController.repaint();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		currentlyVisible = true;

		// Process Gatt updates from the BLE service
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}

		// Process availability of a new stroke dataset from the Analyzer
		registerReceiver(mStrokeDataReceiver, makeStrokeDataIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	// this is called when the screen rotates.
	// (onCreate is no longer called when screen rotates due to manifest, see:
	// android:configChanges)
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// setContentView(R.layout.paddlepower);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Log.i(TAG, "ORIENTATION_LANDSCAPE");
		} else {
			Log.i(TAG, "ORIENTATION_PORTRAIT");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		currentlyVisible = false;
		Log.i(TAG, "In onPause");
		//unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		currentlyVisible = false;
		unbindService(mServiceConnection);
		// mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
			if (logging) {
				menu.findItem(R.id.menu_start_logging).setVisible(false);
				menu.findItem(R.id.menu_stop_logging).setVisible(true);
				mButtonStart.setVisibility(View.GONE);
				mButtonStop.setVisibility(View.VISIBLE);
			} else {
				menu.findItem(R.id.menu_start_logging).setVisible(true);
				menu.findItem(R.id.menu_stop_logging).setVisible(false);
				mButtonStart.setVisibility(View.VISIBLE);
				mButtonStop.setVisibility(View.GONE);
			}
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
			menu.findItem(R.id.menu_start_logging).setVisible(false);
			menu.findItem(R.id.menu_stop_logging).setVisible(false);
			mButtonStart.setVisibility(View.GONE);
			mButtonStop.setVisibility(View.GONE);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case R.id.menu_start_logging:
			startLogging();
			return true;
		case R.id.menu_stop_logging:
			stopLogging();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final boolean connected) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (connected) {
					mButtonStart.setVisibility(View.VISIBLE);
					mButtonStop.setVisibility(View.GONE);
				} else {
					mButtonStart.setVisibility(View.GONE);
					mButtonStop.setVisibility(View.GONE);
				}
			}
		});
	}

	int x = 0;

	private void updateDisplay() {
		// A new stroke dataset is available for display
		// Update the current power reading
		mDataField.setText("Power: " + analyzer.getStrokePower());
		List<StrokePoint> points = analyzer.getReadings();
		mChartController.addSeries(points);
	}

	// Demonstrates how to iterate through the supported GATT
	// Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the
	// ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = getResources().getString(
				R.string.unknown_service);
		String unknownCharaString = getResources().getString(
				R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME,
					SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

				if (UUID.fromString(SampleGattAttributes.PADDLE_POWER_MEASUREMENT)
						.equals(gattCharacteristic.getUuid())) {
					Log.d(TAG, "Found paddle power!");
					mNotifyCharacteristic = gattCharacteristic;
				}

				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(LIST_NAME,
						SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	private static IntentFilter makeStrokeDataIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Analyzer.STROKE_POINTS_AVAILABLE);
		return intentFilter;
	}

	/*
	We need to binary write stroke points to save space
	 */
	public void appendLog(StrokePoint sp) {
		// Convert the point to bytes
		byte[] outBytes = sp.toBytes();

		try {
			output.write(outBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startLogging() {
		mButtonStop.setVisibility(View.VISIBLE);
		mButtonStart.setVisibility(View.GONE);
		mBluetoothLeService.setCharacteristicNotification(
				mNotifyCharacteristic, true);
		invalidateOptionsMenu();
		logging = true;

		// Open the log file
		File logFile = new File(Environment.getExternalStorageDirectory()
				.getPath() + "/ppdata.dat");

		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "Error while creating file. ", e);
				e.printStackTrace();
			}
		}

		try {
			output = new BufferedOutputStream(new FileOutputStream(logFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void stopLogging() {
        Log.d(TAG, "stopLogging");
		mButtonStop.setVisibility(View.GONE);
		mButtonStart.setVisibility(View.VISIBLE);
		mBluetoothLeService.setCharacteristicNotification(
				mNotifyCharacteristic, false);
		invalidateOptionsMenu();
		logging = false;

		// Close the log file
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean isCurrentlyVisible() {
		return currentlyVisible;
	}
}
