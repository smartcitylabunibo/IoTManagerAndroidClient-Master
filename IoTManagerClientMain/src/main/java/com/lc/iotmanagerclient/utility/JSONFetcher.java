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

package com.lc.iotmanagerclient.utility;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import com.lc.iotmanagerclient.R;


/**
 * Interfaccia al database remoto (via JSON).
 * 
 * Implementa la connessione al database tramite un'interfaccia PHP lato server.
 * I dati vengono letti come stringhe via HTTPS e poi convertiti in JSON per l'utilizzo
 * nell'applicazione.
 */
public class JSONFetcher {

	private ParamFetcher pf;
	private Context context;

	/**
	 * Istanzia un ParamFetcher in base al contesto dell'applicazione
	 *
	 * @param context contesto dell'applicazione android
	 */
	public JSONFetcher(Context context) {
		this.context = context;
		pf = new ParamFetcher(context);
	}

	/**
	 * Scarica i dati dal servizio web PHP effettuando una connessione HTTPS.
	 * Il metodo riceve come risposta una stringa formattata secondo lo standard JSON,
	 * di cui effettua il return.
	 *
	 * @param query_parameters Stringa contenente i parametri da passare al web service
	 *                         e l'URL dello stesso.
	 * @return Stringa contenente i dati
	 */
	public String get_data(String query_parameters) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

		String fetched_string = "";

		// Recupera i dati tramite HTTPS connettendosi alla pagina PHP e inviando i parametri via POST
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream caInput = context.getResources().openRawResource(R.raw.smartcatchercsruniboit);

		// Gestione speciale per consentire l'handshake HTTPS con certificato self signed
		Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
			System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
		} finally {
			caInput.close();
		}

		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("ca", ca);

		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		tmf.init(keyStore);

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, tmf.getTrustManagers(), null);

		//Apertura della connessione e invio dei parametri in POST
		URL url = new URL(pf.getURLParam());
		HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
		urlConnection.setSSLSocketFactory(context.getSocketFactory());
		urlConnection.setRequestMethod("POST");

		DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
		dStream.writeBytes(query_parameters);
		dStream.flush();
		dStream.close();
		//int responseCode = urlConnection.getResponseCode();

		// Converte la risposta ottenuta in stringa
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			reader.close();

			fetched_string = sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return fetched_string;

	}


	/**
	 * Trasforma una stringa in un JSON array.
	 *
	 * @param fetched_string Stringa input da trasformare
	 * @return JSON array contenente i dati della stringa
	 */
	public JSONArray parse_data(String fetched_string) {

		JSONArray jArray;
		jArray = new JSONArray();

		try {
			jArray = new JSONArray(fetched_string);
			return jArray;
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return jArray;
	}

}