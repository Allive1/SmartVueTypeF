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

import java.util.ArrayList;

import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.SvActivity;
import main.java.com.vsu.smartvuetypef.util.Face;
import main.java.com.vsu.smartvuetypef.util.Session;
import main.java.com.vsu.smartvuetypef.util.ZoomControl;

public class ZoomFragment extends Fragment implements ZoomControl.OnZoomChangedListener, SvActivity.OnFaceRecognizedListener {
    private static final String TAG = "ZoomFragment";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    private static boolean zoomSwitch = true;
    Session session;

    int maxWidth = 720, maxHeight = 576, faceSize, initZoom = -1, phaseResult, totalPhases = 1;

    static int stage = 0;
    Face lkgFace = new Face();

    boolean faceCheck, sampleReady = false, contentActive = false, facialSwitch = true;

    Context mContext;
    TextView mSampleView, contentTextView, mInstrTextView;
    TextView mContentView;

    public static ZoomControl zoomController;
    View showView, hideView;
    Animation in, out;
    ValueAnimator instrAnim;
    ArrayList<Face> faceList = new ArrayList<Face>();
    static boolean sampleSwitch = false;

    static AnimatorSet phaseIn = new AnimatorSet();
    CountDownTimer t;
    Session mySession;
    static ValueAnimator fadeIn, fadeOut;
    ToastHandler myToast = new ToastHandler(Looper.getMainLooper());
//    SampleHandler mySample = new SampleHandler();
    InstructionHandler myInstruct = new InstructionHandler(Looper.getMainLooper());
    ZoomHandler myZoom = new ZoomHandler(Looper.getMainLooper());

    public ZoomFragment() {
        // Required empty public constructor
        Log.d(TAG, "Zoom Constructor");

        phaseIn.play(instrAnim);

        String[] mDetectorName = new String[2];
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

        session = new Session(this.getContext());
    }

    // Fragment Method
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "Attached");
        mContext = context;
        session = new Session(context);

        Bundle b = getArguments();
        int c = b.getInt("calibration_face");

        if (c > 0) {
            session.setCalibrationFace(c);
            //zoomController.runZoomLevelCorrect(c);
        } else {
            session.setCalibrationFace(170);
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
        fadeIn = ObjectAnimator.ofFloat(contentTextView,
                "alpha", 0f, 1f);
        fadeOut = ObjectAnimator.ofFloat(contentTextView,
                "alpha", 1f, 0f);
        fadeIn.setDuration(1500);
        fadeOut.setDuration(1500);
        startSampleSession();

        return view;
    }

    // Session Methods
    public void loadPhase() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                contentTextView.setTextSize(zoomController.getCurrentModeTextSize());
                final AnimatorSet instructionSet = new AnimatorSet();
                final AnimatorSet sampleSet = new AnimatorSet();

                //Instruction Animation
                instructionSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // load instruction
                        loadInstruction();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        sampleSet.play(fadeIn);
                        sampleSet.start();
                        Log.d(TAG, "Instruction Set");
                    }
                });

                //Sample Animation
                sampleSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        loadSample();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        stage = 2;
                        sampleSwitch = true;
                        Log.d(TAG, "Sample Set");
                    }
                });

                instructionSet.play(fadeOut).after(1000).after(fadeIn);
                instructionSet.start();
            }
        });
    }

    private void loadInstruction() {
        contentTextView.setText(session.getInstruction());
    }

    private void loadSample() {
        contentTextView.setText(session.getSample());
    }

    private void displayResampleMessage() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final AnimatorSet confirmPhaseSet = new AnimatorSet();
                final CharSequence loopMsg = "Ok. Time for another sample";
                confirmPhaseSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        contentTextView.setText(loopMsg);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        contentTextView.setText("");
                        loadPhase();
                    }
                });

                confirmPhaseSet.play(fadeOut).after(1000).after(fadeIn);
                confirmPhaseSet.start();
            }
        });
    }

    public void playPhaseResult() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final AnimatorSet endPhaseSet = new AnimatorSet();
