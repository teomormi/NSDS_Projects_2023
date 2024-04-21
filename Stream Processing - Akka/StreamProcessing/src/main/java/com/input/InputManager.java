package com.input;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.japi.pf.DeciderBuilder;
import com.message.CreatePipeline;
import com.message.KillActor;
import com.message.ReturnPipeline;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

public class InputManager extends AbstractActor {

	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private static final scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(5, SECONDS);
	private int counter_input = 0; // number of input created
	// ip address of the machine running pipeline manager (server)
	private final ActorSelection pipelineManager = getContext().actorSelection("akka://System@" + InputMain.serverIP + ":2551/user/PipelineManager");

	// the actor subscribes to the cluster
	@Override
	public void preStart(){
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	// the actor leaves the cluster
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}

	// supervision strategy for child actors
	private final static SupervisorStrategy strategy =
			new OneForOneStrategy(
					10, // Max no of retries
					Duration.ofMinutes(1), // Within what time period
					DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.resume()).build());

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CreatePipeline.class, this::onCreatePipeline)
				// Creates the child actor within the supervisor actor context
				.match(Props.class, props -> { getSender().tell(getContext().actorOf(props,"input" + counter_input), getSelf()); counter_input++; })
				.match(KillActor.class, this::killInput)
				.build();
	}

	void onCreatePipeline(CreatePipeline command) {
		// ask to PipelineManager to create the pipeline
		Future<Object> waitingForReturnPipeline = ask(pipelineManager, command, 5000);
		ReturnPipeline returnPipeline = null;
		try {
			returnPipeline = (ReturnPipeline) waitingForReturnPipeline.result(timeout, null);
		} catch (TimeoutException | InterruptedException e) {
			e.printStackTrace();
		}
		// reply to inputMain
		sender().tell(returnPipeline, self());
	}

	void killInput (KillActor msg){
		ActorSelection selection = getContext().actorSelection("akka://System/user/InputManager/" + msg.getName());
		if (selection != null)
			selection.tell(new KillActor("time to die"),self());
		else
			System.out.println("Actor not found!");
	}

	public static Props props() {
		return Props.create(InputManager.class);
	}
}
