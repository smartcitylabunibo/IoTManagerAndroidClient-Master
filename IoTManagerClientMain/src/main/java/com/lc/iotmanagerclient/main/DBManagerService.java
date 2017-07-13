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

import java.sql.Timestamp;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.SCO.SCLO;
import com.lc.iotmanagerclient.SCO.SCO;
import com.lc.iotmanagerclient.SCO.SCOFactory;
import com.lc.iotmanagerclient.utility.City;
import com.lc.iotmanagerclient.utility.JSONFetcher;
import com.lc.iotmanagerclient.utility.ParamFetcher;

/**
 * Service principale di appoggio alle activities.
 * 
 * Il service gestisce dei task complessi in un thread separato in modo da non risultare bloccante per l'interfaccia utente
 * A loro volta, i task di una complessità rilevante sono eseguiti in thread separati e comunicano con il service
 * attraverso interfacce private (Handler privati).
 * Due task complessi sono ad esempio la comunicazione con il back-end e la gestione della georeferenziazione del device
 */
public class DBManagerService extends Service {

    // Versione SDK del device
    private int SDK_VERSION = android.os.Build.VERSION.SDK_INT;

	// Messenger sul quale il service riceve i messaggi provenienti dalle client activity.
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Handler per la ricezione dei messaggi dal working thread per aggiornamento dal db
    UpdateFromDBThreadHandler uFDBThreadHandler;

    // Handler per la ricezione dei messaggi dal working thread per localizzazione
    LocationThreadHandler locThreadHandler;

    // Thread per la georeferenziazione
    LocationThread workingLocThread;
    
    // Gestore della connessione wifi
    ConnectivityManager connectionManager;
    
    // Tabella hash che tiene traccia di ogni activity bindata al service.
    // Ad ogni id dell'activity collegata corrisponde l'oggetto messenger al quale il service invierà i messaggi.
    Hashtable<String, Messenger> mClients = new Hashtable<String, Messenger>();

    // Tabella hash che tiene traccia dello stato di ogni activity bindata al service.
    // Ad ogni id dell'activity collegata corrisponde lo stato (true se in foreground, false altrimenti).
    Hashtable<String, Boolean> mClientsState = new Hashtable<String, Boolean>();
    
    // Attuali coordinate di latitudine e longitudine del device, aggiornate dal location thread.
    // Se il flag di controllo IS_GEOPOINT è false significa che il device non è ancora georeferenziato o ha perso il segnale
    // da più di 2 minuti; in questo caso non sarà possibile eseguire richieste di lista oggetti al db thread.
    private double LONGITUDINE = 0;
    private double LATITUDINE = 0;
    private boolean IS_GEOPOINT = false;

    // L'oggetto CITY mantiene in memoria i dati della città attualmente selezionata nei settings.
    // In caso sia settato, sostituisce le coordinate del device con quelle della città in questione in fase di download oggetti
    private City CITY = null;


    /**
     * CODICI IDENTIFICATIVI DELLE TIPOLOGIE DI MESSAGGIO
     */

    // Registrazione di una client activity sulla hash table
    static final int MSG_REGISTER_CLIENT = 1;

    // Cancellazione di una client activity dalla hash table
    static final int MSG_UNREGISTER_CLIENT =2;

    // Inizio ricerca coordinate geografiche
    static final int MSG_BEGIN_COORDS_SEARCH =3;
    
    // Setta un nuovo valore di avanzamento della progress bar dell'activity a cui viene inviato
    static final int MSG_SET_PROG_BAR = 4;
    
    // Indica al client che comincia un blocco critico per il quale è necessaria una progress bar
    static final int MSG_START_PROGRESS = 5;
    
    // Indica al client che termina un blocco critico per il quale è necessaria una progress bar
    static final int MSG_END_PROGRESS = 6;
    
    // Il thread di update da db indica al service un aggiornamento sul progresso totale del task in esecuzione
    static final int MSG_UPDATE_PROGRESS = 7;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati
    static final int MSG_DONE_PROGRESS = 8;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati (caso singolo oggetto)
    static final int MSG_DONE_SINGLE_PROGRESS = 9;
    
    // Invia all'activity una lista di SCLO da mostrare
    static final int MSG_GET_OBJ_LIST = 10;
    
    // Invia all'activity un singolo oggetto e tutti i suoi dettagli
    static final int MSG_GET_SINGLE_OBJ = 11;
    
    // Indica al client che comincia un blocco critico per il quale è necessario un dialog spinner
    static final int MSG_START_SPINNER = 12;
    
    // Indica al client che termina un blocco critico per il quale è necessario un dialog spinner
    static final int MSG_END_SPINNER = 13;
    
    // Il location thread invia le prime coordinate al service
    static final int MSG_FIRST_GEOPOINT = 14;
    
    // Il location thread invia nuove coordinate al service
    static final int MSG_UPDATE_GEOPOINT = 15;
    
    // Il service comunica alla main activity di ricalcolare le distanze degli oggetti in base alle nuove coordinate
    static final int MSG_REFRESH_OBJ_DISTANCE = 16;
    
    // Un client segnala di essere in stato paused
    static final int MSG_PAUSE_CLIENT = 17;
    
    // Un client segnala di essere in stato resumed
    static final int MSG_RESUME_CLIENT = 18;
    
    // Richiesta di lista categorie
    static final int MSG_GET_CAT_LIST = 19;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati (caso lista categorie)
    static final int MSG_DONE_CAT_PROGRESS = 20;
    
    // Segnala a una activity di abilitare la rete wireless o 3g
    static final int MSG_NETWORK_STATE = 21;
    
    // Segnala che il gps è disabilitato e quindi non è possibile ricercare coordinate gps
    static final int MSG_GPS_DISABLED = 22;
    
    // Segnala che la network è disabilitata e quindi non è possibile ricercare coordinate network
    static final int MSG_NET_DISABLED = 23;
    
    // Richiesta di lista città
    static final int MSG_GET_CITIES_LIST = 24;
    
    // Richiesta di dati singola citta
    static final int MSG_GET_CITY = 25;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati (caso lista città)
    static final int MSG_DONE_CITIES_PROGRESS = 26;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati (caso singola città)
    static final int MSG_DONE_CITY_PROGRESS = 27;
    
    // Richiesta di dati lista figli di un oggetto
    static final int MSG_GET_SONS_LIST = 28;
    
    // Il thread di update da db indica al service di aver terminato il task e fornisce i dati elaborati (caso lista figli oggetto)
    static final int MSG_DONE_SONS_PROGRESS = 29;

    // La welcome activity segnala al service di far partire il location thread (da SDK 23 in poi)
    static final int MSG_START_LOCATION_THREAD = 30;
    
    // Elenco job da passare al JSONFetcher
    static final int JOB_LIST_OBJECTS = 1;
    static final int JOB_DETAILS_OBJECT = 2;
    static final int JOB_LIST_CATEGORIES = 3;
    static final int JOB_LIST_CITIES = 4;
    static final int JOB_DETAILS_CITY = 5;
    static final int JOB_LIST_SONS = 6;

    // ID da passare al JSONFetcher per la lista completa nel job JOB_LIST_OBJECTS
    static final int JOB_ID_COMPLETELIST = 0;
    
    // Elenco dei possibili stati della rete
    static final int NETWORK_AVAILABLE = 1;
    static final int NETWORK_UNAVAILABLE = 2;
    static final int NETWORK_DISABLED = 3;
    
