package com.utils;

// types of pipeline operator implemented in com.pipeline.operator;
public enum OperationType {
	AVERAGE("avg"),
	MAX("max"),
	MIN("min"),
	SUM("sum"),
	DISTINCT_COUNT("distinct count");

	private String description;

	OperationType(String description) {
		this.description = description;
	}

	// retrive instance based on description
	public static OperationType fromDescription(String description) {
		for (OperationType instance : values()) {
			if (instance.description.equals(description)) {
				return instance;
			}
		}
		throw new IllegalArgumentException(description + " not found!");
	}

	public String getDescription(){
		return description;
	}
}