package com.paddlesense.paddlepower;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class StrokePoint {
    public float force;
    public long time;

    private static ByteBuffer
            forceBuffer = ByteBuffer.allocate(Integer.SIZE/8),
            timeBuffer = ByteBuffer.allocate(Long.SIZE/8),
            outBuffer = ByteBuffer.allocate(timeBuffer.capacity() + forceBuffer.capacity());

    private static int FB_C = forceBuffer.capacity(),
            TB_C = timeBuffer.capacity();


    public StrokePoint (float force, long time) {
        this.force = force;
        this.time = time;
    }

    public StrokePoint () {
    }

    // TODO: add a method that returns an int (= 4 bytes) delta time rather than epoch time (long = 8 bytes)
    // to save space
    public byte[] toBytes() {
        byte[] out = new byte[outBuffer.capacity()];
        // force * 100 is transformed to an int
        int forceInt = (int)(force * 100);
        timeBuffer.position(0);
        forceBuffer.position(0);
        outBuffer.position(0);

        timeBuffer.putLong(time);
        forceBuffer.putInt(forceInt);
        outBuffer.put(timeBuffer.array());
        outBuffer.put(forceBuffer.array());

        return outBuffer.array();
    }

    public static StrokePoint fromBytes(byte[] bytes) {
        StrokePoint sp = new StrokePoint();

        // Need to keep resetting the buffer position after each operation
        timeBuffer.position(0);
        timeBuffer.put(Arrays.copyOfRange(bytes, 0, TB_C),0, TB_C);
        timeBuffer.position(0);
        sp.time = timeBuffer.getLong();

        // force * 100 is transformed to an int
        forceBuffer.position(0);
        forceBuffer.put(Arrays.copyOfRange(bytes, TB_C, TB_C + FB_C), 0, FB_C);
        forceBuffer.position(0);
        sp.force = (float)(forceBuffer.getInt() * 0.01);

        return sp;
    }
}