    /**
     * Creazione del service, contestuale al lancio dell'applicazione
     * Viene subito creato il location thread handler per la gestione della georeferenziazione
     * Il thread corrispondente verrà creato e lanciato all'atto della prima registrazione di un'activity al service
     * Viene anche istanziato il gestore della wifi che si occupa di verificare la presenza della connessione
     */
    @Override
    public void onCreate() {

        locThreadHandler = new LocationThreadHandler();

        if(this.SDK_VERSION < 23){
            workingLocThread = new LocationThread(locThreadHandler);
        }
        else{
            // Dalla sdk 23 in poi, il location thread deve essere avviato a seguito di un messaggio
            // della welcome activity che segnala che l'utente ha confermato il grant dei permessi relativi
        }

    	uFDBThreadHandler = new UpdateFromDBThreadHandler();
    	
    	connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Gestione dell'evento onDestroy
     */
    @Override
    public void onDestroy() {
    	if(DBManagerService.this.workingLocThread.isAlive()){
        	DBManagerService.this.workingLocThread.setState(LocationThread.DONE);
        }
    }

    /**
     * Chiamato all'atto di bind del service da parte di un client
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Chiamato all'atto di unbind del service da parte di un client
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }


    /**
     * Verifica dello stato della connessione wi-fi sul dispositivo
     */
    private int checkNetworkState(){
    	NetworkInfo ni = connectionManager.getActiveNetworkInfo();
    	if(ni == null){
    		return NETWORK_DISABLED;
    	}
    	else{
    		if(ni.isConnectedOrConnecting()) return NETWORK_AVAILABLE;
    		else{
    			return NETWORK_UNAVAILABLE;
    		}
    	}
    }

    /**
     * Seleziona la longitudine corrente in base alle impostazioni. Se nessuna citta è
     * selezionata usa le coordinate gps/rete altrimenti quelle della citta selezionata.
     */
    public double getCurrentLongitudine(){
    	if(CITY != null) return CITY.getLongitudine();
    	else return this.LONGITUDINE;
    }

    /**
     * Seleziona la latitudine corrente in base alle impostazioni. Se nessuna citta è
     * selezionata usa le coordinate gps/rete altrimenti quelle della citta selezionata.
     */
    public double getCurrentLatitudine(){
    	if(CITY != null) return CITY.getLatitudine();
    	else return this.LATITUDINE;
    }

    /**
     * Handler che gestisce la ricezione dei messaggi dai client
     */
    class IncomingHandler extends Handler {
        int setValue;
        int netState;
        String id;
        @Override
        public void handleMessage(Message msg) {
            ParamFetcher pf = new ParamFetcher(getApplicationContext());
            switch (msg.what) {
                // Registro nuovo client sulla hash table; se il location thread non è ancora avviato eseguo lo start
                case MSG_REGISTER_CLIENT:
                    id = Integer.toString(msg.arg1);
                    mClients.put(id, msg.replyTo);
                    mClientsState.put(id, true);
                    // Se il working thread per la georeferenziazione non è ancora stato lanciato provvede ad avviarlo
                    if(SDK_VERSION < 23) {
                        if (!DBManagerService.this.workingLocThread.isAlive()) {
                            DBManagerService.this.workingLocThread.start();
                        }
                        else{
                            // Dalla sdk 23 in poi, il location thread deve essere avviato a seguito di un messaggio
                            // della welcome activity che segnala che l'utente ha confermato il grant dei permessi relativi
                        }
                    }
                    break;
                // Rimuovo client dalla hash table
                case MSG_UNREGISTER_CLIENT:
                    id = Integer.toString(msg.arg1);
                    mClients.remove(id);
                    mClientsState.remove(id);
                    break;
                // Un client segnala di essere in stato paused, aggiorno la hashtable di stato
                case MSG_PAUSE_CLIENT:
                    id = Integer.toString(msg.arg1);
                    mClientsState.remove(id);
                    mClientsState.put(id, false);
                    break;
                // Un client segnala di essere in stato resumed, aggiorno la hashtable di stato
                case MSG_RESUME_CLIENT:
                    id = Integer.toString(msg.arg1);
                    mClientsState.remove(id);
                    mClientsState.put(id, true);
                    break;
                // Blocco critico di richiesta lista oggetti per il quale verrà lanciato un nuovo thread apposito
                // Il nuovo thread comunica con il service su un messenger dedicato per aggiornare l'avanzamento dei lavori
                // Il task viene lanciato solo se il device risulta attualmente georeferenziato, diversamente viene generato sul client
                // uno spinner di ricerca coordinate gps
                // Dopo aver verificato la presenza di georeferenziazione si controlla la presenza della rete, necessaria
                // per eseguire il download dei dati. Se non presente viene mostrato un messaggio e il thread non viene lanciato
                case MSG_GET_OBJ_LIST:
                    setValue = 0;
                    id = Integer.toString(msg.arg1);
                    if(DBManagerService.this.IS_GEOPOINT || CITY != null){
                        netState = DBManagerService.this.checkNetworkState();
                        if(netState == DBManagerService.NETWORK_AVAILABLE){
                            try {
                                // Setta allo 0 per cento l'avanzamento totale della barra di stato
                                if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_START_PROGRESS, setValue, 0));
                            } catch (RemoteException e) { e.printStackTrace();
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                            UpdateFromDBThread workingThread = new UpdateFromDBThread(id, msg.what, uFDBThreadHandler, 0, 0, false);
                            workingThread.start();
                        }
                        else{
                            if(netState == DBManagerService.NETWORK_UNAVAILABLE){
                                Toast.makeText(DBManagerService.this, getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
                            }
                            if(netState == DBManagerService.NETWORK_DISABLED){
                                try {
                                    if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_NETWORK_STATE, 0, 0));
                                } catch (RemoteException e) { e.printStackTrace();
                                    e.printStackTrace();
                                    mClients.remove(id);
                                    mClientsState.remove(id);
                                }
                            }
                        }
                    }
                    else{
                        try {
                            if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_START_SPINNER));
                        } catch (RemoteException e) { e.printStackTrace();
                            mClients.remove(id);
                            mClientsState.remove(id);
                        }
                    }
                    break;
                // Richiesta per recuperare i figli di un oggetto
                case MSG_GET_SONS_LIST:
                    id = Integer.toString(msg.arg1);
                    int parentId = msg.arg2;
                    int catId = msg.getData().getInt("catID");
                    netState = DBManagerService.this.checkNetworkState();
                    if(netState == DBManagerService.NETWORK_AVAILABLE){
                        UpdateFromDBThread workingCatThread = new UpdateFromDBThread(id, MSG_GET_SONS_LIST, uFDBThreadHandler, parentId, catId, false);
                        workingCatThread.start();
                    }
                    break;
                // Blocco critico di richiesta singolo oggetto per il quale verrà lanciato un nuovo thread apposito.
                // Il nuovo thread comunica con il service su un messenger dedicato per aggiornare l'avanzamento dei lavori.

