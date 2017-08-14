package main.java.com.vsu.smartvuetypef;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import main.java.com.vsu.smartvuetypef.features.FlipFragment;
import main.java.com.vsu.smartvuetypef.features.ZoomFragment;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.model.MyKalmanFilter;
import main.java.com.vsu.smartvuetypef.view.InstructionsFragment;

public class SvActivity extends FeatureDetection implements InstructionsFragment.OnInstructionCompletedListener{
	private static final String TAG = "SvActivity";

	FragmentTransaction transaction;
	FragmentManager manager;
	int phase = 1, faceAvg = 0, faceSum = 0;
    double mCalibrationFace = 0;
	Fragment instruct;
	Fragment fragment;
	private static SvActivity sInstance = null;
    int f = 0;
	OnFaceRecognizedListener mFeatureCallback;
	ArrayList <Integer> faceQueue = new ArrayList<>();
	int faceIndex = 0, calibrate_face = 0;

	// A static block that sets class fields
	static {
		// Creates a single static instance of PhotoManager
		sInstance = new SvActivity();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        this.getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		faceQueue.ensureCapacity(5);
		mLoaderCallback = new BaseLoaderCallback(
				this) {
			@Override
			public void onManagerConnected(int status) {
				System.out.println("OpenCV Loaded: " + status);
				switch (status) {
					case LoaderCallbackInterface.SUCCESS: {
						Log.i(TAG, "OpenCV loaded successfully");

						try {
							File mCascadeFile;
							InputStream is = getResources().openRawResource(
									R.raw.lbpcascade_frontalface);

							File cascadeDir = getDir("cascade",
									Context.MODE_PRIVATE);
							mCascadeFile = new File(cascadeDir,
									"lbpcascade_frontalface.xml");
							FileOutputStream os = new FileOutputStream(mCascadeFile);

							byte[] buffer = new byte[4096];
							int bytesRead;
							while ((bytesRead = is.read(buffer)) != -1) {
								os.write(buffer, 0, bytesRead);
							}

							cascadeProfileFace = new CascadeClassifier(
									mCascadeFile.getAbsolutePath());
							if (cascadeProfileFace.empty()) {
								Log.e(TAG,
										"Failed to load frontal face cascade classifier");
								cascadeProfileFace = null;
							} else {
								Log.i(TAG, "Loaded cascade classifier from "
										+ mCascadeFile.getAbsolutePath());
							}
							is.close();
							os.close();
							System.out.println("OpenCV Loaded");
							cascadeDir.delete();
							mRgba = new Mat(576, 720, 24);
							mGray = new Mat(576, 720, 24);
						} catch (IOException e) {
							e.printStackTrace();
							Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
						}
						// mOpenCvCameraView.enableFpsMeter();
						mOpenCvCameraView.setCameraIndex(1);
						mOpenCvCameraView.enableView();
					}
					break;
					default: {
						super.onManagerConnected(status);
					}
					break;
				}
			}
		};

		// Initialize OpenCV Camera View
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

		if (savedInstanceState == null) {
			instruct = new InstructionsFragment();
			/*Bundle args = new Bundle();

			args.put("FeatureDetector", face);
			args.putString(ARG_PARAM2, param2);
			fragment.setArguments(args);*/
			//fragment = new ZoomFragment();
			manager = getSupportFragmentManager();
			transaction = manager.beginTransaction();
			transaction.add(R.id.content_fragment, instruct);
            //transaction.add(R.id.content_fragment, zoomFragment);
			transaction.addToBackStack("instruction");
			transaction.commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sv, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		/*
		 * int id = item.getItemId(); if (id == R.id.action_settings) { return
		 * true; }
		 */

		switch (item.getItemId()) {
		case android.R.id.home:
			// Navigate "up" the demo structure to the launchpad activity.
			// See http://developer.android.com/design/patterns/navigation.html
			// for more.
			// NavUtils.navigateUpTo(this, new Intent(this,
			// MainActivity.class));

			return true;

		case R.id.action_previous:
			// Go to the previous step in the wizard. If there is no previous
			// step,
			// setCurrentItem will do nothing.
			manager.popBackStack();
			return true;

		case R.id.action_next:
			// Advance to the next step in the wizard. If there is no next step,
			// setCurrentItem
			// will do nothing.
			manager.popBackStack();
			phase++;
			return true;
		case R.id.action_settings:
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble("caliFace", mCalibrationFace);
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
				this, mLoaderCallback);
	}



	public static SvActivity getInstance() {
		return sInstance;
	}

    public void goNextFeature(){
		f = 0;
        switch (f) {
            case 0:
                Log.d(TAG, "ZoomControl Case");
				try {
					//if no faces
					if(faceIndex<0) {
						for (int i = 0; i <= faceIndex; i++) {
							calibrate_face += faceQueue.get(i);
						}
						calibrate_face = calibrate_face / (faceIndex + 1);
					}
					//take average
					else {
						calibrate_face = faceAvg;
					}
					fragment = new ZoomFragment();
					Bundle args = new Bundle();
					args.putInt("calibration_face", calibrate_face);
					fragment.setArguments(args);

                	transaction = manager.beginTransaction();
                	transaction.replace(R.id.content_fragment, fragment);
                	transaction.addToBackStack("ZoomControl");
                	// Commit the transaction
                	transaction.commit();
					mFeatureCallback = (OnFaceRecognizedListener) fragment;
				} catch (ClassCastException e) {
					throw new ClassCastException(fragment.getContext().toString()
							+ " must implement OnHeadlineSelectedListener");
				}
                break;
            case 1:
                Log.d(TAG, "Flip Case");
                fragment = new FlipFragment();
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_fragment, fragment);
                transaction.addToBackStack("Flip");
                // Commit the transaction
                transaction.commit();
                break;
            default:
                Log.d(TAG, "Default Case");
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(instruct);
                transaction.commit();
                break;
        }
    }

	@Override
	public void onFaceRecognized(){
		super.onFaceRecognized();
		if(faceQueue.size()==5) {
			faceSum = faceSum - faceQueue.get(faceIndex) + foundFace;
			faceAvg = faceSum / 5;
			faceQueue.set(faceIndex, foundFace);
		}
		else {
            faceQueue.add(foundFace);
			faceSum = faceSum + foundFace;
			faceAvg = faceSum / faceQueue.size();
		}

		if(faceIndex==4)
			faceIndex=0;
		else
			faceIndex++;

		//if feature is set
		if(mFeatureCallback!=null)
			//if feature is checking
		//if(mFeatureCallback.check==true)
			mFeatureCallback.checkForZoomChange(faceAvg);
	}

	public interface OnFaceRecognizedListener {
		void checkForZoomChange(int face);
	}
}
