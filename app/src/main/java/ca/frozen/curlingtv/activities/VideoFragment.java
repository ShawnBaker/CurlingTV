// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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
	private final static String TAG = "VideoFragment";
	private final static float MIN_ZOOM = 0.1f;
	private final static float MAX_ZOOM = 10;
	private final static int FADEOUT_TIMEOUT = 5000;
	private final static int FADEOUT_ANIMATION_TIME = 500;
	private final static int FADEIN_ANIMATION_TIME = 400;

	// instance variables
	private Camera camera;
	private boolean fullScreen;
	private DecoderThread decoder;
	private TextureView textureView;
	private TextView nameView, messageView;
	private Button snapshotButton;
	private ScaleGestureDetector scaleDetector;
	private GestureDetector simpleDetector;
	private float scale = 1;
	private float panX = 0;
	private float panY = 0;
	private Runnable fadeInRunner, fadeOutRunner, finishRunner;
	private Handler fadeInHandler, fadeOutHandler, finishHandler;
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

		// load the settings and cameras
		Utils.loadData();

		// get the parameters
		camera = getArguments().getParcelable(CAMERA);
		fullScreen = getArguments().getBoolean(FULL_SCREEN);

		// create the gesture recognizers
		simpleDetector = new GestureDetector(getActivity(), new SimpleListener());
		scaleDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());

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
	}

	//******************************************************************************
	// onCreateView
	//******************************************************************************
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_video, container, false);
		view.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				simpleDetector.onTouchEvent(event);
				scaleDetector.onTouchEvent(event);
				return true;
			}
		});

		// configure the name
		nameView = (TextView) view.findViewById(R.id.video_name);
		nameView.setText(camera.name);

		// initialize the message
		messageView = (TextView) view.findViewById(R.id.video_message);
		messageView.setTextColor(App.getClr(R.color.good_text));
		messageView.setText(R.string.initializing_video);

		// set the texture listener
		textureView = (TextureView) view.findViewById(R.id.video_surface);
		textureView.setSurfaceTextureListener(this);

		// create the snapshot button
		snapshotButton = (Button) view.findViewById(R.id.video_snapshot);
		snapshotButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Bitmap image = textureView.getBitmap();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
				String name = camera.network + "_" + camera.name.replaceAll("\\s+", "") + "_" + sdf.format(new Date()) + ".jpg";
				String url = Utils.saveImage(getActivity().getContentResolver(), image, name, null);
				MediaActionSound sound = new MediaActionSound();
				sound.play(MediaActionSound.SHUTTER_CLICK);
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
			decoder.setSurface(new Surface(surfaceTexture));
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
			decoder.setSurface(null);
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
	// adjustPan
	//******************************************************************************
	private boolean adjustPan(float scale)
	{
		boolean adjusted = false;
		int w = textureView.getWidth();
		int h = textureView.getHeight();
		float dx = (w * scale - w) / 2;
		float dy = (h * scale - h) / 2;
		if (panX < -dx)
		{
			panX = -dx;
			adjusted = true;
		}
		if (panX > dx)
		{
			panX = dx;
			adjusted = true;
		}
		if (panY < -dy)
		{
			panY = -dy;
			adjusted = true;
		}
		if (panY > dy)
		{
			panY = dy;
			adjusted = true;
		}
		return adjusted;
	}

	////////////////////////////////////////////////////////////////////////////////
	// SimpleListener
	////////////////////////////////////////////////////////////////////////////////
	private class SimpleListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onDown(MotionEvent e)
		{
			startFadeOutTimer(false);
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			startFadeOutTimer(false);
			if (scale > 1)
			{
				panX -= distanceX;
				panY -= distanceY;
				adjustPan(scale);
				textureView.setTranslationX(panX);
				textureView.setTranslationY(panY);
				return true;
			}
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			startFadeOutTimer(false);
			scale = 1;
			textureView.setScaleX(scale);
			textureView.setScaleY(scale);
			panX = panY = 0;
			textureView.setTranslationX(panX);
			textureView.setTranslationY(panY);
			return true;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// ScaleListener
	////////////////////////////////////////////////////////////////////////////////
	private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener
	{
		float startScale = 1;

		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			float newScale = startScale * detector.getScaleFactor();
			newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));
			textureView.setScaleX(newScale);
			textureView.setScaleY(newScale);
			if (newScale > 1)
			{
				if (adjustPan(newScale))
				{
					textureView.setTranslationX(panX);
					textureView.setTranslationY(panY);
				}
			}
			else if (panX != 0 || panY != 0)
			{
				panX = panY = 0;
				textureView.setTranslationX(panX);
				textureView.setTranslationY(panY);
			}
			return false;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			stopFadeOutTimer();
			startScale = scale;
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector)
		{
			float newScale = startScale * detector.getScaleFactor();
			scale = Math.max(0.1f, Math.min(newScale, 10));
			startFadeOutTimer(false);
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// DecoderThread
	////////////////////////////////////////////////////////////////////////////////
	private class DecoderThread extends Thread
	{
		// local constants
		private final static String TAG = "DecoderThread";
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
		private int videoPort;
		private Connection commandConnection = null;
		private Connection videoConnection = null;

		//******************************************************************************
		// setSurface
		//******************************************************************************
		public void setSurface(Surface surface)
		{
			this.surface = surface;
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
			long presentationTime = System.nanoTime() / 1000;
			boolean gotSPS = false;
			boolean gotHeader = false;
			ByteBuffer[] inputBuffers = null;

			try
			{
				// create the decoder
				decoder = MediaCodec.createDecoderByType("video/avc");

				// create the command connection
				buffer = new byte[BUFFER_SIZE];
				commandConnection = new Connection(camera.address, camera.port);
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

				// get the video port and create the video connection
				videoPort = commandConnection.getVideoPort();
				videoConnection = new Connection(camera.address, videoPort);
				if (!videoConnection.isConnected())
				{
					throw new Exception();
				}
				//hideMessage();

				// read from the source
				while (!Thread.interrupted())
				{
					// read from the stream
					int len = videoConnection.read(buffer);
					if (Thread.interrupted()) break;
					//Log.d(TAG, String.format("len = %d", len));

					// process the input buffer
					if (len > 0)
					{
						numReadErrors = 0;
						for (int i = 0; i < len && !Thread.interrupted(); i++)
						{
							if (buffer[i] == 0)
							{
								numZeroes++;
							}
							else
							{
								if (buffer[i] == 1)
								{
									if (numZeroes == 3)
									{
										if (gotHeader)
										{
											nalLen -= numZeroes;
											if (!gotSPS && (nal[numZeroes + 1] & 0x1F) == 7)
											{
												hideMessage();
												gotSPS = true;
											}
											if (gotSPS)
											{
												int index = decoder.dequeueInputBuffer(BUFFER_TIMEOUT);
												if (Thread.interrupted()) break;
												if (index >= 0)
												{
													ByteBuffer inputBuffer = inputBuffers[index];
													//ByteBuffer inputBuffer = decoder.getInputBuffer(index);
													inputBuffer.put(nal, 0, nalLen);
													decoder.queueInputBuffer(index, 0, nalLen, presentationTime, 0);
													presentationTime += 66666;
													if (Thread.interrupted()) break;
												}
											}
											//Log.d(TAG, String.format("NAL: %d  %d", nalLen, index));
										}
										for (int j = 0; j < numZeroes; j++)
										{
											nal[j] = 0;
										}
										nalLen = numZeroes;
										gotHeader = true;
									}
								}
								numZeroes = 0;
							}

							// add the byte to the NAL
							if (gotHeader)
							{
								if (nalLen == nal.length)
								{
									nal = Arrays.copyOf(nal, nal.length + NAL_SIZE_INC);
									//Log.d(TAG, String.format("NAL size: %d", nal.length));
								}
								nal[nalLen++] = buffer[i];
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
						//Log.d(TAG, "len == 0");
					}

					// send an output buffer to the surface
					if (Thread.interrupted()) break;
					MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
					int index = decoder.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
					if (Thread.interrupted()) break;
					if (index >= 0)
					{
						decoder.releaseOutputBuffer(index, true);
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
				//Log.d(TAG, ex.toString());
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
			Log.d(TAG, "video connection");
			if (videoConnection != null)
			{
				Log.d(TAG, "video connection not null");
				try
				{
					videoConnection.close();
					Log.d(TAG, "video connection closed");
				}
				catch (Exception ex) {}
				videoConnection = null;
			}

			// close the command connection
			Log.d(TAG, "command connection");
			if (commandConnection != null)
			{
				Log.d(TAG, "command connection not null");
				try
				{
					commandConnection.close();
					Log.d(TAG, "command connection closed");
				}
				catch (Exception ex) {}
				commandConnection = null;
			}

			// stop the decoder
			Log.d(TAG, "decoder");
			if (decoder != null)
			{
				Log.d(TAG, "decoder not null");
				try
				{
					setDecodingState(false);
					decoder.release();
					Log.d(TAG, "decoder released");
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
