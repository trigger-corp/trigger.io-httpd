package io.trigger.forge.android.modules.httpd;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;

import fi.iki.elonen.NanoHTTPD;
import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeEventListener;
import io.trigger.forge.android.core.ForgeFile;
import io.trigger.forge.android.core.ForgeLog;
import io.trigger.forge.android.core.ForgeWebView;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;


public class EventListener extends ForgeEventListener {
	private static ForgeHttpd httpd = null;
	private static int port = 46664;
	
	private static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {}
			ForgeLog.d("Found free network port: " + port);
			return port;
		} catch (IOException e) { 
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {}
			}
		}
		throw new IllegalStateException("Could not find a free port to start httpd on");
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		// Read config
		if (ForgeApp.configForPlugin("httpd").has("port")) {
			port = ForgeApp.configForPlugin("httpd").get("port").getAsInt();
			if (port == 0) {
				ForgeLog.d("Disabling httpd server");
				return;
			}
		} else {
			try {
				port = findFreePort();
			} catch (IllegalStateException e) {
				ForgeLog.e(e.getLocalizedMessage());
				return;
			}
		}

		// create and configure web server
		try {
			httpd = new ForgeHttpd("localhost", port);
			if (ForgeApp.configForPlugin("httpd").has("certificate_path") &&
				ForgeApp.configForPlugin("httpd").has("certificate_password")) {
				String certificate_path = ForgeApp.configForPlugin("httpd").get("certificate_path").getAsString();
				String certificate_password = ForgeApp.configForPlugin("httpd").get("certificate_password").getAsString();
				Context context = ForgeApp.getActivity();
				ForgeFile f = new ForgeFile(context, certificate_path);
				AssetFileDescriptor fd = f.fd();
				KeyStore keyStore = KeyStore.getInstance("pkcs12");
				keyStore.load(fd.createInputStream(), certificate_password.toCharArray());
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, certificate_password.toCharArray());
				httpd.makeSecure(NanoHTTPD.makeSSLSocketFactory(keyStore, keyManagerFactory));
				ForgeLog.d("Configured httpd to use SSL");
			}
		} catch (Exception e) {
			e.printStackTrace();
			ForgeLog.e("Failed to configure httpd: " + e.getLocalizedMessage());
			return;
		}

		// startup web server
		try {
			httpd.start();
			ForgeLog.d("Started httpd on port " + port);
		} catch (Exception e) {
			ForgeLog.e("Failed to start httpd: " + e.getLocalizedMessage());
		}
	}

	
	@Override
	public Boolean onLoadInitialPage(final ForgeWebView webView) {
		// Read config
		String url = "http://127.0.0.1:" + port + "/src/index.html";
		if (ForgeApp.configForPlugin("httpd").has("url")) {
			url = ForgeApp.configForPlugin("httpd").get("url").getAsString();
		}
		
		// Load initial page
		ForgeLog.d("Loading initial page: " + url);
		ForgeApp.getActivity().gotoUrl(url);
		
		return true;
	}
	
	
	/**
	 * @hide
	 */
	@Override
	public void onStop() {
		try {
			httpd.stop();
		ForgeLog.d("Pausing httpd while application not focused.");
		} catch (Exception e) {
			ForgeLog.e("Failed to pause httpd: " + e);
		}
	}
	

	/**
	 * @hide
	 */
	@Override
	public void onRestart() {
		try {
			httpd.start();
			ForgeLog.d("Application in focus, resuming httpd.");			
		} catch (Exception e) {
			ForgeLog.e("Failed to start httpd: " + e);
		}
	}
}
