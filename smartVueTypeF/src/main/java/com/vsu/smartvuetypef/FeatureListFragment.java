package main.java.com.vsu.smartvuetypef;

import main.java.com.vsu.smartvuetypef.features.FlipFragment;
import main.java.com.vsu.smartvuetypef.features.ScrollFragment;
import main.java.com.vsu.smartvuetypef.features.ZoomFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FeatureListFragment extends ListFragment {
	static final String[] features = { "Zoom", "Flip", "Scroll" };
	private static final String TAG = "FeatureList";
	FragmentTransaction transaction;
	FragmentManager manager;
	ZoomFragment zoom;
	FlipFragment flip;
	ScrollFragment scroll;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.feature_selection, container,
				false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// remove the dividers from the ListView of the ListFragment
		getListView().setDivider(null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// initialize the items list
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				this.getActivity(), android.R.layout.simple_list_item_1,
				features);

		// initialize and set the list adapter
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView parent, View view, int position,
			long id) {
		onReadyClick(position);
	}

	public void onReadyClick(int position) {
		switch (position) {
		case 0:
			Log.d(TAG, "ZoomControl Case");
			zoom = new ZoomFragment();
			transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content_fragment, zoom);
			transaction.addToBackStack("ZoomControl");
			// Commit the transaction
			transaction.commit();
			break;
		case 1:
			Log.d(TAG, "Flip Case");
			flip = new FlipFragment();
			transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content_fragment, flip);
			transaction.addToBackStack("Flip");
			// Commit the transaction
			transaction.commit();
			break;
		case 2:
			Log.d(TAG, "Scroll Case");
			scroll = new ScrollFragment();
			transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.content_fragment, scroll);
			transaction.addToBackStack("Scroll");
			// Commit the transaction
			transaction.commit();
			break;
		default:
			Log.d(TAG, "Default Case");
			break;
		}
	}
}
