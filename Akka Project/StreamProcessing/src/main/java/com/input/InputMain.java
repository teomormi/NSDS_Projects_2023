package com.input;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import akka.actor.Props;
import com.message.CreatePipeline;
import com.message.ReturnPipeline;
import com.message.StartPipeline;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.utils.OperationType;
import com.utils.Operator;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

public class InputMain {
	private static final Duration timeout = Duration.create(5, SECONDS);
	private static OperationType operationType;
	private static int window, slide; // values read from standard input
	private static ReturnPipeline reply; // answer containing the first actor of the generated pipeline
	private static final List<Operator> operatorList = new ArrayList<>();

	public static void main(String[] args) {
		// load the config file
		Config config = ConfigFactory.load("input.conf");
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
					System.out.println("Enter the operation you want to apply or 'stop': (" + counter + ")");
					input = scanner.next();
					// clear operator list
					operatorList.clear();
					while(!input.equalsIgnoreCase("stop")){
						try {
							operationType = OperationType.fromDescription(input);
							System.out.println("Enter the window width: ");
							window = scanner.nextInt();
							System.out.println("Enter the slide: ");
							slide = scanner.nextInt();
							counter++;
							operatorList.add(new Operator(window,slide,operationType));
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
						if(reply!=null){ // key already used
							// instantiate the new InputGenerator through InputManager (supervisor)
							Future<Object> waitingForInputGenerator = ask(inputManager, Props.create(InputGenerator.class), 5000);
							ActorRef inputGenerator = null;
							try {
								inputGenerator = (ActorRef) waitingForInputGenerator.result(timeout, null);
							} catch (TimeoutException | InterruptedException e) {
								e.printStackTrace();
							}
							// start the stream
							inputGenerator.tell(new StartPipeline(reply.getActorRefValue(), reply.getKey()), ActorRef.noSender());
						}else
							System.out.println("Key already used");
					}
				}
			}
		} finally {
			sys.terminate();
		}
	}
}
