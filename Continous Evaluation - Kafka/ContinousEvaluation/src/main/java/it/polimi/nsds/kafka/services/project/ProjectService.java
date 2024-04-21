package it.polimi.nsds.kafka.services.project;

import it.polimi.nsds.kafka.beans.Project;
import it.polimi.nsds.kafka.beans.Submission;
import it.polimi.nsds.kafka.services.KafkaBroker;
import it.polimi.nsds.kafka.services.course.CourseService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ProjectService extends UnicastRemoteObject implements ProjectRemote {

	private static final String projectTopic = "project_recovery";
	public static final String projectUpdatesTopic = "project_update";

	private static KafkaProducer<String, String> project_producer;
	private static final Map<String, List<Project>> db_projects = new HashMap<>(); // course_name, list of projects

	/*** RMI Info ***/
	public static final int projectServicePort = 2002;
	public static final String projectServiceName = "projectRemote";

	protected ProjectService() throws RemoteException {
		super();
	}

	public static void main(String[] args) {
		// set different kafka broker address
		if(args.length > 0) {
			KafkaBroker.brokerAddress = args[0];
			System.out.println("KAFKA BROKER ADDRESS: " + args[0]);
		}

		project_producer = KafkaBroker.getProducerTransactional();
		final KafkaConsumer<String, String> project_consumer = new KafkaConsumer<>(KafkaBroker.getConsumerProperties("project"));
		// recover phase
		System.out.println(">>>> START RECOVERY PHASE");
		// recover message sent by course service
		recoverProject(project_consumer, CourseService.courseUpdatesTopic);
		// recover message from private recovery
		recoverProject(project_consumer, projectTopic);
		System.out.println("END RECOVERY PHASE<<<");

		// declare thread for RMI listening
		Thread thread = new Thread(() -> {
			try {
				ProjectService projectService = new ProjectService();
				Registry registry = LocateRegistry.createRegistry(projectServicePort);
				registry.bind(projectServiceName, projectService);

				System.out.println("Project RMI Server online");

				Scanner scanner = new Scanner(System.in);
				while (true) {
					String input = scanner.nextLine();
					if (input.equals("exit")) {
						break;
					}
				}
				scanner.close();
				UnicastRemoteObject.unexportObject(projectService, true);
			} catch (Exception e) {
				System.out.println("Project RMI Server OFFLINE!");
			}
			System.out.println("RMI Server CLOSED");
		});

		// open RMI communication to http server
		thread.start();

		// prepare to receive kafka message from other services
		project_consumer.subscribe(Collections.singletonList(CourseService.courseUpdatesTopic));

		while (thread.getState() != Thread.State.TERMINATED) {
			final ConsumerRecords<String, String> records = project_consumer.poll(Duration.of(30, ChronoUnit.SECONDS));
			for (final ConsumerRecord<String, String> record : records) {
				manageKafkaMessage(record.key(), record.value());
			}
		}
		project_consumer.close();
		project_producer.close();
		System.out.println("Kafka consumer closed");
	}

	private static void recoverProject(KafkaConsumer<String, String> consumer, String topic) {
		consumer.subscribe(Collections.singletonList(topic));
		// reset old commit
		consumer.poll(0);
		consumer.seekToBeginning(consumer.assignment());
		final ConsumerRecords<String, String> records = consumer.poll(Duration.of(1, ChronoUnit.MINUTES));
		for (final ConsumerRecord<String, String> record : records) {
			manageKafkaMessage(record.key(), record.value());
		}
		// not read old message anymore
		consumer.commitSync();
		consumer.unsubscribe();
	}

	private static void manageKafkaMessage(String key, String value) {
		// variable to extract data from messages received from course service
		String course_name, project_name, username, file_path, grade;
		String[] values;
		List<Project> projects;

		System.out.print("KAFKA MESSAGE: ");
		switch (key) {
			case "new_course":
				db_projects.put(value, new ArrayList<>());
				System.out.println("Created course <" + value + ">");
				break;
			case "new_project":
				values = value.split("&");
				course_name = values[0];
				project_name = values[1];
				projects = db_projects.get(course_name);
				if (projects != null) {
					projects.add(new Project(project_name));
					System.out.printf("Project <%s> added to course <%s>\n", project_name, course_name);
				}
				break;
			case "new_submission":
				values = value.split("&");
				course_name = values[0];
				project_name = values[1];
				username = values[2];
				file_path = values[3];
				projects = db_projects.get(course_name);
				if (projects != null) {
					for (Project p : projects) {
						if (p.getProjectName().equals(project_name)) { // we found the project inside course_name
							p.addSubmission(username, file_path);
							System.out.printf("Submission added for course <%s> to project <%s> to student <%s>\n", course_name, project_name, username);
						}
					}
				}
				break;
			case "new_grade":
				values = value.split("&"); // course_name&project_name&username&grade
				course_name = values[0];
				project_name = values[1];
				username = values[2];
				grade = values[3];
				projects = db_projects.get(course_name);
				if (projects != null) {
					for (Project p : projects) {
						if (p.getProjectName().equals(project_name)) {
							p.addGrade(username, Integer.parseInt(grade));
							System.out.printf("Grade <%s> added for course <%s> to project <%s> to student <%s>\n", grade, course_name, project_name, username);
						}
					}
				}
				break;
		}
	}

	/****  RMI for communication between front-end and back-end  ****/

	/*** STUDENT DASHBOARD ***/
	@Override
	public boolean addSubmission(String course_name, String project_name, String username, String file_path) throws RemoteException {
		List<Project> projects = db_projects.get(course_name);
		if (projects == null)
			return false;
		for (Project p : projects) {
			if (p.getProjectName().equals(project_name)) { // we found the project inside course_name
				if (p.addSubmission(username, file_path)) {
					// publish submission for recovery
					String par = course_name + "&" + project_name + "&" + username + "&" + file_path;
					project_producer.beginTransaction();
					ProducerRecord<String, String> record = new ProducerRecord<>(projectTopic, "new_submission", par);
					project_producer.send(record);
					project_producer.commitTransaction();
					System.out.printf("HTTP MESSAGE: Submission added for student <%s> to course <%s> for project <%s>\n", username, course_name, project_name);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Submission getSubmissionDetails(String course_name, String project_name, String username) throws
			RemoteException {
		List<Project> projects = db_projects.get(course_name);
		if (projects == null)
			return null;
		for (Project p : projects) {
			if (p.getProjectName().equals(project_name)) { // we found the project inside course_name
				if (p.getSubmissionDetails(username) != null) {
					return p.getSubmissionDetails(username);
				}
			}
		}
		return null;
	}

	/*** PROFESSOR DASHBOARD ***/
	@Override
	public boolean addGrade(String course_name, String project_name, String username, String grade) throws RemoteException {
		int grade_int;
		try {
			grade_int = Integer.parseInt(grade);
		} catch (NumberFormatException e) {
			return false;
		}
		List<Project> projects = db_projects.get(course_name);
		if (projects == null)
			return false;
		for (Project p : projects) {
			if (p.getProjectName().equals(project_name)) {
				if (p.addGrade(username, grade_int)) {
					System.out.printf("HTTP MESSAGE: Grade <%s> added for student <%s> to course <%s> for project <%s>\n", grade, username, course_name, project_name);
					String par = course_name + "&" + project_name + "&" + username + "&" + grade;
					project_producer.beginTransaction();
					ProducerRecord<String, String> record_project = new ProducerRecord<>(projectUpdatesTopic, "new_grade", par);
					// publish grade for registration service
					project_producer.send(record_project);
					// publish grade for recovery
					record_project = new ProducerRecord<>(projectTopic, "new_grade", par);
					project_producer.send(record_project);
					project_producer.commitTransaction();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Map<String, Submission> getSubmissions(String course_name, String project_name) throws RemoteException {
		List<Project> projects = db_projects.get(course_name);
		if (projects == null)
			return null;
		for (Project p : projects) {
			if (p.getProjectName().equals(project_name)) {
				if (p.getSubmissions().isEmpty())
					return null;
				else
					return p.getSubmissions();
			}
		}
		return null;
	}
}
