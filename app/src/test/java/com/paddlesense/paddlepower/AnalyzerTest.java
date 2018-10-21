package com.paddlesense.paddlepower;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalyzerTest {
    @Test
    public void analyzer_addReading() {
        float force = 1.2f;
        long time = 123;
        Analyzer analyzer = new Analyzer();
        StrokePoint strokePoint = analyzer.addReading(force, time);
        assertEquals(strokePoint.force, force, 0.01);
        assertEquals(strokePoint.time, time);
    }

    @Before
    public void setUp() throws Exception {
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
    }
}
