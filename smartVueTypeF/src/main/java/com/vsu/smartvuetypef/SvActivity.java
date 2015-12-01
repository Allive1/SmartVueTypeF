package main.java.com.vsu.smartvuetypef;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import main.java.com.vsu.smartvuetypef.features.ZoomFragment;
import main.java.com.vsu.smartvuetypef.view.InstructionsFragment;

import android.widget.Toast;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class SvActivity extends FragmentActivity implements InstructionsFragment.OnFragmentInteractionListener{
	private static final String TAG = "SvActivity";
	int random;
	FragmentTransaction transaction;
	FragmentManager manager;
	int phase = 1;
    double mCalibrationFace=0;
	Sample mSample;
	Fragment features, instruct;
	private static SvActivity sInstance = null;
	Toast toast;
	private Context context;

	// A static block that sets class fields
	static {
		// Creates a single static instance of PhotoManager
		sInstance = new SvActivity();
	}

	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			// Gets the image task from the incoming Message object.
			// ZoomFragment task = (ZoomFragment) msg.obj;
			switch (msg.what) {
				case 0:
					toast = Toast.makeText(context, "Instruction Failed",
							Toast.LENGTH_SHORT);
					toast.show();
					break;

				case 1:
					toast = Toast.makeText(context, "Instruction Completed",
							Toast.LENGTH_SHORT);
					toast.show();
					break;
				default:
					Log.d(TAG, "Handle failure");
					break;
			}
		}
	};

	public void handleState(ZoomFragment task, int state, Context t) {
		context = t;
		//Log.d(TAG, "Context: " + context.toString());
		Message completeMessage;
		switch (state) {
			// The task finished downloading and decoding the image
			case 0:
				completeMessage = mHandler.obtainMessage(state, task);
				completeMessage.sendToTarget();
				break;
			case 1:
				completeMessage = mHandler.obtainMessage(state, task);
				completeMessage.sendToTarget();
				break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_activity_main);
		if (savedInstanceState == null) {
			manager = getSupportFragmentManager();
			transaction = manager.beginTransaction();
			instruct = new InstructionsFragment();
			transaction.replace(R.id.content_fragment, instruct);
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
    public void onFragmentInteraction(int index) {

    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble("caliFace", mCalibrationFace);
	}

	public void onReadyClick(int position, int faceAvg) {
        mCalibrationFace = faceAvg;
		switch (position) {
		case 0:
			Log.d(TAG, "Zoom Case");
			ZoomFragment zoomFragment;
					//(ZoomFragment)getFragmentManager().findFragmentById(R.id.content_fragment);
			if(faceAvg==0)
				zoomFragment = new ZoomFragment();
			else
				zoomFragment = ZoomFragment.newInstance(faceAvg);


			transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.content_fragment, zoomFragment);
			transaction.addToBackStack("Zoom");
			// Commit the transaction
			transaction.commit();
			break;
		case 1:
			Log.d(TAG, "Flip Case");		
			/*FlipFragment flipFragment = new FlipFragment();		
			transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.pager, flipFragment);
			transaction.addToBackStack("Flip");
			setContentView(R.layout.fd_screen_slide);*/
			
			startActivity(new Intent(SvActivity.this, mSample.activityClass));
	    
			// Commit the transaction
			//transaction.commit();
			break;
		case 2:
			Log.d(TAG, "Scroll Case");
			//ScrollFragment scrollFragment = new ScrollFragment();
			/*setContentView(R.layout.fd_surface_view);
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

	private class Sample {
        private CharSequence title;
        private Class<? extends FragmentActivity> activityClass;

        public Sample(int titleResId, Class<? extends FragmentActivity> activityClass) {
            this.activityClass = activityClass;
            this.title = getResources().getString(titleResId);
        }

        @Override
        public String toString() {
            return title.toString();
        }
    }
}
