package main.java.com.vsu.smartvuetypef.features;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.objdetect.CascadeClassifier;

import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.R;
import main.java.com.vsu.smartvuetypef.view.ScreenSlidePageFragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

public class FlipFragment extends FeatureDetection {
	private static final int JAVA_DETECTOR = 0;
	private static final int NUM_PAGES = 5;
	private static final String TAG = "FlipFrag";
	public static final String ARG_PAGE = "page";

	private static ViewPager mPager;
	private static long lastPageTurn = 0;
	private static boolean flippable = true;

	boolean faceCheck, faceOrgDir = true;
	private int index = 0, mPageNumber;
	Mat flippedGray, m;

	public static final int NATIVE_DETECTOR = 1;
	String[] mDetectorName;
	String[] instructions = new String[4];

	int maxWidth = 720, maxHeight = 576; // Cols, Rows
	Context mContext;

	Animation in, out;
	Paint mPaint;
	Point center;

	Animation fadeIn = new AlphaAnimation(0, 1);
	Animation fadeOut = new AlphaAnimation(1, 0);

	private PagerAdapter mPagerAdapter;
	Message msg;

	long facePeriodStart, facePeriodEnd;
	boolean faceDetectState;
	double initialFacePX, moveFacePX;
	int searchMode = 0;

	private static Handler flipHandler = new Handler() {
		public void handleMessage(Message msg) {
			//Log.d(TAG, "Flip attempt");
			// Check direction of flip
			switch (msg.arg1) {
			case 1:
				if (mPager.getCurrentItem() == NUM_PAGES)
					Log.d(TAG, "End of Collection");
				else {
					Log.d(TAG, "Flip Right");
					mPager.setCurrentItem(mPager.getCurrentItem() + 1);
					lastPageTurn = System.currentTimeMillis();
				}
				break;
			case 2:
				if (mPager.getCurrentItem() == 1)
					Log.d(TAG, "Start of Collection");
				else {
					Log.d(TAG, "Flip Left");
					mPager.setCurrentItem(mPager.getCurrentItem() - 1);
					lastPageTurn = System.currentTimeMillis();
				}
				break;
			}
			// Lock flip state
			flippable = false;

			// Unlock flip state handler in 2 secs
			unlockFlip();
		}
	};

