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

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.lc.iotmanagerclient.R;


/**
 * Schermata di benvenuto.
 * 
 * Activity che mostra il logo dell'applicazione; si collega al service in modo da istanziare subito la richiesta di coordinate
 * gps o network. Quando vengono rese disponibili delle coordinate l'activity manda un intent alla main activity.
 * Questa activity non viene salvata nella pila delle activity aperte, quindi se si esegue uno step back viene distrutta
 * (non avrebbe senso visionarla dopo il lancio)
 * Layout di riferimento "res/layout/welcome.xml".
 */
public class WelcomeActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback{

    // Versione SDK del device
    private int SDK_VERSION = android.os.Build.VERSION.SDK_INT;

    private boolean requestPermission = false;

	private boolean IS_FOREGROUND;
	
	// Dialog per la progressione dei task di background
	ProgressDialog spinnerDialog;
	
	// Tipologia di dialog per la ricerca di coordinate gps o network
    static final int DIALOG_SEARCH_COORDS = 0;

    // Codice di richiesta permessi (da API 23 o superiore)
    static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 100;
   
	
	/** 
	 * Creazione dell'activity. Viene subito richiesto il bind al service che fornisce i dati circa
	 * l'attuale posizione geografica
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.welcome);
        
        this.IS_FOREGROUND = true;
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        List<String> listPermissionsNeeded = new ArrayList<>();
        // Check permissions
        if(this.SDK_VERSION >= 23) {
            int perm_fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int perm_coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            int perm_netstate = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
            if (perm_coarse != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (perm_fine != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (perm_netstate != PackageManager.PERMISSION_GRANTED)
                listPermissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE);

            if (listPermissionsNeeded.isEmpty()) {
                // I permessi sono già stati accordati, segnalo al service di far partire il location thread
                requestPermission = true;
            }
        }

        // Collegamento al service
        doBindService();

        // Check permissions
        if(this.SDK_VERSION >= 23) {
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        }


    }

    /**
     * Metodo di supporto alla gestione dei permessi da API 23 o superiore
     */
    private static Intent getForegroundIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    /**
     * Metodo scatenato quanto vengono accreditati o meno una serie di permessi
     * @param requestCode Codice di richiesta permessi
     * @param permissions Permessi richiesti
     * @param grantResults Permessi accordati
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_NETWORK_STATE, PackageManager.PERMISSION_GRANTED);

                // Riempie l'array con i permessi accordati
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED){
                    // Tutti i permessi necessari sono stati accordati, riporta la WelcomeActivity
                    // in foreground e segnala al service di far partire il location thread
                    try {
                        int id = WelcomeActivity.this.hashCode();
                        Message msg = Message.obtain(null, DBManagerService.MSG_START_LOCATION_THREAD, id, 0);
                        msg.replyTo = mMessenger;
                        mService.send(msg);
                    } catch (RemoteException e) {

                    }
                    Intent intent = getForegroundIntent(getApplicationContext(), WelcomeActivity.class);
                    startActivity(intent);
                }
                else {
                    // Permessi non accordati
                    Toast abortMsg = Toast.makeText(this, R.string.permission_denied_dialog,Toast.LENGTH_LONG);
                    abortMsg.show();
                    finish();
                }

            }
            break;

            default:
                break;

        }
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
                	int id = WelcomeActivity.this.hashCode();
                	// Segnalo al service che l'activity va in pausa
                    Message msg = Message.obtain(null, DBManagerService.MSG_PAUSE_CLIENT, id, 0);
                    mService.send(msg);
                } catch (RemoteException e) {
                    
                }
            }
        }
    }

    /**
     * Gestione dell'evento onStop dell'activity.
     */
    @Override
    public void onStop() {
        super.onStop();   
    }

    /**
     * Gestione dell'evento onStart dell'activity.
     */
    @Override
    public void onStart() {
        super.onStart();   
    }

    /**
     * Gestione dell'evento onRestart dell'activity.
     */
    @Override
    public void onRestart() {
        super.onRestart();   
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
                	int id = WelcomeActivity.this.hashCode();
                	// Segnalo al service che l'activity è di nuovo in foreground
                    Message msg = Message.obtain(null, DBManagerService.MSG_RESUME_CLIENT, id, 0);
                    mService.send(msg);
                } catch (RemoteException e) {
                    
                }
            }
        }
    }
    
    
    /**
     * Metodo richiamato alla richiesta di creazione di un dialog per ricerca coordinate geografiche
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_SEARCH_COORDS:
            spinnerDialog = new ProgressDialog(this);
            spinnerDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            spinnerDialog.setCancelable(false);
            spinnerDialog.setCanceledOnTouchOutside(false);
            spinnerDialog.setMessage(getString(R.string.gps_search));
            return spinnerDialog;
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
     * Classe Handler che gestisce operativamente le azioni da eseguire in base ai messaggi ricevuti dal service
     */
    class IncomingHandler extends Handler {
    	Bundle b;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	// Comincia un task di ricerca coordinate geografiche; mostro uno spinner
            	case DBManagerService.MSG_START_SPINNER:
            		showDialog(WelcomeActivity.DIALOG_SEARCH_COORDS);
            		break;
                // Termina il task di richiesta coordinate geografiche; chiudo lo spinner e lancio la main activity
            	case DBManagerService.MSG_END_SPINNER:
            		if(spinnerDialog.isShowing()){
            			dismissDialog(WelcomeActivity.DIALOG_SEARCH_COORDS);
            		}
            		b = msg.getData();
            		double lng = b.getDouble("longitudine");
            		double lat = b.getDouble("latitudine");
            		Intent intentMainActivity = new Intent(getApplicationContext(), TabMainActivity.class);
            		intentMainActivity.putExtra("longitudine", lng);
            		intentMainActivity.putExtra("latitudine", lat);
            		startActivity(intentMainActivity);
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
    	 * L'activity manda subito un messaggio per registrarsi e segnalare la propria identità al service
    	 */
        public void onServiceConnected(ComponentName className,IBinder service) {
           
            mService = new Messenger(service);
            
            try {
            	int id = WelcomeActivity.this.hashCode();
            	// Mi registro al service con l'identità definita in id
                Message msg = Message.obtain(null, DBManagerService.MSG_REGISTER_CLIENT, id, 0);
                msg.replyTo = mMessenger;
                mService.send(msg);
                if (requestPermission) {
                    // Segnalo al service di far partire il location thread
                    msg = Message.obtain(null, DBManagerService.MSG_START_LOCATION_THREAD, id, 0);
                    mService.send(msg);
                }
                
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
        bindService(new Intent(WelcomeActivity.this, DBManagerService.class), mConnection, Context.BIND_AUTO_CREATE);
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
                	int id = WelcomeActivity.this.hashCode();
                	// Cancello la mia identità (id) dal service
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