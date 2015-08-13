package io.trigger.forge.android.modules.httpd;

import io.trigger.forge.android.core.ForgeApp;
import io.trigger.forge.android.core.ForgeFile;
import io.trigger.forge.android.core.ForgeLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import fi.iki.elonen.NanoHTTPD;

public class ForgeHttpd extends NanoHTTPD {

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {
        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("java", "text/x-java-source, text/java");
            put("md", "text/plain");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("svg", "image/svg+xml");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
            put("m3u8", "application/vnd.apple.mpegurl");
            put("ts", " video/mp2t");
        }
    };


    /**
     * Construction
     */
    public ForgeHttpd(String host, int port) {
        super(host, port);
    }

    protected Response FORBIDDEN(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response INTERNAL_ERROR(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }
    
    protected Response NOT_FOUND() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = ForgeHttpd.MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? ForgeHttpd.MIME_DEFAULT_BINARY : mime;
    }
    
    public AssetFileDescriptor getAssetFileDescriptor(Context context, URI uri) {
    	return null;
    }


    @Override
    public Response serve(IHTTPSession session) {    	
		URI uri;
		try {
			uri = (new URI(session.getUri())).normalize();
		} catch (URISyntaxException e) {
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "INTERNAL ERROR Couldn't parse URL '" + session.getUri() + "': " + e);
		}
		
		// Get asset file descriptor - TODO this logic should really live in ForgeCore
		Context context = ForgeApp.getActivity();
		AssetFileDescriptor fileDescriptor = null;
		if (uri.getPath().startsWith("/favicon.ico")) {
			return NOT_FOUND();

		} else if (uri.getPath().startsWith("/forge")) {
			// Always load from assets folder
			fileDescriptor = ForgeFile.assetForUri(context, Uri.parse("file:///android_asset" + uri.getPath()));

		} else if (uri.getPath().startsWith("/src")) {
			// TODO: Cache manifest in memory? - seems fast enough without
			File liveFolder = context.getDir("reload-live", Context.MODE_PRIVATE);
			File manifestFile = new File(liveFolder, "manifest");
			if (manifestFile.exists()) {
				try {
					String manifestStr = Files.toString(manifestFile, Charsets.UTF_8);
					JsonObject manifest = new JsonParser().parse(manifestStr).getAsJsonObject();
					String fileKey = uri.getPath().substring(5);
					String fileUrl = manifest.get(fileKey).getAsString();
					File file = new File(liveFolder, fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
					if (!file.exists()) {
						// See if a file in assets matches that hash
						JsonObject assetManifest = new JsonObject();
						try {
							InputStreamReader assetManifestReader = new InputStreamReader(context.getAssets().open("hash_to_file.json"), "UTF-8");
							BufferedReader br = new BufferedReader(assetManifestReader);
							String assetManifestString = br.readLine();
							br.close();
							assetManifest = new JsonParser().parse(assetManifestString).getAsJsonObject();
						} catch (IOException e) {}
						if (assetManifest.has(fileUrl.substring(fileUrl.lastIndexOf("/") + 1))) {
							fileDescriptor = ForgeFile.assetForUri(context, Uri.parse("file:///android_asset/src/" + assetManifest.get(fileUrl.substring(fileUrl.lastIndexOf("/") + 1)).getAsString()));
						} else {
							ForgeLog.w("Loading of reload updated file failed: " + file.toString());
							return INTERNAL_ERROR("Loading of reload updated file failed: " + file.toString());
						}
					} else {
						fileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
					}
				} catch (IOException e) {
					ForgeLog.w("Failed to obtain reload asset '" + uri.getPath()  + "': " + e.getLocalizedMessage());
					return INTERNAL_ERROR("Failed to obtain reload asset '" + uri.getPath()  + "': " + e.getLocalizedMessage());
				}
			} else {
				// Otherwise grab from assets
				fileDescriptor = ForgeFile.assetForUri(context, Uri.parse("file:///android_asset" + uri.getPath()));
			}
		} else if (uri.getPath().equals("/file")) {
			// Provide access to files, resize images if specified
			// File details encoded in query string... decode
			JsonObject params = new JsonObject();
			List<NameValuePair> query;
			try {
				query = URLEncodedUtils.parse(new URI(uri.toString()), "UTF-8");
				for (NameValuePair param : query) {
					params.addProperty(param.getName(), param.getValue());
				}
			} catch (URISyntaxException e) {
				ForgeLog.w("URI Syntax Exception '" + uri.toString() + "':" + e.getLocalizedMessage());
				return INTERNAL_ERROR(e.getLocalizedMessage());
			}
			fileDescriptor = new ForgeFile(context, params).fd();
		} else {
			ForgeLog.w("Unsupported root path: " + uri.getPath());
			return FORBIDDEN("Unsupported root path");
		}
		
		// check that we have a valid fileDescriptor
		if (fileDescriptor == null) {
			ForgeLog.w("404 Not found: " + uri.getPath());
			return NOT_FOUND();
		}
		
		// open asset stream for reading
		InputStream assetStream;
		try {
			assetStream = fileDescriptor.createInputStream();
		} catch (IOException e) {
			ForgeLog.w("Couldn't open input stream for URL '" + uri.getPath() + "': " + e);
			return INTERNAL_ERROR("Couldn't open input stream for URL '" + uri.getPath() + "': " + e);
		}

		// send response
		String mimeType = getMimeTypeForFile(uri.getPath());
		return newChunkedResponse(Response.Status.OK, mimeType, assetStream);
    }
}
