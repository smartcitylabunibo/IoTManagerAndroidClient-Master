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

import org.json.JSONArray;

/**
 * Classe fabbrica di oggetti SCO: a seconda della categoria ritorna l'oggetto opportuno
 */
public class SCOFactory{
	
	public SCOFactory(){
		
	}
	
	/**
	 * Metodo che esegue il mapping Categoria->oggetto
	 * @param idCategoria il codice univoco che identifica la tipologia di sensore sul framework
	 * @param parsed_json stringa parsata restituita dal back-end, da utilizzare per istanziare l'oggetto
	 */
	public SCO getSCO(int idCategoria, JSONArray parsed_json){
		
		switch(idCategoria){
		case 1:
			return new SCOTc(parsed_json);
		case 2:
			return new SCOArLu(parsed_json);
		case 3:
			return new SCOLamp(parsed_json);
		case 6:
			return new SCOUdooWhst(parsed_json);
		default:
			return new SCODefault(parsed_json);
		}
		
	}

}