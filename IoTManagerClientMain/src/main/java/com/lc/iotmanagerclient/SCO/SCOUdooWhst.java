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

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.utility.DisplayConvertionUtility;



/**
 * Classe SCO per gestire oggetti di tipo Udoo Weather Station (UdooWhst)
 */
public class SCOUdooWhst extends SCO implements Parcelable{

	//Id della categoria dell'oggetto sul data base remoto
	public static final int SCO_ID = 6;

	private int id;
	private int idCategoria;
	private String nome;
	private double latitudine;
	private double longitudine;
	private int stato;
	private double[] temperatura;
	private double[] umidita;
	private double[] luce;
	private double[] pressione;
	private double[] polvere;
	private double[] gasMQ3Ratio;
	private String timestampRegistrazione;

	private String toString;


	/**
	 * Riempie i campi dell'oggetto cercando i corrispettivi nella stringa Json passata in input
	 * La stringa json è il risultato di una richiesta al server di singolo oggetto da db.
	 */
	SCOUdooWhst(JSONArray parsed_json){

		try {
			this.id = parsed_json.getJSONObject(0).getInt("id");
			this.idCategoria = parsed_json.getJSONObject(0).getInt("idCategoria");
			this.nome = parsed_json.getJSONObject(0).getString("nome");
			this.latitudine = parsed_json.getJSONObject(0).getDouble("latitudine");
			this.longitudine = parsed_json.getJSONObject(0).getDouble("longitudine");
			this.stato = parsed_json.getJSONObject(0).getInt("stato");

			temperatura = new double[1];
			luce = new double[1];
			umidita = new double[1];
			pressione = new double[1];
			polvere = new double[1];
			gasMQ3Ratio = new double[1];

			this.temperatura[0] = parsed_json.getJSONObject(0).getDouble("HumidityTemperature");
			this.umidita[0] = parsed_json.getJSONObject(0).getDouble("HumidityHumidity");
			this.luce[0] = parsed_json.getJSONObject(0).getDouble("LightSpectrum");
			this.pressione[0] = parsed_json.getJSONObject(0).getDouble("BarometerPressure");
			this.polvere[0] = parsed_json.getJSONObject(0).getDouble("DustConcentration");
			this.gasMQ3Ratio[0] = parsed_json.getJSONObject(0).getDouble("GasMQ3Ratio");
			this.timestampRegistrazione = parsed_json.getJSONObject(0).getString("GasMQ3Timestamp");

			Map<String, String> fieldDetails = new LinkedHashMap<String, String>();
			fieldDetails.put("Nome Stazione Meteo", this.nome);
			fieldDetails.put("Identificativo", Integer.toHexString(this.id));

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

		return SCO.getIcon(SCOUdooWhst.SCO_ID);
	}


	/**
	 * CODICE PER L'IMPLEMENTAZIONE DEI METODI ASTRATTI DELLA CLASSE PADRE
	 */

	/**
	 * Metodo che ritorna la view da mostrare nella ObjectDetailsActivity adattata per UDOO Weather Station
	 */
	@Override
	public View createView(Context context){

		LinearLayout mainView = new LinearLayout(context);
		mainView.setOrientation(LinearLayout.VERTICAL);

		LinearLayout iconLayout = new LinearLayout(context);
		iconLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.setGravity(Gravity.CENTER);
		/* Icon view */
		ImageView iconView = new ImageView(context);
		iconView.setBackground(ContextCompat.getDrawable(context, SCOUdooWhst.getIcon()));
		iconView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		iconLayout.addView(iconView);
		mainView.addView(iconLayout);
		/* Title  */
		TextView tvName = new TextView(context);
		tvName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvName.setText("UDOO-based Weather Station");
		tvName.setTextSize(18f);
		tvName.setGravity(Gravity.CENTER);
		tvName.setTextColor(ContextCompat.getColor(context, R.color.Gold));
		mainView.addView(tvName);
		/* Details */
		TextView tvDetails = new TextView(context);
		tvDetails.setPadding(15, 10, 0, 0);
		tvDetails.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvDetails.setText(toString);
		tvDetails.setTextSize(18f);
		tvDetails.setTextColor(ContextCompat.getColor(context,R.color.White));
		mainView.addView(tvDetails);
		/* Status */
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
		/* Last update */
		TextView tvTimeStamp = new TextView(context);
		tvTimeStamp.setPadding(15, 10, 0, 0);
		tvTimeStamp.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		tvTimeStamp.setText("Last update: " + this.timestampRegistrazione);
		tvTimeStamp.setTextColor(ContextCompat.getColor(context,R.color.White));
		tvTimeStamp.setTextSize(18f);
		mainView.addView(tvTimeStamp);

		/* GRAPH LAYOUT */
		/* 1st Line (Temperature, Humidity, Pressure) */
		LinearLayout graphLayout_firstLine = new LinearLayout(context);
		graphLayout_firstLine.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		graphLayout_firstLine.setGravity(Gravity.CENTER);
		graphLayout_firstLine.setOrientation(LinearLayout.HORIZONTAL);
		graphLayout_firstLine.setBackgroundColor(Color.WHITE);

		LinearLayout.LayoutParams one = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		one.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		one.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		one.weight = 1f;

		LinearLayout.LayoutParams two = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		two.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		two.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		two.weight = 1f;

		LinearLayout.LayoutParams three = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		three.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		three.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		three.weight = 1f;

		BarChart temperature = new BarChart(context);
		temperature.setLayoutParams(one);

		BarChart humidity = new BarChart(context);
		humidity.setLayoutParams(two);

		BarChart pressure = new BarChart(context);
		pressure.setLayoutParams(three);
		/* End 1st Line*/

		/* 2nd Line (Light, Dust, Gas) */
		LinearLayout graphLayout_secondLine = new LinearLayout(context);
		graphLayout_secondLine.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		graphLayout_secondLine.setGravity(Gravity.CENTER);
		graphLayout_secondLine.setOrientation(LinearLayout.HORIZONTAL);
		graphLayout_secondLine.setBackgroundColor(Color.WHITE);

		LinearLayout.LayoutParams four = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		four.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		four.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		four.weight = 1f;

		LinearLayout.LayoutParams five = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		five.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		five.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		five.weight = 1f;

		LinearLayout.LayoutParams six = new LinearLayout.LayoutParams(0, (int) DisplayConvertionUtility.convertDpToPixel(420f, context));
		six.topMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		six.bottomMargin = (int) DisplayConvertionUtility.convertDpToPixel(15, context);
		six.weight = 1f;

		BarChart light = new BarChart(context);
		light.setLayoutParams(four);

		BarChart dust = new BarChart(context);
		dust.setLayoutParams(five);

		BarChart gas = new BarChart(context);
		gas.setLayoutParams(six);
		/*End 2nd Line*/

		CreateTemperatureChart(temperature, temperatura);
		CreateHumidityChart(humidity, umidita);
		CreatePressureChart(pressure, pressione);

		CreateLightChart(light, luce);
		CreateDustChart(dust, polvere);
		CreateGasChart(gas, gasMQ3Ratio);

		graphLayout_firstLine.addView(temperature);
		graphLayout_firstLine.addView(pressure);
		graphLayout_firstLine.addView(humidity);

		graphLayout_secondLine.addView(light);
		graphLayout_secondLine.addView(dust);
		graphLayout_secondLine.addView(gas);

		mainView.addView(graphLayout_firstLine);
		mainView.addView(graphLayout_secondLine);

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

	public SCOUdooWhst(Parcel in) {
		readFromParcel(in);
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(idCategoria);
		dest.writeString(nome);
		dest.writeDouble(longitudine);
		dest.writeDouble(latitudine);
		dest.writeInt(stato);
		dest.writeDouble(temperatura[0]);
		dest.writeDouble(umidita[0]);
		dest.writeDouble(luce[0]);
		dest.writeDouble(pressione[0]);
	}

	private void readFromParcel(Parcel in) {
		id = in.readInt();
		idCategoria = in.readInt();
		nome = in.readString();
		longitudine = in.readDouble();
		latitudine = in.readDouble();
		stato = in.readInt();
		temperatura[0] = in.readDouble();
		umidita[0] = in.readDouble();
		luce[0] = in.readDouble();
		pressione[0] = in.readDouble();
	}

	public static final Creator<SCOUdooWhst> CREATOR =
	    	new Creator<SCOUdooWhst>() {
	            public SCOUdooWhst createFromParcel(Parcel in) {
	                return new SCOUdooWhst(in);
	            }

	            public SCOUdooWhst[] newArray(int size) {
	                return new SCOUdooWhst[size];
	            }
	};


	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE
	*/


	/**
	 * SEGUE CODICE PER L'UTILIZZO DELLA LIBRERIA MPAndroidChart
	 */

	class TempFormatter implements IAxisValueFormatter {

		private DecimalFormat mFormat;

		public TempFormatter (){
			mFormat = new DecimalFormat("##.##");
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " °C";
		}
	}

	class PressureFormatter implements IAxisValueFormatter {

		private DecimalFormat mFormat;

		public PressureFormatter (){
			mFormat = new DecimalFormat("####.##");
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " hPa";
		}
	}


	class LuxFormatter implements IAxisValueFormatter {

		private DecimalFormat mFormat;

		public LuxFormatter (){
			mFormat = new DecimalFormat("####.##");
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " lx";
		}
	}

	class DustFormatter implements  IAxisValueFormatter {
		private DecimalFormat mFormat;

		public DustFormatter() { mFormat = new DecimalFormat("####.##");}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " Pcs/Lt";
		}
	}

	class GasFormatter implements  IAxisValueFormatter {
		private DecimalFormat mFormat;

		public GasFormatter() { mFormat = new DecimalFormat("###.##");}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " RS/R0";
		}
	}

