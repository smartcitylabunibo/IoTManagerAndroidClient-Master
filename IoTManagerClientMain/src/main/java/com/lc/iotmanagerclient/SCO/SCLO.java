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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;



/**
 * Smart Catcher Light Object (oggetto contenente solo i parametri obbligatori in relazione
 * allo standard richiesto dal framework georeferenziato)
 * 
 * Ogni oggetto contiene i dati per popolare una riga della list view.
 * Ll campo tipo serve a specificare la categoria di sensore in modo da poterlo gestire nell'activity di
 * dettaglio con l'opportuna classe SCO di dettaglio, che presenterà una view e dei metodi specializzati
 */
public class SCLO implements Parcelable, Comparable<SCLO> {
	
	private int id;
	private String nome;
	private int categoria;
	private int stato;
	private double longitudine;
	private double latitudine;
	
	// Distanza dell'oggetto dal device in metri
	private int distanzaMt;
	// Distanza dell'oggetto dal device in formato user friendly
	private String distanza;
	
	/**
	 * Costruttore
	 * @param id id sul database dell'oggetto
	 * @param nome nome da stampare a video
	 * @param categoria campo per classificare l'oggetto tra quelli previsti dal framework
	 * @param stateDevice campo per valutare alcuni stati notevoli dell'oggetto
	 * @param lng longitudine dell'oggetto
	 * @param lat latitudine dell'oggetto
	 * @param lngDevice longitudine del device al momento della creazione dell'oggetto
	 * @param latDevice latitudine del device al momento della creazione dell'oggetto
	 */
	public SCLO(int id, String nome, int categoria, double lng, double lat, double lngDevice, double latDevice, int stateDevice){
		this.id = id;
		this.nome = nome;
		this.categoria = categoria;
		this.longitudine = lng;
		this.latitudine = lat;
		this.stato = stateDevice;
		
		this.setDistanceTo(lngDevice, latDevice);
	}


	/**
	 * GETTER, SETTER
	 */

	public int getId(){
		return this.id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public String getNome(){
		return this.nome;
	}
	
	public void setNome(String nome){
		this.nome = nome;
	}
	
	public int getCategoria(){ return this.categoria; }
	
	public void setCategoria(int cat){
		this.categoria = cat;
	}
	
	public int getStato(){
		return this.stato;
	}
	
	public void setStato(int v){
		this.stato = v;
	}
	
	public double getLongitudine(){
		return this.longitudine;
	}
	
	public void setLongitudine(double lng){
		this.longitudine = lng;
	}
	
	public double getLatitudine(){
		return this.latitudine;
	}
	
	public void setLatitudine(double lat){
		this.latitudine = lat;
	}
	
	public String getDistanza(){
		return this.distanza;
	}
	
	public int getDistanzaMt(){
		return this.distanzaMt;
	}

	/**
	 * FINE GETTER, SETTER
	 */

	/**
	 * Se il nome è troppo lungo ritorna solo i primi caratteri
	 */
	public String formattaNome(){
		if(this.nome.length()<30) return this.nome;
		else return this.nome.substring(0, 22)+"...";
	}
	
	/**
	 * calcola la distanza da un punto georeferenziato passato in input e la salva nei campi
	 * relativi alla distanza (distanza, distanzaMt)
	 */
	public void setDistanceTo(double lng, double lat){
		
		Location destinazione = new Location("destinazione");
		destinazione.setLatitude(lat);
		destinazione.setLongitude(lng);
		Location itsMe = new Location("itsMe");
		itsMe.setLatitude(this.latitudine);
		itsMe.setLongitude(this.longitudine);
		int dist = (int) itsMe.distanceTo(destinazione);
		
		this.distanzaMt = dist;
		this.distanza = this.formattaDistanza(dist);
	}
	
	
	/**
	 * trasforma la distanza espressa come un intero rappresentante i metri in una stringa
	 * user friendly espressa in metri e km
	 */
	private String formattaDistanza(int distanza){
		String KM = new String("Km");
		String MT = new String("Mt");
		String toReturn = null;
		if(distanza<1000) toReturn = Integer.toString(distanza)+MT;
		else{
			int km = (int) Math.floor(distanza/1000);
			int mt = distanza%1000;
			if(mt!=0) toReturn = Integer.toString(km)+KM+" "+Integer.toString(mt)+MT;
			else toReturn = Integer.toString(km)+KM;
		}
		return toReturn;
	}
	
	/**
	 * Fornisce la giusta icona da mostrare sulla listview in base alla categoria dell'oggetto
	 */
	public int getIcon(){
		return SCO.getIcon(categoria);
	}


	/**
	 * Effettua la comparazione del nome dell'oggetto con quello di un altro
	 */
	public int compareTo(SCLO another) {
		return nome.compareTo(another.nome);
	}


	/**
	 * SEGUE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE
	 */

	public SCLO(Parcel in) {
		readFromParcel(in);
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(nome);
		dest.writeInt(categoria);
		dest.writeDouble(longitudine);
		dest.writeDouble(latitudine);
		dest.writeInt(distanzaMt);
		dest.writeString(distanza);
		dest.writeInt(stato);
	}

	private void readFromParcel(Parcel in) {
		id = in.readInt();
		nome = in.readString();
		categoria = in.readInt();
		longitudine = in.readDouble();
		latitudine = in.readDouble();
		distanzaMt = in.readInt();
		distanza = in.readString();
		stato = in.readInt();
	}

	public static final Parcelable.Creator<SCLO> CREATOR =
			new Parcelable.Creator<SCLO>() {
				public SCLO createFromParcel(Parcel in) {
					return new SCLO(in);
				}

				public SCLO[] newArray(int size) {
					return new SCLO[size];
				}
			};

	/**
	 * FINE CODICE PER L'IMPLEMENTAZIONE DELL'INTERFACCIA PARCELABLE
	 */


}
