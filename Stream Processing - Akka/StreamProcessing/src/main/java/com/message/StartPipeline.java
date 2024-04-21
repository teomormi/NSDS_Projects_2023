package com.message;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;

public class StartPipeline {
	// message sent to input generator to initiate the production of values
	private final ActorRef actorRefValue;
	private final String key;

	@JsonCreator
	public StartPipeline(ActorRef actorRefValue, String key) {
		this.actorRefValue = actorRefValue;
		this.key = key;
	}

	public ActorRef getActorRefValue() {
		return actorRefValue;
	}

	public String getKey() {
		return key;
	}
}
