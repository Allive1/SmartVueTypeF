package main.java.com.vsu.smartvuetypef.util;

import android.util.Log;
import android.util.SparseArray;

public class ZoomControl {
	protected int maxModes = 0, currentFaceSize = 0;
	protected SparseArray<Mode> zoomModes = new SparseArray<Mode>();
	protected int currentMode;
	// private int faceAvg;
	public boolean modeChange = false;
	private String TAG = "ZoomControl";
	OnZoomChangedListener mCallback;

	// Defines a field that contains the calling object of type PhotoTask.
	// final TaskRunnableZoomMethods mFaceFragment;
	// A static block that sets class fields
	

	public ZoomControl(android.support.v4.app.Fragment fragment) {
		try {
			mCallback = (OnZoomChangedListener) fragment;
		} catch (ClassCastException e) {
			throw new ClassCastException(fragment.toString()
					+ " must implement OnHeadlineSelectedListener");
		}
	}

	public void defaultZoom() {
		clearModes();
		addMode(290, 576, 7);
		addMode(160, 289, 14);
		addMode(0, 159, 28);
	}

	public void addMode(int min, int max, int size) {
		zoomModes.put(maxModes, new Mode(min, max, size));
		maxModes++;
	}

	public void setMode(int index) {
		currentMode = index;
	}

	public int getCurrentMode() {
		return currentMode;
	}

	public void clearModes() {
		zoomModes.clear();
	}

	public int getCurrentModeTextSize() {
		return zoomModes.get(currentMode).textSize;
	}

	public boolean checkCurrentZoomLevel() {
		// Measure size against current mode
		if (currentFaceSize < zoomModes.get(currentMode).max_face
				&& currentFaceSize > zoomModes.get(currentMode).min_face) {
			// Leave mode as is
			return true;
		} else {
			return false;
		}
	}

	public void runZoomLevelCorrect(int newFace) {
		updateCurrentFace(newFace);
		if (checkCurrentZoomLevel()) {

		} else {
			// Measure size against mode list
			for (int i = 0; i < maxModes; i++) {
				if (currentFaceSize <= zoomModes.get(i).max_face)
					if (currentFaceSize >= zoomModes.get(i).min_face) {
						// Set mode to Level
						currentMode = i;
						modeChange = true;
						Log.d(TAG, "ZoomControl Level Change to " + (i + 1));
						// Notify the parent activity of selected item
						mCallback.onZoomChanged(currentMode);
						break;
					}
			}
		}
	}

	public int checkFaceLevel(int faceDimension) {
		for (int i = 0; i < maxModes; i++) {
			if (faceDimension <= zoomModes.get(i).max_face
					&& faceDimension >= zoomModes.get(i).min_face) {
				return i;

			}
		}
		return -1;
	}

	private class Mode {
		int min_face, max_face, textSize;

		Mode(int min, int max, int text) {
			min_face = min;
			max_face = max;
			textSize = text;
		}
	}

	public void updateCurrentFace(int i) {
		// TODO Auto-generated method stub
		currentFaceSize = i;
	}

	public interface OnZoomChangedListener {
		// TODO: Update argument type and name
		public void onZoomChanged(int index);
	}
}
