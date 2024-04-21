package com.message;

import com.fasterxml.jackson.annotation.JsonCreator;

public class KeyValueMessage  {
	// message exchanged between operator actors in the pipeline
	final String key;
	final double value;

	@JsonCreator
	public KeyValueMessage(String key, double value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public double getValue() {
		return value;
	}
}
