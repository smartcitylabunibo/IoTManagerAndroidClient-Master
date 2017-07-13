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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.SCO.SCO;

/**
 * Activity di dettaglio
 * 
 * Questa activity si occupa sostanzialmente di mostrare i dettagli di un sensore presente sulla
 * Main Activity; anche qui è previsto il bind del service per la comunicazione
 * Layout di riferimento provvisorio "res/layout/object_detail.xml".
 */
public class ObjectDetailsActivity extends Activity {
    
	private boolean IS_FOREGROUND;
	
	//Tipologia di dialog per la ricerca di coordinate gps o network
    static final int DIALOG_SEARCH_COORDS = 0;
    
	//Tipologia di dialog per l'aggiornamento di dati dal db remoto
    static final int DIALOG_DATA_FROM_DB = 1;
	
	//Dialog per la progressione dei task di background
	ProgressDialog progDialog;
	ProgressDialog spinnerDialog;
	
	//Oggetto da mostrare
	SCO oggetto;
	
	//Id (chiave primaria sul data base remoto) dell'oggetto da mostrare
	int ID;
	//Categoria (che implica univocamente un sottosistema di sensori) dell'oggetto da mostrare
	int CAT;
	
	/**
	 * Creazione dell'activity, viene collegato il service e reperito dalla main activity chiamante
     * l'id dell'oggetto che occorre scaricare dal back-end
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        //Setta una view provvisoria vuota in attesa di ricevere la view specifica pertinente al tipo
        //di oggetto richiesto al db che sarà ritornato dal service
        setContentView(R.layout.object_detail);
        
        this.IS_FOREGROUND = true;
        
        doBindService();
        
        ID = (getIntent().getIntExtra("obj_details",0));
        CAT = (getIntent().getIntExtra("cat_details",0));
        
    }
    
    
    /**
     * Distruzione dell'activity e contestuale scollegamento dal service.
     * Se l'activity era l'unica rimasta collegata al service, si distruggerà anche quest'ultimo.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();       
    }

    /**
     * Gestione dell'evento onPause dell'activity.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mIsBound) {
            if (mService != null) {
                try {
                	int id = ObjectDetailsActivity.this.hashCode();
                	//segnalo al service che l'activity va in pausa
                    Message msg = Message.obtain(null, DBManagerService.MSG_PAUSE_CLIENT, id, 0);
                    mService.send(msg);
                } catch (RemoteException e) {
                    
                }
            }
        }
    }

    /**
     * Gestione dell'evento onResume dell'activity.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mIsBound) {
            if (mService != null) {
                try {
                	int id = ObjectDetailsActivity.this.hashCode();
                	//segnalo al service che l'activity è di nuovo in foreground
                    Message msg = Message.obtain(null, DBManagerService.MSG_RESUME_CLIENT, id, 0);
                    mService.send(msg);
                } catch (RemoteException e) {
                    
                }
            }
        }
    }
    
    /**
     * Metodo richiamato alla richiesta di creazione di un dialog
     * Sono gestite due tipologie, il dialog con progress bar (per task quali l'aggiornamento dei dati dal db)
     * e il dialog con spinner, per task quali la ricerca di coordinate gps per i quali non è possibile prevedere a
     * priori una durata complessiva e uno stato di avanzamento
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_SEARCH_COORDS:
        	spinnerDialog = new ProgressDialog(this);
        	spinnerDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        	spinnerDialog.setMessage(getString(R.string.gps_search));
            return spinnerDialog;
        case DIALOG_DATA_FROM_DB:
            progDialog = new ProgressDialog(this);
            progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progDialog.setMax(100);
            progDialog.setMessage(getString(R.string.db_download));
            return progDialog;
        default:
            return null;
        }
    }
    
    
     /**
     * SEGUE PORZIONE DI CODICE PER IL COLLEGAMENTO AL DB MANAGER SERVICE
     */
    
      // Messenger per inviare messaggi al service.
      Messenger mService = null;
      // Flag che indica se il service è stato bindato.
      boolean mIsBound;
      // Messenger per ricevere dati dal service
      final Messenger mMessenger = new Messenger(new IncomingHandler());
      
