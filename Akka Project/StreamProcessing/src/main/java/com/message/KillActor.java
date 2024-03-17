package com.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class KillActor  {
	// message to indicate the operator to be killed
	final String name;

	@JsonCreator
	public KillActor(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}