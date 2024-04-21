package com.message;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;

public class ReturnPipeline {
	// message in response to pipeline creation containing address of first actor in the pipeline
	private final ActorRef actorRefValue;
	private final String key;

	@JsonCreator
	public ReturnPipeline(ActorRef actorRefValue, String key) {
		this.actorRefValue = actorRefValue;
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public ActorRef getActorRefValue() {
		return actorRefValue;
	}
}

