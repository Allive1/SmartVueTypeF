package main.java.com.vsu.smartvuetypef.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;

/**
 * Created by Omi on 11/14/15.
 */
public class OpenCam extends FeatureDetection{
    private static final String TAG = "OpenCam";
    Context mContext;
    private BaseLoaderCallback mLoaderCallback;
    private CameraBridgeViewBase mOpenCvCameraBridge;





    public void setCam(CameraBridgeViewBase cbvb){
        mOpenCvCameraBridge = cbvb;
        mOpenCvCameraView.setCvCameraViewListener(this);
    }



    public void setContext(Context context){
            mContext = context;
            setBaseLoaderCallback();
    }

    private void setBaseLoaderCallback(){
        mLoaderCallback = new BaseLoaderCallback(
                this.getActivity()) {
            @Override
            public void onManagerConnected(int status) {
                System.out.println("OpenCV Loaded: "+status);
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");
                        try {
                            // load cascade file from application resources
                            // -------------------Frontal
                            // ParseFace-----------------------
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
                            buffer = null;

                            buffer = null;
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
    }

    /*
	 * OpenCV Methods
	 */
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
        onFaceRecognized();
        // get the averaging face size
        double[] width = state.get(4, 0);
    }
}
