/* global module, asyncTest, ok, start */

module("httpd");

asyncTest("Test that content is being served via http", 1, function () {
	if (window.location.protocol == "http:") {
        ok(true, "Expected 'http:'");
        start();
	} else {
        ok(false, "Expected 'http:', got: " + window.location.protocol);
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