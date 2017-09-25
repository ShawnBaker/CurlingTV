// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import ca.frozen.library.classes.Log;
import ca.frozen.curlingtv.classes.Camera;
import ca.frozen.curlingtv.classes.Connection;
import ca.frozen.curlingtv.classes.Utils;
import ca.frozen.curlingtv.R;
import ca.frozen.curlingtv.classes.VideoParams;

public class CameraActivity extends AppCompatActivity
{
	// public constants
	public final static String CAMERA = "camera";

	// instance variables
	private Camera camera;
	private VideoParams videoParams = null;
	private Bitmap image = null;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the camera object
		Bundle data = getIntent().getExtras();
		camera = data.getParcelable(CAMERA);
		Log.info("camera: " + (camera.name.isEmpty() ? "new" : camera.name));

		// set the name
		TextView textView = (TextView) findViewById(R.id.camera_name);
		textView.setText(camera.name);

		// set the address
		textView = (TextView) findViewById(R.id.camera_address);
		textView.setText(camera.address);

		// set the address
		textView = (TextView) findViewById(R.id.camera_port);
		textView.setText(Integer.toString(camera.port));

		// get the saved data if it exists
		if (savedInstanceState != null)
		{
			videoParams = savedInstanceState.getParcelable("videoParams");
			image = savedInstanceState.getParcelable("image");
		}

		// set the video parameters
		if (videoParams != null)
		{
			setVideoParams();
		}
		else
		{
			GetParamsTask getParams = new GetParamsTask();
			getParams.execute("");
		}

		// set the image
		if (image != null)
		{
			setImage();
		}
		else
		{
			GetImageTask getImage = new GetImageTask();
			getImage.execute("");
		}
	}

	//******************************************************************************
	// onSaveInstanceState
	//******************************************************************************
	@Override
	protected void onSaveInstanceState(Bundle state)
	{
		state.putParcelable("videoParams", videoParams);
		state.putParcelable("image", image);
		super.onSaveInstanceState(state);
	}

	//******************************************************************************
	// setVideoParams
	//******************************************************************************
	private void setVideoParams()
	{
		TextView textView = (TextView) findViewById(R.id.camera_resolution);
		textView.setText(String.format("%d x %d", videoParams.width, videoParams.height));
		textView = (TextView) findViewById(R.id.camera_frame_rate);
		textView.setText(Integer.toString(videoParams.fps));
		textView = (TextView) findViewById(R.id.camera_bit_rate);
		textView.setText(Integer.toString(videoParams.bps));
		Log.info("videoParams: " + videoParams.toString());
	}

	//******************************************************************************
	// setImage
	//******************************************************************************
	private void setImage()
	{
		ImageView imageView = (ImageView) findViewById(R.id.camera_image);
		imageView.setImageBitmap(image);
	}

	////////////////////////////////////////////////////////////////////////////////
	// GetParamsTask
	////////////////////////////////////////////////////////////////////////////////
	private class GetParamsTask extends AsyncTask<String, Void, Integer>
	{
		@Override
		protected Integer doInBackground(String... params)
		{
			Connection commandConnection = new Connection(camera.address, camera.port, Connection.CONNECT_TIMEOUT);
			if (commandConnection.isConnected())
			{
				videoParams = commandConnection.getVideoParams();
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (videoParams != null)
			{
				setVideoParams();
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// GetImageTask
	////////////////////////////////////////////////////////////////////////////////
	private class GetImageTask extends AsyncTask<String, Void, Integer>
	{
		@Override
		protected Integer doInBackground(String... params)
		{
			Connection commandConnection = new Connection(camera.address, camera.port, Connection.CONNECT_TIMEOUT);
			int imagePort = commandConnection.getImagePort();
			Connection imageConnection = new Connection(camera.address, imagePort, Connection.CONNECT_TIMEOUT);
			if (imageConnection.isConnected())
			{
				byte[] buffer = new byte[4];
				int len = imageConnection.read(buffer);
				Log.info(String.format("len = %d", len));
				if (len == 4)
				{
					int imageSize = 0;
					for (int i = len - 1; i >= 0; i--)
					{
						imageSize = (imageSize << 8) + (buffer[i] & 0xFF);
					}
					Log.info(String.format("numBytes = %d", imageSize));
					buffer = new byte[imageSize];
					len = 0;
					while (len < imageSize)
					{
						len += imageConnection.read(buffer, len, imageSize - len);
					}
					Log.info(String.format("len = %d", len));
					image = BitmapFactory.decodeByteArray(buffer, 0, len);
				}
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (image != null)
			{
				setImage();
			}
		}
	}
}