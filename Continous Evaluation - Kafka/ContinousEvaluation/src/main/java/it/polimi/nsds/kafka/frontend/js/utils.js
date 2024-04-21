// MUST be equal to ServerHTTP.java configurations
const http_server_address = "127.0.0.1";
const http_port = 8600;

const http_login_path = "login"
const http_signup_path = "signup"
const http_course_path = "course"
const http_project_path = "project"
const http_registration_path = "registration"

// function used to create an ajax request
function makeCall(method, url, elements, cback, reset = true) {
    let req = new XMLHttpRequest();
    req.onreadystatechange = function() {
        cback(req)
    }; // closure
    req.open(method, url, true);
    if (elements == null) {
        req.send();
    } else {
        req.send(elements);
    }
}

// show a popup with a message
function show_error(message) {
    let popup = document.getElementById('popup');
    let popupMessage = document.getElementById('popupMessage');
    popupMessage.textContent = message;
    popup.style.display = 'block';
}

// hide error popup
document.getElementById('closeBtn').addEventListener('click', function() {
    let popup = document.getElementById('popup');
    popup.style.display = 'none';
});
