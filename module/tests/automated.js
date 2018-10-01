/* global module, asyncTest, ok, start, $, forge */

module("forge.httpd");

asyncTest("Test that content is being served via https", 1, function () {
    if (window.location.protocol == "https:") {
        ok(true, "Expected 'https:'");
        start();
    } else {
        ok(false, "Expected 'https:', got: " + window.location.protocol);
        start();
    }
});

asyncTest("Test that content is being served from localhost", 1, function () {
    if (window.location.hostname == "localhost") {
        ok(true, "Expected 'localhost'");
        start();
    } else {
        ok(false, "Expected 'localhost', got: " + window.location.hostname);
        start();
    }
});

var numrequests = 512;

asyncTest("Test that we can make a lot of requests", numrequests, function () {
    var url = window.location.protocol + "//" +
              window.location.host + "/src/fixtures/httpd/icon-512.png";
    forge.logging.log("Using url: " + url);
    var count = 0;
    var options = {
        url: url,
        success: function () {
            count++;
            ok(true);
            if (count === numrequests) {
                start();
            }
        },
        error: function (error) {
            count++;
            ok(false, "Error: " + JSON.stringify(error));
            if (count === numrequests) {
                start();
            }
        }
    };
    for (var i = 0; i < numrequests; i++) {
        options.url = url + "?count=" + i;
        $.ajax(options);
    }
});
