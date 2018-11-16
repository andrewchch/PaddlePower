package com.paddlesense.paddlepower;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.*;



public class Analyzer {

    private boolean inReturn = false;
    private String TAG = "Analyzer";

    private ArrayList<StrokePoint> strokePoints = new ArrayList<>();
    private ArrayList<StrokePoint> returnPoints = new ArrayList<>();
    private Context mContext;

    public final static String STROKE_POINTS_AVAILABLE = "com.paddlesense.paddlepower.STROKE_POINTS_AVAILABLE";

    public final static int BLE_DATA_LENGTH = 2;
    public final static int BLE_HEADER_BYTES = 2;
    public final static int BLE_MAX_BYTES = 20;
    public final static int BLE_PAYLOAD_BYTES = 18;
    public final static int MIN_READINGS = 50;
    public final static int MAX_READINGS = 100;

    public Analyzer (Context context) {
        mContext = context;
    }

    public StrokePoint addReading(float reading, long time) {
        StrokePoint strokePoint = null;

        if (time == 0) {
            time = (new Date()).getTime();
        }

        // We're either stroking or returning to the start of the stroke
        float FORCE_THRESHOLD = 0.1f;
        if (reading > FORCE_THRESHOLD) {
            Log.d(TAG, "Stroking");
            // Now stroking
            if (inReturn) {
                // Was just returning so clear the readings
                strokePoints.clear();
            }
            inReturn = false;
            returnPoints.clear();

            strokePoint = new StrokePoint();
            strokePoint.force = reading;
            strokePoint.time = time;
            strokePoints.add(strokePoint);
        }
        else {
            Log.d(TAG, "Force less than threshold");
            // Returning
            if (!inReturn) {
                Log.d(TAG, "Returning");
                strokePoint = new StrokePoint();
                strokePoint.force = reading;
                strokePoint.time = time;
                returnPoints.add(strokePoint);

                int RETURN_POINTS_THRESHOLD = 5;
                if (returnPoints.size() >= RETURN_POINTS_THRESHOLD) {
                    inReturn = true;

                    // Broadcast that we have stroke points
                    if (strokePoints.size() > 0) {
                        broadcastPointsAvailable();
                    }
                }
            }
        }

        return strokePoint;
    }

    public void broadcastPointsAvailable () {
        Log.d(TAG, "Broadcasting points availability");
        final Intent intent = new Intent(STROKE_POINTS_AVAILABLE);
        mContext.sendBroadcast(intent);
    }

    public List<StrokePoint> getReadings() {
        return strokePoints;
    }

    public void clearReadings() {
        strokePoints.clear();
        returnPoints.clear();
    }

    /*
    Returns an approximation of stroke power, in our case the sum of force times time, = newton seconds
     */
    public float getStrokePower() {
        float totalPower = 0;
        long lastTime = 0;

        for (StrokePoint point: strokePoints) {
            if (lastTime == 0) {
                lastTime = point.time;
            }
            totalPower += point.force * 0.001 * (point.time - lastTime);
            lastTime = point.time;
        }

        return totalPower;
    }

    public boolean isInReturn () {
        return inReturn;
    }

    /*

    The data schema is:

    - 2 bytes sequencing header:
      - byte 1: stroke number (0-255, cycles back to 0)
      - byte 2: sequence number of a block of readings (0-255 max, will give 2.5 seconds at 50Hz)
    - 18 bytes of data

    totalling the maximum of 20 bytes per GATT packet. The client is expected to reassemble blocks of
    readings using the sequence number for ordering.

    We also send a terminating packet of:

      - byte 1: stroke number
      - byte 2: block number
      - byte 3: 0xff
      - byte 4: 0xff

    */
    public void processData(byte[] data) {

        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(
                    data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n"
                    + stringBuilder.toString());
        }

        // Get first two sequence bytes
        int stroke_seq_no = data[0] & 0xFF;
        int b1ock_seq_no = data[1] & 0xFF;

        // Convert bytes to a force
        double force = 0.01 * (b1 * 256 + b0);
        Log.d(TAG, String.format("Received force value: %2.2f, %d, %d", force, b0, b1));

        if (data != null) {
            long time = (new Date()).getTime();
            float dataElement = Float.parseFloat(data);

            // Log a stroke point
            StrokePoint sp = new StrokePoint();
            sp.time = time;
            sp.force = dataElement;

            appendLog(sp);

            // Update the analyzer
            addReading(dataElement, time);
        }
    }

    public void appendLog(String text) {
        File logFile = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/pplog.csv");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Error while creating file. ", e);
                e.printStackTrace();
            }
        }
        try {
            // BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
                    true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
