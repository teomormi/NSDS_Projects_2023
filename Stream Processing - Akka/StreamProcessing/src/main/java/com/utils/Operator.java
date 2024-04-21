package com.utils;

// class used by input to specify how to build the pipeline
public class Operator {
	private final int windowSize;
	private final int slideSize;
	private final OperationType operationType;

	public Operator(int windowSize, int slideSize, OperationType operationType) {
		this.windowSize = windowSize;
		this.slideSize = slideSize;
		this.operationType = operationType;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getSlideSize() {
		return slideSize;
	}
}
