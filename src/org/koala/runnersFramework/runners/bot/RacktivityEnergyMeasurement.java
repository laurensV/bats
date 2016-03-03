package org.koala.runnersFramework.runners.bot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RacktivityEnergyMeasurement {

	String rpdu;
	static final int OUTLETS_PER_PORT = 8;
	static final Map<Integer, String> errorCodes;
	static final Map<String, Integer> properties;
	static final Map<Integer, Integer> count;

	String urlString;
	static String cgiData = "username=guest&password=1234";
	static String cookie = ""; 
	static ArrayList<Double> current;
	static ArrayList<Short> power;
	static ArrayList<String> outlets;
	
	static {
		errorCodes = new HashMap<Integer, String>();
		errorCodes.put(0, "NO_ERROR");
		errorCodes.put(1, "UNKNOWN_CMD");
		errorCodes.put(2, "WRONG_CODE");	  
		errorCodes.put(3, "UNKNOWN_MODE");
		errorCodes.put(4, "WRONG_INDEX");
		errorCodes.put(5, "WRONG_INDEX_NUM");
		errorCodes.put(6, "WRONG_DATA_LEN");
		errorCodes.put(7, "WRONG_ARGUMENT");
		errorCodes.put(8, "WRITE_PROTECTION");
		errorCodes.put(9, "READ_PROTECTION");
		errorCodes.put(10, "OPERATION_FAILED");
		errorCodes.put(11, "CRC_ERROR");
		errorCodes.put(12, "NOT_READY");
		errorCodes.put(13, "RECEIVE_BUFFER_TOO_SMALL");
		errorCodes.put(14, "MISMATCH");
		errorCodes.put(15, "UNKNOWN_MODULE_TYPE");
		errorCodes.put(16, "MODULE_NOT_PRESENT");
		errorCodes.put(17, "NO_TRANSPARANT");
		errorCodes.put(255, "UNAUTHORIZED");

		// guids 
		properties = new HashMap<String, Integer>();
		properties.put("Current", 6);
		properties.put("Power", 7);
		properties.put("PortName", 10034);

		// count
		count = new HashMap<Integer, Integer>();
		count.put(6, 8);
		count.put(7, 8);
		count.put(10034, 8);	
	}

	static {
		disableSslVerification();
	}
	
	public RacktivityEnergyMeasurement(String rpdu) {
		super();
		this.rpdu = rpdu;
		this.urlString = "https://"+rpdu+":443";
		try {
			connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void disableSslVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType) throws CertificateException {
				}
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType) throws CertificateException {
				}
			}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}


	private boolean connect() throws IOException {
		// get content from URLConnection;
		// cookies are set by web site
		URL url = new URL(urlString+"/login.cgi");
		
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-length", String.valueOf(cgiData.length())); 
		connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		connection.setDoOutput(true); 
		connection.setDoInput(true); 

		DataOutputStream output = new DataOutputStream(connection.getOutputStream());  
		output.writeBytes(cgiData);
		output.close();

		connection.getContent();
		cookie = connection.getHeaderField("Set-Cookie");
		return (!cookie.isEmpty());
	}


	private void getOutlets(String addr) throws IOException {
		URL url = new URL(urlString + "/API.cgi?ADDR="+addr+"&GUID=10034&TYPE=G&INDEX=1&COUNT=8");

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("Cookie", cookie);
		connection.connect();
		
		try {
			outlets = new ArrayList<String>();
			StringBuilder outlet;
			InputStream is = connection.getInputStream();

			// 8 outlets, 16 bytes for each name
			for (int i = 0; i < 8; i++) {
				outlet = new StringBuilder();
				for (int j = 0; j < 16; j++) {
					char c = (char)is.read();
					if (c != 0) { 
						outlet.append(c);
					}
				}
				outlets.add(outlet.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	ArrayList<Short> getPower() throws IOException {
		String addr = "P1";
		URL url = new URL(urlString + "/API.cgi?ADDR="+addr+"&GUID=7&TYPE=G&INDEX=1&COUNT=8");

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("Cookie", cookie);
		connection.connect();
		try {
			power = new ArrayList<Short>();
			DataInputStream dis = new DataInputStream(connection.getInputStream());

			// 8 outlets, 2 bytes for each power value
			dis.readByte();
			for (int i = 0; i < 8; i++) { 
				ByteBuffer bb = ByteBuffer.allocate(2);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				bb.put(dis.readByte());
				bb.put(dis.readByte());
				power.add(bb.getShort(0));
			}
		} catch (IOException e) {
			//reconnect
			connect();
			//e.printStackTrace();
			return getPower();
			//System.out.println("Exception in " + this.getClass()); 
		}
		return power;
	}

	private void getCurrent(String addr) throws IOException {
		URL url = new URL(urlString + "/API.cgi?ADDR="+addr+"&GUID=6&TYPE=G&INDEX=1&COUNT=8");

		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("Cookie", cookie);
		connection.connect();
		try {
			current = new ArrayList<Double>();
			InputStream is = connection.getInputStream();

			DataInputStream dis = new DataInputStream(is);

			double scale = 0.001; 

			// 8 outlets, 2 bytes for each current value
			dis.readByte();
			for (int i = 0; i < 8; i++) { 
				ByteBuffer bb = ByteBuffer.allocate(2);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				bb.put(dis.readByte());
				bb.put(dis.readByte());
				current.add(bb.getShort(0)*scale);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
