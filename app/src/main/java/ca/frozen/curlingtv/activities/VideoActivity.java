// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import ca.frozen.library.classes.Log;
import ca.frozen.curlingtv.classes.Camera;
import ca.frozen.curlingtv.classes.Utils;
import ca.frozen.curlingtv.R;

public class VideoActivity extends AppCompatActivity implements VideoFragment.OnFadeListener
{
	// public constants
	public final static String CAMERA = "camera";

	// instance variables
	private Camera camera;
	private FrameLayout frameLayout;
	private VideoFragment videoFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);
		Log.info("camera: " + camera.toString());

		// get the frame layout, handle system visibility changes
		frameLayout = (FrameLayout) findViewById(R.id.video);
		frameLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
		{
			@Override
			public void onSystemUiVisibilityChange(int visibility)
			{
				if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
				{
					videoFragment.startFadeIn();
				}
			}
		});

		// set full screen layout
		int visibility = frameLayout.getSystemUiVisibility();
		visibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
		frameLayout.setSystemUiVisibility(visibility);

		// create the video fragment
		videoFragment = videoFragment.newInstance(camera, true);
		FragmentTransaction fragTran = getSupportFragmentManager().beginTransaction();
		fragTran.add(R.id.video, videoFragment);
		fragTran.commit();
	}

	//******************************************************************************
	// onStartFadeIn
	//******************************************************************************
	@Override
	public void onStartFadeIn()
	{
	}

	//******************************************************************************
	// onStartFadeOut
	//******************************************************************************
	@Override
	public void onStartFadeOut()
	{
		// hide the status and navigation bars
		int visibility = frameLayout.getSystemUiVisibility();
		visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		frameLayout.setSystemUiVisibility(visibility);
	}
}