	class HygroFormatter implements IAxisValueFormatter {

		private DecimalFormat mFormat;

		public HygroFormatter (){
			mFormat = new DecimalFormat("###.##");
		}

		@Override
		public String getFormattedValue(float value, AxisBase axis) {
			return mFormat.format(value) + " %";
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


	private void CreateTemperatureChart(BarChart graph, double[] temperature) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		if(temperature[0]>=0) {
			graph.getAxisLeft().setAxisMaximum(55);
			graph.getAxisLeft().setAxisMinimum(0);
		}
		else
		{
			graph.getAxisLeft().setAxisMaximum(0);
			graph.getAxisLeft().setAxisMinimum(-30);
		}

		for (int i=0; i<temperature.length; i++) {
			entries.add(new BarEntry(i,(float)temperature[i]));
			labels.add("MPL 3115");
		}

		BarDataSet dataSet = new BarDataSet(entries,"temp");

		if (isBetween((int) temperature[0],20, 24))
			dataSet.setColor(Color.rgb(178,255,102));
		else if (isBetween((int) temperature[0], 25, 29))
			dataSet.setColor(Color.rgb(255,178,102));
		else if (temperature[0] > 30)
			dataSet.setColor(Color.rgb(255,102,102));
		else if (isBetween((int) temperature[0], 15, 19))
			dataSet.setColor(Color.rgb(153,255,204));
		else if (temperature[0] < 15)
			dataSet.setColor(Color.rgb(102,178,255));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Temperatura");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new TempFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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

	private void CreatePressureChart(BarChart graph, double[] pressure) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		graph.getAxisLeft().setAxisMaximum(1500);
		graph.getAxisLeft().setAxisMinimum(500);

		float tempMaxValue = 1300;

		if (tempMaxValue >= graph.getAxisLeft().getAxisMaximum())
			graph.getAxisLeft().setAxisMaximum((float) (tempMaxValue + 0.5 * tempMaxValue));

		for (int i=0; i<pressure.length; i++) {
			entries.add(new BarEntry(i,10*(float)pressure[i]));
			labels.add("MPL 3115");
		}

		BarDataSet dataSet = new BarDataSet(entries,"baro");
		if (pressure[0] < 80) dataSet.setColor(Color.rgb(153,204,255));
		else dataSet.setColor(Color.rgb(255,102,102));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Pressione");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new PressureFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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


	private void CreateLightChart(BarChart graph, double[] temperature) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		graph.getAxisLeft().setAxisMaximum(1000);
		graph.getAxisLeft().setAxisMinimum(0);

		for (int i=0; i<temperature.length; i++) {
			entries.add(new BarEntry(i,(float)temperature[i]));
			labels.add("TSL 2561 T");
		}

		BarDataSet dataSet = new BarDataSet(entries,"lux");

		if (temperature[0] < 200 )
			dataSet.setColor(Color.rgb(255,228,116));
		else dataSet.setColor(Color.rgb(255,255,102));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Illuminazione");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new LuxFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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

	private void CreateHumidityChart(BarChart graph, double[] humidity) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		graph.getAxisLeft().setAxisMaximum(100);
		graph.getAxisLeft().setAxisMinimum(0);

		for (int i=0; i<humidity.length; i++) {
			entries.add(new BarEntry(i,(float)humidity[i]));
			labels.add("SI 7006 A20");
		}

		BarDataSet dataSet = new BarDataSet(entries,"hygro");
		if (humidity[0] < 50)
			dataSet.setColor(Color.rgb(102,255,102));
		else dataSet.setColor(Color.rgb(255,178,102));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Umidità");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new HygroFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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
	private void CreateGasChart(BarChart graph ,double[] gas) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		graph.getAxisLeft().setAxisMaximum(100);
		graph.getAxisLeft().setAxisMinimum(0);

		for (int i=0; i < gas.length; i++) {
			entries.add(new BarEntry(i,(float)gas[i]));
			labels.add("Grove GASMQ3");
		}

		BarDataSet dataSet = new BarDataSet(entries,"gas");
		dataSet.setColor(Color.rgb(173,216,230));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Gas");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new GasFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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

	private void CreateDustChart(BarChart graph ,double[] polvere) {
		List<BarEntry> entries = new ArrayList<>();
		List<String> labels = new ArrayList<>();

		graph.getAxisLeft().setAxisMaximum((float)(polvere[0] + polvere[0] * 0.5));
		graph.getAxisLeft().setAxisMinimum(0);

		for (int i=0; i < polvere.length; i++) {
			entries.add(new BarEntry(i,(float)polvere[i]));
			labels.add("Grove Dust");
		}

		BarDataSet dataSet = new BarDataSet(entries,"dust");
		dataSet.setColor(Color.rgb(102,102,255));

		BarData barData = new BarData(dataSet);
		barData.setBarWidth(0.9f);
		graph.setData(barData);

		/** Imposta la descrizione del grafico (libreria default, in basso a destra)*/
		graph.getDescription().setText("Polvere");
		graph.getDescription().setTextSize(12);
		YAxis yleft = graph.getAxisLeft();
		XAxis xAxis = graph.getXAxis();

		yleft.setValueFormatter(new DustFormatter());
		yleft.setTextSize(12);
		yleft.setGranularity(1f);
		yleft.setDrawGridLines(false);

		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setValueFormatter(new StringsValueFormatter(labels));
		xAxis.setTextSize(12);
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
	 *  FINE CODICE PER L'UTILIZZO DELLA LIBRERIA MPAndroidChart
	 */

	public static boolean isBetween(int x, int lower, int upper) {
		return lower <= x && x <= upper;
	}

}