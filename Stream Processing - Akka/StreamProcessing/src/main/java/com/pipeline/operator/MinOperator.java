package com.pipeline.operator;

import akka.actor.Props;
import java.util.Collections;
import java.util.List;

public class MinOperator extends OperatorAbstract {

	@Override
	protected double performOperation(List<Double> values) {
		if (values.isEmpty()) {
			throw new IllegalStateException("La lista dei valori non può essere vuota.");
		}
		return Collections.min(values);
	}

	public static Props props() {
		return Props.create(MinOperator.class);
	}
}
