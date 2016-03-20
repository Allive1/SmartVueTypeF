package main.java.com.vsu.smartvuetypef.features;

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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.SvActivity;
import main.java.com.vsu.smartvuetypef.util.Face;
import main.java.com.vsu.smartvuetypef.util.ZoomControl;

public class ZoomFragment extends Fragment implements ZoomControl.OnZoomChangedListener, SvActivity.OnFaceRecognizedListener {
    private static final String TAG = "ZoomFragment";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private String[] mDetectorName;

    int maxWidth = 720, maxHeight = 576, phaseIndex = 0, faceSize, initZoom = -1, expectedMode, phaseResult, totalPhases = 1;

    static int stage = 0;
    Face lkgFace = new Face();

    boolean faceCheck, sampleReady = false;

    String incFont = "Please increase text size",
            decFont = "Please decrease text size", currentInstruction,
            currentSample, ackMsg = "Phase Completed";

    public static Context mContext;
    static TextView mSampleView, contentTextView, mInstrTextView,
            mContentView;

    static ZoomControl zoomController;
    View showView, hideView;
    Animation in, out;
    ValueAnimator instrAnim;
    ArrayList<Face> faceList = new ArrayList<Face>();
    static boolean sampleSwitch = false;

    static AnimatorSet phaseIn = new AnimatorSet();
    CountDownTimer t;

    private static int calibrationFace;
    ToastHandler myToast = new ToastHandler();

    public ZoomFragment() {
        // Required empty public constructor
        Log.d(TAG, "Zoom Constructor");

        phaseIn.play(instrAnim);

        mDetectorName = new String[2];
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        mDetectorName[JAVA_DETECTOR] = "Java";

        in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(3000);

        out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(3000);

        zoomController = new ZoomControl(this);
        zoomController.defaultZoom();

        faceCheck = true;
        faceSize = 0;

        // initialize phase to zero
        phaseIndex = 0;
    }

