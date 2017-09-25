// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import ca.frozen.library.classes.Log;
import ca.frozen.library.views.ZoomPanTextureView;
import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.R;
import ca.frozen.curlingtv.classes.Camera;
import ca.frozen.curlingtv.classes.Connection;
import ca.frozen.curlingtv.classes.Utils;
import ca.frozen.curlingtv.classes.VideoParams;

public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener
{
	// public interfaces
	public interface OnFadeListener
	{
		void onStartFadeIn();
		void onStartFadeOut();
	}

	// public constants
	public final static String CAMERA = "camera";
	public final static String FULL_SCREEN = "full_screen";

	// local constants
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;
	private final static int FADEOUT_TIMEOUT = 5000;
	private final static int FADEOUT_ANIMATION_TIME = 500;
	private final static int FADEIN_ANIMATION_TIME = 400;
	private final static int REQUEST_WRITE_EXTERNAL_STORAGE = 73;

	// instance variables
	private Camera camera;
	private boolean fullScreen;
	private DecoderThread decoder;
	private ZoomPanTextureView textureView;
	private TextView nameView, messageView;
	private Button snapshotButton;
	private Runnable fadeInRunner, fadeOutRunner, finishRunner, startVideoRunner;
	private Handler fadeInHandler, fadeOutHandler, finishHandler, startVideoHandler;
	private OnFadeListener fadeListener;

	//******************************************************************************
	// newInstance
	//******************************************************************************
	public static VideoFragment newInstance(Camera camera, boolean fullScreen)
	{
		VideoFragment fragment = new VideoFragment();

		Bundle args = new Bundle();
		args.putParcelable(CAMERA, camera);
		args.putBoolean(FULL_SCREEN, fullScreen);
		fragment.setArguments(args);

		return fragment;
	}

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// configure the activity
		super.onCreate(savedInstanceState);

		// initialize the logger
		Utils.initLogFile(getClass().getSimpleName());

		// load the settings and cameras
		Utils.loadData();

		// get the parameters
		camera = getArguments().getParcelable(CAMERA);
		fullScreen = getArguments().getBoolean(FULL_SCREEN);
		Log.info("camera: " + camera.toString());

		// create the fade in handler and runnable
		fadeInHandler = new Handler();
		fadeInRunner = new Runnable()
		{
			@Override
			public void run()
			{
				Animation fadeInName = new AlphaAnimation(0, 1);
				fadeInName.setDuration(FADEIN_ANIMATION_TIME);
				fadeInName.setFillAfter(true);
				Animation fadeInSnapshot = new AlphaAnimation(0, 1);
				fadeInSnapshot.setDuration(FADEIN_ANIMATION_TIME);
				fadeInSnapshot.setFillAfter(true);
				nameView.startAnimation(fadeInName);
				snapshotButton.startAnimation(fadeInSnapshot);
				fadeListener.onStartFadeIn();
			}
		};

		// create the fade out handler and runnable
		fadeOutHandler = new Handler();
		fadeOutRunner = new Runnable()
		{
			@Override
			public void run()
			{
				Animation fadeOutName = new AlphaAnimation(1, 0);
				fadeOutName.setDuration(FADEOUT_ANIMATION_TIME);
				fadeOutName.setFillAfter(true);
				Animation fadeOutSnapshot = new AlphaAnimation(1, 0);
				fadeOutSnapshot.setDuration(FADEOUT_ANIMATION_TIME);
				fadeOutSnapshot.setFillAfter(true);
				nameView.startAnimation(fadeOutName);
				snapshotButton.startAnimation(fadeOutSnapshot);
				fadeListener.onStartFadeOut();
			}
		};

		// create the finish handler and runnable
		finishHandler = new Handler();
		finishRunner = new Runnable()
		{
			@Override
			public void run()
			{
				getActivity().finish();
			}
		};

		// create the start video handler and runnable
		startVideoHandler = new Handler();
		startVideoRunner = new Runnable()
		{
			@Override
			public void run()
			{
				MediaFormat format = decoder.getMediaFormat();
				int videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
				int videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
				textureView.setVideoSize(videoWidth, videoHeight);
			}
		};
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_video, container, false);

		// configure the name
		nameView = (TextView) view.findViewById(R.id.video_name);
		nameView.setText(camera.name);

		// initialize the message
		messageView = (TextView) view.findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// set the texture listener
		textureView = (ZoomPanTextureView)view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);
		textureView.setZoomRange(MIN_ZOOM, MAX_ZOOM);
		textureView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent e)
			{
				switch (e.getAction())
				{
					case MotionEvent.ACTION_DOWN:
						stopFadeOutTimer();
						break;
					case MotionEvent.ACTION_UP:
						if (e.getPointerCount() == 1)
						{
							startFadeOutTimer(false);
						}
						break;
				}
				return false;
			}
		});

		// create the snapshot button
		snapshotButton = (Button) view.findViewById(R.id.video_snapshot);
		snapshotButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				int check = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (check != PackageManager.PERMISSION_GRANTED)
				{
					Log.info("ask for external storage permission");
					requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
										REQUEST_WRITE_EXTERNAL_STORAGE);
				}
				else
				{
					takeSnapshot();
				}
			}
		});

		// move the snapshot button over to account for the navigation bar
		if (fullScreen)
		{
			float scale = getContext().getResources().getDisplayMetrics().density;
			int margin = (int)(5 * scale + 0.5f);
			int extra = Utils.getNavigationBarHeight(getContext(), Configuration.ORIENTATION_LANDSCAPE);
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) snapshotButton.getLayoutParams();
			lp.setMargins(margin, margin, margin + extra, margin);
		}

		return view;
	}

	//******************************************************************************
	// onRequestPermissionsResult
	//******************************************************************************
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && grantResults.length > 0 &&
			grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			Log.info("external storage permission granted");
			takeSnapshot();
		}
	}

	//******************************************************************************
	// onAttach
	//******************************************************************************
	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		try
		{
			Activity activity = (Activity) context;
			fadeListener = (OnFadeListener) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(context.toString() + " must implement OnFadeListener");
		}
	}

	//******************************************************************************
	// onDestroy
	//******************************************************************************
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		finishHandler.removeCallbacks(finishRunner);
	}

	//******************************************************************************
	// onStart
	//******************************************************************************
	@Override
	public void onStart()
	{
		super.onStart();

		// create the decoder thread
		decoder = new DecoderThread();
		decoder.start();
	}

	//******************************************************************************
	// onStop
	//******************************************************************************
	@Override
	public void onStop()
	{
		super.onStop();

		if (decoder != null)
		{
			decoder.interrupt();
			decoder.cleanup();
			decoder = null;
		}
	}

	//******************************************************************************
	// onPause
	//******************************************************************************
	@Override
	public void onPause()
	{
		super.onPause();
		stopFadeOutTimer();
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();
		if (snapshotButton.getVisibility() == View.VISIBLE)
		{
			startFadeOutTimer(false);
		}
	}

	//******************************************************************************
	// onSurfaceTextureAvailable
	//******************************************************************************
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
	{
		if (decoder != null)
		{
			decoder.setSurface(new Surface(surfaceTexture), startVideoHandler, startVideoRunner);
		}
	}

	//******************************************************************************
	// onSurfaceTextureSizeChanged
	//******************************************************************************
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
	{
	}

	//******************************************************************************
	// onSurfaceTextureDestroyed
	//******************************************************************************
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
	{
		if (decoder != null)
		{
			decoder.setSurface(null, null, null);
		}
		return true;
	}

	//******************************************************************************
	// onSurfaceTextureUpdated
	//******************************************************************************
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
	{
	}

	//******************************************************************************
	// startFadeIn
	//******************************************************************************
	public void startFadeIn()
	{
		stopFadeOutTimer();
		fadeInHandler.removeCallbacks(fadeInRunner);
		fadeInHandler.post(fadeInRunner);
		startFadeOutTimer(true);
	}

	//******************************************************************************
	// startFadeOutTimer
	//******************************************************************************
	private void startFadeOutTimer(boolean addFadeInTime)
	{
		fadeOutHandler.removeCallbacks(fadeOutRunner);
		fadeOutHandler.postDelayed(fadeOutRunner, FADEOUT_TIMEOUT + (addFadeInTime ? FADEIN_ANIMATION_TIME : 0));
	}

	//******************************************************************************
	// stopFadeOutTimer
	//******************************************************************************
	private void stopFadeOutTimer()
	{
		fadeOutHandler.removeCallbacks(fadeOutRunner);
	}

	//******************************************************************************
	// takeSnapshot
	//******************************************************************************
	private void takeSnapshot()
	{
		// get the snapshot image
		Bitmap image = textureView.getBitmap();

		// save the image
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String name = camera.network + "_" + camera.name.replaceAll("\\s+", "") + "_" + sdf.format(new Date()) + ".jpg";
		Utils.saveImage(getActivity().getContentResolver(), image, name, null);
		Log.info("takeSnapshot: " + name);

		// play the shutter sound
		MediaActionSound sound = new MediaActionSound();
		sound.play(MediaActionSound.SHUTTER_CLICK);

		// display a message
		String msg = String.format(getString(R.string.image_saved), getString(R.string.app_name));
		Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	//******************************************************************************
	// stop
	//******************************************************************************
	public void stop()
	{
		if (decoder != null)
		{
			messageView.setText(R.string.closing_video);
			messageView.setTextColor(App.getClr(R.color.good_text));
			messageView.setVisibility(View.VISIBLE);
			decoder.interrupt();
			try
			{
				decoder.join(Connection.CONNECT_TIMEOUT * 2);
			}
			catch (Exception ex) {}
			decoder = null;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static int BUFFER_TIMEOUT = 10000;
		private final static int FINISH_TIMEOUT = 5000;
		private final static int BUFFER_SIZE = 16384;
		private final static int NAL_SIZE_INC = 4096;
		private final static int MAX_READ_ERRORS = 300;

		// instance variables
		private MediaCodec decoder = null;
		private MediaFormat format;
		private boolean decoding = false;
		private Surface surface;
		private byte[] buffer = null;
		private long presentationTime = System.nanoTime() / 1000;
		private long presentationTimeInc = 66666;
		private ByteBuffer[] inputBuffers = null;
		private int videoPort;
		private Connection commandConnection = null;
		private Connection videoConnection = null;
		private Handler startVideoHandler;
		private Runnable startVideoRunner;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		public void setSurface(Surface surface, Handler handler, Runnable runner)
		{
			this.surface = surface;
			this.startVideoHandler = handler;
			this.startVideoRunner = runner;
			if (decoder != null)
			{
				if (surface != null)
				{
					boolean newDecoding = decoding;
					if (decoding)
					{
						setDecodingState(false);
					}
					if (format != null)
					{
						try
						{
							decoder.configure(format, surface, null, 0);
						}
						catch (Exception ex) {}
						if (!newDecoding)
						{
							newDecoding = true;
						}
					}
					if (newDecoding)
					{
						setDecodingState(newDecoding);
					}
				}
				else if (decoding)
				{
					setDecodingState(false);
				}
			}
		}

		//******************************************************************************
		// getMediaFormat
		//******************************************************************************
		public MediaFormat getMediaFormat()
		{
			return format;
		}

		//******************************************************************************
		// setDecodingState
		//******************************************************************************
		private synchronized void setDecodingState(boolean newDecoding)
		{
			try
			{
				if (newDecoding != decoding && decoder != null)
				{
					if (newDecoding)
					{
						decoder.start();
					}
					else
					{
						decoder.stop();
					}
					decoding = newDecoding;
				}
			} catch (Exception ex) {}
		}

		//******************************************************************************
		// run
		//******************************************************************************
		@Override
		public void run()
		{
			byte[] nal = new byte[NAL_SIZE_INC];
			int nalLen = 0;
			int numZeroes = 0;
			int numReadErrors = 0;
			boolean gotSPS = false;

			try
			{
				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");

				// create the command connection
				buffer = new byte[BUFFER_SIZE];
				commandConnection = new Connection(camera.address, camera.port, Connection.CONNECT_TIMEOUT);
				if (!commandConnection.isConnected())
				{
					throw new Exception();
				}

				// get the video parameters and configure the decoder
				VideoParams params = commandConnection.getVideoParams();
				format = MediaFormat.createVideoFormat("video/avc", params.width, params.height);
				format.setInteger(MediaFormat.KEY_FRAME_RATE, params.fps);
				format.setInteger(MediaFormat.KEY_BIT_RATE, params.bps);
				decoder.configure(format, surface, null, 0);
				setDecodingState(true);
				inputBuffers = decoder.getInputBuffers();
				presentationTimeInc = 1000000 / params.fps;

				// get the video port and create the video connection
				videoPort = commandConnection.getVideoPort();
				videoConnection = new Connection(camera.address, videoPort, Connection.CONNECT_TIMEOUT);
				if (!videoConnection.isConnected())
				{
					throw new Exception();
				}

				// read from the source
				while (!isInterrupted())
				{
					// read from the stream
					int len = videoConnection.read(buffer);
					if (isInterrupted()) break;
					//Log.info(String.format("len = %d", len));

					// process the input buffer
					if (len > 0)
					{
						numReadErrors = 0;
						for (int i = 0; i < len && !isInterrupted(); i++)
						{
							// add the byte to the NAL
							if (nalLen == nal.length)
							{
								nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
								if (isInterrupted()) break;
								//Log.info(String.format("NAL size: %d", nal.length));
							}
							nal[nalLen++] = buffer[i];

							// process the byte
							if (buffer[i] == 0)
							{
								numZeroes++;
							}
							else
							{
								if (buffer[i] == 1 && numZeroes == 3)
								{
									// get the NAL type
									nalLen -= 4;
									int nalType = (nalLen > 4 && nal[0] == 0 && nal[1] == 0 && nal[2] == 0 && nal[3] == 1) ? (nal[4] & 0x1F) : -1;

									// process the first SPS record we encounter
									if (nalType == 7 && !gotSPS)
									{
										hideMessage();
										startVideoHandler.post(startVideoRunner);
										gotSPS = true;
										if (isInterrupted()) break;
									}

									// reset the buffer for invalid NALs
									if (nalType == -1)
									{
										nal[0] = nal[1] = nal[2] = 0;
										nal[3] = 1;
									}

									// process valid NALs after getting the first SPS record
									else if (gotSPS)
									{
										int index = decoder.dequeueInputBuffer(presentationTimeInc);
										if (isInterrupted()) break;
										if (index >= 0)
										{
											ByteBuffer inputBuffer = inputBuffers[index];
											//ByteBuffer inputBuffer = decoder.getInputBuffer(index);
											inputBuffer.put(nal, 0, nalLen);
											if (isInterrupted()) break;
											decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
											presentationTime += presentationTimeInc;
											if (isInterrupted()) break;
										}
										//Log.info(String.format("dequeueInputBuffer index = %d", index));
									}
									nalLen = 4;
								}
								numZeroes = 0;
							}
						}
					}
					else
					{
						numReadErrors++;
						if (numReadErrors >= MAX_READ_ERRORS)
						{
							setMessage(R.string.error_lost_connection);
							break;
						}
						//Log.info("len == 0");
					}

					// send output buffers to the surface
					if (gotSPS)
					{
						MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
						int index = decoder.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
						if (isInterrupted()) break;
						while (index >= 0)
						{
							decoder.releaseOutputBuffer(index, true);
							if (isInterrupted()) break;
							index = decoder.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
							if (isInterrupted()) break;
						}
						if (isInterrupted()) break;
					}
				}
			}
			catch (Exception ex)
			{
				if (commandConnection == null || !commandConnection.isConnected() ||
					videoConnection == null || !videoConnection.isConnected())
				{
					setMessage(R.string.error_couldnt_connect);
					finishHandler.postDelayed(finishRunner, FINISH_TIMEOUT);
				}
				else
				{
					setMessage(R.string.error_lost_connection);
				}
				Log.error(ex.toString());
				ex.printStackTrace();
			}

			// close everything
			cleanup();
		}

		//******************************************************************************
		// cleanup
		//******************************************************************************
		public synchronized void cleanup()
		{
			// close the video connection
			if (videoConnection != null)
			{
				try
				{
					videoConnection.close();
					Log.info("video connection closed");
				}
				catch (Exception ex) {}
				videoConnection = null;
			}

			// close the command connection
			if (commandConnection != null)
			{
				try
				{
					commandConnection.close();
					Log.info("command connection closed");
				}
				catch (Exception ex) {}
				commandConnection = null;
			}

			// stop the decoder
			if (decoder != null)
			{
				try
				{
					setDecodingState(false);
					decoder.release();
					Log.info("decoder released");
				}
				catch (Exception ex) {}
				decoder = null;
			}
		}

		//******************************************************************************
		// hideMessage
		//******************************************************************************
		private void hideMessage()
		{
			getActivity().runOnUiThread(new Runnable()
			{
				public void run()
				{
					messageView.setVisibility(View.GONE);
				}
			});
		}

		//******************************************************************************
		// setMessage
		//******************************************************************************
		private void setMessage(final int id)
		{
			getActivity().runOnUiThread(new Runnable()
			{
				public void run()
				{
					messageView.setText(id);
					messageView.setTextColor(App.getClr(R.color.bad_text));
					messageView.setVisibility(View.VISIBLE);
				}
			});
		}
	}
}
