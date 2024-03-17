package it.polimi.nsds.kafka.services.registration;

import it.polimi.nsds.kafka.beans.RegisteredProject;
import it.polimi.nsds.kafka.services.KafkaBroker;
import it.polimi.nsds.kafka.services.course.CourseService;
import it.polimi.nsds.kafka.services.project.ProjectService;
import it.polimi.nsds.kafka.utils.TupleCourseUserGrade;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RegistrationService extends UnicastRemoteObject implements RegistrationRemote {

	private static final Map<String, List<RegisteredProject>> db_registrations = new HashMap<>(); // key: couse_name
	private static final List<TupleCourseUserGrade> final_grades = new ArrayList<>(); // course_name,username, final grade

	/*** RMI Info ***/
	public static final int registrationServicePort = 2003;
	public static final String registrationServiceName = "registrationRemote";

	protected RegistrationService() throws RemoteException {
		super();
	}

	public static void main(String[] args) {
		try {
			System.out.println("CURRENT ADDRESS " + InetAddress.getLocalHost().getHostAddress() + ":" + registrationServicePort);
		} catch (UnknownHostException ignored) {}
		// set different kafka broker address
		if(args.length > 0) {
			KafkaBroker.brokerAddress = args[0];
			System.out.println("KAFKA BROKER ADDRESS: " + args[0]);
		}

		final KafkaConsumer<String, String> registration_consumer = new KafkaConsumer<>(KafkaBroker.getConsumerProperties("registration"));
		// recover phase
		System.out.println(">>>> START RECOVERY PHASE");
		recoverRegistration(registration_consumer,CourseService.courseUpdatesTopic);
		recoverRegistration(registration_consumer,ProjectService.projectUpdatesTopic);
		System.out.println("END RECOVERY PHASE<<<");

		// open RMI communication
		Thread thread = new Thread(() -> {
			try {
				RegistrationService registrationService = new RegistrationService();
				Registry registry = LocateRegistry.createRegistry(registrationServicePort);
				registry.bind(registrationServiceName, registrationService);

				System.out.println("Registration RMI Server online");

				Scanner scanner = new Scanner(System.in);
				while (true) {
					String input = scanner.nextLine();
					if (input.equals("exit")) {
						break;
					}
				}
				scanner.close();
				UnicastRemoteObject.unexportObject(registrationService, true);
			} catch (Exception e) {
				System.out.println("Registration RMI Server OFFLINE!");
			}
			System.out.println("RMI Server CLOSED");
		});

		// open RMI communication to http server
		thread.start();

		// receive message phase
		registration_consumer.subscribe(Arrays.asList(ProjectService.projectUpdatesTopic, CourseService.courseUpdatesTopic));

		// receive message from other services (course and project)
		while (thread.getState() != Thread.State.TERMINATED) {
			final ConsumerRecords<String, String> records = registration_consumer.poll(Duration.of(1, ChronoUnit.MINUTES));
			for (final ConsumerRecord<String, String> record : records) {
				manageKafkaMessage(record.key(), record.value());
			}
		}
		registration_consumer.close();
		System.out.println("Kafka consumer and producer closed");
	}

	private static void recoverRegistration(KafkaConsumer<String, String> consumer, String topic) {
		consumer.subscribe(Collections.singletonList(topic));
		// Reset old commit
		consumer.poll(0);
		consumer.seekToBeginning(consumer.assignment());
		final ConsumerRecords<String, String> records = consumer.poll(Duration.of(30, ChronoUnit.SECONDS));
		for (final ConsumerRecord<String, String> record : records) {
			manageKafkaMessage(record.key(), record.value());
		}
		// not read old message anymore
		consumer.commitSync();
		consumer.unsubscribe();
	}

	private static void manageKafkaMessage(String key, String value) {
		// variable to extract data from messages
		String course_name, project_name, username, grade;
		String[] values;
		List<RegisteredProject> registeredProjects;
		System.out.print("KAFKA MESSAGE: ");
		switch (key) {
			case "new_course":
				db_registrations.put(value, new ArrayList<>());
				System.out.println("Created course <" + value + ">");
				break;
			case "new_project":
				values = value.split("&");
				course_name = values[0];
				project_name = values[1];
				registeredProjects = db_registrations.get(course_name);
				if (registeredProjects != null) {
					registeredProjects.add(new RegisteredProject(project_name));
					System.out.printf("Project <%s> added to old course <%s>\n", project_name, course_name);
				}else {
					db_registrations.put(course_name, new ArrayList<>(Collections.singletonList(new RegisteredProject(project_name))));
					System.out.printf("Project <%s> added to NEW course <%s>\n", project_name, course_name);
				}
				// remove eventually final grades
				removeCompletedCourse(course_name);
				break;
			case "new_grade":
				values = value.split("&"); // course_name&project_name&username&grade
				course_name = values[0];
				project_name = values[1];
				username = values[2];
				grade = values[3];
				registeredProjects = db_registrations.get(course_name);
				if (registeredProjects != null) { // found at least one project for the course
					for (RegisteredProject rp : registeredProjects) {
						if (rp.getProjectName().equals(project_name)) { // founded the desidered project
							rp.addUsernameAndGrade(username, grade); // added grade to the user
							System.out.printf("Grade <%s> added for course <%s> to project <%s> to student <%s>\n", grade, course_name, project_name, username);
							updateCompletedCourse(course_name, username);
						}
					}
				}
				break;
		}
	}

	// called when a new grade is insert. Check if all submission for a project are graded and calculate final grade
	private static void updateCompletedCourse(String course_name, String username) {
		List<RegisteredProject> registeredProjects = db_registrations.get(course_name);
		boolean all = true;
		int sum = 0;
		for (RegisteredProject rp : registeredProjects) {
			try {
				if (rp.getGradefromUsername(username) != null)
					sum += Integer.parseInt(rp.getGradefromUsername(username));
				else
					all = false;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if (all)
			final_grades.add(new TupleCourseUserGrade(course_name, username, sum));
	}

	// adding a project to a completed course
	public static void removeCompletedCourse(String course_name) {
		final_grades.removeIf(final_grade -> final_grade.getCourse().equals(course_name));
	}

	/****  RMI for communication between front-end and back-end  ****/

	@Override
	public Integer checkCompletedCourse(String course_name, String username) throws RemoteException {
		for (TupleCourseUserGrade final_grade : final_grades) {
			if (final_grade.getCourse().equals(course_name) && final_grade.getUsername().equals(username)) {
				return final_grade.getGrade();
			}
		}
		return null;
	}
}
