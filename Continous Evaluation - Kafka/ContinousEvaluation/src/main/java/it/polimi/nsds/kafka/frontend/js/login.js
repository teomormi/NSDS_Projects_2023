// Add event to form login submission
document.getElementById("login").addEventListener("submit", function(event) {
    event.preventDefault();
    var form = event.target.closest("form");
    var elements = "username=" + form.username.value + "&password="+ form.password.value
    makeCall('POST', 'http://' + http_server_address + ':'+ http_port + '/' + http_login_path, elements,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        sessionStorage.setItem('username', form.username.value);
                        switch(req.responseText){
                            case "student":
                                window.location.href = "student.html";
                                break;
                            case "professor":
                                window.location.href = "professor.html";
                                break;
                            case "admin":
                                window.location.href = "admin.html";
                                break;
                        }
                        break;
                    case 400: // bad request
                        show_error("Wrong username or password");
                        break;
                    case 405: // method not allowed
                        show_error("Method not allowed on the server");
                        break;
                    case 503: // service unavailable
                        show_error("Unable to communicate with back end");
                        break;
                }
            }
        }
    );
});