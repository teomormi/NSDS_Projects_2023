package com.message;

import com.utils.Operator;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;

public class CreatePipeline {
	// message sent to the pipeline manager containing the requested pipeline
	private final List<Operator> operatorList;
	private final String key;

	@JsonCreator
	public CreatePipeline(List<Operator> operatorList, String key) {
		this.operatorList = operatorList;
		this.key = key;
	}

	public List<Operator> getOperatorList() {
		return new ArrayList<>(operatorList);
	}

	public String getKey() {
		return key;
	}
}
