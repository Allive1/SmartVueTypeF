package main.java.com.vsu.smartvuetypef.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.objdetect.CascadeClassifier;

import com.parse.ParseObject;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.SvActivity;
import main.java.com.vsu.smartvuetypef.util.Face;
import main.java.com.vsu.smartvuetypef.util.ZoomControl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ZoomFragment extends FeatureDetection implements
		ZoomControl.OnZoomChangedListener {
	private static final String TAG = "ZoomFragment";
	public static final int JAVA_DETECTOR = 0;
	public static final int NATIVE_DETECTOR = 1;

	private String[] mDetectorName;

	// Cols, Rows
	int maxWidth = 720, maxHeight = 576,  phaseIndex = 0, faceSize, initZoom = -1, expectedLevel, phaseResult, totalPhases = 1;

	static int stage = 0;

	Face calibrationFDD = new Face(), currentFDD = new Face();

	long secBuffer, phaseStart, phaseEnd;
	double ticks;

	boolean faceCheck, found = false, sampleVis = false, setup = false, result, sampleReady = false;

	String incFont = "Please increase text size",
			decFont = "Please decrease text size", currentInstruction,
			currentSample, ackMsg = "Phase Completed";

	Context mContext;
	static TextView mSampleView, mContentTextView, mInstrTextView,
			mContentView;
	
	static ZoomControl zoomStack;
	MatOfRect face, faceBox;
	Mat faceTemplate;
	View showView, hideView, ocvView;
	Animation in, out;
	ValueAnimator instrAnim, fadeInstrOut, fadeSampleIn, fadeSampleOut;
	ArrayList<Face> faceList = new ArrayList<Face>(),
			calibrationFaces = new ArrayList<Face>();

	ParseObject phaseObject, faceObject;

	static AnimatorSet phaseIn = new AnimatorSet(),
			phaseOut = new AnimatorSet();

	private static SvActivity sActivity;
	private Handler mHandler = new Handler();
	CountDownTimer t;
	Message msg = new Message();
	private ProgressBar calibrationProgress;
	private static FrameLayout sContent, mContent, iContent;


	//  Classifier Loader
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
			this.getActivity()) {
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
					mOpenCvCameraView.setCameraIndex(0);
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

	 // Class Constructor
    public static ZoomFragment newInstance(double avgFace) {
        ZoomFragment fragment = new ZoomFragment();
        Bundle args = new Bundle();
        args.putDouble("calibrationFace", avgFace);
        fragment.setArguments(args);

        return fragment;
    }

	public ZoomFragment() {
		// Required empty public constructor
		Log.d(TAG, "Zoom Constructor");
		sActivity = SvActivity.getInstance();
		if(mLoaderCallback==null)
			Log.d(TAG, "Callback Null");
		else
			Log.d(TAG, "Callback Set");

		phaseIn.play(instrAnim);

		mDetectorName = new String[2];
		mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
		mDetectorName[JAVA_DETECTOR] = "Java";

		in = new AlphaAnimation(0.0f, 1.0f);
		in.setDuration(3000);

		out = new AlphaAnimation(1.0f, 0.0f);
		out.setDuration(3000);


		zoomStack = new ZoomControl(this);

		faceCheck = true;
		faceSize = 0;

		// initialize phase to zero
		phaseIndex = 0;
	}

    public int getCalibrationFace() {
        return getArguments().getInt("calibrationFace");
    }

	 // Fragment Method
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mContext = context;
	}

	@Override
	public void onStart() {
		super.onStart();
		// Set instructions and text for this phase
		Log.d(TAG, "Load create phase");
		startSession();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.getActivity().getWindow()
				.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		View view = inflater
				.inflate(R.layout.fd_surface_view, container, false);
		assert view != null;

		// Initialize OpenCV Camera View
		mOpenCvCameraView = (CameraBridgeViewBase) view
				.findViewById(R.id.opencv_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

        // Display Content
        mContent = (FrameLayout) view.findViewById(R.id.ContentFrame);
		// Initialize text view
		mContentTextView = (TextView) view.findViewById(R.id.sample_text_view);

//Setup views
       // mContentView = (TextView) sContent
         //       .findViewById(R.id.sample_text_view);
        // initialize zoom levels
        //zoomStack.defaultZoom();

		// Display OCV
		// OCVFrame = (FrameLayout) view.findViewById(R.id.CalibrationFrame);

		// Instructions View
		//iContent = (FrameLayout) inflater.inflate(R.layout.instruction_content,
		//		container, false);

		// Sample View
		sContent = (FrameLayout) inflater.inflate(R.layout.sample_content,
				container, false);

		//calibrationProgress = (ProgressBar) view
		//		.findViewById(R.id.progressBar1);

		//phaseObject = new ParseObject("TestObject");
		//phaseObject.put("foo", "bar");
        calibrationFDD.saveFace(getCalibrationFace());
        startSession();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
                this.getActivity(), mLoaderCallback);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}


	 // OpenCV Methods
	@Override
	public void onCameraViewStopped() {
		super.onCameraViewStopped();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
		super.onCameraFrame(inputFrame);
		detectKalmanFaces();
		return mRgba;
	}

	@Override
	public void onFaceRecognized() {
		super.onFaceRecognized();
        if(sampleReady) {
            logUserData();
        }
	}

	 // Session Methods
	public void setupSession() {
		Log.d(TAG, "New session setup...");
        calibrationFDD.saveFace(0, 0);
		// run the initialize face method
		//checkInitialPhaseFace();
	}

	public void startSession() {
		// if system has been initialized
		if (sessionCheck()) {
            for(int phase = 0; phase < totalPhases; phase++) {
                Log.d(TAG, "Session Check Passed");
                preInitPhase();
            }
            endSession();
		}
		// if not
		else {
			// Setup
			setupSession();
            startSession();
		}
	}

	public boolean sessionCheck() {
		if (calibrationFDD.getFaceSize() > 0)
			return true;
		else if(calibrationFDD.getFaceSize() == 0) {
            return true;
        }
        else {
            return false;
        }
	}

    public void endSession(){
        // return to SV activity
        getActivity().getSupportFragmentManager().popBackStack();
    }

	/*
	 * Phase Methods
	 */
	public void preInitPhase() {
        Log.d(TAG, "PreInitial Phase: ready");
        initPhase();
	}

	public void initPhase() {
		// Choose text
		chooseInstruction();
		chooseSample();
		Log.d(TAG, "Initial Phase: ready");
		loadPhase();
	}

	public void loadPhase() {
        ValueAnimator fadeIn = ObjectAnimator.ofFloat(mContentView,
                "alpha", 0f, 1f);
        ValueAnimator fadeOut = ObjectAnimator.ofFloat(mContentView,
                "alpha", 1f, 0f);
        fadeIn.setDuration(1500);
        final AnimatorSet instructionSet = new AnimatorSet();
        final AnimatorSet sampleSet = new AnimatorSet();

        instructionSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // load instruction
                loadInstruction();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                sampleSet.start();
                Log.d(TAG, "Sample Phase: ready");
            }
        });

        sampleSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //load sample
                loadSample();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                stage = 2;
                sampleReady = true;
                Log.d(TAG, "Sample Phase: ready");
            }
        });

        instructionSet.play(fadeOut).after(1000).after(fadeIn);
        instructionSet.start();
	}

	public void endPhase() {
		phaseIndex++;
		if (phaseIndex < 1) {
			// Save data
			// savePhaseData();
			// Increment the sample counter

		} else {
			// Save data
			// savePhaseData();
			// return to SV activity
			getActivity().getSupportFragmentManager().popBackStack();
		}
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
				is = getResources().openRawResource(R.raw.text_sample1);
				break;
			case 1:
				is = getResources().openRawResource(R.raw.text_sample2);
				break;
			case 2:
				is = getResources().openRawResource(R.raw.text_sample3);
				break;
			default:
				is = getResources().openRawResource(R.raw.text_sample1);
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

	public void loadSample() {
		// <------- assign entireFile to TextView
		mContentView.setText(currentSample);
	}

	/*
	 * Instruction Method
	 */
	public void chooseInstruction() {
		// see which level it fits into
		int zoom = zoomStack.checkFaceLevel(calibrationFDD.getFaceSize());
		Log.d(TAG, "Calibration Face: " + calibrationFDD.getFaceSize()
				+ " fits in zoom " + zoom);
		zoomStack.setMode(zoom);

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
			endPhase();
			break;
		}
	}

	public void setInstruction(int index) {
		if (index == 0) {
			// Set the intended instruction
			currentInstruction = decFont;
			// Set the expected mode change
			expectedLevel = zoomStack.getCurrentMode() - 1;
		} else if (index == 1) {
			// Set the intended instruction
			currentInstruction = incFont;
			// Set the expected mode change
			expectedLevel = zoomStack.getCurrentMode() + 1;
		} else {
			Log.d(TAG, "Instruction set failed");
		}
		Log.d(TAG, "Current ZoomControl: " + zoomStack.getCurrentMode()
				+ " Expected ZoomControl: " + expectedLevel);

	}

	public void loadInstruction() {
		//load content into view
        mContentView.setText(currentInstruction);
	}

	/*
	 * Fade Methods
	 */
	public void loadInstructionView() {
		// showView = mInstrTextView;
		// hideView = mSampleView;

		// hideView.setVisibility(View.INVISIBLE);

		mInstrTextView.setAlpha(0f);
		// loadInstruction();
		mInstrTextView.setText(currentInstruction);
		mContentView.setText(currentSample);
		instrHandler.sendEmptyMessage(0);
		// showView.setVisibility(View.VISIBLE);
	}

	public void loadSampleView() {
		showView = mSampleView;
		hideView = mInstrTextView;

		hideView.setVisibility(View.INVISIBLE);
		showView.setAlpha(0f);
		showView.setVisibility(View.VISIBLE);

	}

	/*
	 * ZoomControl Management
	 */
	public void checkForZoomChange() {
		Message msg = zoomHandler.obtainMessage();
		Message xmsg = toastHandler.obtainMessage();
		// if the zoom level has changed
		if (initZoom != zoomStack.getCurrentMode()) {
			zoomHandler.sendMessage(msg);
			// Disable OpenCV View
			this.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mOpenCvCameraView.disableView();
					mOpenCvCameraView.setVisibility(View.INVISIBLE);
				}
			});
			// mOpenCvCameraView.setVisibility(View.INVISIBLE);
			// Instruction Check
			result = zoomInstructionCheck(zoomStack.getCurrentMode());
			// Put the result into parse
			// phaseObject.put("result", result);
			// Close out current phase
			toastHandler.sendMessage(xmsg);
			endPhase();
		}
		// else
		else {
			// just log the time
			logUserData();
		}
	}

	public boolean zoomInstructionCheck(int recognizedLevel) {
		boolean result;
		// if currentFDD is greater than X from faceList-0
		if (currentFDD.getFaceSize() > faceList.get(0).getFaceSize() + 10) {
			Message msg = zoomHandler.obtainMessage();
			msg.arg1 = 1;
			zoomHandler.sendMessage(msg);
		}
		// if currentFDD is less than X from faceList-0
		else if (currentFDD.getFaceSize() > faceList.get(0).getFaceSize() - 10) {
			Message msg = zoomHandler.obtainMessage();
			msg.arg1 = 0;
			zoomHandler.sendMessage(msg);
		}

		// check recognized level against the expected level
		if (recognizedLevel == expectedLevel) {
			result = true;
			// Confirm success
			handleState(1);
		}
		// else L register a fail
		else {
			result = false;
			// Display failed message
			handleState(0);
		}

		return result;
	}

	public void onZoomChanged(int position) {
		// If view is available
		if (mContentView.getVisibility() == View.VISIBLE) {
			// Update zoom
			zoomHandler.sendEmptyMessage(0);
			// checkInstruction();
			// stage++;
		} else {
			Log.d(TAG, "Zoom attempt with Sample Text View invisible");
		}
	}

	/*
	 * Data Management
	 */
	public void checkInitialPhaseFace() {
		if (calibrationFDD.getFaceSize() == -1) {
			// getInitialPhaseFace();

			// new FaceFinder().execute(initialFDD);
			// Move to
			// startSession();
		} else {
			//
			mInstrTextView.setText("Success");
			startSession();
		}
	}

	public void getInitialPhaseFace() {
		// show loading message in text view
		mInstrTextView.setText("Initializing ");
		// count down for three seconds
		t = new CountDownTimer(4000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub
				if (calibrationFDD.getFaceSize() == -1) {
					Log.d(TAG, "Checking for face");
					mInstrTextView.append(".");
				} else {
					Log.d(TAG, "ParseFace found");
					mInstrTextView.setText("Found");
					// checkInitialPhaseFace();
					// this.cancel();
				}
			}

			@Override
			public void onFinish() {
				getInitialPhaseFace();
			}
		};
		t.start();
	}

	public void initializeFaceLevel() {
		// if the system has not been initialized
		if (calibrationFDD.getFaceSize() == -1) {
			calibrationFDD = currentFDD;
			try {
				t.cancel();
				// Disable OCV
				// mOpenCvCameraView.disableView();
				mOpenCvCameraView.setVisibility(View.INVISIBLE);
				initPhase();
			} catch (Exception e) {
				Log.d(TAG,
						"CountDownTimer cancellation failed: " + e.getMessage());
			}
		} else {

		}
	}

	public void handleState(int state) {
		Context t = this.getActivity();
		sActivity.handleState(this, state, t);
	}

	private int calcAvgCalibFace() {
		int avg = 0;
		// TODO Auto-generated method stub
		for (int i = 0; i < 10; i++)
			avg += calibrationFaces.get(i).getFaceSize();
		avg = avg / 10;
		return avg;
	}

	public void manageCurrentFace() {
		if (faceList.isEmpty()) {
			faceList.add(currentFDD);
		}
		// if currentFDD is less keep adding to list
		else {
			faceList.add(currentFDD);
			// if the displacement has changed greater than 10
			// if (Math.abs(currentFDD.getFaceSize()
			// - initialFDD.getFaceSize()) > 10){
			// Update current face

			// }
		}
	}

	public void checkInstruction() {
		// if expected zoom was made
		if (zoomStack.getCurrentMode() == expectedLevel) {
			phaseResult = 1;
		}
		// if incorrect zoom was made
		else {
			phaseResult = 0;
		}
	}

	/*
	 * Parse Methods
	 */
	public void logUserData() {
        // get the averaging face size
		double[] width = state.get(4, 0);

		//currentFDD.saveFace((int) width[0], System.currentTimeMillis());
		/*
		 * 0 - session start 1 - phase start 2 - instruction phase 3 - sample
		 * phase
		 */
		switch (stage) {
			case 0: // calibration view
				if (calibrationFDD.getFaceSize() == -1)
					// If the instructions are visible
					if (mContentTextView.getVisibility() == View.VISIBLE)
						// if the initialFace is empty
						if (calibrationFaces.size() < 10) {
							calibrationFaces.add(currentFDD);
							mHandler.post(new Runnable() {
								public void run() {
									calibrationProgress
											.setProgress(calibrationFaces.size() * 10);
								}
							});
						} else if (calibrationFaces.size() == 10) {
							// Average calibration faces
							calibrationFDD.saveFace(calcAvgCalibFace(),
									System.currentTimeMillis());
							startSession();
						}
				break;
			case 1:
				// instruction view is visible
				break;
			case 2:
				if (mContentView.getVisibility() == View.VISIBLE) {
					// Adjust Zoom Level
					zoomStack.runZoomLevelCorrect(currentFDD.getFaceSize());
					// if faceList has not been initialized
					manageCurrentFace();
				}
				break;
			case 3:
				new CountDownTimer(2000, 1000) {
					public void onTick(long millisUntilFinished) {

					}

					public void onFinish() {
						endPhase();
					}
				}.start();
				endPhase();
				break;
			default:
				break;
		}
	}

	public void savePhaseData() {
		phaseObject.put("phaseStart", phaseStart);
		phaseObject.put("phaseEnd", phaseEnd);
		phaseObject.saveInBackground();
	}

	/*
	 * Handlers
	 */
	private static Handler zoomHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			// Set Mode level
			mContentView.setTextSize(zoomStack.getCurrentModeTextSize());
		}
	};

	private static Handler toastHandler = new Handler(Looper.myLooper()) {
		public void handleMessage(Message msg) {
			// Set Mode level
			// zoomStack.setMode(msg.arg1);
			// endPhase();
		}
	};

	private static Handler caliHandler = new Handler(Looper.myLooper()) {
		public void handleMessage(Message msg) {
			// Set Mode level
			if (msg.arg1 == 0)
				// mOpenCvCameraView.disableView();
				// mContent.removeView(mOpenCvCameraView);

				// mOpenCvCameraView.setVisibility(View.INVISIBLE);
				mContent.removeAllViews();
			// Log.d(TAG, "g" + mContent.getChildCount());
			mContent.addView(iContent);
			mContent.addView(sContent);
			mContentView.setAlpha(0f);
		}
	};

	private static Handler instrHandler = new Handler(Looper.myLooper()) {
		public void handleMessage(Message msg) {
			ValueAnimator fadeIn = ObjectAnimator.ofFloat(mInstrTextView,
					"alpha", 0f, 1f);
			fadeIn.setDuration(3000);
			ValueAnimator fadeOut = ObjectAnimator.ofFloat(mInstrTextView,
					"alpha", 1f, 0f);
			fadeOut.setDuration(1500);

			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.play(fadeOut).after(3000).after(fadeIn);
			animatorSet.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					Log.d(TAG, "Instructions Phase: completed");
					sampHandler.sendEmptyMessage(0);
				}
			});
			animatorSet.start();
		}
	};

	private static Handler sampHandler = new Handler() {
		public void handleMessage(Message msg) {
			ValueAnimator fadeIn = ObjectAnimator.ofFloat(mContentView,
					"alpha", 0f, 1f);
			fadeIn.setDuration(1500);
			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					stage = 2;
					Log.d(TAG, "Sample Phase: ready");
				}
			});
			animatorSet.play(fadeIn).after(1000);
			animatorSet.start();
		}
	};


}
