/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stepio.kafka.support.metrics;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;

/**
 * Container for {@link KafkaMetric}, defining the name of the appropriate metric,
 * which should be exposed to Spring Boot Actuator.
 *
 * @author Igor Stepanov
 */
public class KafkaMetricContainer {

	protected static final String PREFIX_KAFKA = "kafka.";

	private String metricName;
	private Metric value;

	public KafkaMetricContainer(Metric value) {
		this.value = value;
		this.metricName = metricName(value);
	}

	public String getMetricName() {
		return this.metricName;
	}

	public Metric getValue() {
		return this.value;
	}

	protected String metricName(Metric metric) {
		MetricName name = metric.metricName();
		StringBuilder builder = new StringBuilder();
		builder.append(PREFIX_KAFKA);
		builder.append(name.group());
		builder.append('.');
		builder.append(name.name());
		return builder.toString();
	}
}
