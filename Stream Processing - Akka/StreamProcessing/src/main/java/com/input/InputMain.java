package com.input;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import akka.actor.Props;
import com.message.CreatePipeline;
import com.message.KillActor;
import com.message.ReturnPipeline;
import com.message.StartPipeline;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.utils.OperationType;
import com.utils.Operator;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

public class InputMain {

	private static final Duration timeout = Duration.create(5, SECONDS);
	private static ReturnPipeline reply; // answer containing the first actor of the generated pipeline
	private static final List<Operator> operatorList = new ArrayList<>();
	public static String serverIP = "127.0.0.1";

	public static void main(String[] args) {
		// load the config file
		Config config = ConfigFactory.load("application.conf");
		// set random port for client
		config = config.withValue("akka.remote.artery.canonical.port", ConfigValueFactory.fromAnyRef(Integer.valueOf("0")));

		// local or distributed mode
		if (args.length < 2) {
			System.out.println("IP addresses needed to be able to communicate with remote server <CLIENT_IP> <SERVER_IP>.");
			System.out.println("System will start in local mode!");
		}else{
			// set client and server properties
			String clientIP = args[0];
			serverIP = args[1];
			System.out.printf("Distributed mode activated: client (%s) server (%s)\n",clientIP,serverIP);
			// add seed node from parameter
			List<String> seed_nodes = new ArrayList<>();
			seed_nodes.add("akka://System@" + serverIP + ":2551");
			config = config.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seed_nodes));
			config = config.withValue("akka.remote.artery.canonical.hostname", ConfigValueFactory.fromAnyRef(clientIP));
		}

		// create the system where actors have to be created
		final ActorSystem sys = ActorSystem.create(("System"), config);
		// create an actor for the input manager
		ActorRef inputManager = sys.actorOf(InputManager.props(), "InputManager");

		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.println("Enter 'operation' to create a new stream or 'exit' to terminate");
				String input = scanner.next();
				if ("exit".equalsIgnoreCase(input)) {
					break;
				}
				if("operation".equalsIgnoreCase(input)){
					int counter = 1; // number of the operation in the pipeline
					System.out.println("Enter the type/key of data (temperature/humidity/pressure)");
					String dataType = scanner.next();
					System.out.println("Enter the operation you want to apply (avg/max/min) or 'stop': (" + counter + ")");
					input = scanner.next();
					// clear operator list
					operatorList.clear();
					while(!input.equalsIgnoreCase("stop")){
						try {
							OperationType operationType = OperationType.fromDescription(input);
							System.out.println("Enter the window width: ");
							int window = scanner.nextInt();
							System.out.println("Enter the slide: ");
							// values read from standard input
							int slide = scanner.nextInt();
							if(slide > window) throw new Exception();
							counter++;
							operatorList.add(new Operator(window, slide, operationType));
						} catch (Exception e){
							System.out.println("INVALID Input!");
						}
						System.out.println("Enter the operation you want to apply or 'stop': (" + counter + ")");
						input = scanner.next();
					}
					if(!operatorList.isEmpty()){
						CreatePipeline msg = new CreatePipeline(operatorList, dataType);
						// send message to InputManager with pipeline details required
						Future<Object> waitingForReply = ask(inputManager, msg, 5000);
						try {
							reply = (ReturnPipeline) waitingForReply.result(timeout, null);
						} catch (InterruptedException | TimeoutException e) {
							e.printStackTrace();
						}
						if(reply.getActorRefValue()!=null){ // key not already used
							// instantiate the new InputGenerator through InputManager (supervisor)
							Future<Object> waitingForInputGenerator = ask(inputManager, Props.create(InputGenerator.class), 5000);
							ActorRef inputGenerator = null;
							try {
								inputGenerator = (ActorRef) waitingForInputGenerator.result(timeout, null);
							} catch (TimeoutException | InterruptedException e) {
								e.printStackTrace();
							}
							// print the name of the created input generator
							System.out.printf("The input actor created for <%s> is <%s>\n",dataType,inputGenerator.path());
							System.out.println("You can now kill the input actor by insert 'kill' command");
							// start the stream
							inputGenerator.tell(new StartPipeline(reply.getActorRefValue(), reply.getKey()), ActorRef.noSender());
						}else
							System.out.println("Key already used");
					}
				}
				if("kill".equalsIgnoreCase(input)){
					System.out.println("Enter name of the input actor to be killed:");
					String name = scanner.next();
					inputManager.tell(new KillActor(name), ActorRef.noSender());
				}
			}
		} finally {
			sys.terminate();
		}
	}
}
