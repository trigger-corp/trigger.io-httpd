#import "httpd_EventListener.h"

#import "Criollo/CRHTTPServer.h"
#import "Criollo/CRResponse.h"

#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>

@implementation httpd_EventListener

static ServerDelegate* delegate = nil;
static CRHTTPServer*   server = nil;
static int port = 46665;

static bool isOnLoadInitialPage = NO;
static bool isApplicationWillEnterForeground = NO;


// = Helpers ==================================================================

+ (int) findFreePort {
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_port = 0;
    inet_aton("0.0.0.0", &addr.sin_addr);
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        [ForgeLog e:@"socket() failed to find free port"];
        return 0;
    }
    if (bind(sock, (struct sockaddr*) &addr, sizeof(addr)) != 0) {
        [ForgeLog e:@"bind() failed to find free port"];
        return 0;
    }
    socklen_t len = sizeof(addr);
    if (getsockname(sock, (struct sockaddr*) &addr, &len) != 0) {
        [ForgeLog e:@"getsockname() failed to find free port"];
        return 0;
    }
    [ForgeLog d:[NSString stringWithFormat:@"Found free network port: %d", addr.sin_port]];
    return (addr.sin_port);
}


+ (bool) startServer {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd startServer");
#endif // DEBUG_HTTPD
    if (server != nil) {
        NSLog(@"httpd startServer server already started");
        return YES;
    }

    NSError* error;

    // Read config
    if ([[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"port"]) {
        port = ((NSNumber*)[[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"port"]).intValue;
        if (port == 0) {
            [ForgeLog d:@"Disabling httpd server"];
            return NO;
        }
    } else {
        port = [httpd_EventListener findFreePort];
        if (port == 0) {
            [ForgeLog e:@"Could not find a free port to start httpd on"];
            return NO;
        }
    }

    // Create server
    delegate = [[ServerDelegate alloc] init];
    server   = [[CRHTTPServer alloc] initWithDelegate:delegate];

    // Configure SSL
    NSString *certificate_path     = [[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"certificate_path"];
    NSString *certificate_password = [[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"certificate_password"];
    if (certificate_path != nil && certificate_password != nil) {
        NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
        NSURL *assetsFolder = [[ForgeApp sharedApp] assetsFolderLocationWithPrefs:prefs];
        NSURL *url = [[assetsFolder URLByAppendingPathComponent:@"src"] URLByAppendingPathComponent:certificate_path];
        if ([url checkResourceIsReachableAndReturnError:&error] == YES) {
            [ForgeLog d:[NSString stringWithFormat:@"Configured httpd to use SSL"]];
            server.isSecure = YES;
            server.identityPath = [url path];
            server.password = certificate_password;
        } else {
            [ForgeLog e:[NSString stringWithFormat:@"Failed to configure httpd to use SSL: %@", error.localizedDescription]];
        }
    }

    // Add assets folder
    NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
    NSURL *assetsFolder = [[ForgeApp sharedApp] assetsFolderLocationWithPrefs:prefs];
    [server mount:@"/" directoryAtPath:[assetsFolder path]];

    // Start Server
    [server startListening:&error portNumber:port];
    if (error) {
        [ForgeLog e:[NSString stringWithFormat:@"Failed to start httpd: %@", error.description]];
        server = nil;
        return NO;
    } else {
        [ForgeLog d:[NSString stringWithFormat:@"Started httpd on port: %d", port]];
    }

    return YES;
}


+ (void) stopServer {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd stopServer");
#endif // DEBUG_HTTPD
    if (server == nil) {
        [ForgeLog e:@"Failed to stop httpd: Server is not initialized"];
    }
    [server stopListening];
    server = nil;
}


// = Life-cycle ===============================================================

+ (void)applicationWillEnterForeground:(UIApplication *)application {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd applicationWillEnterForeground");
#endif // DEBUG_HTTPD
    if (server == nil) {
        [httpd_EventListener startServer];
    }
    isApplicationWillEnterForeground = YES;
}

+ (void)applicationWillResignActive:(UIApplication *)application {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd applicationWillResignActive");
#endif // DEBUG_HTTPD
}

+ (void)applicationDidBecomeActive:(UIApplication *)application {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd applicationDidBecomeActive");
#endif // DEBUG_HTTPD
    if (server == nil) {
        [httpd_EventListener startServer];
    }
}

+ (void)applicationDidEnterBackground:(UIApplication *)application {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd applicationDidEnterBackground");
#endif // DEBUG_HTTPD
    [httpd_EventListener stopServer];
}

+ (void)applicationWillTerminate:(UIApplication *)application {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd applicationWillTerminate");
#endif // DEBUG_HTTPD
    [httpd_EventListener stopServer];
}


// = onLoadInitialPage ========================================================

+ (NSNumber*) onLoadInitialPage {
    isOnLoadInitialPage = YES;
    if (![httpd_EventListener startServer]) {
        [ForgeLog e:@"Failed to start server for httpd module"];
        return @NO;
    }
    return @YES;
}

@end


// = CRServerDelegate =========================================================

@implementation ServerDelegate

- (void)serverDidStartListening:(CRServer *)server {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd serverDidStartListening");
#endif // DEBUG_HTTPD

    // Handle onLoadInitialPage
    if (isOnLoadInitialPage == YES) {
        CRHTTPServer *crhttpserver = (CRHTTPServer*)server;
        NSString *url;
        if (crhttpserver.isSecure) {
            url = [NSString stringWithFormat:@"https://127.0.0.1:%d/src/index.html", port];
        } else {
            url = [NSString stringWithFormat:@"http://127.0.0.1:%d/src/index.html", port];
        }
        if ([[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"url"]) {
            url = [[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"url"];
        }
        [ForgeLog d:[NSString stringWithFormat:@"Loading initial page: %@", url]];
        dispatch_async(dispatch_get_main_queue(), ^{
            [[[ForgeApp sharedApp] viewController] loadURL:[NSURL URLWithString: url]];
        });
        isOnLoadInitialPage = NO;
    }

    if (isApplicationWillEnterForeground == YES) {
        // TODO This is a particularly ugly hack to make sure the app can access
        //      content served by this module in the appResumed handler
        NSLog(@"httpd module started, now firing event.appResumed");
        [[ForgeApp sharedApp] event:@"event.appResumed" withParam:[NSNull null]];
        isApplicationWillEnterForeground  = NO;
    }
}

- (void)serverDidStopListening:(CRServer *)s {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd serverDidStopListening");
#endif // DEBUG_HTTPD
    server = nil;
}

- (void)server:(CRServer *)server didAcceptConnection:(CRConnection *)connection {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd didAcceptConnection\t%lu", (unsigned long)connection.hash);
#endif // DEBUG_HTTPD
}

- (void)server:(CRServer *)server didReceiveRequest:(CRRequest *)request {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd didReceiveRequest\t\t%lu %@", (unsigned long)request.connection.hash, request.URL);
#endif // DEBUG_HTTPD
}

- (void)server:(CRServer *)server didFinishRequest:(CRRequest *)request {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd didFinishRequest\t\t%lu %@", (unsigned long)request.connection.hash, request.URL);
#endif // DEBUG_HTTPD
}

- (void)server:(CRServer  *)server didCloseConnection:(CRConnection *)connection {
#ifdef DEBUG_HTTPD
    NSLog(@"httpd didCloseConnection\t%lu", (unsigned long)connection.hash);
#endif // DEBUG_HTTPD
}

@end
