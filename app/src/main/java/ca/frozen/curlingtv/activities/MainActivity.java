// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.classes.Camera;
import ca.frozen.curlingtv.classes.CameraAdapter;
import ca.frozen.curlingtv.classes.Utils;
import ca.frozen.curlingtv.R;

public class MainActivity extends AppCompatActivity
{
	// local constants
	private final static String TAG = "MainActivity";

	// instance variables
	private CameraAdapter adapter;
	private ScannerFragment scannerFragment;
	private ConnectivityChangeReceiver receiver = null;
	private int dummy = 43;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set the view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// create the toolbar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// load the settings and cameras
		Utils.loadData();

		// set the list adapter
		adapter = new CameraAdapter(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startScanner();
			}
		});
		adapter.refresh();
		ListView listView = (ListView)findViewById(R.id.cameras);
		listView.setAdapter(adapter);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adaptr, View view, int position, long id)
			{
				startVideoActivity(adapter.getCameras().get(position));
			}
		});

		// do a scan if there are no cameras
		if (savedInstanceState == null && adapter.getCameras().size() == 0 && Utils.connectedToNetwork())
		{
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					startScanner();
				}
			}, 500);
		}
	}

	//******************************************************************************
	// onStart
	//******************************************************************************
	@Override
	public void onStart()
	{
		super.onStart();
		if (receiver == null)
		{
			receiver = new ConnectivityChangeReceiver();
			registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}
	}

	//******************************************************************************
	// onStop
	//******************************************************************************
	@Override
	public void onStop()
	{
		super.onStop();
		if (receiver != null)
		{
			unregisterReceiver(receiver);
			receiver = null;
		}
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();
		Utils.reloadData();
		adapter.refresh();
	}

	//******************************************************************************
	// onSaveInstanceState
	//******************************************************************************
	@Override
	protected void onSaveInstanceState(Bundle state)
	{
		state.putInt("dummy", dummy);
		super.onSaveInstanceState(state);
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	//******************************************************************************
	// onPrepareOptionsMenu
	//******************************************************************************
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// disable Delete All if there are no cameras
		MenuItem item = menu.findItem(R.id.action_delete_all);
		item.setEnabled(adapter.getCameras().size() != 0);

		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int id = item.getItemId();

		// scan for cameras
		if (id == R.id.action_scan)
		{
			startScanner();
			return true;
		}

		// delete all the cameras
		else if (id == R.id.action_delete_all)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.ok_to_delete_all_cameras);
			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					List<Camera> allCameras = Utils.getCameras();
					for (Camera camera : adapter.getCameras())
					{
						allCameras.remove(camera);
					}
					updateCameras();
					dialog.dismiss();
				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});

			alert.show();
		}

		// edit the settings
		else if (id == R.id.action_settings)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		// display the about information
        else if (id == R.id.action_about)
        {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// startScanner
	//******************************************************************************
	private void startScanner()
	{
		FragmentManager fm = getFragmentManager();
		scannerFragment = new ScannerFragment();
		scannerFragment.show(fm, "Scanner");
	}

	//******************************************************************************
	// startVideoActivity
	//******************************************************************************
	private void startVideoActivity(Camera camera)
	{
		Intent intent = new Intent(App.getContext(), VideoActivity.class);
		intent.putExtra(VideoActivity.CAMERA, camera);
		startActivity(intent);
	}

	//******************************************************************************
	// updateCameras
	//******************************************************************************
	public void updateCameras()
	{
		Collections.sort(Utils.getCameras());
		Utils.saveData();
		adapter.refresh();
	}

	////////////////////////////////////////////////////////////////////////////////
	// ConnectivityChangeReceiver
	////////////////////////////////////////////////////////////////////////////////
	private class ConnectivityChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context c, Intent intent)
		{
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
			{
				if (adapter != null)
				{
					adapter.refresh();
				}
			}
		}
	}
}