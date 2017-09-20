// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.classes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import ca.frozen.library.classes.Log;
import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.R;

public class Connection
{
	// public constants
	public final static int QUERY_TIMEOUT = 500;
	public final static int COMMAND_TIMEOUT = 1000;
	public final static int VIDEO_TIMEOUT = 400;
	public final static int IMAGE_TIMEOUT = 400;

	// instance variables
	private Socket socket = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	//******************************************************************************
	// Connection
	//******************************************************************************
	public Connection(String address, int port, int timeout)
	{
		try
		{
			socket = new Socket();
			InetSocketAddress socketAddress = new InetSocketAddress(address, port);
			socket.connect(socketAddress, timeout);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
		}
		catch (Exception ex)
		{
			Log.error("Connection: " + ex.toString());
			close();
		}
	}

	//******************************************************************************
	// read
	//******************************************************************************
	public int read(byte[] buffer, int offset, int count)
	{
		try
		{
			return (inputStream != null) ? inputStream.read(buffer, offset, count) : 0;
		}
		catch (IOException ex)
		{
			Log.error("read: " + ex.toString());
			return 0;
		}
	}

	//******************************************************************************
	// read
	//******************************************************************************
	public int read(byte[] buffer)
	{
		try
		{
			return (inputStream != null) ? inputStream.read(buffer) : 0;
		}
		catch (IOException ex)
		{
			Log.error("read: " + ex.toString());
			return 0;
		}
	}

	//******************************************************************************
	// write
	//******************************************************************************
	public void write(byte[] buffer, int offset, int count)
	{
		try
		{
			if (outputStream != null)
			{
				outputStream.write(buffer, offset, count);
			}
		}
		catch (IOException ex)
		{
			Log.error("write: " + ex.toString());
		}
	}

	//******************************************************************************
	// write
	//******************************************************************************
	public void write(byte[] buffer)
	{
		write(buffer, 0, buffer.length);
	}

	//******************************************************************************
	// write
	//******************************************************************************
	public void write(String str)
	{
		write(str.getBytes());
	}

	//******************************************************************************
	// isConnected
	//******************************************************************************
	public boolean isConnected()
	{
		return (socket != null) ? socket.isConnected() : false;
	}

	//******************************************************************************
	// close
	//******************************************************************************
	public void close()
	{
		if (inputStream != null)
		{
			try
			{
				inputStream.close();
			}
			catch (Exception ex) {}
			inputStream = null;
		}
		if (outputStream != null)
		{
			try
			{
				outputStream.close();
			}
			catch (Exception ex) {}
			outputStream = null;
		}
		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch (Exception ex) {}
			socket = null;
		}
	}

	//******************************************************************************
	// getName
	//******************************************************************************
	public String getName()
	{
		String name = "";
		if (isConnected())
		{
			write(App.getStr(R.string.get_name));
			byte[] buffer = new byte[1024];
			int len = read(buffer);
			if (len > 0)
			{
				name = new String(buffer, 0, len);
				Log.info("getName: " + name);
			}
		}
		return name;
	}

	//******************************************************************************
	// getVideoParams
	//******************************************************************************
	public VideoParams getVideoParams()
	{
		VideoParams params = new VideoParams();
		try
		{
			if (isConnected())
			{
				write(App.getStr(R.string.get_video_params));
				byte[] buffer = new byte[1024];
				int len = read(buffer);
				if (len > 0)
				{
					String str = new String(buffer, 0, len);
					String[] parts = str.split(",");
					params.width = Integer.parseInt(parts[0]);
					params.height = Integer.parseInt(parts[1]);
					params.fps = Integer.parseInt(parts[2]);
					params.bps = Integer.parseInt(parts[3]);
					Log.info("getVideoParams: " + params.toString());
				}
			}
		}
		catch (Exception ex) {}
		return params;
	}

	//******************************************************************************
	// getVideoPort
	//******************************************************************************
	public int getVideoPort()
	{
		int port = 0;
		if (isConnected())
		{
			write(App.getStr(R.string.get_video_port));
			byte[] buffer = new byte[1024];
			int len = read(buffer);
			if (len > 0)
			{
				port = Integer.parseInt(new String(buffer, 0, len));
				Log.info("video port = " + port);
			}
		}
		return port;
	}

	//******************************************************************************
	// getImagePort
	//******************************************************************************
	public int getImagePort()
	{
		int port = 0;
		if (isConnected())
		{
			write(App.getStr(R.string.get_image_port));
			byte[] buffer = new byte[1024];
			int len = read(buffer);
			if (len > 0)
			{
				port = Integer.parseInt(new String(buffer, 0, len));
				Log.info("image port = " + port);
			}
		}
		return port;
	}
}
