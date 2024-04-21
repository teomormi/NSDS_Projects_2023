// check session
window.addEventListener("load", () => {
    if (sessionStorage.getItem("username") == null)
        window.location.href = "login.html";
}, false);

// show list of enrolled courses
document.addEventListener('DOMContentLoaded', function () {
    let param = "username=" + sessionStorage.getItem("username") + "&type=enrolled_courses";
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
                        document.getElementById("course_list").innerHTML = htmlString;
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with course service");
                        break;
                }
            }
        }
    );
});

// create select of not_enrolled courses
document.addEventListener('DOMContentLoaded', function () {
    let param = "username=" + sessionStorage.getItem("username") + "&type=not_enrolled_courses";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                let message = req.responseText;
                switch (req.status) {
                    case 200:
                        let courses_array = message.split(',');
                        let htmlString = '';
                        courses_array.forEach(function (course) {
                            htmlString += '<option value="' + course + '">' + course + '</option>';
                        });
                        document.getElementById("newCourseSelect").innerHTML = htmlString;
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with course service");
                        break;
                }
            }
        }
    );
});

// register to a new course
function registerToNewCourse() {
    let newCourseSelect = document.getElementById("newCourseSelect");
    let selectedNewCourse = newCourseSelect.options[newCourseSelect.selectedIndex].text;
    let param = "username=" + sessionStorage.getItem("username") + "&course_name=" + selectedNewCourse + "&type=enroll_student";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        window.location.reload();
                        break;
                    case 400:
                        show_error("Bad Request");
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with course service");
                        break;
                }
            }
        }
    );
}

// click on course, show available projects
function showCourseDetails(courseName) {
    // clean old information
    document.getElementById("assignmentDetailsContent").innerHTML = "";
    document.getElementById("assignment_details").textContent = "";
    document.getElementById("add_assignment").style.display = "none";
    let param = "course_name=" + courseName + "&type=get_project_names";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                var message = req.responseText;
                switch (req.status) {
                    case 200:
                        let projects_array = message.split(',');
                        let htmlString = '<h3 id="course_name">' + courseName + '</h3> <select id="assignmentSelect" onchange="showSubmissionDetails(this.value)">';
                        projects_array.forEach(function (project) {
                            htmlString += '<option value="' + project + '">' + project + '</option>';
                        });
                        htmlString += '</select>';
                        document.getElementById("assignmentDetailsContent").innerHTML = htmlString;
                        showSubmissionDetails(projects_array[0]);
                        checkEndedCourse(courseName);
                        break;
                    case 404:
                        document.getElementById("assignmentDetailsContent").innerHTML = "Not projects available in this course!";
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with course service");
                        break;
                }
            }
        }
    );
}

// show submission details (if a submission is present) for a specific project
function showSubmissionDetails(project_name) {
    // clean old information
    document.getElementById("assignment_details").textContent = "";
    document.getElementById("add_assignment").style.display = "none";
    let courseName = document.getElementById("course_name").innerText;
    let username = sessionStorage.getItem("username");
    let param = "course_name=" + courseName + "&username=" + username + "&project_name=" + project_name + "&type=get_info_sub";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_project_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                let message = req.responseText;
                switch (req.status) {
                    case 200:
                        document.getElementById("assignment_details").innerHTML = message;
                        break;
                    case 404:
                        // Not submission found
                        document.getElementById("add_assignment").style.display = "block";
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with project service");
                        break;
                }
            }
        }
    );
}

// add a submission for a project
function submitAssignment() {
    const reader = new FileReader();
    reader.onload = function(e) {
        const fileContent = e.target.result;
        let project_name = document.getElementById("assignmentSelect").value;
        let courseName = document.getElementById("course_name").innerText;
        let fileName = document.getElementById("fileUpload").files[0].name;
        let username = sessionStorage.getItem("username");
        let param = "course_name=" + courseName + "&username=" + username + "&project_name=" + project_name
            + "&file_path=" + fileName + "&file=" + fileContent + "&type=add_submission";
        makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_project_path, param,
            function (req) {
                if (req.readyState === XMLHttpRequest.DONE) {
                    switch (req.status) {
                        case 200:
                            showSubmissionDetails(project_name);
                            break;
                        case 400:
                            show_error("Bad Request, impossible to add submission");
                            break;
                        case 405:
                            show_error("Method Not Allowed");
                            break;
                        case 503:
                            show_error("Unable to communicate with project service");
                            break;
                    }
                }
            }
        );
    };
    reader.readAsText(document.getElementById("fileUpload").files[0]);
}

// Show selected file name
document.getElementById("fileUpload").addEventListener("change", function () {
    let fileNameDisplay = document.getElementById("fileName");
    fileNameDisplay.textContent = this.files[0].name;
});

// Check if there is a final grade
function checkEndedCourse(courseName){
    // clear old grade if present
    let existingGrade= document.getElementById("finalGrade");
    if (existingGrade) {
        existingGrade.parentNode.removeChild(existingGrade);
    }

    let param = "course_name=" + courseName + "&username=" + sessionStorage.getItem("username") + "&type=check_end_course";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_registration_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        let newGrade = document.createElement('h4');
                        newGrade.id = "finalGrade";
                        newGrade.textContent = "Final grade: " +req.responseText;
                        document.getElementById("course_name").parentNode.insertBefore(newGrade, document.getElementById("course_name").nextSibling);
                        break;
                    case 404:
                        console.log("Final grade not found");
                        break;
                    case 405:
                        show_error("Method Not Allowed");
                        break;
                    case 503:
                        show_error("Unable to communicate with registration service");
                        break;
                }
            }
        }
    );
}