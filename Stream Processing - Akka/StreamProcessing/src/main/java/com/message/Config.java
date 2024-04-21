package com.message;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Config {
	// message send to configure actors in the pipeline
	private final ActorRef nextActor;
	private final String operationKey;
	private final int window;
	private final int slide;

	@JsonCreator
	public Config(ActorRef nextActor,String operationKey,int window,int slide){
		this.nextActor = nextActor;
		this.operationKey = operationKey;
		this.window = window;
		this.slide = slide;
	}

	public ActorRef getNextActor() {
		return nextActor;
	}

	public String getOperationKey() {
		return operationKey;
	}

	public int getWindow() {
		return window;
	}

	public int getSlide() {
		return slide;
	}
}
