package com.pipeline;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.message.KillActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Scanner;

public class PipelineMain {

	public static void main(String[] args) {

		Config config = ConfigFactory.load("pipeline.conf");

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
