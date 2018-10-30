package com.paddlesense.paddlepower;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;
import java.util.*;

public class Analyzer {

    private boolean inReturn = false;
    private String TAG = "Analyzer";

    private ArrayList<StrokePoint> strokePoints = new ArrayList<>();
    private ArrayList<StrokePoint> returnPoints = new ArrayList<>();
    private Context mContext;

    public final static String STROKE_POINTS_AVAILABLE = "com.paddlesense.paddlepower.STROKE_POINTS_AVAILABLE";

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
}
