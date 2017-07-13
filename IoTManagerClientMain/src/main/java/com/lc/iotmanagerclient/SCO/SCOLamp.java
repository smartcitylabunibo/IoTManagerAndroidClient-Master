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

package com.lc.iotmanagerclient.SCO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.utility.DisplayConvertionUtility;


/**
 * Classe SCO per gestire oggetti di tipo lamp
 */
public class SCOLamp extends SCO implements Parcelable{

	//Id della categoria dell'oggetto sul data base remoto
	public static final int SCO_ID = 3;
	
	private int idCategoria;
	private int id;
	private int stato;
	private String toString;
	
	private String nome;
	private String ubicazione;
	private String descrizioneLamp;
	private String descrizioneLampada;
	private String descrizioneArmatura;
	private String condensatore;
	private String reattore;
	private String supporto;


	/**
	 * Riempie i campi dell'oggetto cercando i corrispettivi nella stringa Json passata in input
	 * La stringa json è il risultato di una richiesta al server di singolo oggetto da db.
	 */
	SCOLamp(JSONArray parsed_json){
		try {
			this.idCategoria = parsed_json.getJSONObject(0).getInt("idCategoria");
			this.id = parsed_json.getJSONObject(0).getInt("id");
			this.stato =  parsed_json.getJSONObject(0).getInt("STATUS");
			this.nome =  parsed_json.getJSONObject(0).getString("NOME");
			this.ubicazione =  parsed_json.getJSONObject(0).getString("UBICAZIONE");
			this.descrizioneLamp =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_SYRA");
			this.descrizioneLampada =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_LAMPADA");
			this.descrizioneArmatura =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_ARMATURA");
			this.condensatore =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_CONDENSATORE");
			this.reattore =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_REATTORE");
			this.supporto =  parsed_json.getJSONObject(0).getString("DESCRIZIONE_SUPPORTO");

			Map<String, String> fieldDetails = new LinkedHashMap<String, String>();
			fieldDetails.put("NOME", this.nome);
			fieldDetails.put("UBICAZIONE", this.ubicazione);
			fieldDetails.put("MODELLO", this.descrizioneLamp);
			fieldDetails.put("LAMPADA", this.descrizioneLampada);
			fieldDetails.put("ARMATURA", this.descrizioneArmatura);
			fieldDetails.put("REATTORE", this.reattore);
			fieldDetails.put("CONDENSATORE", this.condensatore);
			fieldDetails.put("SUPPORTO", this.supporto);

			toString = "";
			for (Entry<String, String> obj : fieldDetails.entrySet()) {
				String property = obj.getKey().toString();
				String value = obj.getValue().toString();
				toString += property +" : "+value +"\n";
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Ritorna l'icona associata a questo tipo di sensore riferendosi al metodo statico della classe padre astratta
	 */
	public static int getIcon(){

		return SCO.getIcon(SCOLamp.SCO_ID);
	}



	/**
	 * CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */

	/**
	 * Metodo che ritorna la view da mostrare nella ObjectDetailsActivity adattata per gli oggetti Lamp
	 */
	@Override
	public View createView(Context context){
		
		LinearLayout mainView = new LinearLayout(context);
		mainView.setOrientation(android.widget.LinearLayout.VERTICAL);
		mainView.setWeightSum(1.0f);
		
		LinearLayout iconLayout = new LinearLayout(context);
		iconLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.setGravity(Gravity.CENTER);
		
		ImageView iconView = new ImageView(context);
		iconView.setBackground(ContextCompat.getDrawable(context, SCOLamp.getIcon()));
		if (stato != 0) iconView.setImageResource(R.drawable.ic_error_status);
		iconView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.addView(iconView);
		mainView.addView(iconLayout);
		
		TextView tvName = new TextView(context);
		tvName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvName.setText("LAMPADA");
		tvName.setTextSize(18f);
		tvName.setGravity(Gravity.CENTER);
		tvName.setTextColor(ContextCompat.getColor(context,R.color.Gold));
		mainView.addView(tvName);
		
		TextView tvDetails = new TextView(context);
		tvDetails.setPadding(15, 10, 0, 0);
		tvDetails.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvDetails.setText(toString);
		mainView.addView(tvDetails);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);

		TextView tvStatus = new TextView(context);
		tvStatus.setPadding(15, 10, 0, 0);
		tvStatus.setLayoutParams(params);
		tvStatus.setText("STATO: " + resolveStatus());
		tvStatus.setTextSize(18f);
		if (SCO.getStatus(idCategoria, stato)) {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Green));
		}else {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Red));
		}
		mainView.addView(tvStatus);
		
		ScrollView root = new ScrollView(context);
		root.addView(mainView);
		
		return root;
		
	}

	/**
	 * Metodo che traduce il codice di stato del sensore con una descrizione da stampare a video
	 */
	@Override
	public String resolveStatus() {
		switch (stato) {
		case 0:
			return "Lampada spenta";
		case 1:
			return "Lampada accesa";
		case 2:
			return "Lampada in dimmer";
		case -1:
			return "Problema generico";
		case -2:
			return "Lampada non installata";
		case -3:
			return "Lampada non risponde";
		case 8:
			return "Lampada spenta con problema";
		case 9:
			return "Lampada accesa con problema";
		case 10:
			return "Lampada in dimmer con problema";
		default:
			return "Stato sconosciuto";
		}
	}

	/**
	 * Metodo che restituisce l'identificativo univoco del sensore sul database
	 */
	@Override
	public int getIDObj() {
		return id;
	}


	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */


	/**
	 * SEGUE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE 
	*/
	
	public SCOLamp(Parcel in) {
		readFromParcel(in);
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(idCategoria);
	}
	
	private void readFromParcel(Parcel in) {
		idCategoria = in.readInt();
	}
	
	public static final Parcelable.Creator<SCOLamp> CREATOR =
	    	new Parcelable.Creator<SCOLamp>() {
	            public SCOLamp createFromParcel(Parcel in) {
	                return new SCOLamp(in);
	            }
	 
	            public SCOLamp[] newArray(int size) {
	                return new SCOLamp[size];
	            }
	};

	        
	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE 
	*/
	        
}