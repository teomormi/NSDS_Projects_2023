package com.pipeline;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.message.KillActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PipelineMain {

	public static void main(String[] args) {

		Config config = ConfigFactory.load("application.conf");

		// local or distributed mode
		if (args.length < 1) {
			System.out.println("IP address needed to be able to publish server <SERVER_IP>.");
			System.out.println("System will start in local mode!");
		}else{
			// set server properties
			String serverIP = args[0];
			System.out.printf("Distributed mode activated: server (%s)\n",serverIP);
			// update seed node value from parameter
			List<String> seed_nodes = new ArrayList<>();
			seed_nodes.add("akka://System@" + serverIP + ":2551");
			config = config.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seed_nodes));
			config = config.withValue("akka.remote.artery.canonical.hostname", ConfigValueFactory.fromAnyRef(serverIP));
		}

		// create the system where actors have to be created
		ActorSystem sys = ActorSystem.create(("System"), config);

		// create the pipeline manager
		ActorRef pipelineManager = sys.actorOf(PipelineManager.props(), "PipelineManager");

		// wait for the names of the actors to be killed
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.println("Enter name of the actor to be killed or 'exit':");
				String name = scanner.next();
				if ("exit".equalsIgnoreCase(name)) {
					break;
				}
				pipelineManager.tell(new KillActor(name), ActorRef.noSender());
			}
		}
	}
}
