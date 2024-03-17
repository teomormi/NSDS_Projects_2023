package com.pipeline.operator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.message.Config;
import com.message.KeyValueMessage;

import java.util.ArrayList;
import java.util.List;

public abstract class OperatorAbstract extends AbstractActor {
	// abstract class representing pipeline operators
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private ActorRef nextActor;
	private String operationKey;
	private int window;
	private int slide;
	private List<Double> dataList = new ArrayList<>();

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(KeyValueMessage.class, this::onKeyValueMessage)
				.match(Config.class,this::onConfigMsg)
				.matchAny(this::handleOtherMessage)
				.build();
	}

	// upon receiving a message containing a value, fills a buffer with window size, calculates the result, and decides whether to forward or print
	private void onKeyValueMessage(KeyValueMessage message) {
		if (operationKey.equals(message.getKey())) {
			dataList.add(message.getValue());
			if (dataList.size() >= window) { // the window is complete
				double result = performOperation(dataList.subList(0, window));
				if(nextActor!=null) { // forward value obtained
					nextActor.tell(new KeyValueMessage(operationKey, result), getSelf());
					log.info("Forward result <"+ operationKey + "," + result + "> to " + nextActor.path().name());
				}else
					log.info("Pipeline ended, the result is <"+ operationKey + "," + result + ">");
				dataList = dataList.subList(slide, dataList.size()); // shift buffer of slide size
			}
		} else {
			throw new IllegalArgumentException("Key not valid: " + message.getKey() + ". This actor is configured for key: " + operationKey);
		}
	}

	private void onConfigMsg(Config msg){
		log.info("Received config message with window " + msg.getWindow() +", slide " + msg.getSlide() + " and next " + msg.getNextActor());
		this.nextActor = msg.getNextActor();
		this.operationKey = msg.getOperationKey();
		this.window = msg.getWindow();
		this.slide = msg.getSlide();
	}

	private void handleOtherMessage(Object message) throws Exception {
		throw new Exception("Actor killed!");
	}

	// Abstract method to be implemented
	protected abstract double performOperation(List<Double> values);
}
