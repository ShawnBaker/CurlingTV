// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.classes;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import ca.frozen.curlingtv.App;
import ca.frozen.curlingtv.R;

public class Settings implements Parcelable
{
	// instance variables
	public int port;

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings()
	{
		port = App.getInt(R.integer.default_port);
		//Log.info("init: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(Parcel in)
	{
		readFromParcel(in);
		//Log.info("parcel: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(Settings settings)
	{
		port = settings.port;
		//Log.info("settings: " + toString());
	}

	//******************************************************************************
	// Settings
	//******************************************************************************
	public Settings(JSONObject obj)
	{
		try
		{
			port = obj.getInt("port");
		}
		catch (JSONException ex)
		{
			port = App.getInt(R.integer.default_port);
		}
		//Log.info("json: " + toString());
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(port);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		port = in.readInt();
	}

	//******************************************************************************
	// describeContents
	//******************************************************************************
	public int describeContents()
	{
		return 0;
	}

	//******************************************************************************
	// Parcelable.Creator
	//******************************************************************************
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
	{
		public Settings createFromParcel(Parcel in)
		{
			return new Settings(in);
		}
		public Settings[] newArray(int size)
		{
			return new Settings[size];
		}
	};

	//******************************************************************************
	// toString
	//******************************************************************************
	@Override
	public String toString()
	{
		return "Settings: " + port;
	}

	//******************************************************************************
	// toJson
	//******************************************************************************
	public JSONObject toJson()
	{
		try
		{
			JSONObject obj = new JSONObject();
			obj.put("port", port);
			return obj;
		}
		catch(JSONException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
