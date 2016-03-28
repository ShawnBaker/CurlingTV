// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.classes.Settings;
import ca.frozen.curlingtv.classes.Utils;
import ca.frozen.curlingtv.R;

public class SettingsActivity extends AppCompatActivity
{
	// local constants
	private final static String TAG = "SettingsActivity";

	// instance variables
	private EditText portEdit;
	private Settings settings;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// get the settings
		settings = (savedInstanceState == null)
						? new Settings(Utils.getSettings())
						: (Settings) savedInstanceState.getParcelable("settings");

		portEdit = (EditText) findViewById(R.id.settings_port);
		portEdit.setText(Integer.toString(settings.port));
	}

	//******************************************************************************
	// onSaveInstanceState
	//******************************************************************************
	@Override
	protected void onSaveInstanceState(Bundle state)
	{
		settings.port = Integer.parseInt(portEdit.getText().toString().trim());
		state.putParcelable("settings", settings);
		super.onSaveInstanceState(state);
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_save, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		// save the camera
		if (id == R.id.action_save)
		{
			if (getAndCheckSettings())
			{
				Utils.setSettings(settings);
				Utils.saveData();
				finish();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// getAndCheckSettings
	//******************************************************************************
	private boolean getAndCheckSettings()
	{
		// get the port number
		String text = portEdit.getText().toString().trim();
		if (text.length() == 0)
		{
			App.error(this, R.string.error_no_port);
			return false;
		}
		try
		{
			settings.port = Integer.parseInt(text);
		}
		catch (Exception ex)
		{
			App.error(this, String.format(getString(R.string.error_bad_port), Utils.MIN_PORT, Utils.MAX_PORT));
			return false;
		}

		// make sure the port is within range
		if (settings.port < Utils.MIN_PORT || settings.port > Utils.MAX_PORT)
		{
			App.error(this, String.format(getString(R.string.error_bad_port), Utils.MIN_PORT, Utils.MAX_PORT));
			return false;
		}

		// indicate success
		return true;
	}
}