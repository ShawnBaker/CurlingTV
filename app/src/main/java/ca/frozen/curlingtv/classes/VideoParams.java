// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.curlingtv.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoParams implements Parcelable
{
	public int width = 1280;
	public int height = 720;
	public int fps = 15;
	public int bps = 1000000;

	//******************************************************************************
	// VideoParams
	//******************************************************************************
	public VideoParams()
	{
		//Log.d(TAG, "init: " + toString());
	}

	//******************************************************************************
	// VideoParams
	//******************************************************************************
	public VideoParams(Parcel in)
	{
		readFromParcel(in);
		//Log.d(TAG, "parcel: " + toString());
	}

	//******************************************************************************
	// writeToParcel
	//******************************************************************************
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(fps);
		dest.writeInt(bps);
	}

	//******************************************************************************
	// readFromParcel
	//******************************************************************************
	private void readFromParcel(Parcel in)
	{
		width = in.readInt();
		height = in.readInt();
		fps = in.readInt();
		bps = in.readInt();
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
		public VideoParams createFromParcel(Parcel in)
		{
			return new VideoParams(in);
		}
		public VideoParams[] newArray(int size)
		{
			return new VideoParams[size];
		}
	};
}
