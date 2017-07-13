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

package com.lc.iotmanagerclient.utility;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Gestisce l'interfacciamento con la SettingsActivity.
 * Recupera le informazioni salvate dall'utente nei settings e le predispone in formato adeguato
 * per l'invio di richieste al back-end (eseguite dal JSONFetcher)
 */
public class ParamFetcher {

	//Oggetto di riferimento delle impostazioni dell'applicazione
	private SharedPreferences settings;
	
	/**
	 * Istanzia i settings in base al contesto in input
	 * @param context contesto dell'applicazione android
	 */
	public ParamFetcher(Context context){
		settings = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	
	/**
	 * I valori di autenticazione (username e password) vengono aggiunti alla stringa passata
	 * in argomento, che viene restituita. I valori sono presi dai settings dell'applicazione.
	 * 
	 * @param query_parameters La stringa alla quale si vogliono aggiungere i valori di autenticazione.
	 * @return La stringa originale, modificata.
	 */
    public String getAuthParam(String query_parameters) {

		try {
			//la password viene elaborata con SHA-512 prima di essere aggiunta alla stringa
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			String pwd = settings.getString("settings_pwd", "");
			byte[] digest = md.digest(pwd.getBytes("UTF-8"));

			StringBuilder sb = new StringBuilder();
			for(int i=0; i< digest.length ;i++){
				sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
			}

			query_parameters = this.AddParam(query_parameters,"user",settings.getString("settings_user", ""));
			query_parameters = this.AddParam(query_parameters,"pwd", sb.toString());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

    	return query_parameters;
    }
    
    /**
	 * Il filtro di categoria viene aggiunto alla stringa passata in argomento, che viene
	 * restituita. Il valore è preso dai settings dell'applicazione.
	 *
	 * @param query_parameters La stringa alla quale si vuole aggiungere il filtro.
	 * @return La stringa originale, modificata.
	 */
    public String getCatFilterParam(String query_parameters) {

    	String catFilter = settings.getString("settings_categories", "");
		query_parameters = this.AddParam(query_parameters,"filter",catFilter);

    	return query_parameters;
    }
    
    
    /**
	 * Aggiunge il range di azione definito nelle impostazioni alla stringa parametri passata in
	 * input, e il conseguente bounding box.
	 * Il valore è preso dai settings dell'applicazione.
	 * 
	 * @param query_parameters La stringa alla quale si vuole aggiungere il range.
	 * @param longitudine attuale longitudine del device 
	 * @param latitudine attuale latitudine del device
	 * @return La stringa originale, modificata.
	 */
    public String getRangeParam(String query_parameters, double longitudine, double latitudine) {

    	String range = settings.getString("settings_range", "");
		query_parameters = this.AddParam(query_parameters,"range", range);
    	
    	//calcolo del bounding box
    	GeoLocation myLocation = GeoLocation.fromDegrees(latitudine, longitudine);
    	int kmRange = Integer.parseInt(range) / 1000;
    	GeoLocation[] boundingBox = myLocation.boundingCoordinates(kmRange, 6371.01);

		query_parameters = this.AddParam(query_parameters,"minLon", Double.toString(boundingBox[0].getLongitudeInDegrees()));
		query_parameters = this.AddParam(query_parameters,"maxLon", Double.toString(boundingBox[1].getLongitudeInDegrees()));
		query_parameters = this.AddParam(query_parameters,"minLat", Double.toString(boundingBox[0].getLatitudeInDegrees()));
		query_parameters = this.AddParam(query_parameters,"maxLat", Double.toString(boundingBox[1].getLatitudeInDegrees()));
    	
    	return query_parameters;
    }
    
    /**
     * Verifica che il campo username sia stato impostato nei settings dell'applicazione
     * 
     * @return false se non è stato impostato
     */
    public boolean isSetUsername() {
    	
    	if(settings.getString("settings_user", "")!="")return true;
    	else return false;
    }
    
    /**
     * Verifica che il campo password sia stato impostato nei settings dell'applicazione
     * 
     * @return false se non è stato impostato
     */
    public boolean isSetPassword() {
    	
    	if(settings.getString("settings_pwd", "")!="")return true;
    	else return false;
    }


	/**
	 * Ritorna l'id della categoria attualmente selezionata nei settings
	 */
    public int getSelectedCat(){
    	String catFilter = settings.getString("settings_categories", "");
    	return Integer.parseInt(catFilter);
    }


	/**
	 * Ritorna l'id della città attualmente selezionata nei settings
	 */
    public int getSelectedCity(){
    	String cityFilter = settings.getString("settings_cities", "");
    	return Integer.parseInt(cityFilter);
    }

	/**
	 * Ritorna true se nell'ultima setting activity è stato modificato il campo settings_cities
	 */
    public boolean isSelectedCityChanged(){
    	return settings.getBoolean("settings_cities_changed", false);
    }

	/**
	 * Ritorna se nell'ultima setting activity è stato modificato il campo settings_range
	 */
	public boolean isSelectedRangeChanged(){
		return settings.getBoolean("settings_range_changed", false);
	}
    
    
    /**
     * Ritorna l'URL dell'interfaccia PHP al database
	 * 
     * @return L'url del servizio web che interroga il database
     */
    public String getURLParam() {
		String url = "https://smartcatcher.csr.unibo.it/iotmanager/request.php";
    	return url;
    }

	/**
	 * Formatta la stringa parametri presa in input accodando un nuovo parametro in base al
	 * formato richiesto (key = value &)
	 */
	public String AddParam(String query_parameters, String key, String value) {
		StringBuilder sb = new StringBuilder(query_parameters);
		sb.append(key);
		sb.append("=");
		sb.append(value);
		sb.append("&");

		return sb.toString();
	}

}
