/* global module, forge, asyncTest, askQuestion, ok, start */

module("forge.httpd");


if (typeof forge["file"] !== "undefined") {

    asyncTest("Test that we can read cached URL's from /file", 1, function () {
        var url = "https://trigger.io/forge-static/img/trigger-light/trigger-io-command-line.jpg";
        forge.file.cacheURL(url, function (file) {
            forge.file.URL(file, function (cached_url) {
                var normalized_url = forge.httpd.normalize(cached_url);
                askQuestion("Can you see this image: <br><img src='" + normalized_url + "' style='max-width: 100px; max-height: 100px'>", {
                    Yes: function () {
                        ok(true, "success with cacheURL");
                        start();
                    },
                    No: function () {
                        ok(false, "failure with cacheURL");
                        start();
                    }
                });
            }, function (error) {
                forge.logging.error("forge.file.URL failed: ", error);
            });
        }, function (error) {
            forge.logging.error("forge.file.cacheURL failed: ", error);
        });
    });


    asyncTest("Test that we can read local URLs from /file", 1, function () {
        var fixture = "fixtures/httpd/icon-512.png";
        forge.file.getLocal(fixture, function (file) {
            forge.file.URL(file, function (url) {
                var normalized_url = forge.httpd.normalize(url);
                askQuestion("Can you see this image: <br><img src='" + normalized_url + "' style='max-width: 100px; max-height: 100px'>", {
                    Yes: function () {
                        ok(true, "success with getLocal");
                        start();
                    },
                    No: function () {
                        ok(false, "failure with getLocal");
                        start();
                    }
                });
            }, function (error) {
                forge.logging.error("forge.file.URL failed: ", error);
            });
        }, function (error) {
            forge.logging.error("forge.file.getLocal failed: ", error);
        });
    });


    asyncTest("Test that we can read local fixtures from /file", 1, function () {
        var fixture = forge.inspector.getFixture("httpd", "icon-512.png");
        forge.file.URL(fixture, function (url) {
            var normalized_url = forge.httpd.normalize(url);
            askQuestion("Can you see this image: <br><img src='" + normalized_url + "' style='max-width: 100px; max-height: 100px'>", {
                Yes: function () {
                    ok(true, "success with getLocal");
                    start();
                },
                No: function () {
                    ok(false, "failure with getLocal");
                    start();
                }
            });
        }, function (error) {
            forge.logging.error("forge.file.URL failed: ", error);
        });
    });

}
