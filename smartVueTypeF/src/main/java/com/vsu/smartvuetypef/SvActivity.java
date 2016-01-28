package main.java.com.vsu.smartvuetypef;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import main.java.com.vsu.smartvuetypef.features.FlipFragment;
import main.java.com.vsu.smartvuetypef.features.ZoomFragment;
import main.java.com.vsu.smartvuetypef.model.FeatureDetection;
import main.java.com.vsu.smartvuetypef.model.MyKalmanFilter;
import main.java.com.vsu.smartvuetypef.view.InstructionsFragment;

import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.core.Mat;


public class SvActivity extends FragmentActivity implements InstructionsFragment.OnInstructionCompletedListener{
	private static final String TAG = "SvActivity";

	int random;
	FragmentTransaction transaction;
	FragmentManager manager;
	int phase = 1;
    double mCalibrationFace=0;
	Fragment features, instruct;
	Fragment fragment;
	private static SvActivity sInstance = null;
	Toast toast;
    public static FeatureDetection faceDetector;
    private MyKalmanFilter KF = null;
    int iscene = 0, f = 0;
    //ZoomFragment.OnZoomChanged mZoomCallback;

	// A static block that sets class fields
	static {
		// Creates a single static instance of PhotoManager
		sInstance = new SvActivity();
	}

    private Mat mGray;
    private Mat mRgba;
    private Mat state, measurement;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        this.getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (savedInstanceState == null) {
			instruct = new InstructionsFragment();
			//fragment = new ZoomFragment();
			manager = getSupportFragmentManager();
			transaction = manager.beginTransaction();
			transaction.add(R.id.content_fragment, instruct);
            //transaction.add(R.id.content_fragment, zoomFragment);
			transaction.addToBackStack("instruction");
			transaction.commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sv, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		/*
		 * int id = item.getItemId(); if (id == R.id.action_settings) { return
		 * true; }
		 */

		switch (item.getItemId()) {
		case android.R.id.home:
			// Navigate "up" the demo structure to the launchpad activity.
			// See http://developer.android.com/design/patterns/navigation.html
			// for more.
			// NavUtils.navigateUpTo(this, new Intent(this,
			// MainActivity.class));

			return true;

		case R.id.action_previous:
			// Go to the previous step in the wizard. If there is no previous
			// step,
			// setCurrentItem will do nothing.
			manager.popBackStack();
			return true;

		case R.id.action_next:
			// Advance to the next step in the wizard. If there is no next step,
			// setCurrentItem
			// will do nothing.
			manager.popBackStack();
			phase++;
			return true;
		case R.id.action_settings:
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble("caliFace", mCalibrationFace);
	}

	public void onReadyClick(int i) {
        //mCalibrationFace = faceAvg;
		switch (i) {
		case 0:
			//if(faceAvg==0)
				fragment = new ZoomFragment();
			//else
			//	zoomFragment = ZoomFragment.newInstance(faceAvg);
			transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_fragment, fragment);
            transaction.addToBackStack("Zoom");
			transaction.commit();

			break;
		case 1:
			Log.d(TAG, "Flip Case");
			/*FlipFragment flipFragment = new FlipFragment();		
			transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.pager, flipFragment);
			transaction.addToBackStack("Flip");
			setContentView(R.layout.fd_screen_slide);*/

			//startActivity(new Intent(SvActivity.this, mSample.activityClass));

			// Commit the transaction
			//transaction.commit();
			break;
		case 2:
			Log.d(TAG, "Scroll Case");
			//ScrollFragment scrollFragment = new ScrollFragment();
			/*setContentView(R.layout.feature_view);
			transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.content_fragment, scrollFragment);
			transaction.addToBackStack("Scroll");
			// Commit the transaction
			transaction.commit();*/
			break;
		default:
			Log.d(TAG, "Default Case");
			break;
		}
	}

	public static SvActivity getInstance() {
		return sInstance;
	}

    public void goNextFeature(){
		f = 0;
        switch (f) {
            case 0:
                Log.d(TAG, "ZoomControl Case");
                fragment = new ZoomFragment();
                transaction = manager.beginTransaction();
                transaction.replace(R.id.content_fragment, fragment);
                transaction.addToBackStack("ZoomControl");
                // Commit the transaction
                transaction.commit();
                break;
            case 1:
                Log.d(TAG, "Flip Case");
                fragment = new FlipFragment();
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_fragment, fragment);
                transaction.addToBackStack("Flip");
                // Commit the transaction
                transaction.commit();
                break;
            default:
                Log.d(TAG, "Default Case");
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(instruct);
                transaction.commit();
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        void onClick();
    }
}
