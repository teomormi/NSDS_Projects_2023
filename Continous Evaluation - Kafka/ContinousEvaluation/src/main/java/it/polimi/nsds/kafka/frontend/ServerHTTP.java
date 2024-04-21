package it.polimi.nsds.kafka.frontend;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import it.polimi.nsds.kafka.beans.Submission;
import it.polimi.nsds.kafka.enums.Role;
import it.polimi.nsds.kafka.services.user.UserRemote;
import it.polimi.nsds.kafka.services.user.UserService;
import it.polimi.nsds.kafka.services.course.CourseRemote;
import it.polimi.nsds.kafka.services.course.CourseService;
import it.polimi.nsds.kafka.services.project.ProjectRemote;
import it.polimi.nsds.kafka.services.project.ProjectService;
import it.polimi.nsds.kafka.services.registration.RegistrationRemote;
import it.polimi.nsds.kafka.services.registration.RegistrationService;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHTTP {
	// MUST BE equal to frontend/js/utils.js configuration
	private static final String httpAddress = "127.0.0.1"; // local IP address
	private static final int httpPort = 8600;

	public static final String httpLoginPath = "/login";
	public static final String httpSignUpPath = "/signup";
	public static final String httpProjectPath = "/project";
	public static final String httpRegistrationPath = "/registration";
	public static final String httpCoursePath = "/course";

	// ip addresses rmi services
	private static final String userServiceAddress = "localhost";
	private static final String courseServiceAddress = "localhost";
	private static final String projectServiceAddress = "localhost";
	private static final String registrationServiceAddress = "localhost";

	protected static StringBuilder response;
	protected static OutputStream os;

	public static void main(String[] args) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(httpAddress, httpPort), 0);

			// create http handler and retrieve remote object via RMI
			server.createContext(httpLoginPath,
					new LoginHandler(LocateRegistry.getRegistry(userServiceAddress, UserService.userServicePort).lookup(UserService.userServiceName)));
			server.createContext(httpSignUpPath,
					new SignUpHandler(LocateRegistry.getRegistry(userServiceAddress, UserService.userServicePort).lookup(UserService.userServiceName)));

			server.createContext(httpCoursePath,
					new CourseHandler(LocateRegistry.getRegistry(courseServiceAddress, CourseService.courseServicePort).lookup(CourseService.courseServiceName)));

			server.createContext(httpProjectPath,
					new ProjectHandler(LocateRegistry.getRegistry(projectServiceAddress, ProjectService.projectServicePort).lookup(ProjectService.projectServiceName)));

			server.createContext(httpRegistrationPath,
					new RegistrationHandler(LocateRegistry.getRegistry(registrationServiceAddress, RegistrationService.registrationServicePort).lookup(RegistrationService.registrationServiceName)));

			server.setExecutor(null); // default executor for http server
			server.start();
			System.out.println("HTTP Server started!");
		} catch (Exception e) {
			System.out.println("ERROR - unable to start HTTP Server - one or more services are not available");
			e.printStackTrace();
		}
	}

	/*** Implement HTTP Handler in 2 ways:
	 - 2 different handler for same service (user_service), each handler with a different path
	 - same handler but with "type" parameter in the request  ***/

	static class LoginHandler implements HttpHandler {
		UserRemote remote_user;

		protected LoginHandler(Remote remote_user) {
			this.remote_user = (UserRemote) remote_user;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			System.out.print("Received a Login invocation: ");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			Map<String, String> params = parseFormInput(exchange.getRequestBody());

			// manage request for login
			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					String username = params.get("username");
					String password = params.get("password");

					// check identity using RMI
					Role user_role = remote_user.checkUser(username, password);
					if (user_role != null) {
						// response with Role to redirect to proper dashboard
						String response = user_role.getDescription();
						exchange.sendResponseHeaders(200, response.length());
						os = exchange.getResponseBody();
						os.write(response.getBytes());
					} else {
						exchange.sendResponseHeaders(400, 0); // Bad Request
					}
				} catch (RemoteException e) {
					System.out.println("Unable to communicate with back end user service");
					exchange.sendResponseHeaders(503, 0); // Service Unavailable
					// try to reconnect
					try {
						remote_user = (UserRemote) LocateRegistry.getRegistry(userServiceAddress, UserService.userServicePort).lookup(UserService.userServiceName);
					} catch (NotBoundException ex) {
						System.out.println("Reconnection FAILED!");
					}
				}
			} else {
				exchange.sendResponseHeaders(405, 0); // Method Not Allowed
			}
			exchange.getResponseBody().close();
		}
	}


	static class SignUpHandler implements HttpHandler {
		UserRemote remote_user;

		protected SignUpHandler(Remote remote_user) {
			this.remote_user = (UserRemote) remote_user;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			System.out.print("Received a SignUp invocation: ");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			Map<String, String> params = parseFormInput(exchange.getRequestBody());
			// manage request for register a new user
			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					if (remote_user.addUser(params.get("username"), params.get("password"), params.get("role"), params.get("gender"))) {
						exchange.sendResponseHeaders(200, 0);
					} else {
						System.out.println("Username already used");
						exchange.sendResponseHeaders(400, 0); // Bad Request
					}
				} catch (RemoteException e) {
					System.out.println("Unable to communicate with back end user service");
					exchange.sendResponseHeaders(503, 0); // Service Unavailable
					// try to reconnect
					try {
						remote_user = (UserRemote) LocateRegistry.getRegistry(userServiceAddress, UserService.userServicePort).lookup(UserService.userServiceName);
					} catch (NotBoundException ex) {
						System.out.println("Reconnection FAILED!");
					}
				}
			} else {
				exchange.sendResponseHeaders(405, 0); // Method Not Allowed
			}
			exchange.getResponseBody().close();
		}
	}


	static class CourseHandler implements HttpHandler {
		CourseRemote remote_course;

		protected CourseHandler(Remote remote_course) {
			this.remote_course = (CourseRemote) remote_course;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			System.out.print("Received a Course invocation: ");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			Map<String, String> params = parseFormInput(exchange.getRequestBody());

			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					// analise the type parameter
					switch (params.get("type")) {
						case "enrolled_courses":
							// returns list of courses in which student is enrolled
							List<String> courses_enrolled = remote_course.getCoursesNameEnrolled(params.get("username"));
							response = new StringBuilder(courses_enrolled.get(0));
							for (int i = 1; i < courses_enrolled.size(); i++) {
								response.append(",").append(courses_enrolled.get(i));
							}
							exchange.sendResponseHeaders(200, response.length());
							os = exchange.getResponseBody();
							os.write(response.toString().getBytes());
							break;
						case "not_enrolled_courses":
							// returns list of courses in which student is not enrolled
							List<String> courses_not_enrolled = remote_course.getCoursesNameNotEnrolled(params.get("username"));
							response = new StringBuilder(courses_not_enrolled.get(0));
							for (int i = 1; i < courses_not_enrolled.size(); i++) {
								response.append(",").append(courses_not_enrolled.get(i));
							}
							exchange.sendResponseHeaders(200, response.length());
							os = exchange.getResponseBody();
							os.write(response.toString().getBytes());
							break;
						case "enroll_student":
							// enroll student to a new course
							if (remote_course.enrollStudent(params.get("username"), params.get("course_name")))
								exchange.sendResponseHeaders(200, 0); // Ack
							else
								exchange.sendResponseHeaders(400, 0); // Bad Request
							break;
						case "get_project_names":
							// return list of projects for a specific course
							List<String> project_names = remote_course.getProjectsName(params.get("course_name"));
							if (project_names.size() > 0) {
								response = new StringBuilder(project_names.get(0));
								for (int i = 1; i < project_names.size(); i++) {
									response.append(",").append(project_names.get(i));
								}
								exchange.sendResponseHeaders(200, response.length());
								os = exchange.getResponseBody();
								os.write(response.toString().getBytes());
							} else
								exchange.sendResponseHeaders(404, 0); // Not Found
							break;
						case "all_courses":
							// returns list of all courses
							List<String> all_courses = remote_course.getCoursesName();
							response = new StringBuilder(all_courses.get(0));
							for (int i = 1; i < all_courses.size(); i++) {
								response.append(",").append(all_courses.get(i));
							}
							exchange.sendResponseHeaders(200, response.length());
							os = exchange.getResponseBody();
							os.write(response.toString().getBytes());
							break;
						case "new_course":
							// create new course
							if (remote_course.addCourse(params.get("course_name")))
								exchange.sendResponseHeaders(200, 0);
							else
								exchange.sendResponseHeaders(400, 0); // Bad Request
							break;
						case "remove_course":
							// remove (if present) an old course
							if (remote_course.removeCourse(params.get("course_name")))
								exchange.sendResponseHeaders(200, 0);
							else
								exchange.sendResponseHeaders(404, 0); // Not Found
							break;
						case "new_project":
							// create a new project inside a specific course
							if (remote_course.addProject(params.get("course_name"), params.get("project_name")))
								exchange.sendResponseHeaders(200, 0);
							else
								exchange.sendResponseHeaders(400, 0); // Bad Request
							break;
						default:
					}
				} catch (RemoteException e) {
					System.out.println("Unable to communicate with back end course service");
					exchange.sendResponseHeaders(503, 0); // Service Unavailable
					// try to reconnect
					try {
						remote_course = (CourseRemote) LocateRegistry.getRegistry(courseServiceAddress, CourseService.courseServicePort).lookup(CourseService.courseServiceName);
					} catch (NotBoundException ex) {
						System.out.println("Reconnection FAILED!");
					}
				}
			} else
				exchange.sendResponseHeaders(405, 0); // Method Not Allowed
			exchange.getResponseBody().close();
		}
	}


	static class ProjectHandler implements HttpHandler {
		ProjectRemote remote_project;

		protected ProjectHandler(Remote remote_project) {
			this.remote_project = (ProjectRemote) remote_project;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			System.out.print("Received a Project invocation: ");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			Map<String, String> params = parseFormInput(exchange.getRequestBody());

			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					switch (params.get("type")) {
						// return specific student submission file to allow professor to download it
						case "get_file":
							Submission subm = remote_project.getSubmissionDetails(params.get("course_name"), params.get("project_name"), params.get("username"));
							if (subm != null) {
								try {
									String currentDir = System.getProperty("user.dir");
									String filePath = "file_db" + File.separator + params.get("course_name") + File.separator + params.get("project_name") +
											File.separator + params.get("username");
									File file = new File(currentDir + File.separator + filePath + File.separator + subm.getFile_path());
									// Send file
									exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + subm.getFile_path());
									exchange.sendResponseHeaders(200, file.length());
									OutputStream outputStream = exchange.getResponseBody();
									Files.copy(file.toPath(), outputStream);
									outputStream.close();
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else
								exchange.sendResponseHeaders(404, 0); // Not Found
							break;
							// return infos for a specific submission like file_path and grade
						case "get_info_sub":
							Submission sub = remote_project.getSubmissionDetails(params.get("course_name"), params.get("project_name"), params.get("username"));
							if (sub != null) {
								response = new StringBuilder("Submitted file: " + sub.getFile_path());
								if (sub.getGrade() != 0)
									response.append("</br> Grade: ").append(sub.getGrade());
								exchange.sendResponseHeaders(200, response.length());
								os = exchange.getResponseBody();
								os.write(response.toString().getBytes());
							} else
								exchange.sendResponseHeaders(404, 0); // Not Found
							break;
							// add submission for a specific project in a course
						case "add_submission":
							if (remote_project.addSubmission(params.get("course_name"), params.get("project_name"),
									params.get("username"), params.get("file_path"))) {
								try {
									String currentDir = System.getProperty("user.dir");
									String filePath = "file_db" + File.separator + params.get("course_name") + File.separator + params.get("project_name") +
											File.separator + params.get("username");
									File file = new File(currentDir + File.separator + filePath + File.separator + params.get("file_path"));
									File parentDir = file.getParentFile();
									if (!parentDir.exists()) {
										parentDir.mkdirs(); // Create directory if not exist
									}
									if (!file.createNewFile()) {
										exchange.sendResponseHeaders(400, 0); // File already present for this delivery!
										break;
									}
									FileWriter writer = new FileWriter(file);
									writer.write(params.get("file"));
									writer.close();
								} catch (IOException | SecurityException e) {
									e.printStackTrace();
								}
								exchange.sendResponseHeaders(200, 0); // Ack
							} else
								exchange.sendResponseHeaders(400, 0); // Bad Request
							break;
							// returns list of submissions for a specific project
						case "get_all_submissions":
							Map<String, Submission> map = remote_project.getSubmissions(params.get("course_name"), params.get("project_name"));
							if (map != null) {
								String rep = (new Gson()).toJson(map);
								exchange.sendResponseHeaders(200, rep.length());
								os = exchange.getResponseBody();
								os.write(rep.getBytes());
							} else
								exchange.sendResponseHeaders(404, 0); // Not Found
							break;
						case "add_grade":
							if (remote_project.addGrade(params.get("course_name"), params.get("project_name"), params.get("username"), params.get("grade")))
								exchange.sendResponseHeaders(200, 0); // Ack
							else
								exchange.sendResponseHeaders(400, 0); // Bad Request
							break;
					}
				} catch (RemoteException e) {
					System.out.println("Unable to communicate with back end project service");
					exchange.sendResponseHeaders(503, 0); // Service Unavailable
					// try to reconnect
					try {
						remote_project = (ProjectRemote) LocateRegistry.getRegistry(projectServiceAddress, ProjectService.projectServicePort).lookup(ProjectService.projectServiceName);
					} catch (NotBoundException ex) {
						System.out.println("Reconnection FAILED!");
					}
				}
			} else
				exchange.sendResponseHeaders(405, 0); // Method Not Allowed
			exchange.getResponseBody().close();
		}
	}

	static class RegistrationHandler implements HttpHandler {
		RegistrationRemote remote_registration;

		protected RegistrationHandler(Remote remote_registration) {
			this.remote_registration = (RegistrationRemote) remote_registration;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			System.out.print("Received a Registration invocation: ");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			Map<String, String> params = parseFormInput(exchange.getRequestBody());

			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					// return final grade for a specific course
					if ("check_end_course".equals(params.get("type"))) {
						Integer result = remote_registration.checkCompletedCourse(params.get("course_name"), params.get("username"));
						if ((result != null) && (result >= 18)) {
							String rep = result.toString();
							exchange.sendResponseHeaders(200, rep.length());
							os = exchange.getResponseBody();
							os.write(rep.getBytes());
						} else
							exchange.sendResponseHeaders(404, 0); // Not found
					}
				} catch (RemoteException e) {
					System.out.println("Unable to communicate with back end course service");
					exchange.sendResponseHeaders(503, 0); // Service Unavailable
					// try to reconnect
					try {
						remote_registration = (RegistrationRemote) LocateRegistry.getRegistry(registrationServiceAddress, RegistrationService.registrationServicePort).lookup(RegistrationService.registrationServiceName);
					} catch (NotBoundException ex) {
						System.out.println("Reconnection FAILED!");
					}
				}
			} else
				exchange.sendResponseHeaders(405, 0); // Method Not Allowed
			exchange.getResponseBody().close();
		}
	}

	/**** Convert InputStream to Map of parameters ****/
	private static Map<String, String> parseFormInput(InputStream input) {
		Map<String, String> params = new HashMap<>();
		try {
			// retrieve data
			BufferedReader in = new BufferedReader(new InputStreamReader(input));
			StringBuilder requestData = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				requestData.append(line);
			}
			in.close();
			// split it
			String[] pairs = requestData.toString().split("&");
			for (String pair : pairs) {
				String[] keyValue = pair.split("=");
				if (keyValue.length == 2) {
					String key = keyValue[0];
					String value = keyValue[1];
					params.put(key, value);
				}
			}
		} catch (IOException ex) {
			System.out.println("Exception during parsing input");
		}
		System.out.println(params);
		return params;
	}
}