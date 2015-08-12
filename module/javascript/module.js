// Expose the native API to javascript
forge.httpd = {
    showAlert: function (text, success, error) {
        forge.internal.call('httpd.showAlert', {text: text}, success, error);
    }
};

// Register for our native event
forge.internal.addEventListener("httpd.resume", function () {
	alert("Welcome back!");
});
