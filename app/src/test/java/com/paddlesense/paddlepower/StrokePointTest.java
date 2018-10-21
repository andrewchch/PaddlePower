package com.paddlesense.paddlepower;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StrokePointTest {
    @Test
    public void strokePoint_SerializationToBytes_matches() {
        testToFromBytes(123, 1.2f);
        testToFromBytes(123, 0.0f);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private void testToFromBytes (long time, float force) {
        StrokePoint sp = new StrokePoint(), sp_in;
        sp.force = force;
        sp.time = time;

        byte[] out = sp.toBytes();
        sp_in = StrokePoint.fromBytes(out);

        assertEquals(sp.force, sp_in.force, 0.01);
        assertEquals(sp.time, sp_in.time);
    }
}
