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
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.main.TabMainActivity;


/**
 * Classe SCO per gestire oggetti di tipo ArLu (Armadio Luci)
 * Questo tipo di sensore è un concentratore, quindi può avere dei sensori figli ad esso relativi
 * * Layout di riferimento: "res/layout/details_with_sons_layout.xml", "res/layout/sons_layout", "res/layout/sons_line_layout", "res/layout/loading_layout".
 */
public class SCOArLu extends SCO implements Parcelable, View.OnClickListener{

	//Id della categoria dell'oggetto sul data base remoto
	public static final int SCO_ID = 2;

	private int id;
	private int idCategoria;
	private int parent_int;
	private int pswcat;
	private String id_str;
	private String parent;
	private String nome; 
	private int options;
	private String open;
	private int status;
	private int selected;
	private String latitude;
	private String longitude;
	private String toString;
	private String numeroTelefonico;
	private String ubicazione;

	private TabMainActivity context;
	
	
	/**
	 * Riempie i campi dell'oggetto cercando i corrispettivi nella stringa Json passata in input
	 * La stringa json è il risultato di una richiesta al server di singolo oggetto da db.
	 */
	SCOArLu(JSONArray parsed_json){
		
		try {
			
			this.id = parsed_json.getJSONObject(0).getInt("id");
			this.idCategoria = parsed_json.getJSONObject(0).getInt("idCategoria");
			this.parent_int = parsed_json.getJSONObject(0).getInt("PARENT_INT");
			this.id_str =  parsed_json.getJSONObject(0).getString("ID_STR");
			this.parent =  parsed_json.getJSONObject(0).getString("PARENT");
			this.nome =  parsed_json.getJSONObject(0).getString("NOME");
			this.options =  parsed_json.getJSONObject(0).getInt("options");
			this.open =  parsed_json.getJSONObject(0).getString("OPEN");
			this.status =  parsed_json.getJSONObject(0).getInt("STATUS");
			this.selected =  parsed_json.getJSONObject(0).getInt("SELECTED");
			this.latitude =  parsed_json.getJSONObject(0).getString("LATITUDE");
			this.numeroTelefonico =  parsed_json.getJSONObject(0).getString("NUMERO_TELEFONICO");
			this.ubicazione =  parsed_json.getJSONObject(0).getString("UBICAZIONE");

			Map<String, String> fieldDetails = new LinkedHashMap<String, String>();
			fieldDetails.put("NOME IMPIANTO", this.nome);
			fieldDetails.put("UBICAZIONE", this.ubicazione);
			fieldDetails.put("NUMERO TELEFONICO", this.numeroTelefonico);

			toString = "";
			for (Entry<String, String> obj : fieldDetails.entrySet()) {
				String property = obj.getKey().toString();
				String value = obj.getValue().toString();
				toString += property +" : "+value +"\n";
			}
			
			Log.i(this.getClass().getSimpleName(), toString);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}


	/**
	 * Ritorna l'icona associata a questo tipo di sensore riferendosi al metodo statico della classe padre astratta
	 */
	public static int getIcon(){

		return SCO.getIcon(SCOArLu.SCO_ID);
	}



	/**
	 * CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */

	/**
	 * Metodo che ritorna la view da mostrare nella ObjectDetailsActivity adattata per gli ArLu
	 */
	@Override
	public View createView(Context context){
		this.context = (TabMainActivity) context;
		View root = LayoutInflater.from(context).inflate(R.layout.details_with_sons_layout, null);
		
		//view dove inserire informazioni di dettaglio
		LinearLayout details = (LinearLayout) root.findViewById(R.id.detailsToFill);
		LinearLayout iconLayout = new LinearLayout(context);
		iconLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.setGravity(Gravity.CENTER);
		
		ImageView iconView = new ImageView(context);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			iconView.setBackground(ContextCompat.getDrawable(context, SCOArLu.getIcon()));
		}
		else {
			iconView.setBackgroundDrawable(ContextCompat.getDrawable(context, SCOArLu.getIcon()));
		}
		if (status != 0) iconView.setImageResource(R.drawable.ic_error_status);
		iconView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.addView(iconView);
		details.addView(iconLayout);
		
		TextView tvName = new TextView(context);
		tvName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvName.setText("ARLU");
		tvName.setTextSize(18f);
		tvName.setGravity(Gravity.CENTER);
		tvName.setTextColor(ContextCompat.getColor(context,R.color.Gold));
		details.addView(tvName);
		
		TextView tvDetails = new TextView(context);
		tvDetails.setPadding(15, 10, 0, 0);
		tvDetails.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvDetails.setTextSize(18f);
		tvDetails.setTextColor(ContextCompat.getColor(context,R.color.White));
		tvDetails.setText(toString);
		details.addView(tvDetails);
		
		TextView tvStatus = new TextView(context);
		tvStatus.setPadding(15, 10, 0, 0);
		tvStatus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvStatus.setText("STATO: " + resolveStatus());
		tvStatus.setTextSize(18f);
		if (SCO.getStatus(idCategoria, status)) {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Green));
		}else {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Red));
		}
		details.addView(tvStatus);
		
		//se ho sensori figli popolo anche la relativa parte sottostante
		SCLO[] sons = this.getSonsList();
		if(sons != null){
			LinearLayout sonsRoot =  (LinearLayout) root.findViewById(R.id.sons_linear_layout);
			int j = 0;
			View line = null;
			View box1 = null, box2 = null, box3 = null, box4 = null;
			TextView title;
			ImageView image;
			int tot = sons.length-1;
			for(SCLO son:sons){
				
				if(j%4==0){
					//creo linea, istanzio elementi, riempio primo elemento
					line = LayoutInflater.from(context).inflate(R.layout.sons_line_layout, null);
					box1 = line.findViewById(R.id.firstSon);
					box2 = line.findViewById(R.id.secondSon);
					box3 = line.findViewById(R.id.thirdSon);
					box4 = line.findViewById(R.id.fourthSon);
					title = (TextView) box1.findViewById(R.id.sonTitle);
					image = (ImageView) box1.findViewById(R.id.sonImage);
					//imposto nome e immagine
					title.setText(son.getNome());
					image.setBackgroundResource(son.getIcon());
					if (!SCO.getStatus(son.getCategoria(), son.getStato())) image.setImageResource(R.drawable.ic_error_status);
					box1.setTag(son);
					box1.setOnClickListener(this);
					if(j == tot){
						box2.setVisibility(View.INVISIBLE);
						box3.setVisibility(View.INVISIBLE);
						box4.setVisibility(View.INVISIBLE);
					}
				}
				if(j%4==1){
					//creo secondo elemento
					title = (TextView) box2.findViewById(R.id.sonTitle);
					image = (ImageView) box2.findViewById(R.id.sonImage);
					//imposto nome e immagine
					title.setText(son.getNome());
					image.setBackgroundResource(son.getIcon());
					if (!SCO.getStatus(son.getCategoria(), son.getStato())) image.setImageResource(R.drawable.ic_error_status);
					box2.setTag(son);
					box2.setOnClickListener(this);
					if(j == tot){
						box3.setVisibility(View.INVISIBLE);
						box4.setVisibility(View.INVISIBLE);
					}
				}
				if(j%4==2){
					//creo terzo elemento
					title = (TextView) box3.findViewById(R.id.sonTitle);
					image = (ImageView) box3.findViewById(R.id.sonImage);
					//imposto nome e immagine
					title.setText(son.getNome());
					image.setBackgroundResource(son.getIcon());
					if (!SCO.getStatus(son.getCategoria(), son.getStato())) image.setImageResource(R.drawable.ic_error_status);
					box3.setTag(son);
					box3.setOnClickListener(this);
					if(j == tot){
						box4.setVisibility(View.INVISIBLE);
					}
				}
				if(j%4==3){
					//creo quarto elemento
					title = (TextView) box4.findViewById(R.id.sonTitle);
					image = (ImageView) box4.findViewById(R.id.sonImage);
					//imposto nome e immagine
					title.setText(son.getNome());
					image.setBackgroundResource(son.getIcon());
					if (!SCO.getStatus(son.getCategoria(), son.getStato())) image.setImageResource(R.drawable.ic_error_status);
					box4.setTag(son);
					box4.setOnClickListener(this);
					sonsRoot.addView(line);
				}
				j++;
			}
			//se sono uscito prima di aggiungere la riga la aggiungo (ci sarà una riga con meno di 4 elementi)
			if((j-1)%4!=3) sonsRoot.addView(line);	
		}else{
			//caso in cui non ho figli
			root.findViewById(R.id.sons_master_layout).setVisibility(View.GONE);
			root.findViewById(R.id.loading_view).setVisibility(View.VISIBLE);
		}
		
		return root;
	}

	/**
	 * Metodo che traduce il codice di stato del sensore con una descrizione da stampare a video
	 */
	@Override
	public String resolveStatus() {
		switch (status) {
		case 0:
			return "Impianto spento";
		case 1:
			return "Impianto acceso";
		case -1:
			return "Guasto presente (solamente mancanza di tensioni su ArLu o sui pl)";
		case 3:
			return "ArLu non configurato";
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
	
	public void onClick(View v) {
		SCLO obj = (SCLO) v.getTag();
		Log.i("SCOArLu", "Click to: "+obj.getId() + " name: "+obj.getNome());
		if(context != null) context.onItemSelected(obj.getId(), obj.getCategoria(), true);
	}


	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */


	
	/**
	 * SEGUE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE 
	*/
	
	public SCOArLu(Parcel in) {
		readFromParcel(in);
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(idCategoria);
	}
	
	private void readFromParcel(Parcel in) {
		id = in.readInt();
		idCategoria = in.readInt();
	}
	
	public static final Parcelable.Creator<SCOArLu> CREATOR =
	    	new Parcelable.Creator<SCOArLu>() {
	            public SCOArLu createFromParcel(Parcel in) {
	                return new SCOArLu(in);
	            }
	 
	            public SCOArLu[] newArray(int size) {
	                return new SCOArLu[size];
	            }
	        };

	        
	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE 
	*/
	        
}