package com.vsu.smartvuetypef.model;

import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;
import org.opencv.ml.CvSVMParams;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.util.Vector;

;

public class FeatureDetection extends FragmentActivity implements CvCameraViewListener2 {
	private static final String TAG = "FeatureDetection";

	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	private static final Scalar FACE_CENTER_COLOR = new Scalar(0, 255, 0, 255);
	private static final Scalar PRE_CENTER_COLOR = new Scalar(20, 150, 20, 0);
	private static final Scalar PRE_FACE_COLOR = new Scalar(20, 150, 20, 0);

	private MyKalmanFilter KF = null;
	private Mat templateP;// labels_mat = null, training_mat = null;
	protected Mat mRgba, mGray, measurement, state, m;
	protected MatOfRect faces;
	Vector<Rect> faces_list = new Vector<Rect>();

	private Point center = new Point(720 / 2, 576 / 2);
	protected Point origin = new Point(720 / 2, 576 / 2);
	private double ticks;

	private boolean mStopThread = false;
	protected boolean found;

	private float mRelativeFaceSize;
	private int notFoundCount, mAbsoluteFaceSize, frameSize;// train_files = 5;

	public static CameraBridgeViewBase mOpenCvCameraView;

	protected CascadeClassifier cascadeProfileFace;
	protected BaseLoaderCallback mLoaderCallback;
	private Thread mThread;

	private int learn_frames, type = 1;// train_index = 0;
	public static int foundFace = -1;


	CvSVMParams params2 = null;
	CvSVM svm = null;

	protected Rect predRect;
	Rect [] facesArray;
	protected Point predCenter;

	protected double[] predWidth, predHeight, predX, predY;

