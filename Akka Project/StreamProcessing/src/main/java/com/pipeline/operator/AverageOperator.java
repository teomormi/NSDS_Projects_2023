package com.pipeline.operator;

import akka.actor.Props;
import java.util.List;

public class AverageOperator extends OperatorAbstract {

	@Override
	protected double performOperation(List<Double> values) {
		double sum = 0.0;
		for (Double value : values) {
			sum += value;
		}
		return sum / values.size();
	}

	public static Props props() {
		return Props.create(AverageOperator.class);
	}
}