package com.pipeline;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.message.Config;
import com.message.CreatePipeline;
import com.message.KillActor;
import com.message.ReturnPipeline;
import com.pipeline.operator.AverageOperator;
import com.pipeline.operator.MaxOperator;
import com.pipeline.operator.MinOperator;
import com.utils.OperationType;
import com.utils.Operator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PipelineManager extends AbstractActor {

	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final List<String> keys = new ArrayList<>();
	private int counter = 0;

	// the actors subscribe to the cluster
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
	}

	// the actor leaves the cluster
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
	}

	// the pipeline manager also acts as a supervisor for the instances of the pipelines
	private final static SupervisorStrategy strategy = new OneForOneStrategy(
			10,
			Duration.ofMinutes(1),
			// implement a resume strategy after a crash
			DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.resume()).build());

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}

	@Override
	public AbstractActor.Receive createReceive() {
		return receiveBuilder()
				.match(CreatePipeline.class, this::onCreatePipeline)
				.match(KillActor.class, this::onKillActor)
				.build();
	}

	// search actor and send message to kill him
	void onKillActor(KillActor msg){
		ActorSelection selection = getContext().actorSelection("akka://System/user/PipelineManager/" + msg.getName());
		if (selection != null)
			selection.tell(new KillActor("time to die"),self());
		else
			System.out.println("Actor not found!");
	}

	// creates the list of pipeline operators following the input manager instructions
	void onCreatePipeline(CreatePipeline msg) {
		if (keys.contains(msg.getKey())) { // key already used for another pipeline
			log.info("Key <" + msg.getKey() + "> is already used for another stream");
			sender().tell(new ReturnPipeline(null, msg.getKey()), self());
		}else {
			keys.add(msg.getKey());
			List<Operator> operators = new ArrayList<>(msg.getOperatorList());
			// retrieve operator types for printing them
			String operatorsType = operators.stream()
					.map(op -> op.getOperationType().getDescription())
					.collect(Collectors.joining(" "));
			log.info("PipelineManager received list of " + operators.size() + " operations: " + operatorsType);

			ActorRef prev = null;
			ActorRef start = null;
			// create actors and link them
			for (int i=0;i<operators.size();i++) {
				ActorRef next = createOperator(operators.get(i).getOperationType(), msg.getKey());
				if (prev != null) {
					prev.tell(new Config(next, msg.getKey(), operators.get(i-1).getWindowSize(), operators.get(i-1).getSlideSize()), self());
				} else {
					start = next;
				}
				prev = next;
			}
			if (prev != null) {
				prev.tell(new Config(null, msg.getKey(), operators.get(operators.size() - 1).getWindowSize(), operators.get(operators.size() - 1).getSlideSize()), self());
			}
			ReturnPipeline resp = new ReturnPipeline(start, msg.getKey());
			sender().tell(resp, self());
			counter = 0;
		}
	}

	private ActorRef createOperator(OperationType operationType, String key) {
		counter++;
		switch(operationType) {
			case MIN:
				return getContext().actorOf(MinOperator.props(), key + counter);
			case MAX:
				return getContext().actorOf(MaxOperator.props(), key + counter);
			case AVERAGE:
				return getContext().actorOf(AverageOperator.props(), key + counter);
			default:
				return null;
		}
	}

	public static Props props() {
		return Props.create(PipelineManager.class);
	}
}
