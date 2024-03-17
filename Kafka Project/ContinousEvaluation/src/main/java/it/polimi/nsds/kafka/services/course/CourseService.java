package it.polimi.nsds.kafka.services.course;

import com.google.gson.Gson;
import it.polimi.nsds.kafka.beans.Course;
import it.polimi.nsds.kafka.services.KafkaBroker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CourseService extends UnicastRemoteObject implements CourseRemote {

	private static final String courseTopic = "course_recovery";
	public static final String courseUpdatesTopic = "course_updates";

	private static final KafkaProducer<String, String> course_producer = KafkaBroker.getProducerTransactional();
	private static final Map<String, Course> db_courses = new HashMap<>();
	private static final Gson gson = new Gson();

	/*** RMI Info ***/
	public static final int courseServicePort = 2001;
	public static final String courseServiceName = "courseRemote";

	protected CourseService() throws RemoteException {
		super();
		// Prepare db with some values (useless after first launch)
		/*
		addCourse("Informatica");
		addCourse("Matematica");
		addCourse("Storia");
		enrollStudent("student", "Informatica");
		addProject("Informatica", "Consegna1");
		addProject("Informatica", "Consegna2");
		addProject("Matematica", "Consegna3");*/
	}

	public static void main(String[] args) {
		try {
			System.out.println("CURRENT ADDRESS " + InetAddress.getLocalHost().getHostAddress() + ":" + courseServicePort);
		} catch (UnknownHostException ignored) {}
		// set different kafka broker address
		if(args.length > 0) {
			KafkaBroker.brokerAddress = args[0];
			System.out.println("KAFKA BROKER ADDRESS: " + args[0]);
		}

		final KafkaConsumer<String, String> course_consumer = new KafkaConsumer<>(KafkaBroker.getConsumerProperties("course"));
		// recover phase
		System.out.println(">>>> START RECOVERY PHASE");
		recoverCourse(course_consumer);
		System.out.println("END RECOVERY PHASE<<<");

		// open RMI communication
		try {
			CourseService courseService = new CourseService();
			Registry registry = LocateRegistry.createRegistry(courseServicePort);
			registry.bind(courseServiceName, courseService);

			System.out.println("Course RMI Server online");

			Scanner scanner = new Scanner(System.in);
			while (true) {
				String input = scanner.nextLine();
				if (input.equals("exit")) {
					break;
				}
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Course RMI Server OFFLINE!");
		}
	}

	// Recover course info from course backup topic
	private static void recoverCourse(KafkaConsumer<String, String> consumer) {
		consumer.subscribe(Collections.singletonList(courseTopic));
		final ConsumerRecords<String, String> records = consumer.poll(Duration.of(30, ChronoUnit.SECONDS));
		consumer.seekToBeginning(records.partitions());
		for (final ConsumerRecord<String, String> record : records) {
			if (record.value() != null) {
				System.out.printf("KAFKA MESSAGE: Created Course <%s>\n", record.value());
				db_courses.put(record.key(), gson.fromJson(record.value(), Course.class));
			} else
				db_courses.remove(record.key());
		}
		consumer.unsubscribe();
	}

	/****  RMI for communication between front-end and back-end  ****/
	@Override
	public List<String> getCoursesName() throws RemoteException {
		return new ArrayList<>(db_courses.keySet());
	}

	@Override
	public List<String> getProjectsName(String course_name) throws RemoteException {
		Course course = db_courses.get(course_name);
		if (course == null)
			return null;
		return course.getAvailable_projects();
	}

	/*** ADMIN DASHBOARD ***/
	@Override
	public boolean addCourse(String course_name) throws RemoteException {
		try {
			if (db_courses.get(course_name) != null)
				return false;
			Course course = new Course(course_name);
			db_courses.put(course_name, course);
			course_producer.beginTransaction();
			// publish course for recovery
			ProducerRecord<String, String> record_course = new ProducerRecord<>(courseTopic, course_name, gson.toJson(course));
			course_producer.send(record_course);
			// publish course for update other services
			record_course = new ProducerRecord<>(courseUpdatesTopic, "new_course", course_name);
			course_producer.send(record_course);
			course_producer.commitTransaction();
			System.out.println("HTTP MESSAGE: Created course <" + course_name + ">");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			// abort transaction
			course_producer.flush();
			course_producer.abortTransaction();
			return false;
		}
		return true;
	}

	@Override
	public boolean removeCourse(String course_name) throws RemoteException {
		try {
			if (db_courses.get(course_name) == null)
				return false;
			db_courses.remove(course_name);
			// publish course for recovery  (null to remove old course)
			course_producer.beginTransaction();
			ProducerRecord<String, String> record = new ProducerRecord<>(courseTopic, course_name, null);
			course_producer.send(record);
			course_producer.commitTransaction();
			System.out.println("HTTP MESSAGE: Removed course <" + course_name + ">");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			// abort transaction
			course_producer.flush();
			course_producer.abortTransaction();
		}
		return true;
	}

	/*** STUDENT DASHBOARD ***/
	@Override
	public boolean enrollStudent(String username, String course_name) throws RemoteException {
		Course course = db_courses.get(course_name);
		if (course == null)
			return false;
		if (course.AddStudent(username)) {
			// publish course to update recovery data of enrolled students
			course_producer.beginTransaction();
			ProducerRecord<String, String> record = new ProducerRecord<>(courseTopic, course_name, gson.toJson(course));
			course_producer.send(record);
			course_producer.commitTransaction();
			System.out.println("HTTP MESSAGE: Student <" + username + "> enrolled in course <" + course_name + ">");
			return true;
		}
		return false;
	}

	@Override
	public List<String> getCoursesNameEnrolled(String username) throws RemoteException {
		List<String> enrolled_courses = new ArrayList<>();
		for (String key : db_courses.keySet()) {
			if (db_courses.get(key).getEnrolled_students().contains(username))
				enrolled_courses.add(key);
		}
		return enrolled_courses;
	}

	@Override
	public List<String> getCoursesNameNotEnrolled(String username) throws RemoteException {
		List<String> not_enrolled_courses = new ArrayList<>();
		for (String key : db_courses.keySet()) {
			if (!db_courses.get(key).getEnrolled_students().contains(username))
				not_enrolled_courses.add(key);
		}
		return not_enrolled_courses;
	}

	/*** PROFESSOR DASHBOARD ***/
	@Override
	public boolean addProject(String course_name, String project_name) throws RemoteException {
		Course course = db_courses.get(course_name);
		if (course == null)
			return false;
		if (course.AddProject(project_name)) {
			course_producer.beginTransaction();
			// publish course to update recovery data of project
			ProducerRecord<String, String> record_course = new ProducerRecord<>(courseTopic, course_name, gson.toJson(course));
			course_producer.send(record_course);
			// publish course for update project database
			record_course = new ProducerRecord<>(courseUpdatesTopic, "new_project", course_name + "&" + project_name);
			course_producer.send(record_course);
			course_producer.commitTransaction();
			System.out.printf("HTTP MESSAGE: Project <%s> added to course <%s>\n", project_name, course_name);
			return true;
		}
		return false;
	}
}
