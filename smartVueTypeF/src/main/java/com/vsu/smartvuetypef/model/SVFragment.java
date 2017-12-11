package com.vsu.smartvuetypef.model;

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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vsu.smartvuetypef.SvActivity;
import com.vsu.smartvuetypef.util.Face;
import com.vsu.smartvuetypef.util.Session;
import com.vsu.smartvuetypef.util.ZoomControl;

import java.util.ArrayList;

import main.java.com.vsu.smartvuetypef.R;

public class SVFragment extends Fragment{
    private static final String TAG = "SVFragment";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    public Session session;

    public int maxWidth = 720, maxHeight = 576, faceSize, initZoom = -1, phaseResult, totalPhases = 1;

    static int stage = 0;
    public Face lkgFace = new Face();

    public boolean faceCheck, sampleReady = false, contentActive = false, facialSwitch = true;

    public Context mContext;
    public TextView mSampleView, contentTextView, mInstrTextView, mContentView;

    public View showView, hideView;
    public Animation in, out;
    public ValueAnimator instrAnim;
    public static boolean sampleSwitch = false;

    public static AnimatorSet phaseIn = new AnimatorSet();
    public static ValueAnimator fadeIn, fadeOut;
    public ToastHandler myToast = new ToastHandler(Looper.getMainLooper());
    public InstructionHandler myInstruct = new InstructionHandler(Looper.getMainLooper());
    public Button button;

    public SVFragment() {
        // Required empty public constructor
        Log.d(TAG, "SV Constructor");

        phaseIn.play(instrAnim);

        String[] mDetectorName = new String[2];
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
        mDetectorName[JAVA_DETECTOR] = "Java";

        in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(3000);

        out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(3000);

        faceCheck = true;
        faceSize = 0;

        session = new Session(this.getContext());
    }

    // Fragment Method
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
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
                "alpha", 0f, 1f).setDuration(1500);
        fadeOut = ObjectAnimator.ofFloat(contentTextView,
                "alpha", 1f, 0f).setDuration(1500);

        return view;
    }

    public void clearPhase(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final AnimatorSet instructionSet = new AnimatorSet();

                //Instruction Animation
                instructionSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // load instruction
                        contentTextView.setText("");
                    }
                });

                instructionSet.play(fadeOut).after(1000).after(fadeIn);
                instructionSet.start();
            }
        });
    }

    public void loadPhase() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
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

    public void startSampleSession() {
        loadPhase();
        contentActive = true;
    }

    public void endSampleSession(){
        //Turn off switches
        clearPhase();
        contentActive = false;
    }

    public void handleState(int state) {
        Context t = this.getActivity();
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
}
