package com.vsu.smartvuetypef.util;

import android.os.AsyncTask;
import android.util.Log;



public class FaceFinder extends AsyncTask<Face, Integer, Integer> {
	protected Integer doInBackground(Face... faces) {
		int faceSize = 0;
		while (faces[0].faceSize == -1) {
			// Wait for face to be found
			Log.d("FFinder", "Looking for face");
			// Escape early if cancel() is called
			if (isCancelled())
				break;
		}
		return faceSize;
	}

	// protected void onProgressUpdate(Integer... progress) {
	// setProgressPercent(progress[0]);
	// }

	protected void onPostExecute(int result) {
		//setPost(result + " bytes");
	}
}
