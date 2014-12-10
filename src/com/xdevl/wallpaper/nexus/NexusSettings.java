package com.xdevl.wallpaper.nexus;

import com.xdevl.wallpaper.R;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
		{
			if(preference!=null && getString(R.string.key_about).equals(preference.getKey()))
			{
				getFragmentManager().beginTransaction().replace(android.R.id.content,new AboutFragment()).addToBackStack(null).commit() ;
				return true ;
			}
			else return super.onPreferenceTreeClick(preferenceScreen, preference);
		}
	}
	
	public static class AboutFragment extends Fragment
	{
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			return inflater.inflate(R.layout.fragment_about,container,false) ;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(savedInstanceState==null)
			getFragmentManager().beginTransaction().replace(android.R.id.content,new NexusPreference()).commit() ;
	}
	
}
