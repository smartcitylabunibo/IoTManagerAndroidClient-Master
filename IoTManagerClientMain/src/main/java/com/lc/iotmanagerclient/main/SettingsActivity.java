/*
        IoT Manager Android Client (master)
        Copyright (C) 2017  Luca Calderoni, Antonio Magnani,
        University of Bologna

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU Lesser General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU Lesser General Public License for more details.

        You should have received a copy of the GNU Lesser General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lc.iotmanagerclient.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.lc.iotmanagerclient.R;


/**
 * Impostazioni dell'applicazione.
 * Activity di gestione delle opzioni dell'applicazione.
 * * Layout di riferimento "res/xml/preferences.xml".
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	/**
	 * Creazione dell'activity.
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		Intent intent = getIntent();
		
		/**
		 * Aggiorna le categorie disponibili in base ai valori segnalati dalla main activity
		 * la quale a sua volta li riceve dal service
		 */
		if(intent.getCharSequenceArrayExtra("CatEntriesList") != null && intent.getCharSequenceArrayExtra("CatValuesList") != null){
			CharSequence[] entries = intent.getCharSequenceArrayExtra("CatEntriesList");
			CharSequence[] entryValues = intent.getCharSequenceArrayExtra("CatValuesList");
			ListPreference lp = (ListPreference) findPreference("settings_categories");
			lp.setEntries(entries);
			lp.setEntryValues(entryValues);
		}
		
		/**
		 * Aggiorna le città disponibili in base ai valori segnalati dalla main activity
		 * la quale a sua volta li riceve dal service
		 */
		if(intent.getCharSequenceArrayExtra("CitiesEntriesList") != null && intent.getCharSequenceArrayExtra("CitiesValuesList") != null){
			CharSequence[] entries = intent.getCharSequenceArrayExtra("CitiesEntriesList");
			CharSequence[] entryValues = intent.getCharSequenceArrayExtra("CitiesValuesList");
			ListPreference lp = (ListPreference) findPreference("settings_cities");
			lp.setEntries(entries);
			lp.setEntryValues(entryValues);
		}
		
	}

	/**
	 * Metodo invocato quando viene cambiata una sharedPreferences.
	 * Controlla se è setting_cities o settings_range e segnala il cambiamento.
 	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Editor editor;
		if(key.equalsIgnoreCase("settings_cities")){
			editor = sharedPreferences.edit();
			editor.putBoolean("settings_cities_changed", true);
			editor.commit();
		}
		if(key.equalsIgnoreCase("settings_range")){
			editor = sharedPreferences.edit();
			editor.putBoolean("settings_range_changed", true);
			editor.commit();
		}
	}

	/**
	 * Gestione dell'evento onResume dell'activity.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean("settings_cities_changed", false);
		editor.putBoolean("settings_range_changed", false);
		editor.commit();
	}

	/**
	 * Gestione dell'evento onPause dell'activity.
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

	}
	
}
