package com.xdevl.wallpaper.nexus;

import com.xdevl.wallpaper.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class NexusSettings extends Activity
{
	public static class NexusPreference extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState) ;
			addPreferencesFromResource(R.xml.preference_nexus) ;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nexus_settings) ;
	}
	
}
