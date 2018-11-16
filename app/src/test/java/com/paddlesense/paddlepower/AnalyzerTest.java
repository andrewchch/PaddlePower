package com.paddlesense.paddlepower;

import android.content.Context;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AnalyzerTest {
    private Context context;
    private Analyzer analyzer;

    @Test
    public void analyzer_addReading() {
        float force = 1.2f;
        long time = 123;
        StrokePoint strokePoint = analyzer.addReading(force, time);
        assertEquals(strokePoint.force, force, 0.01);
        assertEquals(strokePoint.time, time);
    }

    @Test
    public void analyzer_addStrokeAndReturnPeriods() {
        float force = 1.2f;
        long now = new Date().getTime();

        // Add some return points
        for (int i=0; i<8; i++) {
            analyzer.addReading(0, now + i * 20);
        }

        // Add some stroke points
        for (int i=0; i<5; i++) {
            analyzer.addReading(1.0f, now + i * 20);
        }

        // Add some return points
        for (int i=0; i<8; i++) {
            analyzer.addReading(0, now + i * 20);
        }

        // Add some stroke points
        for (int i=0; i<5; i++) {
            analyzer.addReading(1.0f, now + i * 20);
        }

        // Add some return points
        for (int i=0; i<8; i++) {
            analyzer.addReading(0, now + i * 20);
        }

        // We should see that broadcastPointsAvailable has been called twice.
        verify(context, times(2)).sendBroadcast((Intent)any());
    }

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        DataLogger logger = mock(DataLogger.class);
        analyzer = new Analyzer(context, logger);
        analyzer.clearReadings();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void addReading() {
    }

    @Test
    public void clearReadings() {
    }

    @Test
    public void getReadings() {
    }

    @Test
    public void getStrokePower() {
        long now = new Date().getTime();
        analyzer.addReading(1.2f, now);
        analyzer.addReading(1.2f, now + 100);
        analyzer.addReading(1.2f, now + 100);
    }
}
