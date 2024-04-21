// check session
window.addEventListener("load", () => {
    if (sessionStorage.getItem("username") == null)
        window.location.href = "login.html";
}, false);

// show list of courses
document.addEventListener('DOMContentLoaded', function () {
    var param = "type=all_courses";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                let message = req.responseText;
                switch (req.status) {
                    case 200:
                        var courses_array = message.split(',');
                        var htmlString = '';
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

// click on course, show available projects
function showCourseDetails(courseName) {
    document.getElementById("div_add_project").style.display = "block";
    let param = "course_name=" + courseName + "&type=get_project_names";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                var message = req.responseText;
                switch (req.status) {
                    case 200:
                        let projects_array = message.split(',');
                        let htmlString = '<h3 id="course_name">' + courseName + '</h3> <select id="assignmentSelect" onchange="showSubmissions(this.value)">';
                        projects_array.forEach(function (project) {
                            htmlString += '<option value="' + project + '">' + project + '</option>';
                        });
                        htmlString += '</select>';
                        document.getElementById("assignmentDetailsContent").innerHTML = htmlString;
                        showSubmissions(projects_array[0]);
                        break;
                    case 404:
                        let string = '<h3 id="course_name">' + courseName + '</h3>';
                        document.getElementById("assignmentDetailsContent").innerHTML = string + "Not projects available in this course!";
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

// create new project
function addProject() {
    let projectName = document.getElementById("projectInput").value;
    let courseName = document.getElementById("course_name").innerText;
    if (projectName.trim() === "") {
        alert("Please enter a project name.");
        return;
    }
    var param = "type=new_project&course_name=" + courseName + "&project_name=" + projectName;
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_course_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        showCourseDetails(courseName);
                        document.getElementById("projectInput").value = "";
                        break;
                    case 400:
                        show_error("Project already present");
                        document.getElementById("projectInput").value = "";
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

// show (if present) submissions for a project
function showSubmissions(project_name) {
    cleanTable();
    let courseName = document.getElementById("course_name").innerText;
    let param = "course_name=" + courseName + "&project_name=" + project_name + "&type=get_all_submissions";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_project_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                var message = req.responseText;
                switch (req.status) {
                    case 200:
                        createTable(project_name,message);
                        break;
                    case 404:
                        // Still no submissions for this project
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

// remove submissions table to create a newer one
function cleanTable(){
    let existingTable = document.getElementById("studentTable");
    if (existingTable) {
        existingTable.parentNode.removeChild(existingTable);
    }
}

// create table with submissions
function createTable(project_name,string_data) {
    let data = JSON.parse(string_data);
    cleanTable();
    let table = document.createElement('table');
    table.id = "studentTable";
    // Create thead
    let thead = document.createElement('thead');
    let headerRow = document.createElement('tr');
    let headers = ['Username', 'File Path', 'Grade'];
    headers.forEach(function (headerText) {
        let th = document.createElement('th');
        th.textContent = headerText;
        headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    table.appendChild(thead);

    // Create tbody
    let tbody = document.createElement('tbody');
    for (let key in data) {
        if (data.hasOwnProperty(key)) {
            let row = document.createElement('tr');

            let usernameCell = document.createElement('td');
            usernameCell.textContent = key;
            row.appendChild(usernameCell);

            let filePathCell = document.createElement('td');
            getFileContent(project_name, key,filePathCell,data[key].file_path); // key is the username
            row.appendChild(filePathCell);

            let gradeCell = document.createElement('td');
            if (data[key].grade === 0) {
                let gradeInput = document.createElement('input');
                gradeInput.type = 'number';
                gradeInput.min = 1;
                gradeInput.max = 30;
                gradeInput.value = 1;
                gradeCell.appendChild(gradeInput);
                let gradeButton = document.createElement('button');
                gradeButton.textContent = 'Grade';
                gradeButton.addEventListener('click', function() {
                    submitEvaluation(project_name, key, gradeInput.value);
                });
                gradeCell.appendChild(gradeButton);
            } else {
                gradeCell.textContent = data[key].grade;
            }
            row.appendChild(gradeCell);
            tbody.appendChild(row);
        }
    }
    table.appendChild(tbody);

    // Add table to container
    document.getElementById("assignmentDetailsContent").appendChild(table);
}

// add grade to a submission
function submitEvaluation(project_name,username,grade) {
    let courseName = document.getElementById("course_name").innerText;
    let param = "course_name=" + courseName + "&username=" + username + "&project_name=" + project_name
        + "&grade=" + grade + "&type=add_grade";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_project_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        showSubmissions(project_name);
                        break;
                    case 400:
                        show_error("Bad Request");
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

// retrieve file content from project service and create a link for download
function getFileContent(project_name,username,fileCell,file_name){
    let courseName = document.getElementById("course_name").innerText;
    let param = "course_name=" + courseName + "&username=" + username + "&project_name=" + project_name + "&type=get_file";
    makeCall("POST", 'http://' + http_server_address + ':' + http_port + '/' + http_project_path, param,
        function (req) {
            if (req.readyState === XMLHttpRequest.DONE) {
                let message = req.responseText;
                switch (req.status) {
                    case 200:
                        let blob = new Blob([message], { type: 'text/plain' });
                        let url = window.URL.createObjectURL(blob);
                        let downloadLink = document.createElement('a');
                        downloadLink.href = url;
                        downloadLink.download = file_name;
                        downloadLink.innerHTML = file_name;
                        fileCell.appendChild(downloadLink)
                        break;
                    case 404:
                        show_error("File content not found!");
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