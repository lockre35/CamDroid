package app.camdroid.ui;

import app.camdroid.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TabletFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.tablet,container,false);
		return rootView ;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

}
