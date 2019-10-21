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

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * Classe SCO per gestire oggetti di tipo generico o sconosciuto
 */
public class SCODefault extends SCO implements Parcelable{

	//Id della categoria dell'oggetto sul data base remoto
	public static final int SCO_ID = -1;
	
	private int idCategoria;
	private int id;
	private String toString;
	
	/**
	 * 
	 * Costruttore
	 * 
	 * Riempie i campi dell'oggetto cercando i corrispettivi nella stringa Json parsata passata in input
	 * La stringa json è il risultato di una richiesta al server di singolo oggetto da db.
	 * @param parsed_json
	 */
	SCODefault(JSONArray parsed_json){
		try {
			this.idCategoria = parsed_json.getJSONObject(0).getInt("idCategoria");
			this.id = parsed_json.getJSONObject(0).getInt("id");
			
			toString = "";
			Iterator it = parsed_json.getJSONObject(0).keys();
			while (it.hasNext()){
				String property = it.next().toString();
				String value = parsed_json.getJSONObject(0).getString(property);
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

		return SCO.getIcon(SCODefault.SCO_ID);
	}


	/**
	 * CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */
	
	/**
	 * Metodo che ritorna la view da mostrare nella ObjectDetailsActivity
	 */
	@Override
	public View createView(Context context){
		
		
		LinearLayout mainView = new LinearLayout(context);
		mainView.setOrientation(LinearLayout.VERTICAL);
		mainView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mainView.setWeightSum(1.0f);
		
		LinearLayout iconLayout = new LinearLayout(context);
		iconLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,0.1f));
		iconLayout.setGravity(Gravity.CENTER);
		
		ImageView iconView = new ImageView(context);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			iconView.setBackground(ContextCompat.getDrawable(context, SCODefault.getIcon()));
		}
		else {
			iconView.setBackgroundDrawable(ContextCompat.getDrawable(context, SCODefault.getIcon()));
		}
		iconView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.addView(iconView);
		mainView.addView(iconLayout);
		
		TextView tvDetails = new TextView(context);
		tvDetails.setPadding(15, 10, 0, 0);
		tvDetails.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvDetails.setText(toString);
		mainView.addView(tvDetails);
		
		ScrollView root = new ScrollView(context);
		root.addView(mainView);
		
		return root;
		
	}

	/**
	 * Lo stato del sensore è sconosciuto
	 */
	@Override
	public String resolveStatus() {
		return null;
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
	
	public SCODefault(Parcel in) {
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
	
	public static final Parcelable.Creator<SCODefault> CREATOR =
	    	new Parcelable.Creator<SCODefault>() {
	            public SCODefault createFromParcel(Parcel in) {
	                return new SCODefault(in);
	            }
	 
	            public SCODefault[] newArray(int size) {
	                return new SCODefault[size];
	            }
	};

	        
	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE 
	*/
	        
}