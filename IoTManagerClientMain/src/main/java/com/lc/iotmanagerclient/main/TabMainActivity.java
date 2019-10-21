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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.fragment.app.FragmentActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.lc.iotmanagerclient.R;
import com.lc.iotmanagerclient.SCO.SCLO;
import com.lc.iotmanagerclient.SCO.SCO;
import com.lc.iotmanagerclient.SCO.SCODefault;
import com.lc.iotmanagerclient.utility.ParamFetcher;
import com.lc.iotmanagerclient.utility.SCMarkerGroup;


/**
 * Schermata principale.
 * 
 * Activity che mostra una lista di SCLO richiedendoli al back-end.
 * Ogni sensore può appartenere a una tre le categorie gestite; al click
 * dell'oggetto viene richiamata un'activity che ne mostra i dettagli. Il
 * caricamento della lista è subordinato alla georeferenziazione del device
 * client in quanto devono essere ritornati solo gli oggetti in un raggio di
 * interesse rispetto alla posizione dell'utente.
 * Layout di riferimento: "res/layout/tab_layout.xml".
 */
public class TabMainActivity extends FragmentActivity implements
		OnNavigationListener, OnMarkerClickListener, OnMapReadyCallback {

	// Longitudine e latitudine attualmente in uso (aggiornate dal service)
	private double myLongitude;
	private double myLatitude;

	// Oggetto per gestire la Google Map
	private GoogleMap mGoogleMap;

	// Gestore dei parametri e dei settings
	ParamFetcher pf;

	// Dialog per la progressione dei task di background
	ProgressDialog progDialog;
	ProgressDialog spinnerDialog;


	 // Array per la gestione dinamica della lista cetgorie e delle citta nei setting
	 // dell'applicazione questi array vengono aggiornati in base alle categorie e alle citta
	 // ritornate dal service diversamente viene lasciato il default
	private CharSequence[] CATEGORIES_ENTRIES = { "Tutte le categorie" };
	private CharSequence[] CATEGORIES_VALUES = { "0" };
	private CharSequence[] CITIES_ENTRIES = { "Usa la tua posizione" };
	private CharSequence[] CITIES_VALUES = { "0" };

	// Tipologia di dialog per la ricerca di coordinate gps o network
	static final int DIALOG_SEARCH_COORDS = 0;

	// Tipologia di dialog per l'aggiornamento di dati dal back-end
	static final int DIALOG_DATA_FROM_DB = 1;

	// Vettore degli oggetti mostrati nella listview (eventualmente filtrati in base alla scelta nella action bar)
	private SCLO[] OLIST;
	// Lista complessiva di sensori attualmente presenti nel bounding box (non filtrata)
	private ArrayList<SCLO> itemList;

	// Mappa di SCMarkerGroup per la gestione dei gruppi di icone mostrate in overlay
	private Map<Integer, SCMarkerGroup> overlayMap;

	private FragmentManager fm;
	private TabListFragment list;
	private TabDetailsFragment details;
	private SupportMapFragment map;
	private LinearLayout mapLL;
	private LinearLayout detailsLL;

	// Categoria attualmente selezionata nello spinner dell'action bar
	private int currentSpinnerCat = 0;

	// Oggetto di dettaglio
	SCO oggetto;

	// Flag di supporto per gestire l'avvio automatico della richiesta oggetti a seguito
	// dell'inserimento dei parametri di login alla prima connessione
	private boolean firstConnection = false;

	/**
	 * Creazione dell'activity. Viene subito richiesto il bind al service che
	 * fornisce i dati critici quali l'attuale posizione geografica e i dati
	 * provenienti dal back-end
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tab_layout);

		this.myLongitude = getIntent().getDoubleExtra("longitudine", 0);
		this.myLatitude = getIntent().getDoubleExtra("latitudine", 0);

		pf = new ParamFetcher(getApplicationContext());

		updateCatSpinner();

		fm = getFragmentManager();
		list = (TabListFragment) fm.findFragmentById(R.id.listFragment);

		map = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
		map.getMapAsync(this);

		details = (TabDetailsFragment) fm.findFragmentById(R.id.detailsFragment);
		mapLL = (LinearLayout) findViewById(R.id.mapLinearLayout);
		detailsLL = (LinearLayout) findViewById(R.id.detailsLinearLayout);

		// Collegamento al service
		doBindService();

	}

	/**
	 * Metodo che gestisce la visibilità della mappa o del contenitore di dettaglio del sensore
	 */
	public void toggleView() {
		if (mapLL.getVisibility() == View.VISIBLE) {
			mapLL.setVisibility(View.GONE);
			detailsLL.setVisibility(View.VISIBLE);
		} else {
			mapLL.setVisibility(View.VISIBLE);
			detailsLL.setVisibility(View.GONE);
		}
	}


	/**
	 * Crea un options menu.
	 * Layout di riferimento: "res/menu/menu.xml".
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Collega ogni voce del menu alle opportune azioni
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			if (mService != null) {
				if (TabMainActivity.this.CATEGORIES_ENTRIES.length == 1 || TabMainActivity.this.CATEGORIES_VALUES.length == 1) {
					try {
						int id = TabMainActivity.this.hashCode();
						// Segnala al service di aggiornare la lista categorie disponibili
						Message msg = Message.obtain(null,
								DBManagerService.MSG_GET_CAT_LIST, id, 0);
						mService.send(msg);

					} catch (RemoteException e) {

					}
				}
				if (TabMainActivity.this.CITIES_ENTRIES.length == 1 || TabMainActivity.this.CITIES_VALUES.length == 1) {
					try {
						int id = TabMainActivity.this.hashCode();
						// Segnala al service di aggiornare la lista città disponibili
						Message msg = Message.obtain(null,
								DBManagerService.MSG_GET_CITIES_LIST, id, 0);
						mService.send(msg);

					} catch (RemoteException e) {

					}
				}
				try {
					int id = TabMainActivity.this.hashCode();
					// Richiede la lista di sensori da mostrare
					Message msg = Message.obtain(null,
							DBManagerService.MSG_GET_OBJ_LIST, id, 0);
					mService.send(msg);

				} catch (RemoteException e) {

				}
			}
			return true;
		case R.id.menu_settings:
			Intent intentSettings = new Intent(getApplicationContext(),
					SettingsActivity.class);
			intentSettings.putExtra("CatEntriesList",
					TabMainActivity.this.CATEGORIES_ENTRIES);
			intentSettings.putExtra("CatValuesList",
					TabMainActivity.this.CATEGORIES_VALUES);
			intentSettings.putExtra("CitiesEntriesList",
					TabMainActivity.this.CITIES_ENTRIES);
			intentSettings.putExtra("CitiesValuesList",
					TabMainActivity.this.CITIES_VALUES);
			startActivity(intentSettings);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Distruzione dell'activity e contestuale scollegamento dal service. Se l'activity era
	 * l'unica rimasta collegata al service, si distruggerà anche quest'ultimo.
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
					int id = TabMainActivity.this.hashCode();
					// Segnalo al service che l'activity va in pausa
					Message msg = Message.obtain(null,
							DBManagerService.MSG_PAUSE_CLIENT, id, 0);
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
		updateCatSpinner();
		if (mIsBound) {
			if (mService != null) {
				Message msg;
				try {
					int id = TabMainActivity.this.hashCode();
					// Segnalo al service che l'activity è di nuovo in foreground
					msg = Message.obtain(null,
							DBManagerService.MSG_RESUME_CLIENT, id, 0);
					mService.send(msg);

					if (TabMainActivity.this.CATEGORIES_ENTRIES.length == 1 || TabMainActivity.this.CATEGORIES_VALUES.length == 1) {
						try {
							// Segnala al service di aggiornare la lista categorie disponibili
							msg = Message.obtain(null,
									DBManagerService.MSG_GET_CAT_LIST, id, 0);
							mService.send(msg);

						} catch (RemoteException e) {

						}
					}
					if (TabMainActivity.this.CITIES_ENTRIES.length == 1 || TabMainActivity.this.CITIES_VALUES.length == 1) {
						try {
							// Segnala al service di aggiornare la lista città disponibili
							msg = Message.obtain(null,
									DBManagerService.MSG_GET_CITIES_LIST, id, 0);
							mService.send(msg);

						} catch (RemoteException e) {

						}
					}

					// Blocco eseguito solo a seguito di inserimento credenziali di login alla prima
					// connessione
					if(firstConnection){
						firstConnection = false;
						msg = Message.obtain(null,DBManagerService.MSG_GET_OBJ_LIST, id, 0);
						mService.send(msg);
					}
					// Blocco eseguito nel caso sia variata la città selezionata nelle preferences
					else if(pf.isSelectedCityChanged()){
						msg = Message.obtain(null, DBManagerService.MSG_GET_CITY,
									id, 0);
						msg.replyTo = mMessenger;
						mService.send(msg);
					}
					// Blocco eseguito nel caso sia variato il raggio d'azione nelle preferences
					else if(pf.isSelectedRangeChanged()){
						// Richiede una lista di sensori da mostrare
						msg = Message.obtain(null,
								DBManagerService.MSG_GET_OBJ_LIST, id, 0);
						mService.send(msg);
					}
				} catch (RemoteException e) {

				}
			}
		}
	}

	/**
	 * Metodo richiamato alla richiesta di creazione di un dialog. Sono gestite
	 * due tipologie, il dialog con progress bar (per task quali l'aggiornamento
	 * dei dati dal back-end) e il dialog con spinner, per task quali la ricerca di
	 * coordinate gps per i quali non è possibile prevedere a priori una durata
	 * complessiva e uno stato di avanzamento
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SEARCH_COORDS:
			spinnerDialog = new ProgressDialog(this);
			spinnerDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			spinnerDialog.setCancelable(false);
			spinnerDialog.setCanceledOnTouchOutside(false);
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
	 * Classe che gestisce l'ordinamento della lista di oggetti in base alla
	 * distanza dall'utente. Per l'ordinamento viene usato il campo distanza in
	 * metri, non il formato stringa
	 */
	private class SCLOComparator implements Comparator<SCLO> {
		@Override
		public int compare(SCLO o1,	SCLO o2) {
			if (o1.getDistanzaMt() > o2.getDistanzaMt())
				return 1;
			else if (o1.getDistanzaMt() < o2.getDistanzaMt())
				return -1;
			else
				return 0;
		}
	}

	/**
	 * GETTER, SETTER
	 */

	public CharSequence[] getCategoriesEntries() {
		return CATEGORIES_ENTRIES;
	}

	public CharSequence[] getCategoriesValues() {
		return CATEGORIES_VALUES;
	}
	
	public CharSequence[] getCitiesEntries() {
		return CITIES_ENTRIES;
	}

	public CharSequence[] getCitiesValues() {
		return CITIES_VALUES;
	}

	/**
	 * FINE GETTER, SETTER
	 */
	
	/**
	 * Richiede al service i dettagli del singolo oggetto a seguito di press sulla lista o sull'icona nella mappa
	 */
	public void onItemSelected(int objId, int objCat, boolean isInsiderChild) {
		if (detailsLL.getVisibility() == View.GONE)
			toggleView();
		try {
			if(!isInsiderChild) details.resetView();
			// Richiedo il singolo oggetto da mostrare
			Bundle b = new Bundle();
			b.putInt("idObj", objId);
			b.putInt("idCat", objCat);
			b.putBoolean("isInsiderChild", isInsiderChild);
			int id = TabMainActivity.this.hashCode();
			Message msg = Message.obtain(null,
					DBManagerService.MSG_GET_SINGLE_OBJ, id, 0);
			msg.setData(b);
			mService.send(msg);
		} catch (RemoteException e) {
		}
	}

	/**
	 *  Override dell'evento alla pressione del tasto "back":
	 *  se sto mostrando un fragment di dettaglio torno alla vista della mappa
	 *  diversamente esco dall'applicazione come di consueto
	 */
	@Override
	public void onBackPressed() {
		if (detailsLL.getVisibility() == View.VISIBLE) {
			list.clearSelection();
			toggleView();
			updateList();

		} else
			super.onBackPressed();
	}


	/**
	 * Aggiorna la action bar in base al filtro di categoria impostato nelle preference:
	 * se nelle preference ho scelto di mostrare tutte le categorie, aggiorno la action bar impostando
	 * un selettore che permette di filtrare al volo le categorie da mostrare
	 * se invece ho scelto una categoria specifica nei settings, la action bar viene mostrata senza
	 * selettore
	 */
	public void updateCatSpinner() {
		int i=pf.getSelectedCat();
		if (i == 0) {
			PreferenceManager.getDefaultSharedPreferences(this);
			ActionBar actionBar = getActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
					this, android.R.layout.simple_spinner_dropdown_item,
					CATEGORIES_ENTRIES);
			actionBar.setListNavigationCallbacks(adapter, this);
		}else{
			ActionBar actionBar = getActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			currentSpinnerCat=0;
		}
	}

	/**
	 * Metodo chiamato dallo spinner quando viene selezionato un nuovo valore.
	 */
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// Salvo selezione corrente e aggiorno lista e mappa
		currentSpinnerCat=Integer.parseInt((String)CATEGORIES_VALUES[itemPosition]);
		updateListAndMap();
		return false;
	}


	/**
	 * Metodo che filtra la lista oggetti SCLO corrente applicando l'eventuale filtro rapido
	 * che è stato selezionato nello spinner della action bar
	 */
	public void updateList() {
		ArrayList<SCLO> itemList_filtered = new ArrayList<SCLO>();
		for (SCLO item : itemList) {
			if (item.getCategoria() == currentSpinnerCat || currentSpinnerCat == 0)
				itemList_filtered.add(item);
		}
		// Aggiorna la lista di oggetti SCLO del fragment
		list.updateFragmentList(itemList_filtered);
	}



	/**
	 * CODICE PER LA GESTIONE DELLE GOOGLE MAP
	 */

	/**
	 * Gestione dell'evento di pressione su un marker della mappa
	 */
	@Override
	public boolean onMarkerClick(Marker marker) {
		try{
			// Ricava id dell'oggetto e relativa categoria dal tag del marker
			Integer[] id_cat = (Integer[])marker.getTag();
			this.onItemSelected(id_cat[0], id_cat[1], false);
		}
		catch (NullPointerException ex){
			// Se l'id di categoria non è definito (caso utente o overlay di errore) non fare nulla
			return true;
		}

		return true;
	}

	/**
	 * Metodo richiamato quando l'oggetto di tipo GoogleMap è pronto per l'utilizzo
	 * @param googleMap L'oggetto GoogleMap restituito dalle API di Google
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {

		mGoogleMap = googleMap;

		mGoogleMap.setOnMarkerClickListener(this);

		LatLng gp = new LatLng(this.myLatitude, this.myLongitude);

		mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
		mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
		mGoogleMap.setTrafficEnabled(false);
		mGoogleMap.getUiSettings().setScrollGesturesEnabled(true);
		mGoogleMap.getUiSettings().setRotateGesturesEnabled(true);
		mGoogleMap.getUiSettings().setZoomGesturesEnabled(true);

		int initZoom = calculateZoomLevel();
		CameraUpdate location = CameraUpdateFactory.newLatLngZoom(gp,initZoom);
		mGoogleMap.animateCamera(location);

		overlayMap =new HashMap<Integer, SCMarkerGroup>();
		overlayMap.put(0, new SCMarkerGroup(getResources().getDrawable(
				R.drawable.ic_smart_user), this, 0));

		BitmapDescriptor icon = overlayMap.get(0).getBitmapDescriptor();
		Marker overlayitemMe = mGoogleMap.addMarker(new MarkerOptions().position(gp).icon(icon).visible(true));

		overlayMap.get(0).addMarker(overlayitemMe);
	}


	/**
	 * Metodo che aggiorna la lista di sensori da mostrare nel fragment laterale e analogamente
	 * ridisegna i marker relativi sulla mappa
	 */
	public void updateListAndMap() {
		if(itemList != null){
			ArrayList<SCLO> itemList_filtered = new ArrayList<SCLO>();
			SCLO[] OLIST_filtered;
			for (SCLO item : itemList) {
				if (item.getCategoria() == currentSpinnerCat || currentSpinnerCat == 0)
					itemList_filtered.add(item);
			}
			OLIST_filtered = new SCLO[itemList_filtered.size()];
			for (int i = 0; i < itemList_filtered.size(); i++) {
				OLIST_filtered[i] = itemList_filtered.get(i);
			}
			updateListAndMarkers(OLIST_filtered);
			// Aggiorna la lista di oggetti SCLO del fragment
			list.updateFragmentList(itemList_filtered);
		}
	}


	/**
	 * Inizializzazione degli overlay di mappa
	 */
	public void initOverlays(){

		CharSequence[] cat_entries = this.getCategoriesValues();


		for(int i=1; i<cat_entries.length;i++){
			int current_cat=Integer.parseInt((String)cat_entries[i]);
			overlayMap.put(current_cat, new SCMarkerGroup(getResources().getDrawable(
					SCO.getIcon(current_cat)), this, current_cat));
		}

		overlayMap.put(-1, new SCMarkerGroup(getResources()
				.getDrawable(SCODefault.getIcon()), this,
				SCODefault.SCO_ID));

		overlayMap.put(-2, new SCMarkerGroup(getResources().getDrawable(
				R.drawable.ic_error_status), this, -2));

	}


	/**
	 * Aggiorna le coordinate principali dell'activity e aggiorna la mappa di conseguenza
	 */
	public void updateCoords(double latitude, double longitude) {
		myLatitude = latitude;
		myLongitude = longitude;
		refreshMap();
	}


	/**
	 * Imposta l'array di oggetti SCLO dell'activity e disegna i relativi marker sulla mappa
	 */
	public void updateListAndMarkers(SCLO[] objList) {
		OLIST = objList;
		drawMarkers();
	}

	/**
	 * Ridisegna la mappa con le coordinate attuali e sposta l'icona dello user
	 * al centro sulle nuove coordinate; i sensori non vengono ridisegnati perchè
	 * qui è variata solo la posizione dell'utente
	 */
	protected void refreshMap() {
		LatLng gp = new LatLng(this.myLatitude, this.myLongitude);

		overlayMap.get(0).clear();

		BitmapDescriptor icon = overlayMap.get(0).getBitmapDescriptor();
		Marker overlayitemMe = mGoogleMap.addMarker(new MarkerOptions().position(gp).icon(icon).visible(true));
		overlayMap.get(0).addMarker(overlayitemMe);

		int zoom = calculateZoomLevel();
		CameraUpdate location = CameraUpdateFactory.newLatLngZoom(gp,zoom);
		mGoogleMap.animateCamera(location);
	}

	/**
	 * Disegna l'overlay sulla mappa relativo ai sensori; qui vengono ridisegnati
	 * i sensori perchè questo metodo è chiamato a seguito dell'arrivo di una lista
	 * oggetti aggiornata. A seguito viene ridisegnata anche l'icona utente e la
	 * mappa (refreshMap)
	 */
	protected void drawMarkers() {

		LatLng gp;

		// Esegue il clear di tutti i marker group tranne quello utente
		for (Map.Entry<Integer, SCMarkerGroup> entry : overlayMap.entrySet()) {
			Integer key = entry.getKey();
			SCMarkerGroup value = entry.getValue();
			if(key!=0){
				value.clear();
			}
		}


		if (OLIST != null) {
			for (int i = 0; i < OLIST.length; i++) {
				double lat = OLIST[i].getLatitudine();
				double lng = OLIST[i].getLongitudine();
				gp = new LatLng(lat, lng);
				Marker oi;
				BitmapDescriptor icon;


				try{
					icon = overlayMap.get(OLIST[i].getCategoria()).getBitmapDescriptor();
					oi = mGoogleMap.addMarker(new MarkerOptions().position(gp).icon(icon).visible(true));
					Integer[] id_cat = { OLIST[i].getId(), OLIST[i].getCategoria() };
					oi.setTag(id_cat);
					overlayMap.get(OLIST[i].getCategoria()).addMarker(oi);
				}catch(NullPointerException ex){
					// Aggiunge il sensore al layer dei sensori sconosciuti (o di default)
					icon = overlayMap.get(-1).getBitmapDescriptor();
					oi = mGoogleMap.addMarker(new MarkerOptions().position(gp).icon(icon).visible(true));
					Integer[] id_cat = { OLIST[i].getId(), SCODefault.SCO_ID };
					oi.setTag(id_cat);
					overlayMap.get(SCODefault.SCO_ID).addMarker(oi);
				}

				// Se il sensore è in stato di errore disegna sopra di esso la crocetta rossa
				// nell'apposito overlay degli errori
				if (!SCO.getStatus(OLIST[i].getCategoria(), OLIST[i].getStato())) {
					icon = overlayMap.get(-2).getBitmapDescriptor();
					oi = mGoogleMap.addMarker(new MarkerOptions().position(gp).icon(icon).visible(true));
					overlayMap.get(-2).addMarker(oi);
				}
			}

			refreshMap();
		}

	}



	/**
	 * Calcola lo zoom level in modo che sia lo user che i sensori siano visibili
	 */
	protected int calculateZoomLevel() {

		// Calcola la larghezza dello schermo
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int screenWidth = size.x;

		// Reperisce l'action range settato dai settings
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		String range = settings.getString("settings_range", "");
		int dist = Integer.parseInt(range);

		// Calcola il corretto zoom level
		double equatorLength = 40075004; // in meters
		double widthInPixels = screenWidth;
		double metersPerPixel = equatorLength / 256;
		int zoomLevel = 1;
		while ((metersPerPixel * widthInPixels) > 16 * dist) {
			metersPerPixel /= 2;
			++zoomLevel;
		}
		return zoomLevel;
	}

	/**
	 * FINE CODICE PER LA GESTIONE DELLE GOOGLE MAP
	 */





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
	 * Classe Handler che gestisce operativamente le azioni da eseguire in base
	 * ai messaggi ricevuti dal service.
	 */
	class IncomingHandler extends Handler {
		int progress;
		Bundle b;
		
		@Override
		public void handleMessage(Message msg) {
			ParamFetcher pf = new ParamFetcher(getApplicationContext());
			switch (msg.what) {

			// Comincia un task di richiesta dati al db; mostro una progress bar
			case DBManagerService.MSG_START_PROGRESS:
				showDialog(TabMainActivity.DIALOG_DATA_FROM_DB);
				progress = msg.arg1;
				progDialog.setProgress(progress);
				break;
			// Termina un task di richiesta dati al db; chiudo la progress bar
			case DBManagerService.MSG_END_PROGRESS:
				progress = msg.arg1;
				progDialog.setProgress(progress);
				dismissDialog(TabMainActivity.DIALOG_DATA_FROM_DB);
				break;
			// Aggiorno la progress bar in base allo stato di avanzamento dichiarato dal service
			case DBManagerService.MSG_SET_PROG_BAR:
				progress = msg.arg1;
				progDialog.setProgress(progress);
				break;
			// Comincia un task di ricerca coordinate geografiche; mostro uno spinner
			case DBManagerService.MSG_START_SPINNER:
				if(pf.getSelectedCity() == 0) showDialog(TabMainActivity.DIALOG_SEARCH_COORDS);
				break;
			// Termina il task di richiesta coordinate geografiche; chiudo lo spinner e aggiorno la lista
			case DBManagerService.MSG_END_SPINNER:

				if (spinnerDialog.isShowing()) {
					dismissDialog(TabMainActivity.DIALOG_SEARCH_COORDS);
				}
				try {
					myLatitude = msg.getData().getDouble("latitudine");
					myLongitude = msg.getData().getDouble("longitudine");
					boolean refresh = msg.getData().getBoolean("refresh");
					int id = TabMainActivity.this.hashCode();
					// La richiesta di nuova lista oggetti viene lanciata solo se le coordinate
					// sono effettivamente cambiate; infatti è possibile che il service stia
					// utilizzando le coordinate di una città specificata e non quelle del device.
					// In tal caso all'arrivo di questo messaggio chiudo lo spinner perchè
					// il device è nuovamente georeferenziato ma non scarico gli oggetti
					// in quanto sto sempre usando la posizione della città dei settings
					if(refresh){
						Message refreshMsg = Message.obtain(null,
								DBManagerService.MSG_GET_OBJ_LIST, id, 0);
						mService.send(refreshMsg);
					}
				} catch (RemoteException e) {

				}
				break;
			// Il service segnala che la rete è disabilitata, quindi viene
			// segnalato all'utente e gli viene data la possibilità di entrare
			// nei settings delle wireless connection di sistema
			case DBManagerService.MSG_NETWORK_STATE:
				new AlertDialog.Builder(TabMainActivity.this)
						.setTitle(getString(R.string.network_required))
						.setMessage(getString(R.string.network_dialog))
						.setPositiveButton(
								getString(R.string.network_dialog_wifi_button),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										startActivity(new Intent(
												Settings.ACTION_WIFI_SETTINGS));
									}
								})
						.setNeutralButton(
								getString(R.string.network_dialog_mobile_button),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										startActivity(new Intent(
												Settings.ACTION_WIRELESS_SETTINGS));
									}
								})
						.setNegativeButton(
								getString(R.string.network_dialog_cancel_button),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {

									}
								}).show();
				break;
			// Il service segnala che il gps è disabilitato, quindi viene segnalato all'utente
			// e gli viene imposto di entrare nei settings gps di sistema
			case DBManagerService.MSG_GPS_DISABLED:
				Intent gpsIntent = new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(gpsIntent);
				break;
			// Il service segnala che il net è disabilitato, quindi viene segnalato all'utente
			// e gli viene imposto di entrare nei settings network di sistema
			case DBManagerService.MSG_NET_DISABLED:
				Intent netIntent = new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(netIntent);
				break;
			// Qui perviene una lista di SCLO da mostrare nella lista a video. I dati vengono
			// spacchettati dal parcel, caricati nell'array adapter e la view viene aggiornata
			case DBManagerService.MSG_DONE_PROGRESS:
				b = new Bundle();
				b = msg.getData();
				TabMainActivity.this.myLongitude = b.getDouble("longitudine");
				TabMainActivity.this.myLatitude = b.getDouble("latitudine");
				OLIST = (SCLO[]) b
						.getParcelableArray("ObjList");
				itemList = new ArrayList<>();
				for (int i = 0; i < OLIST.length; i++) {
					itemList.add(OLIST[i]);
				}
				Collections.sort(itemList, new SCLOComparator());
				updateCoords(myLatitude, myLongitude);
				
				if(mapLL.getVisibility()==View.GONE){ 
					toggleView();
					list.clearSelection();
				}
				updateListAndMap();
				Toast.makeText(TabMainActivity.this,
						getString(R.string.list_updated), Toast.LENGTH_SHORT)
						.show();
				break;
			// Qui perviene una lista di categorie da usare nella preference activities.
			case DBManagerService.MSG_DONE_CAT_PROGRESS:
				b = new Bundle();
				b = msg.getData();
				TabMainActivity.this.CATEGORIES_ENTRIES = b
						.getCharSequenceArray("CatEntriesList");
				TabMainActivity.this.CATEGORIES_VALUES = b
						.getCharSequenceArray("CatValuesList");
				initOverlays();
				updateCatSpinner();
			break;
			// Qui perviene una lista di città da usare nella preference activities.
			case DBManagerService.MSG_DONE_CITIES_PROGRESS:
				b = new Bundle();
				b = msg.getData();
				TabMainActivity.this.CITIES_ENTRIES = b
						.getCharSequenceArray("CitiesEntriesList");
				TabMainActivity.this.CITIES_VALUES = b
						.getCharSequenceArray("CitiesValuesList");
				break;
			// Con questo messaggio il service segnala di avere ricevuto a
			// disposizione delle coordinate geografiche aggiornate, che vengono
			// passate nel messaggio. L'activity quindi chiama un metodo di
			// aggiornamento delle distanze su tutti gli oggetti che ha in lista
			// ed esegue un refresh della view.
			// Il refresh della view e l'aggiornamento delle distanze viene fatto solo
			// se sul service non c'è una città impostata
			case DBManagerService.MSG_REFRESH_OBJ_DISTANCE:
				if (OLIST != null) {
					if (OLIST.length > 0) {
						b = new Bundle();
						b = msg.getData();
						TabMainActivity.this.myLongitude = b.getDouble("longitudine");
						TabMainActivity.this.myLatitude = b.getDouble("latitudine");
						boolean refresh = b.getBoolean("refresh");
						if(refresh){
							for (int i = 0; i < OLIST.length; i++) {
								OLIST[i].setDistanceTo(
										TabMainActivity.this.myLongitude,
										TabMainActivity.this.myLatitude);
							}
							itemList = new ArrayList<>();
							for (int i = 0; i < OLIST.length; i++) {
								itemList.add(OLIST[i]);
							}
							Collections.sort(itemList, new SCLOComparator());
							updateCoords(myLatitude, myLongitude);
							if(mapLL.getVisibility()==View.VISIBLE)
							updateList();
						}
					}
				}
				break;
			// Riceve l'oggetto che è stato richiesto al db remoto. I dati vengono spacchettati
			// dal parcel, e viene aggiornata la view e invia richiesta per la ricezione di eventuali figli
			case DBManagerService.MSG_DONE_SINGLE_PROGRESS:
				try {
					Bundle b = msg.getData();
					int catId = b.getInt("catID");
					boolean isInsiderChild = b.getBoolean("isInsiderChild");
					oggetto = b.getParcelable("SingleObj");
					// Se il dettaglio di un figlio è richiamato dalla vista di dettaglio del padre
					// mostro all'interno di un dialog, altrimenti nel TabDetailsFragment
					if(!isInsiderChild){
						View v = oggetto.createView(TabMainActivity.this);
						details.updateView(v);
						msg = Message.obtain(null,
								DBManagerService.MSG_GET_SONS_LIST, TabMainActivity.this.hashCode(), oggetto.getIDObj());
						b.clear();
						b.putInt("catID", catId);
						msg.setData(b);
						mService.send(msg);
					} else{
						InsiderChildDialog dialog = new InsiderChildDialog();
						dialog.setArguments(b);
						dialog.show(getFragmentManager(), "InsiderChildDialog");
					}
				} catch (RemoteException e) {
				}
				break;
			// Quando il service comunica di aver scaricato i dati di una determinata città aggiorno oggetti
			case DBManagerService.MSG_DONE_CITY_PROGRESS:
				try {
					int id = TabMainActivity.this.hashCode();
					b = msg.getData();
					TabMainActivity.this.myLongitude = b.getDouble("longitudine");
					TabMainActivity.this.myLatitude = b.getDouble("latitudine");
					// Richiesta di lista sensori da mostrare
					msg = Message.obtain(null,
							DBManagerService.MSG_GET_OBJ_LIST, id, 0);
					mService.send(msg);
				} catch (RemoteException e) {
				}
				break;
			// Arriva una lista di sensori (figli di un oggetto)
			case DBManagerService.MSG_DONE_SONS_PROGRESS:
					
				b = msg.getData();
				int idParent = b.getInt("idParent");
				
				// Controllo che l'identificativo dell'oggetto attualmente visualizzato nel fragment di dettaglio
				// corrisponda all'identificativo del padre degli oggetti pervenuti, diversamente non ha senso
				// allacciarli alla vista del padre
				if (oggetto.getIDObj() == idParent){
					SCLO[] sons = (SCLO[]) b.getParcelableArray("ObjList");
					if(sons != null && sons.length > 0){
						// Aggiungo la lista figli al SCO
						oggetto.setSonsList(sons);
						// Ridisegno la view di dettaglio del SCO nel fragment, che stavolta terrà conto anche dei figli
						details.updateView(oggetto.createView(TabMainActivity.this));
					}
				} 
					
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
		 * Metodo richiamato quando il bind al service va a buon fine. Viene
		 * istanziato il messenger per comunicare messaggi al service. La
		 * activity manda subito un messaggio per registrarsi e segnalare la
		 * propria identità al service
		 */
		public void onServiceConnected(ComponentName className, IBinder service) {

			mService = new Messenger(service);
			
			try {
				int id = TabMainActivity.this.hashCode();
				/** mi registro al service con l'identità definita in id */
				Message msg = Message.obtain(null,
						DBManagerService.MSG_REGISTER_CLIENT, id, 0);
				msg.replyTo = mMessenger;
				mService.send(msg);

				// Se i settings dell'applicazione contengono uno username e una password, richiedo
				// la lista di oggetti da mostrare. Diversamente lancio l'activity dei settings per
				// permettere all'utente di inserie user e pwd.
				if (pf.isSetUsername() && pf.isSetPassword()) {

					// Richiedo subito al service di aggiornare la lista delle categorie e quella delle città
					// Se non ho ancora impostato username e password verrà ritornato il valore
					// di default (All categories, 0)
					msg = Message.obtain(null, DBManagerService.MSG_GET_CAT_LIST, id, 0);
					msg.replyTo = mMessenger;
					mService.send(msg);
					
					msg = Message.obtain(null, DBManagerService.MSG_GET_CITIES_LIST, id, 0);
					msg.replyTo = mMessenger;
					mService.send(msg);
					
					// Se c'è già una città selezionata all'avvio imposto quella sul service
					if (pf.getSelectedCity() != 0){
						msg = Message.obtain(null, DBManagerService.MSG_GET_CITY, id, 0);
						msg.replyTo = mMessenger;
						mService.send(msg);
					}else{
						msg = Message.obtain(null,DBManagerService.MSG_GET_OBJ_LIST, id, 0);
						mService.send(msg);
					}
				
				} else {
					firstConnection = true;
					Intent intentSettings = new Intent(getApplicationContext(),
							SettingsActivity.class);
					startActivity(intentSettings);
				}

			} catch (RemoteException e) {

			}
		}

		/**
		 * Metodo richiamato all'improvvisa disconnessione dal service, ad
		 * esempio per un crash dell'applicazione
		 */
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	/**
	 * Tenta di effettuare una connessione al service
	 */
	void doBindService() {
		bindService(new Intent(TabMainActivity.this, DBManagerService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	/**
	 * Tenta di effettuare una disconnessione dal service Se il service
	 * risultava connesso, effettua contestualmente la cancellazione
	 * dell'identità dell'activity dalla lista dei client connessi al service
	 * inviando un opportuno messaggio
	 */
	void doUnbindService() {
		if (mIsBound) {
			if (mService != null) {
				try {
					int id = TabMainActivity.this.hashCode();
					// Cancello la mia identità (id) dal service
					Message msg = Message.obtain(null,
							DBManagerService.MSG_UNREGISTER_CLIENT, id, 0);
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