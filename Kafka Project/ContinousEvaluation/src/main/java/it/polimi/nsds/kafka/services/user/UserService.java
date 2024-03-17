package it.polimi.nsds.kafka.services.user;

import com.google.gson.Gson;
import it.polimi.nsds.kafka.beans.User;
import it.polimi.nsds.kafka.enums.Gender;
import it.polimi.nsds.kafka.enums.Role;
import it.polimi.nsds.kafka.services.KafkaBroker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class UserService extends UnicastRemoteObject implements UserRemote {

	private static final String usersTopic = "user_recovery";

	private static final KafkaProducer<String, String> user_producer =
			new KafkaProducer<>(KafkaBroker.getProducerProperties());
	private static final Map<String, User> db_users = new HashMap<>();
	private static final Gson gson = new Gson();

	/*** RMI Info ***/
	public static final int userServicePort = 2000;
	public static final String userServiceName = "userRemote";

	protected UserService() throws RemoteException {
		super();
		// Prepare db with some values (useless after first launch)
		/*
		addUser("student","1","student","male");
		addUser("student2","1","student","male");
		addUser("admin","1","admin","male");
		addUser("professor","1","professor","male");*/
	}

	public static void main(String[] args) {
		try {
			System.out.println("CURRENT ADDRESS " + InetAddress.getLocalHost().getHostAddress() + ":" + userServicePort);
		} catch (UnknownHostException ignored) {}
		// set different kafka broker address
		if(args.length > 0) {
			KafkaBroker.brokerAddress = args[0];
			System.out.println("KAFKA BROKER ADDRESS: " + args[0]);
		}

		final KafkaConsumer<String, String> user_consumer = new KafkaConsumer<>(KafkaBroker.getConsumerProperties("user"));
		// recover phase
		System.out.println(">>>> START RECOVERY PHASE");
		recoverUsers(user_consumer);
		user_consumer.close();
		System.out.println("END RECOVERY PHASE<<<");

		// open RMI communication
		try {
			UserService userService = new UserService();
			Registry registry = LocateRegistry.createRegistry(userServicePort);
			registry.bind(userServiceName, userService);

			System.out.println("User RMI Server online");

			Scanner scanner = new Scanner(System.in);
			while (true) {
				String input = scanner.nextLine();
				if (input.equals("exit")) {
					break;
				}
			}
			scanner.close();
		} catch (Exception e) {
			System.out.println("User RMI Server OFFLINE!");
		}
	}

	// Recover user info from user backup topic
	private static void recoverUsers(KafkaConsumer<String, String> consumer) {
		consumer.subscribe(Collections.singletonList(usersTopic));
		final ConsumerRecords<String, String> records = consumer.poll(Duration.of(30, ChronoUnit.SECONDS));
		consumer.seekToBeginning(records.partitions());
		for (final ConsumerRecord<String, String> record : records) {
			db_users.put(record.key(), gson.fromJson(record.value(), User.class));
		}
		if (!db_users.isEmpty()) {
			for (User u : db_users.values()) {
				System.out.printf("KAFKA MESSAGE: Created User <%s>\n", u.toString());
			}
		}
		consumer.unsubscribe();
	}

	/****  RMI for communication between front-end and back-end  ****/
	@Override
	public Role checkUser(String username, String password) throws RemoteException {
		User user = db_users.get(username);
		if ((user == null) || (!user.getPassword().equals(password)))
			return null;
		System.out.println("HTTP MESSAGE: User LOGGED: " + username);
		return user.getRole();
	}

	@Override
	public boolean addUser(String username, String password, String role, String gender) throws RemoteException {
		try {
			if (db_users.get(username) != null)
				return false;
			User user = new User(username, password, Role.getRole(role), Gender.getGender(gender));
			db_users.put(username, user);
			// publish user on kafka broker for recovery
			ProducerRecord<String, String> record = new ProducerRecord<>(usersTopic, username, gson.toJson(user));
			user_producer.send(record);
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			return false;
		}
		System.out.println("HTTP MESSAGE: User ADDED: " + username + " " + role + " " + gender);
		return true;
	}
}
