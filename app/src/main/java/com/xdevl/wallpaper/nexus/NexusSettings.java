/*
 * Copyright (C) 2023 XdevL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.xdevl.wallpaper.nexus;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
			View root=inflater.inflate(R.layout.fragment_about,container,false) ;
			((TextView)root.findViewById(R.id.aosp_about)).setMovementMethod(LinkMovementMethod.getInstance()) ;
			return root ;
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
