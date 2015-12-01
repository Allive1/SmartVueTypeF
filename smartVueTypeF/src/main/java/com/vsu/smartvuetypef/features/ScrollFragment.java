package main.java.com.vsu.smartvuetypef.features;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;
import org.opencv.objdetect.CascadeClassifier;

import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.R.id;
import main.java.com.vsu.smartvuetypef.R.layout;
import main.java.com.vsu.smartvuetypef.R.raw;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;

public class ScrollFragment extends FeatureDetection {
	private static final String TAG = "ScrollFrag";
	
	private static Handler flipHandler = new Handler() {
		public void handleMessage(Message msg) {
			
		}
	};

	public static ScrollFragment newInstance() {
		ScrollFragment fragment = new ScrollFragment();
		return fragment;
	}

	public static ScrollFragment create(int pageNumber) {
		ScrollFragment fragment = new ScrollFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	private Context mContext;


	private AlphaAnimation in;

	private AlphaAnimation out;

	public ScrollFragment() {
		// Required empty public constructor
		/* now we can start update thread */

		in = new AlphaAnimation(0.0f, 1.0f);
		in.setDuration(3000);

		out = new AlphaAnimation(1.0f, 0.0f);
		out.setDuration(3000);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mContext = activity.getBaseContext();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.getActivity().getWindow()
				.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		View view = inflater
				.inflate(R.layout.fd_screen_slide, container, false);

		// assert view != null;

		// Initialize OpenCV Camera View
		mOpenCvCameraView = (CameraBridgeViewBase) view
				.findViewById(R.id.fd_activity_surface_view);

		mOpenCvCameraView.setCvCameraViewListener(this);

		return view;
	}

	

	@Override
	public void onFaceRecognized() {
		Message msg = flipHandler.obtainMessage();
		Log.d(TAG, "Override Format");

			flipHandler.sendMessage(msg);

	}

	@Override
	public void formatMat() {
		// Log.d(TAG, "Override Format");
		//msg = flipHandler.obtainMessage();
		transposeMat();
		detectKalmanEyes();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
				this.getActivity(), mLoaderCallback);
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
			this.getActivity()) {
		private CascadeClassifier cascadeFace;
		private CascadeClassifier cascadeEyeGlasses;

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				try {
					// load cascade file from application resources
					// -------------------Frontal ParseFace-----------------------
					// CascadeClassifier cascadeFace;
					File mCascadeFile;
					InputStream is = getResources().openRawResource(
							R.raw.lbpcascade_frontalface);

					File cascadeDir = mContext.getDir("cascade",
							Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}

					cascadeFace = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (cascadeFace.empty()) {
						Log.e(TAG,
								"Failed to load frontal face cascade classifier");
						cascadeFace = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());
					}
					buffer = null;
					// -------------------Eyes-----------------------
					// CascadeClassifier cascadeFace;
					// File mCascadeFile;
					InputStream egis = getResources().openRawResource(
							R.raw.haarcascade_eye_tree_eyeglasses);

					File cascadeDirEG = mContext.getDir("cascade",
							Context.MODE_PRIVATE);
					File mCascadeFileEG = new File(cascadeDirEG,
							"lbpcascade_frontalface.xml");
					FileOutputStream egos = new FileOutputStream(mCascadeFileEG);

					buffer = new byte[4096];
					while ((bytesRead = egis.read(buffer)) != -1) {
						egos.write(buffer, 0, bytesRead);
					}

					cascadeEyeGlasses = new CascadeClassifier(
							mCascadeFileEG.getAbsolutePath());
					if (cascadeEyeGlasses.empty()) {
						Log.e(TAG,
								"Failed to load eye glasses cascade classifier");
						cascadeEyeGlasses = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFileEG.getAbsolutePath());
					}

					buffer = null;
					is.close();
					os.close();
					egis.close();
					egos.close();

					cascadeDir.delete();
					cascadeDirEG.delete();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				enableCameraView(0);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
}