    // Fragment Method
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "Attached");
        mContext = context;
        Bundle b = getArguments();
        int c = b.getInt("calibration_face");

        if (c > 0) {
            calibrationFace = c;
            zoomController.runZoomLevelCorrect(calibrationFace);
        }else{
            calibrationFace = 170;
            zoomController.setMode(1);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set instructions and text for this phase
        Log.d(TAG, "Load create phase");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.getActivity().getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View view = inflater
                .inflate(R.layout.feature_view, container, false);
        assert view != null;

        // Initialize text view
        contentTextView = (TextView) view.findViewById(R.id.sample_text_view);

        startSession();
        return view;
    }

    // Session Methods
    public void setupSession() {
        Log.d(TAG, "New session setup...");
        //calibrationFDD.saveFace(0, 0);
        // run the initialize face method
        //checkInitialPhaseFace();
    }

    public void startSession() {
        // if system has been initialized

        //if (sessionCheck()) {
        for (int phase = 0; phase < totalPhases; phase++) {
            Log.d(TAG, "Session Check Passed");
            preInitPhase();
        }
        //endSession();
        //}
		/* if not
		else {
			calibrationFDD.saveFace(1);
			// Setup
			setupSession();
            startSession();
		}*/
    }

    public void endSession() {
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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
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
                        sampleSwitch = true;
                        Log.d(TAG, "Sample Phase: ready");
                    }
                });

                instructionSet.play(fadeOut).after(1000).after(fadeIn);
                instructionSet.start();
            }
        });
    }

    public void endPhase() {
        sampleReady = false;
        //phaseIndex++;
        if (phaseIndex < 1) {
            // Save data
            // savePhaseData();
            // Increment the sample counter

        } else {
            // Save data
            // savePhaseData();
            // return to SV activity
            //getActivity().getSupportFragmentManager().popBackStack();
            //calibrationFace.saveFace(foundFace);
            //preInitPhase();
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
        contentTextView.setText(currentSample);
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

    public void loadInstruction() {
        //load content into view
        contentTextView.setText(currentInstruction);
    }

    /*
     * Fade Methods
     */
    public void setCalibrationFace(int initialFace) {
        calibrationFace = initialFace;
    }

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
    public void checkForZoomChange(int face) {
        if (calibrationFace > 0) {
            Message msg = zoomHandler.obtainMessage();
            Message xmsg = myToast.obtainMessage();

            String result;
            zoomController.runZoomLevelCorrect(face);
            Log.d(TAG, "Expected: " + expectedMode + " - Zoom: " + zoomController.getCurrentMode());

            //endPhase();
        }
        //calibrationFace = 0;
//		else{
//			setCalibrationFace(face);
//		}
    }

    public boolean zoomInstructionCheck(int recognizedLevel) {
        boolean result;
        // if currentFDD is greater than X from faceList-0
        if (lkgFace.getFaceSize() > faceList.get(0).getFaceSize() + 10) {
            Message msg = zoomHandler.obtainMessage();
            msg.arg1 = 1;
            zoomHandler.sendMessage(msg);
        }
        // if currentFDD is less than X from faceList-0
        else if (lkgFace.getFaceSize() > faceList.get(0).getFaceSize() - 10) {
            Message msg = zoomHandler.obtainMessage();
            msg.arg1 = 0;
            zoomHandler.sendMessage(msg);
        }

        // check recognized level against the expected level
        if (recognizedLevel == expectedMode) {
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
        if (contentTextView.getVisibility() == View.VISIBLE) {
            // Update zoom
            if(sampleSwitch==true ) {
                zoomHandler.sendEmptyMessage(0);
                sampleSwitch=false;
                // if the zoom level has changed to the correct mode
                if (expectedMode == zoomController.getCurrentMode()) {
                    myToast.sendEmptyMessage(1);
                }
                // else
                else {
                    myToast.sendEmptyMessage(0);
                }
            }
        } else {
            Log.d(TAG, "Zoom attempt with Sample Text View invisible");
        }
    }

    /*
     * Data Management
     */
    public void checkInitialPhaseFace() {
		/*if (calibrationFDD.getFaceSize() == -1) {
			// getInitialPhaseFace();

			// new FaceFinder().execute(initialFDD);
			// Move to
			// startSession();
		} else {
			//
			mInstrTextView.setText("Success");
			startSession();
		}*/
    }

    public void getInitialPhaseFace() {
        // show loading message in text view
        mInstrTextView.setText("Initializing ");
        // count down for three seconds
        t = new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub
                if (calibrationFace == -1) {
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
		/*if (calibrationFDD.getFaceSize() == -1) {
			calibrationFDD = lkgFace;
			try {
				t.cancel();
				// Disable OCV
				// mOpenCvCameraView.disableView();
				//mOpenCvCameraView.setVisibility(View.INVISIBLE);
				initPhase();
			} catch (Exception e) {
				Log.d(TAG,
						"CountDownTimer cancellation failed: " + e.getMessage());
			}
		} else {

		}*/
    }

    public void handleState(int state) {
        Context t = this.getActivity();
        //sActivity.handleState(this, state, t);
    }

    public void manageCurrentFace() {
        if (faceList.isEmpty()) {
            faceList.add(lkgFace);
        }
        // if currentFDD is less keep adding to list
        else {
            faceList.add(lkgFace);
            // if the displacement has changed greater than 10
            // if (Math.abs(currentFDD.getFaceSize()
            // - initialFDD.getFaceSize()) > 10){
            // Update current face

            // }
        }
    }

    public void checkInstruction() {
        // if expected zoom was made
        if (zoomController.getCurrentMode() == expectedMode) {
            phaseResult = 1;
        }
        // if incorrect zoom was made
        else {
            phaseResult = 0;
        }
    }


    /*
     * Handlers
     */
    private static Handler zoomHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
                // Set Mode level
                contentTextView.setTextSize(zoomController.getCurrentModeTextSize());
        }
    };

    private static class ToastHandler extends Handler{//} = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(mContext,
                        "Zoom Succeeded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext,
                        "Zoom Failed", Toast.LENGTH_SHORT).show();
            }

            calibrationFace = 0;
            //contentTextView.setText("");
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