	public void detectFaces() {
		if (mAbsoluteFaceSize == 0) {
			setFaceSize(2);
		}
		Rect[] facesArray = detectFeature();
		for (int i = 0; i < facesArray.length; i++) {
			// Get ParseFace Size
			// faceSize = facesArray[i].width;

			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);

			Point center = new Point();
			center.x = facesArray[i].x + facesArray[i].width / 2;
			center.y = facesArray[i].y + facesArray[i].height / 2;
			Core.circle(mRgba, center, 2, FACE_CENTER_COLOR, -1);
		}
	}

	public void detectKalmanFaces() {
		// Initialize number of ticks from last call
		double precTick = ticks;
		ticks = (double) Core.getTickCount();
		// Calculate number of ticks since last detection
		double dT = (ticks - precTick) / Core.getTickFrequency();

		// Set size of face area
		setFaceSize(2);

		// Once the first face has been found
		if (found) {
			// Update the transtion Matrix
			KF.transitionMatrix.put(0, 2, dT);
			KF.transitionMatrix.put(1, 3, dT);

			// Use predict function to calculate next state
			state = KF.predict();

			// Get the dimensions for the predicted face location
			predWidth = state.get(4, 0);
			predHeight = state.get(5, 0);
			predX = state.get(0, 0);
			predY = state.get(1, 0);

			// Create the boundary rectangle for the predicted face
			predRect = new Rect();
			predRect.width = (int) predWidth[0];
			predRect.height = (int) predHeight[0];
			predRect.x = (int) (predX[0] - predRect.width / 2);
			predRect.y = (int) (predY[0] - predRect.height / 2);

			// Create center point for the predicted face
			predCenter = new Point();
			predCenter.x = (int) predX[0];
			predCenter.y = (int) predY[0];

			// Call for mechanism in use i.e. zoom, flip, scroll
			onFaceRecognized();
		}

		// Raw faces detected by classifier from call function to detect faces
		facesArray = detectFeature();

		// <<<<< Detection result // >>>>> Kalman Update
		// If there were no faces detected
		if (facesArray.length == 0) {
			notFoundCount++;
			// Log.d(TAG, "notFoundCount:" + notFoundCount);
			// If a face has not been detected within a certain amount
			// of frames, set found to false
			if (notFoundCount >= 10) {
				found = false;
			}
			// Else continue to update the post state of the filter
			else
				KF.statePost = state;
		}
		// If there were faces found
		else {
			// Reset not found count
			notFoundCount = 0;
			// Set dimensions from the detected face to measurement matrix
			measurement.put(0, 0, (facesArray[0].x + facesArray[0].width / 2));
			measurement.put(1, 0, (facesArray[0].y + facesArray[0].height / 2));
			measurement.put(2, 0, (float) facesArray[0].width);
			measurement.put(3, 0, (float) facesArray[0].height);

			// If first detection
			if (!found) {
				// Initialization error covariance matrix
				KF.errorCovPre.put(0, 0, 1); // px
				KF.errorCovPre.put(1, 1, 1); // px
				KF.errorCovPre.put(2, 2, 1);
				KF.errorCovPre.put(3, 3, 1);
				KF.errorCovPre.put(4, 4, 1); // px
				KF.errorCovPre.put(5, 5, 1); // px

				// Initialize state matrix from detected face found in
				// measurement matrix
				state.put(0, 0, measurement.get(0, 0));
				state.put(1, 0, measurement.get(1, 0));
				state.put(2, 0, 0);
				state.put(3, 0, 0);
				state.put(4, 0, measurement.get(2, 0));
				state.put(5, 0, measurement.get(3, 0));

				// Set found boolean
				found = true;
			}
			// If this is a following detection
			else {
				// Correct state matrix using the measurement matrix in Kalman
				// filter
				state = KF.correct(measurement);
			}
		}
	}

	public void detectKalmanEyes() {

		double precTick = ticks;
		ticks = (double) Core.getTickCount();

		double dT = (ticks - precTick) / Core.getTickFrequency(); // seconds

		if (mAbsoluteFaceSize == 0) {
			setFaceSize(2);
		}

		if (found) {
			KF.transitionMatrix.put(0, 2, dT);
			KF.transitionMatrix.put(1, 3, dT);

			state = KF.predict();

			// Log.d(TAG, "State post:" + state);
			double[] width = state.get(4, 0);
			double[] height = state.get(5, 0);
			double[] x = state.get(0, 0);
			double[] y = state.get(1, 0);

			Rect predRect = new Rect();
			predRect.width = (int) width[0];
			predRect.height = (int) height[0];
			predRect.x = (int) (x[0] - predRect.width / 2);
			predRect.y = (int) (y[0] - predRect.height / 2);

			Point center = new Point();
			center.x = (int) x[0];
			center.y = (int) y[0];

			Core.circle(mRgba, center, 2, PRE_CENTER_COLOR, -1);
			Core.rectangle(mRgba, predRect.tl(), predRect.br(), PRE_FACE_COLOR,
					2);

			Log.d(TAG, "Predicted ParseFace Size: " + predRect.width);
			onFaceRecognized();
		}

		Rect[] facesArray = detectFeature();

		// <<<<< Detection result // >>>>> Kalman Update
		if (facesArray.length == 0) {
			notFoundCount++;
			Log.d(TAG, "notFoundCount:" + notFoundCount);
			if (notFoundCount >= 10) {
				found = false;
			} else
				KF.statePost = state;

		} else {
			notFoundCount = 0;
			// ParseFace matrix
			measurement.put(0, 0, (facesArray[0].x + facesArray[0].width / 2));
			measurement.put(1, 0, (facesArray[0].y + facesArray[0].height / 2));
			measurement.put(2, 0, (float) facesArray[0].width);
			measurement.put(3, 0, (float) facesArray[0].height);

			if (!found) // First detection!
			{
				// >>>> Initialization
				KF.errorCovPre.put(0, 0, 1); // px
				KF.errorCovPre.put(1, 1, 1); // px
				KF.errorCovPre.put(2, 2, 1);
				KF.errorCovPre.put(3, 3, 1);
				KF.errorCovPre.put(4, 4, 1); // px
				KF.errorCovPre.put(5, 5, 1); // px

				state.put(0, 0, measurement.get(0, 0));
				state.put(1, 0, measurement.get(1, 0));
				state.put(2, 0, 0);
				state.put(3, 0, 0);
				state.put(4, 0, measurement.get(2, 0));
				state.put(5, 0, measurement.get(3, 0));

				// <<<< Initialization
				found = true;
			} else {

				// Kalman Correction
				state = KF.correct(measurement);
			}
		}
	}

	public void detectTemplateFaces() {
		if (mAbsoluteFaceSize == 0) {
			setFaceSize(2);
		}
		Rect[] facesArray = detectFeature();
		for (int i = 0; i < facesArray.length; i++) {
			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			// draw the area - mGray is working grayscale mat, if you want to
			// see area in rgb preview, change mGray to mRgba
			if (learn_frames < 5) {
				templateP = get_template(facesArray[i], 24);
				learn_frames++;
			} else {
				// Learning finished, use the new templates for template
				// matching
				match_template(facesArray[i], templateP);
			}
		}
	}

	public void enableCameraView(int index) {
		// mOpenCvCameraView.enableFpsMeter();
		mOpenCvCameraView.setCameraIndex(index);
		mOpenCvCameraView.enableView();
	}

	public void flipMat() {
		Core.flip(mGray, mGray, 1);
	}

	public void formatMat() {
		transposeMat();
	}

	public void initKalmanFilter() {
		if (KF == null) {
			KF = new MyKalmanFilter(6, 4, 6, CvType.CV_32FC1);
			Core.setIdentity(KF.transitionMatrix);
			KF.measurementMatrix.put(0, 0, 1.0f);
			KF.measurementMatrix.put(1, 1, 1.0f);
			KF.measurementMatrix.put(2, 4, 1.0f);
			KF.measurementMatrix.put(3, 5, 1.0f);
			Core.setIdentity(KF.measurementNoiseCov, new Scalar(1e-1));
		}
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		formatMat();
		detectKalmanFaces();
		return mRgba;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		mGray = new Mat();
		mRgba = new Mat();
		state = new Mat(6, 1, CvType.CV_32FC1);
		measurement = new Mat(4, 1, CvType.CV_32FC1);
		initKalmanFilter();
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		mGray.release();
		mRgba.release();
	}

	protected void setCameraView(CameraBridgeViewBase openView) {
		mOpenCvCameraView = openView;
		mOpenCvCameraView.setCvCameraViewListener(this);
		mThread = new Thread(new CameraWorker());
		mThread.start();
	}



	public void transposeMat() {
		m = Imgproc.getRotationMatrix2D(center, 90, .5);
		Imgproc.warpAffine(mRgba, mRgba, m, mRgba.size());
		Imgproc.warpAffine(mGray, mGray, m, mGray.size());
	}







	private void match_template(Rect area, Mat mTemplate) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		switch (type) {
			case 0:
				Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
				break;
			case 1:
				Imgproc.matchTemplate(mROI, mTemplate, mResult,
						Imgproc.TM_SQDIFF_NORMED);
				break;
			case 2:
				Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
				break;
			case 3:
				Imgproc.matchTemplate(mROI, mTemplate, mResult,
						Imgproc.TM_CCOEFF_NORMED);
				break;
			case 4:
				Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
				break;
			case 5:
				Imgproc.matchTemplate(mROI, mTemplate, mResult,
						Imgproc.TM_CCORR_NORMED);
				break;
		}

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		if (type == 0 || type == 1) {
			matchLoc = mmres.minLoc;
		} else {
			matchLoc = mmres.maxLoc;
		}

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));
		// Rect rec = new Rect(matchLoc_tx,matchLoc_ty);
	}

	private Mat get_template(Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect faces = new MatOfRect();
		Rect face_template = new Rect();
		faces = detectFeature(mROI);

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length;) {
			/*
			 * Rect f = facesArray[i]; f.x = area.x + f.x; f.y = area.y + f.y;
			 * Point f_center = new Point(); Rect face_only_rectangle = new
			 * Rect((int) f.tl().x, (int) (f.tl().y + f.height * 0.4), (int)
			 * f.width, (int) (f.height * 0.6)); mROI =
			 * mGray.submat(face_only_rectangle); Mat vyrez =
			 * mRgba.submat(face_only_rectangle);
			 * 
			 * 
			 * Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
			 * 
			 * Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255),
			 * 2); f_center.x = mmG.minLoc.x + face_only_rectangle.x; f_center.y
			 * = mmG.minLoc.y + face_only_rectangle.y; face_template = new
			 * Rect((int) f_center.x - size, (int) f_center.y - size / 2, size,
			 * size); Core.rectangle(mRgba, face_template.tl(),
			 * face_template.br(), new Scalar(255, 0, 0, 255), 2);
			 */
			template = (mGray.submat(face_template)).clone();
			return template;
		}
		return template;
	}

	public void setFaceSize(int minfacesize) {
		/*
		 * Min ParseFace Face50% - mRelativeFaceSize = 0.5; Face40% -
		 * mRelativeFaceSize = 0.4; Face30% - mRelativeFaceSize = 0.3; Default -
		 * mRelativeFaceSize = 0.5;
		 */
		if (mAbsoluteFaceSize == 0) {
			minfacesize = 2;
		}
		int height = mGray.rows();
		switch (minfacesize) {
			case 3:
				mRelativeFaceSize = 0.3f;
				break;
			case 4:
				mRelativeFaceSize = 0.4f;
				break;
			case 5:
				mRelativeFaceSize = 0.5f;
				break;
			default:
				mRelativeFaceSize = 0.2f;
				break;
		}

		if (Math.round(height * mRelativeFaceSize) > 0) {
			mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
		}
		// mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
	}

	public Rect[] detectFeature() {
		MatOfRect features = new MatOfRect();
		// detect feature using classifier
		if (cascadeProfileFace != null)
			//Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_DO_ROUGH_SEARCH
			cascadeProfileFace.detectMultiScale(mGray, features, 1.2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_DO_ROUGH_SEARCH, 2,
					new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
		Rect[] facesArray = features.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			// Get dimensions for detected face
			Point center = new Point();
			center.x = facesArray[i].x + facesArray[i].width / 2;
			center.y = facesArray[i].y + facesArray[i].height / 2;
			// Draw the rectangle and center point for the detected face
			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			Core.circle(mRgba, center, 2, FACE_CENTER_COLOR, -1);
		}

		return features.toArray();
	}

	public MatOfRect detectFeature(Mat m) {
		MatOfRect features = new MatOfRect();

		if (cascadeProfileFace != null)
			cascadeProfileFace.detectMultiScale(m, features, 1.15, 2,
					Objdetect.CASCADE_FIND_BIGGEST_OBJECT
							| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
					new Size());

		return features;
	}

	public void onFaceRecognized() {
		// Draw the prediction rectangle and center point on the preview
		// frame
		Core.circle(mRgba, predCenter, 2, PRE_CENTER_COLOR, -1);
		Core.rectangle(mRgba, predRect.tl(), predRect.br(), PRE_FACE_COLOR,
				2);
		foundFace = predRect.width;
	}

	public void runTemplateMatcher(Rect roi) {
		if (learn_frames < 5) {
			templateP = get_template(roi, 24);
			learn_frames++;
		} else {
			// Learning finished, use the new templates for template
			// matching
			match_template(roi, templateP);

		}
	}

	private class CameraWorker implements Runnable {
		public void run() {
			do {
				synchronized (FeatureDetection.this) {
					try {
						FeatureDetection.this.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (!mStopThread) {
					formatMat();
				}
			} while (!mStopThread);
			Log.d(TAG, "Finish camera processing thread");
		}
	}
}