                case MSG_GET_SINGLE_OBJ:
                    setValue = 0;
                    id = Integer.toString(msg.arg1);
                    Bundle bd = msg.getData();
                    int idObj = bd.getInt("idObj");
                    int idCat = bd.getInt("idCat");
                    boolean isInsiderChild = bd.getBoolean("isInsiderChild");
                    netState = DBManagerService.this.checkNetworkState();
                    if(netState == DBManagerService.NETWORK_AVAILABLE){
                        try {
                            // Aumenta dello 0 per cento l'avanzamento totale della barra di stato
                            if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_START_PROGRESS, setValue, 0));
                        } catch (RemoteException e) { e.printStackTrace();
                            mClients.remove(id);
                            mClientsState.remove(id);
                        }
                        UpdateFromDBThread wThread = new UpdateFromDBThread(id, msg.what, uFDBThreadHandler, idObj, idCat, isInsiderChild);
                        wThread.start();
                    }
                    else{
                        if(netState == DBManagerService.NETWORK_UNAVAILABLE){
                            Toast.makeText(DBManagerService.this, getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
                        }
                        if(netState == DBManagerService.NETWORK_DISABLED){
                            try {
                                if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_NETWORK_STATE, 0, 0));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                        }
                    }
                    break;
                // Chiedo al service di fornire la lista delle categorie del server.
                case MSG_GET_CAT_LIST:
                    id = Integer.toString(msg.arg1);
                    netState = DBManagerService.this.checkNetworkState();
                    if(netState == DBManagerService.NETWORK_AVAILABLE){
                        UpdateFromDBThread workingCatThread = new UpdateFromDBThread(id, MSG_GET_CAT_LIST, uFDBThreadHandler, 0, 0, false);
                        workingCatThread.start();
                    }
                    break;
                // Chiedo al service di fornire la lista delle città del server.
                case MSG_GET_CITIES_LIST:
                    id = Integer.toString(msg.arg1);
                    netState = DBManagerService.this.checkNetworkState();
                    if(netState == DBManagerService.NETWORK_AVAILABLE){
                        UpdateFromDBThread workingCatThread = new UpdateFromDBThread(id, MSG_GET_CITIES_LIST, uFDBThreadHandler, 0, 0, false);
                        workingCatThread.start();
                    }
                    break;
                // Chiedo al service di fornire i dati di una singola città del server.
                case MSG_GET_CITY:
                    id = Integer.toString(msg.arg1);
                    netState = DBManagerService.this.checkNetworkState();
                    if(netState == DBManagerService.NETWORK_AVAILABLE){
                        // Se nell'array delle città il valore selezionato è 0 significa che devo
                        // tornare ad usare la posizione reale del device, quindi imposto CITY a null
                        // e comunico al client di aggiornare la lista oggetti
                        if(pf.getSelectedCity() == 0){
                            CITY = null;
                            try {
                                // Comunica al client di aggiornare la lista oggetti come se fosse arrivata una nuova città
                                Bundle b = new Bundle();
                                b.putDouble("longitudine", getCurrentLongitudine());
                                b.putDouble("latitudine", getCurrentLatitudine());
                                Message message = Message.obtain(null,MSG_DONE_CITY_PROGRESS);
                                message.setData(b);
                                if(mClientsState.get(id)) mClients.get(id).send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                        }
                        // Altrimenti avvio un thread di download da db per scaricare i dati della città
                        else{
                            UpdateFromDBThread workingCatThread = new UpdateFromDBThread(id, MSG_GET_CITY, uFDBThreadHandler, pf.getSelectedCity() , 0, false);
                            workingCatThread.start();
                        }
                    }
                    else{
                        if(netState == DBManagerService.NETWORK_UNAVAILABLE){
                            Toast.makeText(DBManagerService.this, getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
                        }
                        if(netState == DBManagerService.NETWORK_DISABLED){
                            try {
                                if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_NETWORK_STATE, 0, 0));
                            } catch (RemoteException e) { e.printStackTrace();
                                e.printStackTrace();
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                        }
                    }
                    break;
                // Dalla sdk 23 in poi, il location thread deve essere avviato a seguito di un messaggio
                // della welcome activity che segnala che l'utente ha confermato il grant dei permessi relativi
                case MSG_START_LOCATION_THREAD:
                    workingLocThread = new LocationThread(locThreadHandler);
                    DBManagerService.this.workingLocThread.start();

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    /**
     * Handler che funge da interfaccia di comunicazione con il thread LocationThread
     */
    class LocationThreadHandler extends Handler {
    	
    	@Override
        public void handleMessage(Message msg) {
    		String id = null;
    		Enumeration<String> keys;
    		double lng;
    		double lat;
    		boolean geo;
    		Bundle b;
    		Message closeSpinnerMsg;
    		ParamFetcher pf = new ParamFetcher(getApplicationContext());
    		
            switch (msg.what){
            // Aggiornamento dal location thread:
            // è cominciata la ricerca di coordinate; viene generato uno spinner sui client
        	case MSG_BEGIN_COORDS_SEARCH:
        		Message beginMsg = Message.obtain(null, MSG_START_SPINNER);
                    // Ciclo di scorrimento di tutti i client connessi
                    keys = mClients.keys();
                    while(keys.hasMoreElements()){
                		try{
                			id = keys.nextElement();
                    		if(mClientsState.get(id)) mClients.get(id).send(beginMsg);
                		}
                		catch (RemoteException e) {
                            mClients.remove(id);
                            mClientsState.remove(id);
                        }	
                    }
                break;
            	// Aggiornamento dal location thread:
                // se ci sono nuove coordinate valide aggiorno gli opprtuni campi e segnalo a tutti i client di aggiornare
                // le distanze, se invece la notifica presenta una perdita del segnale mi limito ad aggiornare l'opportuno flag.
                // Se lo stato di geolocalizzazione del service è false, chiudo gli eventuali spinner dialog sui clients
            	case MSG_UPDATE_GEOPOINT:
                	b = msg.getData();
                    lng = b.getDouble("longitudine");
                    lat = b.getDouble("latitudine");
                    geo = b.getBoolean("isgeopoint");
                    if(geo){
                    	// Ci sono coordinate valide
                    	if(DBManagerService.this.IS_GEOPOINT==false){
                    		// Lo stato di georef era false quindi chiudo gli spinner di attesa sui client.
                    		// A seguito della chiusura il client chiederà un aggiornamento della lista oggetti.
                    		DBManagerService.this.LATITUDINE = lat;
                        	DBManagerService.this.LONGITUDINE = lng;
                        	DBManagerService.this.IS_GEOPOINT = true;
                    		closeSpinnerMsg = Message.obtain(null, MSG_END_SPINNER);
                    		// Sovrascrivo le coordinate del bundle con quelle gps/city a seconda della selezione corrente;
                    		// se infatti ho una città attualmente selezionata (e scaricata) devo segnalare al client quelle
                    		// coordinate
                    		b.putDouble("longitudine", getCurrentLongitudine());
                    		b.putDouble("latitudine", getCurrentLatitudine());
                    		if(CITY!=null || pf.getSelectedCity()!=0) b.putBoolean("refresh", false);
                    		else b.putBoolean("refresh", true);
                    		closeSpinnerMsg.setData(b);
                    		keys = mClients.keys();
                            while(keys.hasMoreElements()){
                        		try{
                        			id = keys.nextElement();
                        			if(mClientsState.get(id)) mClients.get(id).send(closeSpinnerMsg);
                        		}
                        		catch (RemoteException e) {
                                    mClients.remove(id);
                                    mClientsState.remove(id);
                                }
                            }
                    	}
                    	else{
                    		// Lo stato di georef era già true, quindi segnalo ai client di eseguire solo un aggiornamento
                    		// della posizione
                    		DBManagerService.this.LATITUDINE = lat;
                    		DBManagerService.this.LONGITUDINE = lng;
                    		DBManagerService.this.IS_GEOPOINT = true;
                    		Message refreshMsg = Message.obtain(null, MSG_REFRESH_OBJ_DISTANCE);
                    		// Sovrascrivo le coordinate del bundle con quelle gps/city a seconda della selezione corrente;
                    		// se infatti ho una città attualmente selezionata (e scaricata) devo segnalare al client quelle
                    		// coordinate
                    		b.putDouble("longitudine", getCurrentLongitudine());
                    		b.putDouble("latitudine", getCurrentLatitudine());
                    		if(CITY!=null || pf.getSelectedCity()!=0) b.putBoolean("refresh", false);
                    		else b.putBoolean("refresh", true);
                    		refreshMsg.setData(b);
                    		// Ciclo di scorrimento di tutti i client connessi
                    		keys = mClients.keys();
                    		while(keys.hasMoreElements()){
                    			try{
                    				id = (String) keys.nextElement();
                    				if(mClientsState.get(id)) mClients.get(id).send(refreshMsg);
                    			}
                    			catch (RemoteException e) {
                    				mClients.remove(id);
                    				mClientsState.remove(id);
                    			}	
                    		}
                    	}
                    }
                    else{
                    	// Connessione gps e network perse da 2 minuti
                    	DBManagerService.this.IS_GEOPOINT = false;
                    }
                    break;
                    
                    // Aggiornamento dal location thread:
                    // il gps è disabilitato sul device, bisogna segnalare all'utente di abilitarlo
                	case MSG_GPS_DISABLED:
                        Message gpsMsg = Message.obtain(null, MSG_GPS_DISABLED);
                        keys = mClients.keys();
                        while(keys.hasMoreElements()){
                            try{
                            	id = (String) keys.nextElement();
                            	if(mClientsState.get(id)) mClients.get(id).send(gpsMsg);
                            }
                            catch (RemoteException e) {
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                        }
                        break;
                        
                    // Aggiornamento dal location thread:
                    // il network è disabilitato sul device, bisogna segnalare all'utente di abilitarlo
                    case MSG_NET_DISABLED:
                        Message netMsg = Message.obtain(null, MSG_NET_DISABLED);
                        keys = mClients.keys();
                        while(keys.hasMoreElements()){
                            try{
                                id = keys.nextElement();
                                if(mClientsState.get(id)) mClients.get(id).send(netMsg);
                            }
                            catch (RemoteException e) {
                                mClients.remove(id);
                                mClientsState.remove(id);
                            }
                        }
                        break;
            		
                default:
                    super.handleMessage(msg);
                
            }
    	}
    }
	
    
    /**
     * Handler che funge da interfaccia di comunicazione con il thread UpdateFromDBThread
     */
    class UpdateFromDBThreadHandler extends Handler {
    	Bundle b;
    	int currentProgress;
    	String id;
    	boolean success;
    	String errorMsg;
    	@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	// Aggiornamento dello stato di avanzamento del task
                case MSG_UPDATE_PROGRESS:
                	b = new Bundle();
                	b = msg.getData();
                    currentProgress = b.getInt("progress");
                    id = b.getString("id");
                    try {
                        // Aggiorna l'avanzamento totale della barra di stato presso il client
                    	if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_SET_PROG_BAR, currentProgress, 0));
                    } catch (RemoteException e) { 
                    	e.printStackTrace(); 
                        mClients.remove(id);
                        mClientsState.remove(id);
                    }
                    break;
                // Task di lista oggetti completato; sono gestiti sia il caso di task riuscito che fallito
                case MSG_DONE_PROGRESS:
                	b = new Bundle();
                	b = msg.getData();
                	currentProgress = b.getInt("progress");
                	id = b.getString("id");
                	success = b.getBoolean("success");
                	errorMsg = b.getString("errorMsg");
                	if(success){
                		try {
                            // Gira al client i dati ricevuti dal working thread
                            Message message = Message.obtain();
                            message.what = MSG_DONE_PROGRESS;
                            b.putDouble("longitudine", getCurrentLongitudine());
                            b.putDouble("latitudine", getCurrentLatitudine());
                            message.setData(b);
                            if(mClientsState.get(id)) mClients.get(id).send(message);
                            // Termina l'avanzamento della barra di stato (task completato)
                            if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_END_PROGRESS, currentProgress, 0));
                        } catch (RemoteException e) { 
                        	e.printStackTrace();
                            mClients.remove(id);
                            mClientsState.remove(id);
                        }
                	}
                	else{
                		// Termina l'avanzamento della barra di stato (task fallito)*/
                        try {
                        	if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_END_PROGRESS, currentProgress, 0));
							Toast.makeText(DBManagerService.this, errorMsg, Toast.LENGTH_SHORT).show();
						} catch (RemoteException e) { 
							e.printStackTrace();
							mClients.remove(id);
							mClientsState.remove(id);
						}
                	}
                	
                    break;
                 // Task di singolo oggetto completato; sono gestiti sia il caso di task riuscito che fallito
                case MSG_DONE_SINGLE_PROGRESS:
                	b = msg.getData();
                	currentProgress = b.getInt("progress");
                	id = b.getString("id");
                	success = b.getBoolean("success");
                	errorMsg = b.getString("errorMsg");
                	if(success){
                		try {
                            // Gira al client i dati ricevuti dal working thread
                            Message message = Message.obtain(null,MSG_DONE_SINGLE_PROGRESS);
                            message.setData(b);
                            if(mClientsState.get(id)) mClients.get(id).send(message);
                            // Termina l'avanzamento della barra di stato (task completato)
                            if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_END_PROGRESS, currentProgress, 0));
                        } catch (RemoteException e) { 
                        	e.printStackTrace();
                            mClients.remove(id);
                            mClientsState.remove(id);
                        }
                	}
                	else{
                		//termina l'avanzamento della barra di stato (task fallito)
                        try {
                        	if(mClientsState.get(id)) mClients.get(id).send(Message.obtain(null, MSG_END_PROGRESS, currentProgress, 0));
							Toast.makeText(DBManagerService.this, errorMsg, Toast.LENGTH_SHORT).show();
						} catch (RemoteException e) { 
							e.printStackTrace();
							mClients.remove(id);
							mClientsState.remove(id);
						}
                	}
                    break;
                    
                // Task di lista categorie completato; giro il risultato al client
                case MSG_DONE_CAT_PROGRESS:
                	b = new Bundle();
                	b = msg.getData();
                	id = b.getString("id");
                	try {
                        // Gira al client i dati ricevuti dal working thread
                        Message message = Message.obtain(null,MSG_DONE_CAT_PROGRESS);
                        message.setData(b);
                        if(mClientsState.get(id)) mClients.get(id).send(message);
                    } catch (RemoteException e) { 
                    	e.printStackTrace();
                        mClients.remove(id);
                        mClientsState.remove(id);
                    }
                    break;
                    
                // Task di lista città completato; giro il risultato al client
                case MSG_DONE_CITIES_PROGRESS:
                	b = new Bundle();
                	b = msg.getData();
                	id = b.getString("id");
                	try {
                        // Gira al client i dati ricevuti dal working thread
                        Message message = Message.obtain(null,MSG_DONE_CITIES_PROGRESS);
                        message.setData(b);
                        if(mClientsState.get(id)) mClients.get(id).send(message);
                    } catch (RemoteException e) { 
                    	e.printStackTrace();
                        mClients.remove(id);
                        mClientsState.remove(id);
                    }
                    break;	
                // Task di singola città completato;
                case MSG_DONE_CITY_PROGRESS:
                	id = msg.getData().getString("id");
                	if(msg.getData().getBoolean("success")) CITY = (City) msg.getData().getParcelable("SingleCity");
                	try {
                        // Gira al client i dati ricevuti dal working thread
                		Bundle b = new Bundle();
         				b.putDouble("longitudine", getCurrentLongitudine());
         				b.putDouble("latitudine", getCurrentLatitudine());
                        Message message = Message.obtain(null,MSG_DONE_CITY_PROGRESS);
                        message.setData(b);
                        if(mClientsState.get(id)) mClients.get(id).send(message);
                    } catch (RemoteException e) { 
                    	e.printStackTrace();
                        mClients.remove(id);
                        mClientsState.remove(id);
                    }
                    break;
                    
                case MSG_DONE_SONS_PROGRESS:
                	b = new Bundle();
                	b = msg.getData();
                	id = b.getString("id");
                	try {
                        // Gira al client i dati ricevuti dal working thread
                        Message message = Message.obtain(null,MSG_DONE_SONS_PROGRESS);
                        message.setData(b);
                        if(mClientsState.get(id)) mClients.get(id).send(message);
                    } catch (RemoteException e) { 
                    	e.printStackTrace();
                        mClients.remove(id);
                        mClientsState.remove(id);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
    	}
    }
    

    
    
    
    /**
     * Thread per la comunicazione con il back-end
     * 
     * Il thread può effettuare varie richieste, tra le quali una lista di oggetti,
     * un singolo oggetto o la lista delle categorie.
     * Al termine del task si distrugge.
     */
    private class UpdateFromDBThread extends Thread {	
        
        final static int DONE = 0;
        final static int RUNNING = 1;
        
        // Handler sul quale inviare messaggi al service
        private Handler mHandler;
        // Identificativo del client che il service dovrà poi aggiornare
        private String clientId;
        // Id dell'oggetto del quale richiedere i dettagli e della sua categoria, che identifica il
        // sottosistema remoto (nel caso di richiesta singolo oggetto) oppure id dell'oggetto padre
        // nel caso di richiesta figli
        private int objectId;
        private int catId;
        
        private int mState;
        private int progress = 0;
        private int mode;
        private boolean isInsiderChild;
        
        private ParamFetcher param;
        private JSONFetcher fetcher;
    
        /**
         * Costruttore
         * @param id Identificativo del client chiamante
         * @param m Mode: lista oggetti, singolo oggetto o lista categorie
         * @param isInsiderChild Indica se le informazioni da scaricare riguardano un sensore da mostrare
         *                       nella view di dettaglio principale o nella view dedicata ai figli di concentratori
         * @param h: handler al quale inviare i messaggi, fornito dal service
         * @param idObj: eventuale id dell'ggetto di cui richiedere i dettagli
         */
        UpdateFromDBThread(String id, int m, Handler h, int idObj, int idCat, boolean isInsiderChild) {
            mHandler = h;
            clientId = id;
            mode = m;
            objectId = idObj;
            this.isInsiderChild = isInsiderChild;
            catId = idCat;
            fetcher = new JSONFetcher(getApplicationContext());
			param = new ParamFetcher(getApplicationContext());
        }
        
        /**
         * Corpo di esecuzione del thread
         */
        @Override
        public void run() {
        	Message msg;
        	Bundle b;
            this.setState(RUNNING);
            while (mState == RUNNING) {
            	switch (mode) {
                // Reperisce una lista di oggetti dal db e aggiorna il service sullo stato delle operazioni
            	case MSG_GET_OBJ_LIST:
                	try {

            			// Creazione lista parametri da passare al JSONFetcher
            			String query_parameters = new String();
            			// Aggiunta user e password
            			query_parameters = param.getAuthParam(query_parameters);
            			
            			
            			// Aggiunta range
            			query_parameters = param.getRangeParam(query_parameters, getCurrentLongitudine(), getCurrentLatitudine());
            			// Aggiunta job e id oggetto e filtro categorie
                        query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_LIST_OBJECTS));
                        query_parameters = param.AddParam(query_parameters,"id",Integer.toString(JOB_ID_COMPLETELIST));

            			// Aggiunta filtro categorie
            			query_parameters = param.getCatFilterParam(query_parameters);
            			
            			String fetched_string = fetcher.get_data(query_parameters);
            			
            			// Gestione di eventuali errori in fase di richiesta.
                        // Segnalo il task come completato specificando il fallimento e un messaggio di errore.
            			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
            				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_PROGRESS);
                			b = new Bundle();
                            b.putInt("progress", 100);
                            b.putString("id", this.clientId);
                            b.putBoolean("success",false);
                            b.putString("errorMsg",fetched_string.substring(30));
                            msg.setData(b);
                            mHandler.sendMessage(msg);
            				this.setState(DONE);
            				break;
            	        }
            			
            			progress += 60;
            			msg = mHandler.obtainMessage(DBManagerService.MSG_UPDATE_PROGRESS);
            			b = new Bundle();
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        msg.setData(b);
                        mHandler.sendMessage(msg);

                        
                        // Parsing della lista oggetti ottenuta
                        JSONArray parsed_result = fetcher.parse_data(fetched_string);
            			progress += 20;
            			msg = mHandler.obtainMessage(DBManagerService.MSG_UPDATE_PROGRESS);
            			b = new Bundle();
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            			
            			// Per ogni oggetto ritornato istanzio un oggetto SCLO; la lista degli oggetti
                        // istanziati viene poi comunicata al service (e quindi all'activity richiedente)
            			SCLO[] olist = new SCLO[parsed_result.length()];
            			for(int i=0;i<parsed_result.length();i++){
            	    		try {
            	    			int oid = parsed_result.getJSONObject(i).getInt("ID");
            	    			String onome = parsed_result.getJSONObject(i).getString("NAME");
            	    			int ocategoria = parsed_result.getJSONObject(i).getInt("CAT");
            	    			double longitudine = parsed_result.getJSONObject(i).getDouble("LNG");
            	    			double latitudine = parsed_result.getJSONObject(i).getDouble("LAT");
            	    			int stato = parsed_result.getJSONObject(i).getInt("STATUS");
            	    			SCLO o = new SCLO(oid, onome, ocategoria, longitudine, latitudine, getCurrentLongitudine(), getCurrentLatitudine(), stato);
            	    			olist[i] = o;
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
            	    	}
            			progress += 20;
            			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_PROGRESS);
            			b = new Bundle();
            			b.putParcelableArray("ObjList", olist);
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        b.putBoolean("success", true);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
                    break;
            	case MSG_GET_SONS_LIST:
                	try {
            			
            			// Creazione lista parametri da passare al JSONFetcher
            			String query_parameters = new String();
            			// Aggiunta user e password
            			query_parameters = param.getAuthParam(query_parameters);
            			// Aggiunta job e id oggetto e filtro categorie
                        query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_LIST_SONS));
                        query_parameters = param.AddParam(query_parameters,"id",Integer.toString(this.objectId));
                        query_parameters = param.AddParam(query_parameters,"filter",Integer.toString(this.catId));
            			
            			String fetched_string = fetcher.get_data(query_parameters);
            			
            			// Gestione di eventuali errori in fase di richiesta:
                        // segnalo il task come completato specificando il fallimento e un messaggio di errore
            			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
            				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_SONS_PROGRESS);
                			b = new Bundle();
                            b.putString("id", this.clientId);
                            b.putBoolean("success",false);
                            b.putString("errorMsg",fetched_string.substring(30));
                            msg.setData(b);
                            mHandler.sendMessage(msg);
            				this.setState(DONE);
            				break;
            	        }
            			
                        // Parsing della lista oggetti ottenuta
                        JSONArray parsed_result = fetcher.parse_data(fetched_string);
            			
            			// Per ogni oggetto ritornato istanzio un oggetto SCLO; la lista degli oggetti
                        // istanziati viene poi comunicata al service (e quindi all'activity richiedente)
            			SCLO[] olist = new SCLO[parsed_result.length()];
            			for(int i=0;i<parsed_result.length();i++){
            	    		try {
            	    			int oid = parsed_result.getJSONObject(i).getInt("ID");
            	    			String onome = parsed_result.getJSONObject(i).getString("NAME");
            	    			int ocategoria = parsed_result.getJSONObject(i).getInt("CAT");
            	    			double longitudine = parsed_result.getJSONObject(i).getDouble("LNG");
            	    			double latitudine = parsed_result.getJSONObject(i).getDouble("LAT");
            	    			int stato = parsed_result.getJSONObject(i).getInt("STATUS");
            	    			SCLO o = new SCLO(oid, onome, ocategoria, longitudine, latitudine, getCurrentLongitudine(), getCurrentLatitudine(), stato);
            	    			olist[i] = o;
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
            	    	}
            			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_SONS_PROGRESS);
            			b = new Bundle();
            			b.putParcelableArray("ObjList", olist);
                        b.putString("id", this.clientId);
                        b.putInt("idParent", this.objectId);
                        b.putBoolean("success", true);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
                    break;
                // Reperisce un singolo oggetto dal db e aggiorna il service sullo stato delle operazioni.
                // L'oggetto viene identificato grazie alla coppia <id, categoria>
                case MSG_GET_SINGLE_OBJ:
                	try {
            			
            			// Creazione lista parametri da passare al JSONFetcher
            			String query_parameters = new String();
            			// Aggiunta user e password
            			query_parameters = param.getAuthParam(query_parameters);
            			// Aggiunta range
            			query_parameters = param.getRangeParam(query_parameters, getCurrentLongitudine(), getCurrentLatitudine());
            			// Aggiunta job e id oggetto e filtro categorie

                        query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_DETAILS_OBJECT));
                        query_parameters = param.AddParam(query_parameters,"id",Integer.toString(this.objectId));
                        query_parameters = param.AddParam(query_parameters,"filter",Integer.toString(this.catId));
            			
            			String fetched_string = fetcher.get_data(query_parameters);
            			
            			// Gestione di eventuali errori in fase di richiesta:
                        // segnalo il task come completato specificando il fallimento e un messaggio di errore
            			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
            				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_SINGLE_PROGRESS);
                			b = new Bundle();
                            b.putInt("progress", 100);
                            b.putString("id", this.clientId);
                            b.putBoolean("success",false);
                            b.putString("errorMsg",fetched_string.substring(30));
                            b.putBoolean("isInsiderChild", this.isInsiderChild);
                            msg.setData(b);
                            mHandler.sendMessage(msg);
            				this.setState(DONE);
            				break;
            	        }
            			
            			progress += 40;
            			msg = mHandler.obtainMessage(DBManagerService.MSG_UPDATE_PROGRESS);
            			b = new Bundle();
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
                        
                        // Parsing dell'oggetto ottenuto
                        JSONArray parsed_result = fetcher.parse_data(fetched_string);
                       
            			progress += 30;
            			msg = mHandler.obtainMessage(DBManagerService.MSG_UPDATE_PROGRESS);
            			b = new Bundle();
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            			
                        progress += 30;
                        // Viene creato un'oggetto da restituire al service e quindi alla activity di chiamata
                        // l'oggetto è restituito dalla factory di oggetti SCO (SmartCatcherObject) che conosce la logica
                        // dei sensori; in base al tipo di oggetto viene creata un'istanza del giusto tipo
                        int idCategoria = 0;
                        try {
							idCategoria = parsed_result.getJSONObject(0).getInt("idCategoria");
						} catch (JSONException e) {
							e.printStackTrace();
						}
                        SCOFactory scof = new SCOFactory();
        	    		SCO sco = scof.getSCO(idCategoria, parsed_result);
                        
            			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_SINGLE_PROGRESS);
            			b = new Bundle();
            			b.putInt("catID", idCategoria);
            			b.putParcelable("SingleObj", sco);
                        b.putInt("progress", progress);
                        b.putString("id", this.clientId);
                        b.putBoolean("success",true);
                        b.putBoolean("isInsiderChild", this.isInsiderChild);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
                    break;
                // Reperisce la lista di categorie dal db e aggiorna il service sullo stato delle operazioni
            	case MSG_GET_CAT_LIST:
                	try {
            			
            			// Creazione lista parametri da passare al JSONFetcher
            			String query_parameters = new String();
            			// Aggiunta user e password
            			query_parameters = param.getAuthParam(query_parameters);
            			// Aggiunta job e id oggetto e filtro categorie
                        query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_LIST_CATEGORIES));
                        query_parameters = param.AddParam(query_parameters,"id",Integer.toString(JOB_ID_COMPLETELIST));
                        query_parameters = param.AddParam(query_parameters,"filter",Integer.toString(JOB_ID_COMPLETELIST));

            			String fetched_string = fetcher.get_data(query_parameters);
            			
            			// Gestione di eventuali errori in fase di richiesta:
                        // segnalo il task come completato specificando negli array delle categorie i valori di default
            			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
            				CharSequence[] entries = new CharSequence[1];
                            CharSequence[] entryValues = new CharSequence[1];
                            entries[0] = "Tutte le categorie";
                            entryValues[0] = "0";
            				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CAT_PROGRESS);
                			b = new Bundle();
                            msg.setData(b);
                            b.putString("id", this.clientId);
                            b.putCharSequenceArray("CatEntriesList", entries);
                			b.putCharSequenceArray("CatValuesList", entryValues);
                            mHandler.sendMessage(msg);
            				this.setState(DONE);
            				break;
            	        }
            			
                        // Parsing della lista categorie ottenuta
                        JSONArray parsed_result = fetcher.parse_data(fetched_string);
            			
            			// Per ogni oggetto ritornato riempio gli array entries e values che serviranno a
                        // istanziare dinamicamente la lista delle categorie che si possono scegliere nelle preferences
                        CharSequence[] entries = new CharSequence[(parsed_result.length()+1)];
                        CharSequence[] entryValues = new CharSequence[(parsed_result.length()+1)];
            			
                        entries[0] = "Tutte le categorie";
                        entryValues[0] = "0";
            			for(int i=0;i<parsed_result.length();i++){
            	    		try {
            	    			int oid = parsed_result.getJSONObject(i).getInt("ID");
            	    			String onome = parsed_result.getJSONObject(i).getString("NAME");
            	    			entries[i+1] = onome;
            	    			entryValues[i+1] = Integer.toString(oid);
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
            	    	}
            			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CAT_PROGRESS);
            			b = new Bundle();
            			b.putString("id", this.clientId);
            			b.putCharSequenceArray("CatEntriesList", entries);
            			b.putCharSequenceArray("CatValuesList", entryValues);
                        b.putBoolean("success", true);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
                    break;
                // Reperisce la lista di città dal db e aggiorna il service sullo stato delle operazioni
            	case MSG_GET_CITIES_LIST:
                	try {
            			
            			// Creazione lista parametri da passare al JSONFetcher
            			String query_parameters = new String();
            			// Aggiunta user e password
            			query_parameters = param.getAuthParam(query_parameters);
            			// Aggiunta job e id oggetto e filtro categorie

                        query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_LIST_CITIES));
                        query_parameters = param.AddParam(query_parameters,"id",Integer.toString(JOB_ID_COMPLETELIST));
                        query_parameters = param.AddParam(query_parameters,"filter",Integer.toString(JOB_ID_COMPLETELIST));

            			String fetched_string = fetcher.get_data(query_parameters);
            			
            			// Gestione di eventuali errori in fase di richiesta:
                        // segnalo il task come completato specificando negli array delle categorie i valori di default
            			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
            				CharSequence[] entries = new CharSequence[1];
                            CharSequence[] entryValues = new CharSequence[1];
                            entries[0] = "Usa la tua posizione";
                            entryValues[0] = "0";
            				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CITIES_PROGRESS);
                			b = new Bundle();
                            msg.setData(b);
                            b.putString("id", this.clientId);
                            b.putCharSequenceArray("CitiesEntriesList", entries);
                			b.putCharSequenceArray("CitiesValuesList", entryValues);
                            mHandler.sendMessage(msg);
            				this.setState(DONE);
            				break;
            	        }
            			
                        // Parsing della lista categorie ottenuta
                        JSONArray parsed_result = fetcher.parse_data(fetched_string);
            			
            			// Per ogni oggetto ritornato riempio gli array entries e values che serviranno a
                        // istanziare dinamicamente la lista delle categorie che si possono scegliere nelle preferences
                        CharSequence[] entries = new CharSequence[(parsed_result.length()+1)];
                        CharSequence[] entryValues = new CharSequence[(parsed_result.length()+1)];
            			
                        entries[0] = "Usa la tua posizione";
                        entryValues[0] = "0";
            			for(int i=0;i<parsed_result.length();i++){
            	    		try {
            	    			int oid = parsed_result.getJSONObject(i).getInt("ID");
            	    			String onome = parsed_result.getJSONObject(i).getString("NAME");
            	    			entries[i+1] = onome;
            	    			entryValues[i+1] = Integer.toString(oid);
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
            	    	}
            			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CITIES_PROGRESS);
            			b = new Bundle();
            			b.putString("id", this.clientId);
            			b.putCharSequenceArray("CitiesEntriesList", entries);
            			b.putCharSequenceArray("CitiesValuesList", entryValues);
                        b.putBoolean("success", true);
                        msg.setData(b);
                        mHandler.sendMessage(msg);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
                    break;
                    // Reperisce una singola città dal db e aggiorna il service sullo stato delle operazioni
                    case MSG_GET_CITY:
                    	try {
                			
                			// Creazione lista parametri da passare al JSONFetcher
                			String query_parameters = new String();
                			// Aggiunta user e password
                			query_parameters = param.getAuthParam(query_parameters);
                			// Aggiunta job e id oggetto
                			query_parameters = param.AddParam(query_parameters,"job",Integer.toString(JOB_DETAILS_CITY));
                            query_parameters = param.AddParam(query_parameters,"id",Integer.toString(this.objectId));
                			
                			String fetched_string = fetcher.get_data(query_parameters);
                			
                			// Gestione di eventuali errori in fase di richiesta:
                            // segnalo il task come completato specificando il fallimento e un messaggio di errore
                			if(fetched_string.contains("##IOTM_SERVER_ERROR_RESPONSE##")) {
                				msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CITY_PROGRESS);
                    			b = new Bundle();
                                b.putString("id", this.clientId);
                                b.putBoolean("success",false);
                                b.putString("errorMsg",fetched_string.substring(30));
                                msg.setData(b);
                                mHandler.sendMessage(msg);
                				this.setState(DONE);
                				break;
                	        }
                            
                            // Parsing dell'oggetto ottenuto
                            JSONArray parsed_result = fetcher.parse_data(fetched_string);
                           
                            // Viene creato un'oggetto city da restituire al service e quindi alla activity chiamante
                            int idCity = 0;
                            String nation = "";
                            String name = "";
                            double lng = -1000;
                            double lat = -1000;
                            int gmt = -1000;
                            try {
    							idCity = parsed_result.getJSONObject(0).getInt("ID");
    							nation = parsed_result.getJSONObject(0).getString("NATION");
    							name = parsed_result.getJSONObject(0).getString("NAME");
    							lng = parsed_result.getJSONObject(0).getDouble("LNG");
    							lat = parsed_result.getJSONObject(0).getDouble("LAT");
    							gmt = parsed_result.getJSONObject(0).getInt("GMT");
    						} catch (JSONException e) {
    							e.printStackTrace();
    						}
                            
            	    		City city = new City(idCity, name, nation, lng, lat, gmt);
                            
                			msg = mHandler.obtainMessage(DBManagerService.MSG_DONE_CITY_PROGRESS);
                			b = new Bundle();
                			b.putParcelable("SingleCity", city);
                            b.putString("id", this.clientId);
                            b.putBoolean("success",true);
                            msg.setData(b);
                            mHandler.sendMessage(msg);
                		} catch (Exception e) {
                			e.printStackTrace();
                		}
                        break;
                    default:
                    	break;
            	}
            	
            	this.setState(DONE);                
            }
        }
        
        /**
         * Imposta lo stato del thread (running o done)
         */
        public void setState(int state) {
            mState = state;
        }
    }
    
    
    
    
    
    /**
     * Thread per la ricerca di coordinate geografiche
     * 
     * Il thread istanzia due listener, uno sul provider gps e uno su quello di rete
     * Questo thread vive per tutta la vita dell'applicazione
     * Le coordinate gps vengono preferite a quelle network se disponibili
     * Il thread comunica al service le coordinate periodicamente e se per 2 minuti non ha
     * aggiornamenti segnala al service di aver perso il segnale.
     */
    private class LocationThread extends Thread {
        
    	final static int DONE = 0;
        final static int RUNNING = 1;
    	
    	// Handler sul quale inviare messaggi al service
        Handler mHandler;
        
        private LocationManager locationManager;
        
        // Attuali coordinate di rete e gps, le quali contengono oltre a longitudine e latitudine anche il timestamp di creazione
        private GeoCoord gpsCoords = null;
        private GeoCoord netCoords = null;
        
        private LocListener gpsListener;
        private LocListener netListener;
        
        private boolean newCoordsAvailable = false;
        
        // Timestamp dell'ultimo invio di coordinate al service
        private Timestamp ultimoInvio = null;
        
        private boolean lostSignalFlag = true;
        
        private Message msg;
    	private Bundle b;
    	private Date date;
    	private Timestamp now;
    	
    	private int mState;
    	
        /**
         * Costruttore
         *
         * Istanzia il location manager e si collega al servizio remoto del sistema Android per la gestione
         * della georeferenziazione; vengono istanziati i due listener che ascoltano cambiamenti di posizione
         * su intervalli di 30 secondi con la precisione del metro.
         * Vengono informati i clients che la ricerca coordinate ha avuto inizio
         * @param h Handler sul quale comunicare fornito dal service
         */
        LocationThread(Handler h) {
        	
        	this.mHandler = h;

        	this.date = new Date();
        	this.now = new Timestamp(date.getTime());


            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        			
            this.gpsListener = new LocListener(LocationManager.GPS_PROVIDER);
            this.netListener = new LocListener(LocationManager.NETWORK_PROVIDER);

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 1, gpsListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 1, netListener);
            }
            catch (SecurityException s){

            }
            
        }
        
        /**
         * Corpo di esecuzione del thread
         */
        @Override
        public void run() {
        	
        	this.mState = RUNNING;
        	
    		try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		
            try{
				Message beginMsg = Message.obtain(null, DBManagerService.MSG_BEGIN_COORDS_SEARCH);
				mHandler.sendMessage(beginMsg);
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
            while (mState == RUNNING) {
            	
               	if(this.newCoordsAvailable){
            		this.newCoordsAvailable = false;
            		this.lostSignalFlag = true;
            		// Verifica se inviare le coordinate al service;
            		// se sono da inviare setta il campo di ultimo invio al timestamp delle coordinate inviate
            		if(this.ultimoInvio==null){
            			// Non ci sono ancora coordinate le prendo comunque
            			if(this.gpsCoords!=null){
            				// Usiamo le nuove coordinate gps e le spediamo al service
            				try{
            					b = new Bundle();
            					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
            					b.putDouble("longitudine", this.gpsCoords.longitudine);
            					b.putDouble("latitudine", this.gpsCoords.latitudine);
            					b.putBoolean("isgeopoint", true);
            					msg.setData(b);
            					mHandler.sendMessage(msg);
            					this.ultimoInvio = this.gpsCoords.lastUpdate;
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
            			}
            			else if(this.netCoords!=null){
            				// Usiamo le nuove coordinate network e le spediamo al service
            				try{
            					b = new Bundle();
            					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
            					b.putDouble("longitudine", this.netCoords.longitudine);
            					b.putDouble("latitudine", this.netCoords.latitudine);
            					b.putBoolean("isgeopoint", true);
            					msg.setData(b);
            					mHandler.sendMessage(msg);
            					this.ultimoInvio = this.netCoords.lastUpdate;
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
            			}
            		}
            		else{
            			// Esistono già delle coordinate inviate, quindi controllo se e quali coordinate inviare al service
            			if(this.gpsCoords!=null){
                			// Esistono coordinate gps
                			if(this.gpsCoords.lastUpdate.after(this.ultimoInvio)){
                				// Ci sono coordinate gps nuove rispetto all'ultimo invio, le usiamo e le notifichiamo al service
                				try{
                					b = new Bundle();
                					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
                					b.putDouble("longitudine", this.gpsCoords.longitudine);
                					b.putDouble("latitudine", this.gpsCoords.latitudine);
                					b.putBoolean("isgeopoint", true);
                					msg.setData(b);
                					mHandler.sendMessage(msg);
                					this.ultimoInvio = this.gpsCoords.lastUpdate;
                				} catch (Exception e) {
                					e.printStackTrace();
                				}
                			}
                			else{
                                // Se le coord gps non hanno timestamp maggiore dell'ultimo invio allora le nuove coordinate
                                // sono di tipo network; le useremo solo se le coordinate gps sono state aggiornate più di un minuto fa
                				Date date = new Date();
                        		Timestamp now = new Timestamp(date.getTime());
                				if(now.getTime() - this.gpsCoords.lastUpdate.getTime() > 60000){
                					// Usiamo le nuove coordinate network e le spediamo al service
                					try{
                    					b = new Bundle();
                    					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
                    					b.putDouble("longitudine", this.netCoords.longitudine);
                    					b.putDouble("latitudine", this.netCoords.latitudine);
                    					b.putBoolean("isgeopoint", true);
                    					msg.setData(b);
                    					mHandler.sendMessage(msg);
                    					this.ultimoInvio = this.netCoords.lastUpdate;
                    				} catch (Exception e) {
                    					e.printStackTrace();
                    				}
                				}
                			}
                		}
                		else{
                			// Non ci sono coordinate gps disponibili, usiamo quelle di rete e le inviamo al service
                			try{
            					b = new Bundle();
            					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
            					b.putDouble("longitudine", this.netCoords.longitudine);
            					b.putDouble("latitudine", this.netCoords.latitudine);
            					b.putBoolean("isgeopoint", true);
            					msg.setData(b);
            					mHandler.sendMessage(msg);
            					this.ultimoInvio = this.netCoords.lastUpdate;
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
                		}
            		}
            		
            	}
            	
               	
            	// Se non ci sono coordinate aggiornate da 2 minuti segnalo al service di aver perso il segnale
        		now.setTime(date.getTime());
            	
        		if(this.ultimoInvio != null){
        			if((now.getTime() - this.ultimoInvio.getTime() > 120000) && this.lostSignalFlag){
        				try{
        					b = new Bundle();
        					msg = Message.obtain(null, DBManagerService.MSG_UPDATE_GEOPOINT);
        					this.lostSignalFlag = false;
        					b.putDouble("longitudine", 0);
        					b.putDouble("latitudine", 0);
        					b.putBoolean("isgeopoint", false);
        					msg.setData(b);
        					mHandler.sendMessage(msg);
        					this.ultimoInvio = null;
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
        			}
        		}
            	
            	b = null;
            	msg = null;
            	
            	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	                
            }
           
            locationManager.removeUpdates(gpsListener);
            locationManager.removeUpdates(netListener);
            this.gpsListener = null;
            this.netListener = null;
            
        }
        
        /**
         * Imposta lo stato del thread (running o done)
         */
        public void setState(int state) {
            mState = state;
        }
        
        
        /**
         * Classe che costituisce semplice rappresentazione di coordinate geografiche con contesto temporale
         */
        private class GeoCoord{
        	
        	double longitudine;
        	double latitudine;
        	Timestamp lastUpdate;
        	
        	/**
        	 * Costruttore
        	 * Salva il timestamp di creazione
             */

        	public GeoCoord(double lat, double lng){
        		this.latitudine = lat;
        		this.longitudine = lng;
        		Date date = new Date();
        		this.lastUpdate = new Timestamp(date.getTime());
        	}
        	
        }
        
        
        /**
         * Implementazione del location listener; può essere di tipo gps o network
         */
        private class LocListener implements LocationListener{
        	
        	private String provider;
        	
        	public LocListener(String provider){
        		this.provider = provider;
        	}
        	
        	/**
        	 * Metodo richiamato quando il listener in ascolto percepisce uno spostamento e quindi nuove coordinate
        	 * del device; il thread valuterà poi se inviarle o meno al service
        	 */
        	public void onLocationChanged(Location location) {
            	
            	double lat = location.getLatitude();
    			double lng = location.getLongitude();
    			
    			if(this.provider.equals(LocationManager.NETWORK_PROVIDER)){
    				LocationThread.this.netCoords = new GeoCoord(lat, lng);
    				// Segnala al thread che ci sono nuove coordinate disponibili
    				LocationThread.this.newCoordsAvailable = true;
    			}
    			if(this.provider.equals(LocationManager.GPS_PROVIDER)){
    				LocationThread.this.gpsCoords = new GeoCoord(lat, lng);
    				// Ssegnala al thread che ci sono nuove coordinate disponibili
    				LocationThread.this.newCoordsAvailable = true;
    			}
    			
        	}

        	public void onStatusChanged(String provider, int status, Bundle extras) {
        		
        	}

        	public void onProviderEnabled(String provider) {
        		
        	}

        	public void onProviderDisabled(String provider) {
        		
        	}
        	
        }
           
        
    }
    
    
}
