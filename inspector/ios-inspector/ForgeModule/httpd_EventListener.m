#import "httpd_EventListener.h"

#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>


@implementation httpd_EventListener

static GCDWebServer *httpd;
static int port = 46665;

+ (int) findFreePort {
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_port = 0;
    inet_aton("0.0.0.0", &addr.sin_addr);
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        [ForgeLog e:@"socket() failed to find free port"];
        return -1;
    }
    if (bind(sock, (struct sockaddr*) &addr, sizeof(addr)) != 0) {
        [ForgeLog e:@"bind() failed to find free port"];
        return -1;
    }
    socklen_t len = sizeof(addr);
    if (getsockname(sock, (struct sockaddr*) &addr, &len) != 0) {
        [ForgeLog e:@"getsockname() failed to find free port"];
        return -1;
    }
    [ForgeLog d:[NSString stringWithFormat:@"Found free network port: %d", addr.sin_port]];
    return (addr.sin_port);
}


+ (NSNumber*) onLoadInitialPage {
    // Read config
    if ([[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"port"]) {
        port = ((NSNumber*)[[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"port"]).intValue;
        if (port == 0) {
            [ForgeLog d:@"Disabling httpd server"];
            return @NO;
        }
    } else {
        port = [httpd_EventListener findFreePort];
        if (port == -1) {
            [ForgeLog e:@"Could not find a free port to start httpd on"];
            return @NO;
        }
    }
    NSString *url = [NSString stringWithFormat:@"http://localhost:%d/src/index.html", port];
    if ([[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"url"]) {
        url = [[[ForgeApp sharedApp] configForModule:@"httpd"] objectForKey:@"url"];
    }
    
    // Server options
    NSMutableDictionary* options = [NSMutableDictionary dictionary];
    [options setObject:[NSNumber numberWithInteger:port] forKey:GCDWebServerOption_Port];
    [options setObject:[NSNumber numberWithBool:true] forKey:GCDWebServerOption_BindToLocalhost];
    
    // Create server
    [GCDWebServer setLogLevel:5];
    httpd = [[GCDWebServer alloc] init];
    
    // Add assets folder
    NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
    NSURL *assetsFolder = [[ForgeApp sharedApp] assetsFolderLocationWithPrefs:prefs];
    [httpd addGETHandlerForBasePath:@"/" directoryPath:[assetsFolder path] indexFilename:nil cacheAge:0 allowRangeRequests:YES];
    
    // Start server
    NSError *error;
    [httpd startWithOptions:options error:&error];
    if (error) {
        [ForgeLog e:[NSString stringWithFormat:@"Failed to start httpd: %@", error.description]];
    } else {
        [ForgeLog d:[NSString stringWithFormat:@"Started httpd on port: %d", port]];
    }
    
    // load initial page
    [ForgeLog d:[NSString stringWithFormat:@"Loading initial page: %@", url]];
    [[[ForgeApp sharedApp] viewController] loadURL:[NSURL URLWithString: url]];
    
    return @YES;
}

@end
