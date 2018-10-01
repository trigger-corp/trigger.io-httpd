/* global module, asyncTest, ok, start, $, console */

module("forge.httpd");

asyncTest("Test that content is being served via https", 1, function () {
    if (window.location.protocol == "https:") {
        ok(true);
        start();
    } else {
        ok(false, "Expected 'https:', got: " + window.location.protocol);
        start();
    }
});

asyncTest("Test that content is being served from localhost", 1, function () {
    if (window.location.hostname == "localhost") {
        ok(true);
        start();
    } else {
        ok(false, "Expected 'localhost', got: " + window.location.hostname);
        start();
    }
});

var numrequests = 64;

asyncTest("Test that we can make a lot of requests", numrequests, function () {
    var url = window.location.protocol + "//" +
              window.location.host + "/src/fixtures/httpd/icon-512.png";
    var count = 0;
    var options = {
        url: url,
        success: function () {
            count++;
            console.log("success: " + count);
            ok(true);
            if (count === numrequests) {
                console.log("success start");
                start();
            }
        },
        error: function (error) {
            count++;
            console.log("error: " + count);
            ok(false, "Error: " + JSON.stringify(error));
            if (count === numrequests) {
                console.log("error start");
                start();
            }
        }
    };
    for (var i = 0; i < numrequests; i++) {
        options.url = url + "?count=" + i;
        $.ajax(options);
    }
});
