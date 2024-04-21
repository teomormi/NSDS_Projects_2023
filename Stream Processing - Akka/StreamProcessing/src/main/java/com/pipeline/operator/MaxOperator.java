package com.pipeline.operator;

import akka.actor.Props;
import java.util.Collections;
import java.util.List;

public class MaxOperator extends OperatorAbstract {

	@Override
	protected double performOperation(List<Double> values) {
		return Collections.max(values);
	}

	public static Props props() {
		return Props.create(MaxOperator.class);
	}
}