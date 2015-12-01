package main.java.com.vsu.smartvuetypef;

import java.util.HashMap;

import org.opencv.core.Point;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Zoom {
	protected int mode;
	protected int maxModes = 0;
	protected HashMap <Integer, Mode> zoomModes = new HashMap<Integer, Mode>();
	protected Mode currentMode;
	private int faceAvg;
	public boolean modeChange = false;
	private String TAG = "Zoom";
	// Defines a field that contains the calling object of type PhotoTask.
    //final TaskRunnableZoomMethods mFaceFragment;	
	
	
	 Zoom(){
		 
	 }
	
	
	protected void addMode(int min, int max, int size){
		zoomModes.put(maxModes, new Mode(min, max, size));
		maxModes++;
	}
	
	protected void setMode(int index){
		currentMode = zoomModes.get(index);
	}
	
	protected int getCurrentModeTextSize(){
		return currentMode.textSize;
	}
	
	public boolean runZoom(int faceSize){
		faceAvg = faceSize;
		//Log.d(TAG, "Zoom Size: " + faceAvg);
    	//Measure size against current mode		
    	if(faceAvg < currentMode.max_face && faceAvg > currentMode.min_face){
    		//Leave mode as is			
    		//Log.d(TAG, "Face: " + faceAvg + " Bounds: " + currentMode.max_face + " - " + currentMode.min_face);	
    		return false;
    	}		
    	//Measure size against mode list
    	else{
    		for(int i = 0; i < maxModes; i++){
    			//Log.d(TAG, "Zoom Level Check: " + zoomModes.get(i).max_face + ", " + zoomModes.get(i).min_face);	
    			if(faceAvg < zoomModes.get(i).max_face && faceAvg > zoomModes.get(i).min_face){
    				//Set mode to Level 		
        			currentMode = zoomModes.get(i);
        			modeChange = true;
        			Log.d(TAG, "Zoom Level Change to " + (i+1));
        			break;
    			}else{
        			//Incompatible
        			Log.d(TAG, "Incompatible Face");	
        		}  				
    		}  
    		return true;
    	}
	}	
	
	private class Mode{
		int min_face, max_face, textSize;
		Mode(int min, int max, int text){
			min_face = min;
			max_face = max;
			textSize = text;
		}
	}
}
