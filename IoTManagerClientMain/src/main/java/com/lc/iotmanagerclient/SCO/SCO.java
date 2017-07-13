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

import android.content.Context;
import android.view.View;
import android.os.Parcelable;

import com.lc.iotmanagerclient.R;

/**
 * Classe astratta di oggetti SCO (Smart Catcher Object)
 * 
 * Il metodo create view andrà specializzato e si occuperà di implementare una opportuna
 * vista di dettaglio sulla base della categoria del sensore in oggetto
 */
public abstract class SCO implements Parcelable{

	// lista di SCLO, popolata nel caso il sensore in questione sia un concentratore, ovvero abbia
	// una lista di sensori 'figli' logicamente riferiti a se stesso (come nel caso ArLu -> Lamp)
	private SCLO[] sonsList;

	abstract public int getIDObj();

	abstract public String resolveStatus();

	abstract public View createView(Context context);

	/**
	 * GETTER, SETTER
	 */

	public SCLO[] getSonsList() {
		return sonsList;
	}

	public void setSonsList(SCLO[] sonsList){
		this.sonsList = sonsList;
	}

	/**
	 * FINE GETTER, SETTER
	 */

	/**
	 * Ritorna la giusta icona in base alla categoria del sensore
	 * -2: icona relativa allo stato di errore
	 * -1: icona relativa al sensore di default (sconosciuto)
	 * 0: icona riservata alla rappresentazione dell'utente sulla mappa
	 */
	public static final int getIcon(int idCategoria){
		
		switch (idCategoria){
			case -2:
				return R.drawable.ic_error_status;
			case SCODefault.SCO_ID:
				return R.drawable.ic_smart_default;
			case 0:
				return R.drawable.ic_smart_user;
			case SCOTc.SCO_ID:
				return R.drawable.ic_smart_tc;
			case SCOArLu.SCO_ID:
				return R.drawable.ic_smart_arlu;
			case SCOLamp.SCO_ID:
				return R.drawable.ic_smart_lamp;
			case SCOUdooWhst.SCO_ID:
				return R.drawable.ic_smart_udoowhst;
			default:
				return R.drawable.ic_smart_default;
		}
		
	}


	/**
	 * Ritorna TRUE (sensore funzionante) / FALSE (sensore in stato di errore)
	 * in base alla categoria del sensore e al suo stato attuale
	 */
	public static boolean getStatus(int categoria, int stato){
		
		switch (categoria) {
			case SCOTc.SCO_ID:
				switch (stato) {
					case 0:
						return true;
					default:
						return false;
				}
			case SCOArLu.SCO_ID:
				switch (stato) {
					case 0:
						return true;
					case 1:
						return true;
					default:
						return false;
				}
			case SCOLamp.SCO_ID:
				switch (stato) {
					case 0:
						return true;
					case 1:
						return true;
					case 2:
						return true;
					default:
						return false;
				}
			case SCOUdooWhst.SCO_ID:
				switch (stato) {
					case 0:
						return true;
					default:
						return false;
				}
			default:
				switch (stato) {
					case 0:
						return true;
					default:
						return false;
				}
		}
	}
	
	
}