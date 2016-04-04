package main.java.com.vsu.smartvuetypef.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import main.java.com.vsu.smartvuetypef.R;

/**
 * Created by Omari on 3/27/2016.
 */
public class Session {
    private static final String TAG = "Session";
    Context context;
    String currentSample;
    public Session(Context c){
        context = c;
    }
    public void start(){

    }

    /*
   * Sample Methods
   */
    public void chooseSample() {
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



    /*
     * Instruction Method
     */
    public void chooseInstruction() {
        int zoom;

        if (calibrationFace < 100) {
            zoom = 0;
            zoomController.setMode(0);
        } else {
            // see which level it fits into
            zoom = zoomController.checkFaceLevel(calibrationFace);
            zoomController.setMode(zoom);
        }
        Log.d(TAG, "Calibration Face: " + zoom);

        // if face is in mid-level
        switch (zoom) {
            case 0:
                setInstruction(1);
                break;
            case 1:
                int random = (int) (Math.random() * 2 + 1);
                if (random == 0)
                    setInstruction(0);
                else
                    setInstruction(1);
                break;
            case 2:
                setInstruction(0);
                break;
            default:
                Log.d(TAG, "Instruction selection failed");
                phaseIndex++;
                //endPhase();
                break;
        }
    }

    public void setInstruction(int index) {
        if (index == 0) {
            // Set the intended instruction
            currentInstruction = decFont;
            // Set the expected mode change
            expectedMode = zoomController.getCurrentMode() - 1;
        } else if (index == 1) {
            // Set the intended instruction
            currentInstruction = incFont;
            // Set the expected mode change
            expectedMode = zoomController.getCurrentMode() + 1;
        } else {
            Log.d(TAG, "Instruction set failed");
        }
        Log.d(TAG, "Current ZoomControl: " + zoomController.getCurrentMode()
                + " Expected ZoomControl: " + expectedMode);

    }

    public String getSample(){
        return currentSample;
    }

}
