package main.java.com.vsu.smartvuetypef.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;

public class InstructionsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
	private static final String TAG = "InstructionsFrag";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";
	int ZOOM_INSTR_PAGES = 4;
    int feature_flag = 0, faceCount = 0;
    private static int page_index, scene_index;
    private Button nextButton;
    // We transition between these Scenes
    private Scene instructScene, readyScene;
    private List<String> zoomInstruct;
    private int calibrationFace = 0;
    OnInstructionCompletedListener mCallback;
    /** A custom TransitionManager */
    //private TransitionManager mTransitionManagerForScene3;

    /** Transitions take place in this ViewGroup. We retain this for the dynamic transition on scene 4. */
    private ViewGroup mSceneRoot;

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;
    private TextView instruction_text;
    Context mContext;
    TextView phaseTitle;

    OnClickListener nextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //if scene_index is on the title page
            switch(scene_index){
                case 0:
                    scene_index = 3;
                    page_index = 0;

                    TransitionManager.go(readyScene);
                    break;
                case 1:
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
                    break;
                case 2:
                    //Average calibration faces and pass
                    calcFaceAvg();
                    // Send the event to the host activity
                    //mCallback.onReadyClick(feature_flag, calibrationFace);
                    //mCallback.onReadyClick(0);
                    //Switch to face detect activity
                    break;
                default:
                    mCallback.goNextFeature();
                    break;
            }
        }
    };

    //private OnFragmentInteractionListener mListener;
    public interface OnInstructionCompletedListener {
        void goNextFeature();
    }

    public static InstructionsFragment newInstance(FeatureDetection faceDetector) {
        InstructionsFragment fragment = new InstructionsFragment();
        /*Bundle args = new Bundle();

        args.put("FeatureDetector", face);
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
            mCallback = (OnInstructionCompletedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    } 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.instructions_transition, container, false);
        assert view != null;
        mSceneRoot = (ViewGroup) view.findViewById(R.id.scene_root);
        nextButton = (Button) view.findViewById(R.id.button);
        phaseTitle = (TextView) mSceneRoot.findViewById(R.id.phaseTitle);

        nextButton.setOnClickListener(nextListener);
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

        instructScene = Scene.getSceneForLayout(mSceneRoot, R.layout.instruct_page, getActivity());
        readyScene = Scene.getSceneForLayout(mSceneRoot, R.layout.ready_page, getActivity());
        return view;
    }

    public void calcFaceAvg(){
        if(faceCount>0)
        calibrationFace = calibrationFace / faceCount;
    }
}