      /**
       * Classe Handler che gestisce operativamente le azioni da eseguire in base ai messaggi ricevuti dal service.
       */
      class IncomingHandler extends Handler {
      	int progress;
          @Override
          public void handleMessage(Message msg) {
              switch (msg.what) {
                // Comincia un task di richiesta dati al db; mostro una progress bar
              	case DBManagerService.MSG_START_PROGRESS:
              		showDialog(DIALOG_DATA_FROM_DB);
              		progress = msg.arg1;
              		progDialog.setProgress(progress);
                    break;
                // Termina un task di richiesta dati al db; chiudo la progress bar
              	case DBManagerService.MSG_END_PROGRESS:
              		progress = msg.arg1;
              		progDialog.setProgress(progress);
              		dismissDialog(DIALOG_DATA_FROM_DB);
                    break;
                // Aggiorno la progress bar in base allo stato di avanzamento dichiarato dal service
                case DBManagerService.MSG_SET_PROG_BAR:
                  	progress = msg.arg1;
                    progDialog.setProgress(progress);
                    break;
                // Comincia un task di ricerca coordinate geografiche; mostro uno spinner
              	case DBManagerService.MSG_START_SPINNER:
              		showDialog(ObjectDetailsActivity.DIALOG_SEARCH_COORDS);
                    break;
                // Termina il task di richiesta coordinate geografiche; chiudo lo spinner
              	case DBManagerService.MSG_END_SPINNER:
              		if(spinnerDialog.isShowing()){
              			dismissDialog(ObjectDetailsActivity.DIALOG_SEARCH_COORDS);
              		}
              		break;
                // Il service segnala che la rete è disabilitata, quindi viene segnalato all'utente
                // e gli viene data la possibilità di entrare nei settings delle wireless connection di sistema
              	case DBManagerService.MSG_NETWORK_STATE:
              		new AlertDialog.Builder(ObjectDetailsActivity.this)
                      .setTitle(getString(R.string.network_required))
                      .setMessage(getString(R.string.network_dialog))
                      .setPositiveButton(getString(R.string.network_dialog_wifi_button), new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int which) {
                              startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                          }
                      })
                      .setNeutralButton(getString(R.string.network_dialog_mobile_button), new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int which) {
                              startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                          }
                      })
                      .setNegativeButton(getString(R.string.network_dialog_cancel_button), new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int which) {
                          	
                          }
                      })
                      .show();
                    break;
                  // Riceve l'oggetto che è stato richiesto al db remoto
                  // I dati vengono spacchettati dal parcel, e viene aggiornata la view
                  case DBManagerService.MSG_GET_SINGLE_OBJ:
                      Bundle b = msg.getData();
                      oggetto = b.getParcelable("SingleObj");
                      View v = oggetto.createView(ObjectDetailsActivity.this);
                      setContentView(v);
                      break;
                  case DBManagerService.MSG_REFRESH_OBJ_DISTANCE:
                      break;
                  default:
                      super.handleMessage(msg);
              }
          }
          
      }

      
      /**
       * Interfaccia per il monitoraggio della connessione al service.
       */
      private ServiceConnection mConnection = new ServiceConnection() {
    	 /**
      	 * Metodo richiamato quando il bind al service va a buon fine
      	 * Viene istanziato il messenger per comunicare messaggi al service.
      	 * La activity manda subito un messaggio per registrarsi e segnalare la propria identità al service
      	 */
    	  public void onServiceConnected(ComponentName className, IBinder service) {
         
              mService = new Messenger(service);
             
              try {
              	  int id = ObjectDetailsActivity.this.hashCode();
              	  // mi registro al service con l'identità definita in id
                  Message msg = Message.obtain(null, DBManagerService.MSG_REGISTER_CLIENT, id, 0);
                  msg.replyTo = mMessenger;
                  mService.send(msg);
                  
                  // richiedo il singolo oggetto da mostrare
                  Bundle b = new Bundle();
                  b.putInt("idObj", ID);
                  b.putInt("idCat", CAT);
                  msg = Message.obtain(null, DBManagerService.MSG_GET_SINGLE_OBJ, id, 0);
                  msg.setData(b);
                  mService.send(msg);
                  
              } catch (RemoteException e) {
                  
              }   
          }

    	 /**
      	 * Metodo richiamato all'improvvisa disconnessione dal service, ad esempio per un crash dell'applicazione
      	 */
          public void onServiceDisconnected(ComponentName className) {
              mService = null;
          }
      };

      /**
       * Tenta di effettuare una connessione al service
       */
      void doBindService() {
          bindService(new Intent(ObjectDetailsActivity.this, DBManagerService.class), mConnection, Context.BIND_AUTO_CREATE);
          mIsBound = true;
      }

      /**
       * Tenta di effettuare una disconnessione dal service
       * Se il service risultava connesso, effettua contestualmente la cancellazione dell'identità dell'activity
       * dalla lista dei client connessi al service inviando un opportuno messaggio
       */
      void doUnbindService() {
          if (mIsBound) {
              if (mService != null) {
                  try {
                  	  int id = ObjectDetailsActivity.this.hashCode();
                  	  // cancello la mia identità (id) dal service
                      Message msg = Message.obtain(null, DBManagerService.MSG_UNREGISTER_CLIENT, id, 0);
                      msg.replyTo = mMessenger;
                      mService.send(msg);
                  } catch (RemoteException e) {
                      
                  }
              }

              unbindService(mConnection);
              mIsBound = false;         
          }
      }

      /**
       * FINE PORZIONE DI CODICE PER IL COLLEGAMENTO AL DB MANAGER SERVICE
       */
    
}