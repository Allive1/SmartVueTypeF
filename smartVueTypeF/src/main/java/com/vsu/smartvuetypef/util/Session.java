package com.vsu.smartvuetypef.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import main.java.com.vsu.smartvuetypef.R;
import com.vsu.smartvuetypef.features.ZoomFragment;

public class Session {
    private static final String TAG = "Session";
    private Context context;
    private String currentSample;

    private int calibrationFace;
    private int phaseIndex = 0;

    private int expectedMode;
    private String incFont = "Please increase text size",
            decFont = "Please decrease text size", currentInstruction, ackMsg = "Phase Completed";

    ZoomFragment zoomFragment;

    public Session(Context c){
        context = c;
    }

    public void start(){
        //Setup parameters
            chooseInstruction();
            chooseSample();
    }

    public void setCalibrationFace(int c){
        calibrationFace = c;
    }

    public int getCalibrationFace(){
        return calibrationFace;
    }
    /*
   * Sample Methods
   */
    private void chooseSample() {
        Random random = new Random();
        long range = 3 - 1 + 1;
        // compute a fraction of the range, 0 <= frac < range
        long fraction = (long) (range * random.nextDouble());
        int sample = (int) (fraction + 1);

        String line, entireFile = "";
        InputStream is;
        BufferedReader br;
        try {
            switch (sample) {
                case 0:
                    is = context.getResources().openRawResource(R.raw.text_sample1);
                    break;
                case 1:
                    is = context.getResources().openRawResource(R.raw.text_sample2);
                    break;
                case 2:
                    is = context.getResources().openRawResource(R.raw.text_sample3);
                    break;
                default:
                    is = context.getResources().openRawResource(R.raw.text_sample1);
                    break;
            }
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) { // <--------- place
                // readLine() inside
                // loop
                entireFile += (line + "\n"); // <---------- add each line to
                // entireFile
            }
            currentSample = entireFile;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getSample(){
        return currentSample;
    }

    /*
     * Instruction Method
     */
    private void chooseInstruction() {
        int zoom;

        if (calibrationFace < 100) {
            zoom = 0;
            ZoomFragment.zoomController.setMode(0);
        } else {
            // see which level it fits into
            zoom = ZoomFragment.zoomController.checkFaceLevel(calibrationFace);
            ZoomFragment.zoomController.setMode(zoom);
        }
        Log.d(TAG, "Calibration Face: " + zoom);

        // if face is in mid-level
        switch (zoom) {
            case 0:
                // Set the intended instruction
                currentInstruction = incFont;
                setInstruction(1);
                break;
            case 1:
                int random = (int) (Math.random() * 2 + 1);
                if (random == 0)
                    // Set the intended instruction
                    currentInstruction = decFont;
                else
                // Set the intended instruction
                currentInstruction = incFont;
                break;
            case 2:
                currentInstruction = decFont;
                break;
            default:
                Log.d(TAG, "Instruction selection failed");
                phaseIndex++;
                //endPhase();
                break;
        }
    }

    public String getInstruction() {
//        if (index == 0) {
//            // Set the intended instruction
//            currentInstruction = decFont;
//            // Set the expected mode change
//            expectedMode = ZoomFragment.zoomController.getCurrentMode() - 1;
//        } else if (index == 1) {
//            // Set the intended instruction
//            currentInstruction = incFont;
//            // Set the expected mode change
//            expectedMode = ZoomFragment.zoomController.getCurrentMode() + 1;
//        } else {
//            Log.d(TAG, "Instruction set failed");
//        }
//        Log.d(TAG, "Current ZoomControl: " + ZoomFragment.zoomController.getCurrentMode()
//                + " Expected ZoomControl: " + expectedMode);
        return currentInstruction;
    }

    public void setInstruction(int index) {
        if (index == 0) {
            // Set the expected mode change
            expectedMode = ZoomFragment.zoomController.getCurrentMode() - 1;
        } else if (index == 1) {

            // Set the expected mode change
            expectedMode = ZoomFragment.zoomController.getCurrentMode() + 1;
        } else {
            Log.d(TAG, "Instruction set failed");
        }
        Log.d(TAG, "Current ZoomControl: " + ZoomFragment.zoomController.getCurrentMode()
                + " Expected ZoomControl: " + expectedMode);

    }

   public int getExpectedMode(){
       return expectedMode;
   }
}
