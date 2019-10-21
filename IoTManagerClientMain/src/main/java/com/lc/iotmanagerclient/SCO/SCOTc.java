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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.graphics.Color;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.utility.DisplayConvertionUtility;


/**
 * Classe SCO per gestire oggetti di tipo traffic controller (TC)
 * 
 * @author luca
 *
 */
public class SCOTc extends SCO implements Parcelable{

	//Id della categoria dell'oggetto sul data base remoto
	public static final int SCO_ID = 1;
	
	private int id;
	private String sistema;
	private int idCategoria;
	private String descrizione;
	private String comune;
	private String indirizzo;
	private String nome;
	private String city;
	private String firmware;
	private double latitudine;
	private double longitudine;
	private int stato;
	private int num_spire;
	private int totCount;
	private double velAvg;
	private int autoCount;
	private int motoCount;
	private int tirCount;
	private String toString;
	private int[] vehicles;
	private int[] averageSpeeds;
	private int[][] spira;


	/**
	 * Riempie i campi dell'oggetto cercando i corrispettivi nella stringa Json passata in input
	 * La stringa json è il risultato di una richiesta al server di singolo oggetto da db.
	 */
	SCOTc(JSONArray parsed_json){

		try {
			this.id = parsed_json.getJSONObject(0).getInt("id");
			this.idCategoria = parsed_json.getJSONObject(0).getInt("idCategoria");
			this.num_spire = parsed_json.getJSONObject(0).getInt("NUMSPIRE");
			this.nome = parsed_json.getJSONObject(0).getString("NOME_IMPIANTO");
			this.indirizzo = parsed_json.getJSONObject(0).getString("ADDRESS");
			this.firmware = parsed_json.getJSONObject(0).getString("FIRMWARE");
			this.city = parsed_json.getJSONObject(0).getString("CITY");
			this.latitudine = parsed_json.getJSONObject(0).getDouble("LATITUDE");
			this.longitudine = parsed_json.getJSONObject(0).getDouble("LONGITUDE");
			this.stato = parsed_json.getJSONObject(0).getInt("STATUS");

			
			vehicles = new int[num_spire];
			averageSpeeds = new int[num_spire];
			
			
			for (int i = 0; i < num_spire; i++) {
				vehicles[i] = parsed_json.getJSONObject(0).getInt("CNTSPIRA_"+i);
				averageSpeeds[i] = parsed_json.getJSONObject(0).getInt("VELSPIRA_"+i);
			}

			
			Map<String, String> fieldDetails = new LinkedHashMap<String, String>();
			fieldDetails.put("NOME IMPIANTO", this.nome);
			fieldDetails.put("INDIRIZZO", this.indirizzo + ", " + this.city);
			fieldDetails.put("FIRMWARE", this.firmware);

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

		return SCO.getIcon(SCOTc.SCO_ID);
	}


	/**
	 * CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */
	
	/**
	 * Metodo che ritorna la view da mostrare nella ObjectDetailsActivity adattata per i traffic controller
	 */
	@Override
	public View createView(Context context){
		
		LinearLayout mainView = new LinearLayout(context);
		mainView.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout iconLayout = new LinearLayout(context);
		iconLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.setGravity(Gravity.CENTER);
		
		ImageView iconView = new ImageView(context);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			iconView.setBackground(ContextCompat.getDrawable(context, SCOTc.getIcon()));
		}
		else {
			iconView.setBackgroundDrawable(ContextCompat.getDrawable(context, SCOTc.getIcon()));
		}
		iconView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.addView(iconView);
		mainView.addView(iconLayout);

		TextView tvName = new TextView(context);
		tvName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvName.setText("TRAFFIC CONTROLLER");
		tvName.setTextSize(18f);
		tvName.setGravity(Gravity.CENTER);
		tvName.setTextColor(ContextCompat.getColor(context, R.color.Gold));
		mainView.addView(tvName);
		
		TextView tvDetails = new TextView(context);
		tvDetails.setPadding(15, 10, 0, 0);
		tvDetails.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvDetails.setText(toString);
		tvDetails.setTextSize(18f);
		tvDetails.setTextColor(ContextCompat.getColor(context,R.color.White));
		mainView.addView(tvDetails);
		
		TextView tvStatus = new TextView(context);
		tvStatus.setPadding(15, 10, 0, 0);
		tvStatus.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvStatus.setText("STATO: " + resolveStatus());
		tvStatus.setTextSize(18f);
		if (SCO.getStatus(idCategoria, stato)) {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Green));
		}else {
			tvStatus.setTextColor(ContextCompat.getColor(context,R.color.Red));
		}
		mainView.addView(tvStatus);
		
		LinearLayout graphLayout = new LinearLayout(context);
		graphLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		graphLayout.setGravity(Gravity.CENTER);
		graphLayout.setOrientation(LinearLayout.HORIZONTAL);
		graphLayout.setBackgroundColor(Color.WHITE);
		
		LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		leftParams.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		leftParams.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		leftParams.weight = 1f;

		LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		rightParams.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		rightParams.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		rightParams.weight = 1f;

		BarChart graphCountVehicles = new BarChart(context);
		graphCountVehicles.setLayoutParams(leftParams);

		BarChart graphAvgSpeed = new BarChart(context);
		graphAvgSpeed.setLayoutParams(rightParams);

		CreateCountVehiclesGraph(graphCountVehicles, vehicles);
		CreateCountAvgSpeedGraph(graphAvgSpeed, averageSpeeds);

		graphLayout.addView(graphCountVehicles);
		graphLayout.addView(graphAvgSpeed);

		mainView.addView(graphLayout);

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
			 return "In funzione";
		case 255:
			 return "Connessione assente";
		default:
			return "Errore - codice " + stato;
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

	public SCOTc(Parcel in) {
		readFromParcel(in);
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(sistema);
		dest.writeInt(idCategoria);
		dest.writeString(descrizione);
		dest.writeString(comune);
		dest.writeString(indirizzo);
		dest.writeDouble(longitudine);
		dest.writeDouble(latitudine);
		dest.writeInt(stato);
		dest.writeInt(totCount);
		dest.writeDouble(velAvg);
		dest.writeInt(autoCount);
		dest.writeInt(motoCount);
		dest.writeInt(tirCount);
	}

	private void readFromParcel(Parcel in) {
		id = in.readInt();
		sistema = in.readString();
		idCategoria = in.readInt();
		descrizione = in.readString();
		comune = in.readString();
		indirizzo = in.readString();
		longitudine = in.readDouble();
		latitudine = in.readDouble();
		stato = in.readInt();
		totCount = in.readInt();
		velAvg = in.readDouble();
		autoCount = in.readInt();
		motoCount = in.readInt();
		tirCount = in.readInt();
	}

	public static final Parcelable.Creator<SCOTc> CREATOR =
	    	new Parcelable.Creator<SCOTc>() {
	            public SCOTc createFromParcel(Parcel in) {
	                return new SCOTc(in);
	            }

	            public SCOTc[] newArray(int size) {
	                return new SCOTc[size];
	            }
	};


	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE
	*/


	/**
	 * SEGUE CODICE PER L'UTILIZZO DELLA LIBRERIA MPAndroidChart
	 */

	class KMHValueFormatter implements IAxisValueFormatter {

		private DecimalFormat mFormat;

		public KMHValueFormatter (){
			mFormat = new DecimalFormat("###,###,##0");
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " Km/h";
		}
	}

	class StringsValueFormatter implements IAxisValueFormatter {

		private List<String> mValues;

		public StringsValueFormatter (List<String> values){
			this.mValues = values;
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mValues.get((int)value);
		}
	}


	private void CreateCountAvgSpeedGraph(BarChart graph, int[] averageSpeeds) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		for (int i=0; i<averageSpeeds.length; i++) {
			entries.add(new BarEntry(i,averageSpeeds[i]));
			labels.add("Spira "+i);
		}

		BarDataSet dataSet = new BarDataSet(entries,"spire");
		dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Velocità media"); //Spostare la description in alto a destra? Mettere un titolo al grafico?
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new KMHValueFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
		xAxis.setLabelRotationAngle(-30);
		xAxis.setGranularity(1);
		xAxis.setDrawGridLines(false);
		xAxis.setDrawLabels(true);

		/** Impostazione griglia ed etichette assi */
		graph.setFitBars(true);
		graph.setAutoScaleMinMaxEnabled(true);
		graph.setDrawGridBackground(true);
		graph.getAxisRight().setDrawLabels(false);

		/** Refresh del grafico*/
		graph.getLegend().setEnabled(false);
		graph.invalidate();

	}

	private void CreateCountVehiclesGraph(BarChart graph, int[] vehicles) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		for (int i=0; i<vehicles.length; i++) {
			entries.add(new BarEntry(i,vehicles[i]));
			labels.add("Spira "+i);
		}
		BarDataSet dataSet = new BarDataSet(entries,"spire");
		dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Numero veicoli"); //Spostare la description in alto a destra? Mettere un titolo al grafico?
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new LargeValueFormatter());
		yleft.setGranularity(1f);
		yleft.setTextSize(12);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
		xAxis.setLabelRotationAngle(-30);
		xAxis.setGranularity(1);
		xAxis.setDrawGridLines(false);
		xAxis.setDrawLabels(true);

		/** Impostazione griglia ed etichette assi */
		graph.setFitBars(true);
		graph.setAutoScaleMinMaxEnabled(true);
		graph.setDrawGridBackground(true);
		graph.getAxisRight().setDrawLabels(false);

		/** Refresh del grafico*/
		graph.getLegend().setEnabled(false);
		graph.invalidate();
	}

	/**
	 * FINE CODICE PER L'UTILIZZO DELLA LIBRERIA MPAndroidChart
	 */


}