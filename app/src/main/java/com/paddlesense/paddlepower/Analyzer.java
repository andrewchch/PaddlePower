package com.paddlesense.paddlepower;

import java.util.Date;
import java.util.*;
import org.junit.Test;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class Analyzer {

    private static float FORCE_THRESHOLD = 0.1f;
    private static int RETURN_POINTS_THRESHOLD = 5;
    private boolean inReturn = false;

    private ArrayList<StrokePoint> strokePoints = new ArrayList<StrokePoint>();
    private ArrayList<StrokePoint> returnPoints = new ArrayList<StrokePoint>();

    StrokePoint addReading(StrokePoint sp) {
        strokePoints.add(sp);
        return sp;
    }

    public StrokePoint addReading(float reading, long time) {
        StrokePoint strokePoint = null;

        if (time == 0) {
            time = (new Date()).getTime();
        }

        // We're either stroking or returning to the start of the stroke
        if (reading > FORCE_THRESHOLD) {
            // Stroking
            inReturn = false;
            if (returnPoints.size() > 0) {
                returnPoints.clear();
            }
            strokePoint = new StrokePoint();
            strokePoint.force = reading;
            strokePoint.time = time;
            strokePoints.add(strokePoint);
        }
        else {
            // Returning
            if (!inReturn) {
                strokePoint = new StrokePoint();
                strokePoint.force = reading;
                strokePoint.time = time;
                returnPoints.add(strokePoint);

                if (returnPoints.size() >= RETURN_POINTS_THRESHOLD) {
                    inReturn = true;
                }
            }
        }

        return strokePoint;
    }

    public void clearReadings () {
        strokePoints.clear();
    }

    public List<StrokePoint> getReadings() {
        return strokePoints;
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