	private static Handler flipStateHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Unlock flip state
			flippable = true;
		}
	};

	protected BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
			this.getActivity()) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				try {
					// load cascade file from application resources
					File mCascadeFile;
					InputStream is = getResources().openRawResource(
							R.raw.haarcascade_profileface);

					File cascadeDir = mContext.getDir("cascade",
							Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"haarcascade_profileface.xml");
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
					os.close();
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

	public static FlipFragment newInstance() {
		FlipFragment fragment = new FlipFragment();
		return fragment;
	}

	public static FlipFragment create(int pageNumber) {
		FlipFragment fragment = new FlipFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, pageNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public FlipFragment() {
		// Required empty public constructor
		/* now we can start update thread */
		fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
		fadeIn.setDuration(1000);

		fadeOut.setInterpolator(new AccelerateInterpolator()); // and this
		fadeOut.setStartOffset(1000);
		fadeOut.setDuration(1000);

		mDetectorName = new String[2];
		mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
		mDetectorName[JAVA_DETECTOR] = "Java";

		mPaint = new Paint();
		mPaint.setColor(Color.BLUE);
		mPaint.setTextSize(20);

		in = new AlphaAnimation(0.0f, 1.0f);
		in.setDuration(3000);

		out = new AlphaAnimation(1.0f, 0.0f);
		out.setDuration(3000);

		faceCheck = true;
		center = new Point(720 / 2, 576 / 2);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mContext = activity.getBaseContext();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mPageNumber = getArguments().getInt(ARG_PAGE);
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

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) view.findViewById(R.id.pager);
		mPagerAdapter = new FlipPagerAdapter(getActivity().getFragmentManager());

		mPager.setAdapter(mPagerAdapter);
		mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When changing pages, reset the action bar actions since they
				// are dependent
				// on which page is currently active. An alternative approach is
				// to have each
				// fragment expose actions itself (rather than the activity
				// exposing actions),
				// but for simplicity, the activity provides the actions in this
				// sample.
				getActivity().invalidateOptionsMenu();
			}
		});
		// mTextView = (TextView) view.findViewById(R.id.textView1);
		// mTextView.setText(getString(R.string.title_template_step,
		// mPageNumber + 1));

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fd_screen_slide, menu);

		menu.findItem(R.id.action_previous).setEnabled(
				mPager.getCurrentItem() > 0);

		// Add either a "next" or "finish" button to the action bar, depending
		// on which page
		// is currently selected.
		MenuItem item = menu
				.add(Menu.NONE,
						R.id.action_next,
						Menu.NONE,
						(mPager.getCurrentItem() == mPagerAdapter.getCount() - 1) ? R.string.action_finish
								: R.string.action_next);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*switch (item.getItemId()) {
		case android.R.id.home:
			// Navigate "up" the demo structure to the launchpad activity.
			// See http://developer.android.com/design/patterns/navigation.html
			// for more.
			// NavUtils.navigateUpTo(this, new Intent(this, SvActivity.class));
			return true;

		case R.id.action_previous:
			// Go to the previous step in the wizard. If there is no previous
			// step,
			// setCurrentItem will do nothing.
			mPager.setCurrentItem(mPager.getCurrentItem() - 1);
			return true;

		case R.id.action_next:
			// Advance to the next step in the wizard. If there is no next step,
			// setCurrentItem
			// will do nothing.
			mPager.setCurrentItem(mPager.getCurrentItem() + 1);
			return true;
		}*/

		return super.onOptionsItemSelected(item);
	}

	public int getPageNumber() {
		return mPageNumber;
	}

	@Override
	public void onFaceRecognized() {
		super.onFaceRecognized();
		
		// if a face turn has not yet been registered
		if (System.currentTimeMillis() > (lastPageTurn + 2000)) {
			runTurnCheck();
		}
		// if a face has been registered, the flip mechanism should be locked
		//else 
			//Log.d(TAG, "Flip Locked");
		
	}

	@Override
	public void formatMat() {
		transposeMat();
		searchForFace();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
				this.getActivity(), mLoaderCallback);
	}

	public void detectFlippedKalmanFaces() {
		flipMat();
		detectKalmanFaces();
	}

	private static void unlockFlip() {
		Message msg = flipStateHandler.obtainMessage();
		flipStateHandler.sendMessageDelayed(msg,
				(System.currentTimeMillis() + 2000));
	}

	private void searchForFace() {
		switch (searchMode) {
		case 0:
			// Reset turnable
			// if(!flippable)
			// flippable = true;

			// Check regular face
			detectKalmanFaces();

			// if a face has not been found
			if (found) {
				facePeriodStart = System.currentTimeMillis();
				// set searchMode to 1
				searchMode = 1;
			}
			// if a face has not been found in original
			else {
				// check the flipped image
				detectFlippedKalmanFaces();
				// if face detected
				if (found) {
					facePeriodStart = System.currentTimeMillis();
					// set searchMode to 2
					searchMode = 2;
				}
				// If a face has not been found
				else {
					searchMode = 0;
					flippable = true;
				}
			}
			break;
		case 1:
			// While the initial face is being tracked, continue tracking
			if (found) {
				detectKalmanFaces();
			}
			// if the face is lost, revert to original search mode
			else {
				searchMode = 0;
				resetFlipVars();
			}

			break;
		case 2:
			// While the initial face is being tracked, continue tracking
			if (found) {
				Log.d(TAG, "Kalman Flipped Center: " + predCenter.x);
				detectFlippedKalmanFaces();
			}
			// if the face is lost, revert to original search mode
			else {
				searchMode = 0;
				resetFlipVars();
			}
			break;
		default:
			searchMode = 0;
			Log.d(TAG, "Default ");
			break;
		}
		// Log.d(TAG, "Kalman Center: " + predCenter.x);
	}

	public void runTurnCheck() {
		Message msg = flipHandler.obtainMessage();
		// if this is a fresh face recognition
		if (initialFacePX == 0) {
			// Initialize face starting X point and time
			initialFacePX = predCenter.x;
			facePeriodStart = System.currentTimeMillis();
			facePeriodEnd = facePeriodStart + 2000;
		}
		// if an initial face has already been recognized
		else {
			// get movement of face on x-plane
			moveFacePX = predCenter.x;
			// if face has turned enough
			if ((Math.abs(moveFacePX-initialFacePX)) > 7) {
				msg.arg1 = searchMode;
				flipHandler.sendMessage(msg);

				// Lock flip
				flippable = false;
				resetFlipVars();
			}
			/*else{
				Log.d(TAG,"Displacement: " + (Math.abs(moveFacePX-initialFacePX)));
			}*/
		}
	}

	private void resetFlipVars() {
		initialFacePX = 0;
		moveFacePX = 0;
		facePeriodStart = 0;
		facePeriodEnd = 0;
		searchMode = 0;
	}

	public class FlipPagerAdapter extends FragmentStatePagerAdapter {
		public FlipPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public android.app.Fragment getItem(int position) {
			Log.d(TAG, "Create: " + position);
			return ScreenSlidePageFragment.create(position);
		}

		@Override
		public int getCount() {
			return NUM_PAGES;
		}
	}
}
