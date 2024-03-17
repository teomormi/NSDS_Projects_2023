// Add event to form signup submission
document.getElementById("signup").addEventListener("submit", function(event) {
    event.preventDefault();
    var form = event.target.closest("form");
    var elements = "username=" + form.username.value + "&password="+ form.password.value + "&gender=" + form.gender.value + "&role=" + form.role.value + "&name=" + form.name.value;

    makeCall('POST', 'http://' + http_server_address + ':'+ http_port + '/' + http_signup_path, elements,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        form.reset();
                        window.location.href = "login.html";
                        break;
                    case 400:
                        show_error("Username already used");
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with back end");
                        break;
                }
            }
        }
    );
});