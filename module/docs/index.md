`httpd`: Simple embedded httpd for serving app assets via http: or https:
=========================================================================

The ``forge.httpd`` module allows you to serve your app assets via the `http:` or `https:` transport protocols rather than the `file:` (iOS) or `content:` (Android) protocols.

This is particularly useful when working with 3rd party libraries that are not transport protocol agnostic. (e.g. Angular, jQuery Mobile, Google jsAPI)


## Config options

Port
:  Port to launch httpd on. Leave empty to automatically choose a free port (recommended). Set to `0` to disable httpd.

URL
:  URL to use as initial app page. Leave empty for default. (recommended).

Certificate Path
:  Path to SSL certificate. Only [PKCS #12](https://en.wikipedia.org/wiki/PKCS_12) format files are supported. (optional)

Certificate Password
:  Password for SSL certificate file.


### ::Important::

#### Configure Trusted Urls

To use the `httpd` module you will also need to add an entry for the local server to the `core.general.trusted_urls` directive in your app's `src/config.json`:

    "core": {
        "general": {
            "trusted_urls": [
                "http://127.0.0.1/*" // use https: if you've configured SSL
            ]
        }
    }

If you are using other remote sources in your app you will also have to add entries for those hosts.

#### Port Settings

While it is recommended to let the `httpd` module choose the port it will run on there is one scenario where you will NOT want this.

If you are using any of the HTML web storage API's such as LocalStorage, WebSQL or IndexedDB you need to ensure that every session of your app is being served from the same domain and port combination.

If you don't do this the storage API's will allocate a new, empty, database for each domain and port combination rather than re-using the database from previous sessions.

tl;dr If you're using web storage, set the `port` configuration to a fixed number for your app!
