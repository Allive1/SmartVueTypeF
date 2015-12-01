package main.java.com.vsu.smartvuetypef.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.util.Face;

public class InstructionsFragment extends FeatureDetection implements Button.OnClickListener{
    // TODO: Rename parameter arguments, choose names that match
	private static final String TAG = "InstructionsFrag";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";
	int ZOOM_INSTR_PAGES = 4;
    int feature_flag = 0, faceCount = 0;
    private static int page_index, scene_index;
    // We transition between these Scenes
    private Scene instructScene, readyScene;
    private List<String> zoomInstruct;
    private int calibrationFace = 0;
    OnFragmentInteractionListener mCallback;

    /** A custom TransitionManager */
    //private TransitionManager mTransitionManagerForScene3;

    /** Transitions take place in this ViewGroup. We retain this for the dynamic transition on scene 4. */
    private ViewGroup mSceneRoot;

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;
    private TextView instruction_text;
    Context mContext;
    //private OnFragmentInteractionListener mListener;

    public static InstructionsFragment newInstance() {
        InstructionsFragment fragment = new InstructionsFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    public InstructionsFragment() {
        // Required empty public constructor
    }
    
    @Override 
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        try { 
            mCallback = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener"); 
        } 
    } 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Set view assests
        View view = inflater.inflate(R.layout.fragment_instructions_transition, container, false);
        assert view != null;

        Button button = (Button) view.findViewById(R.id.next_button);
        button.setOnClickListener(this);

        mOpenCvCameraView = (CameraBridgeViewBase) view
                .findViewById(R.id.opencv_view);
        mOpenCvCameraView.setCvCameraViewListener(this);


        mSceneRoot = (ViewGroup) view.findViewById(R.id.scene_root);
        TextView phaseTitle = (TextView) mSceneRoot.findViewById(R.id.phaseTitle);

        phaseTitle.setText("Instruction description.......");



        //page tracker
        page_index = 0;
        scene_index = 0;
        
        //Read instructions from file
        Scanner s = new Scanner(getResources().openRawResource(R.raw.zoom_instructions));       
        try { 
        	zoomInstruct = new ArrayList<String>();
            while (s.hasNextLine()) {
                zoomInstruct.add(s.nextLine());
            } 
        } finally { 
            s.close();
        } 
        
        // BEGIN_INCLUDE(instantiation_from_resource)
        // You can also inflate a generate a Scene from a layout resource file.
       // titleScene = Scene.getSceneForLayout(mSceneRoot, R.layout.phase_page, getActivity());
        // END_INCLUDE(instantiation_from_resource)

        instructScene = Scene.getSceneForLayout(mSceneRoot, R.layout.instruct_page, getActivity());
        readyScene = Scene.getSceneForLayout(mSceneRoot, R.layout.ready_page, getActivity());
        return view;
    }

    @Override
    public void onClick(View view) {	
    	//if scene_index is on the title page
        if(scene_index == 0){
        	scene_index = 2;
        	page_index = 0;
        	
        	TransitionManager.go(readyScene);
        }
        //If scene_index is on the instruction pages
        else if(scene_index == 1){
        	TransitionManager.go(instructScene);
            instruction_text = (TextView) instructScene.getSceneRoot().findViewById(R.id.instruct_text_view);
            instruction_text.setText(zoomInstruct.get(page_index));
        	
        	//If there are instruction pages to read
        	if(page_index >= 0 &&  page_index < zoomInstruct.size()-1){
            page_index++;
            instruction_text.setText(zoomInstruct.get(page_index));
        	}
        	//If on the final instruction page
        	else{
        		scene_index = 2;
        		page_index = -1;
        		TransitionManager.go(readyScene);
        	}
        }
        //If page_index is on the ready page
        else if(scene_index == 2){
        	//Average calibration faces and pass
            calcFaceAvg();
        	// Send the event to the host activity 
            mCallback.onReadyClick(feature_flag, calibrationFace);
        	//Switch to face detect activity 	
        }
        //Default
        else{
            
        } 
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
                this.getActivity(), mLoaderCallback);
    }

    @Override
    public void onCameraViewStopped() {
        super.onCameraViewStopped();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // TODO Auto-generated method stub
        super.onCameraFrame(inputFrame);
        detectKalmanFaces();
        return mRgba;
    }

    @Override
    public void onFaceRecognized() {
        super.onFaceRecognized();
        double[] width = state.get(4, 0);
        calibrationFace += (int)width[0];
        faceCount++;
    }

    public void calcFaceAvg(){
        calibrationFace = calibrationFace / faceCount;
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(int index);
        public void onReadyClick(int index, int face);
    }

	 // Classifier Loader
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
            this.getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            System.out.println("OpenCV Loaded: "+status);
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
}