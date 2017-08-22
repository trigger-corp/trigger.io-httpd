`httpd`: Simple embedded httpd for serving app assets via http:
===============================================================

The ``forge.httpd`` module allows you to serve your app assets via the `http:` transport protocol rather than the `file:` (iOS) or `content:` (Android) protocols.

This is particularly useful when working with 3rd party libraries that are not transport protocol agnostic. (e.g. Angular, jQuery Mobile, Google jsAPI)


##Config options

Port
:  Port to launch httpd on. Leave empty to automatically choose a free port (recommended). Set to `0` to disable httpd.

URL
:  URL to use as initial app page. Leave empty for default. (recommended).


### ::Important::

To use the `httpd` module you will also need to add an entry for the local server to the `core.general.trusted_urls` directive in your app's `src/config.json`:

	"core": {
		"general": {
			"trusted_urls": [
				"http://localhost/*"
			]
		}
	}

If you are using other remote sources in your app you will also have to add entries for those hosts.
