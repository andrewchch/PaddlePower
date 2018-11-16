package com.paddlesense.paddlepower;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class DataLogger {
    // Open the log file
    private final static String TAG = DataLogger.class
            .getSimpleName();

    private OutputStream output = null;

    public void openLog() {
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

    public void closeLog () {
        // Close the log file
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Deprecated
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
}
