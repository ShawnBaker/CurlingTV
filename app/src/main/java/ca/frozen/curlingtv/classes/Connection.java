// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.classes;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.R;

public class Connection
{
	// local constants
	private final static String TAG = "Connection";
	private final static int SOCKET_TIMEOUT = 200;

	// instance variables
	private Socket socket = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	//******************************************************************************
	// Connection
	//******************************************************************************
	public Connection(String address, int port)
	{
		try
		{
			socket = new Socket();
			InetSocketAddress socketAddress = new InetSocketAddress(address, port);
			socket.connect(socketAddress, SOCKET_TIMEOUT);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
		}
		catch (Exception ex)
		{
			close();
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
		catch (IOException ex) {}
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
				Log.d(TAG, "name = " + name);
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
					Log.d(TAG, "params = " + params.width + " " + params.height + " " + params.fps + " " + params.bps);
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
				Log.d(TAG, "port = " + port);
			}
		}
		return port;
	}
}
