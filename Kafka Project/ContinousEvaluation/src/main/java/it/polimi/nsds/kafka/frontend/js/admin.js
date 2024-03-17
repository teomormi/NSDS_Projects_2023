// check session
window.addEventListener("load", () => {
    if (sessionStorage.getItem("username") == null)
        window.location.href = "login.html";
}, false);

// show list of courses
document.addEventListener('DOMContentLoaded', function () {
    let param = "type=all_courses";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                let message = req.responseText;
                switch (req.status) {
                    case 200:
                        let courses_array = message.split(',');
                        let htmlString = '';
                        courses_array.forEach(function (course) {
                            htmlString += '<li onclick="showCourseDetails(\'' + course + '\')">' + course + '</li>';
                        });
                        document.getElementById("courseList").innerHTML = htmlString;
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

// create new course
function addCourse() {
    let courseName = document.getElementById("courseInput").value;
    if (courseName.trim() === "") {
        alert("Please enter a course name.");
        return;
    }
    let param = "type=new_course&course_name=" + courseName;
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        window.location.reload();
                        document.getElementById("courseInput").value = "";
                        break;
                    case 400:
                        show_error("Course already present!");
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
}

// remove a course
function removeCourse(){
    let courseName = document.getElementById("courseInput").value;
    if (courseName.trim() === "") {
        alert("Please enter a course name.");
        return;
    }
    let param = "type=remove_course&course_name=" + courseName;
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        window.location.reload();
                        document.getElementById("courseInput").value = "";
                        break;
                    case 404:
                        show_error("Course not found!");
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
}

// click on course, show available projects
function showCourseDetails(courseName) {
    let param = "course_name=" + courseName + "&type=get_project_names";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                var message = req.responseText;
                switch (req.status) {
                    case 200:
                        let projects_array = message.split(',');
                        let htmlString = '<h3>' + courseName + '</h3>';
                        projects_array.forEach(function (project) {
                            htmlString += '<h4>' + project + '</h4>';
                        });
                        document.getElementById("courseDetails").innerHTML = htmlString;
                        break;
                    case 404:
                        document.getElementById("courseDetails").innerHTML = "Not projects available in this course!";
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
}