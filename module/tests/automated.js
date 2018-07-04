/* global module, asyncTest, ok, start */

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
    if (window.location.hostname == "toolkit-local.com") {
        ok(true, "Expected 'toolkit-local.com'");
        start();
    } else {
        ok(false, "Expected 'toolkit-local.com', got: " + window.location.hostname);
        start();
    }
});
