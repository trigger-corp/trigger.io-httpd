package io.trigger.forge.android.modules.httpd;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeEventListener;
import io.trigger.forge.android.core.ForgeLog;

import java.io.IOException;
import java.net.ServerSocket;

import android.webkit.WebView;

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
	public void onApplicationCreate() {
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
		
		// startup web server
		try {
			httpd = new ForgeHttpd("localhost", port);
			httpd.start();
			ForgeLog.d("Started httpd on port " + port);
		} catch (Exception e) {
			ForgeLog.e("Failed to start httpd: " + e);
		}
	}

	
	@Override
	public Boolean onLoadInitialPage(final WebView webView) {
		// Read config
		String url = "http://localhost:" + port + "/src/index.html";
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
		httpd.stop();
		ForgeLog.d("Pausing httpd while application not focussed.");		
	}
	

	/**
	 * @hide
	 */
	@Override
	public void onRestart() {
		try {
			httpd.start();
			ForgeLog.d("Application in focus, resuming httpd.");			
		} catch (IOException e) {
			ForgeLog.e("Failed to start httpd: " + e);
		}
	}
}