//
//                instructionSet.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationStart(Animator animation) {
//                        // load instruction
//                        loadInstruction();
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        sampleSet.play(fadeIn);
//                        sampleSet.start();
//                        Log.d(TAG, "Sample Phase: ready");
//                    }
//                });
//
//                sampleSet.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationStart(Animator animation) {
//                        //load sample
//                        loadSample();
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        stage = 2;
//                        sampleSwitch = true;
//                        Log.d(TAG, "Sample Phase: ready");
//                    }
//                });
//
//                instructionSet.play(fadeOut).after(1000).after(fadeIn);
//                instructionSet.start();
            }
        });
    }

    public void startSampleSession() {
        loadPhase();
        contentActive = true;
    }

    public void endSampleSession(){
        //Turn off switches

        //Log results

        //Reset to interim state
    }

    //Record User Input
    public void checkForZoomChange(int face) {
        if (session.getCalibrationFace() > 0 && contentActive) {
            if (facialSwitch)
                zoomController.runZoomLevelCorrect(face);
        }
    }

    public boolean zoomInstructionCheck(int recognizedLevel) {
        boolean result;
        CharSequence response;
        Message msg = new Message();

        // check recognized level against the expected level
        if (recognizedLevel == session.getExpectedMode()) {
            result = true;
            response = "User Success";
            // Confirm success
            contentActive = false;
            msg.obj = response;
            myZoom.handleMessage(msg);
        }
        // else L register a fail
        else {
            result = false;
            contentActive = false;
            response = "User Failure";
            msg.obj = response;
            myZoom.handleMessage(msg);
        }
        facialSwitch = false;
        return result;
    }

    public void onZoomChanged(int position) {
        // If view is available
        if (contentTextView.getVisibility() == View.VISIBLE) {
            // Update zoom
            if (sampleSwitch) {
                myZoom.sendEmptyMessage(0);
                boolean result = zoomInstructionCheck(zoomController.getCurrentMode());
                if (result) {
                    endSampleSession();
                } else {
                    handleState(0);
                }
//                sampleSwitch = false;
//                if (session.getExpectedMode() == zoomController.getCurrentMode())
//                    myToast.sendEmptyMessage(1);
//                else
//                    myToast.sendEmptyMessage(0);
//                myToast.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        final AnimatorSet sessionFinale = new AnimatorSet();
//                        sessionFinale.addListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationStart(Animator animation){
//                                contentTextView.setText("Phase Complete\n\nPrepare for the next sample");
//                            }
//
//                            @Override
//                            public void onAnimationEnd(Animator animation){
//                                //session.start();
//                            }
//                        });
//                    }
//                });
            }
        } else {
            Log.d(TAG, "Zoom attempt with Sample Text View invisible");
        }
    }

    public void handleState(int state) {
        Context t = this.getActivity();
        //sActivity.handleState(this, state, t);
    }

    Handler s = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg){
            //TextView content = (TextView) msg.obj;
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

    private class ZoomHandler extends Handler {
        ZoomHandler(Looper l) {
            super(l);
        }

        public void handleMessage(Message msg) {
            // Set Mode level
            Log.d(TAG, "Zoom Handler is running");
            contentTextView.setText(msg.obj.toString());
            contentTextView.setTextSize(zoomController.getCurrentModeTextSize());
            //Disable zoom handler
            zoomSwitch = false;
        }
    }

    private class ToastHandler extends Handler {
        ToastHandler(Looper l) {
            super(l);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(mContext,
                        "Zoom Succeeded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext,
                        "Zoom Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class InstructionHandler extends Handler {
        InstructionHandler(Looper l) {
            super(l);
        }

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
                    //sampHandler.sendEmptyMessage(0);
                }
            });
            animatorSet.start();
        }
    }
}
