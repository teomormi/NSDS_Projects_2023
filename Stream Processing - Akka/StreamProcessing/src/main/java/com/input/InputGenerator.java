package com.input;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import com.message.KeyValueMessage;
import com.message.KillActor;
import com.message.StartPipeline;
import java.util.Random;

public class InputGenerator extends AbstractActor{

	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final int STREAM_SIZE = 1000; // max input values generated

	// the input actor subscribes to the cluster
	@Override
	public void preStart(){
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	// the actor leaves the cluster
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartPipeline.class, this::onStartPipeline)
				.match(KillActor.class,this::handleKillMessage)
				.build();
	}

	void onStartPipeline(StartPipeline command) {
		// start the streaming
		ActorRef pipeline = command.getActorRefValue();
		new Thread(() -> {
			Random rand = new Random();
			for (int i = 1; i < STREAM_SIZE; i++) {
				// send message to the first operator actor
				pipeline.tell(new KeyValueMessage(command.getKey(), rand.nextDouble()*100), ActorRef.noSender());
				try {
					Thread.sleep(5000); // 5 seconds between message
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void handleKillMessage(Object message) throws Exception {
		throw new Exception("Input generator raise an exception!");
	}
}
