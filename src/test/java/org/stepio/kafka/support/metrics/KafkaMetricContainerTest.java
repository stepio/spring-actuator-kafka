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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import java.util.Random;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.junit.Test;

/**
 * Tests for {@link KafkaMetricContainer}.
 *
 * @author Igor Stepanov
 */
public class KafkaMetricContainerTest {

	private Random random;
	private String metricPrefix;
	private String metricGroup;
	private String metricName;

	public KafkaMetricContainerTest() {
		this.random = new Random();
		this.metricPrefix = "first_dummy_value";
		this.metricGroup = "second_dummy_value";
		this.metricName = "third_dummy_value";
	}

	/**
	 * Getter returns the same instance as was set to constructor.
	 */
	@Test
	public void getValue_withKafkaMetricContainer_same() {
		Metric metric = randomMetric();
		KafkaMetricContainer metricContainer = new KafkaMetricContainer(metric, "test");
		assertThat(metricContainer.getValue()).isSameAs(metric);
	}

	/**
	 * Name is calculated once (upon constructing the object) and then the same instance is returned each time.
	 */
	@Test
	public void getMetricName_withKafkaMetricContainer_same() {
		KafkaMetricContainer metricContainer = randomKafkaMetricContainer();
		String name = metricContainer.getMetricName();
		assertThat(metricContainer.getMetricName()).isSameAs(name);
	}

	/**
	 * Name is calculated once (upon constructing the object) and then the same instance is returned each time.
	 */
	@Test
	public void metricName_withConstantValues() {
		assertThat(randomKafkaMetricContainer().getMetricName())
				.isEqualTo(this.metricPrefix + ":type=" + this.metricGroup);
	}

	/**
	 * {@link MetricName} cannot be initialized without name.
	 */
	@Test
	public void metricName_withNullName_NullPointerException() {
		try {
			new KafkaMetricContainer(randomMetric(new MetricName(null, this.metricGroup)), this.metricPrefix);
			new AssertionError("NullPointerException should be thrown!");
		}
		catch (Exception ex) {
			assertThat(ex).isInstanceOf(NullPointerException.class);
		}
	}

	/**
	 * {@link MetricName} cannot be initialized without group.
	 */
	@Test
	public void metricName_withNullGroup_NullPointerException() {
		try {
			new KafkaMetricContainer(randomMetric(new MetricName(this.metricName, null)), this.metricPrefix);
			new AssertionError("NullPointerException should be thrown!");
		}
		catch (Exception ex) {
			assertThat(ex).isInstanceOf(NullPointerException.class);
		}
	}

	/**
	 * Name's calculation fails for null value of {@link Metric}.
	 */
	@Test
	public void metricName_withNull_NullPointerException() {
		try {
			randomKafkaMetricContainer().metricName(null);
			new AssertionError("NullPointerException should be thrown!");
		}
		catch (Exception ex) {
			assertThat(ex).isInstanceOf(NullPointerException.class);
		}
	}

	private MetricName constMetricName() {
		return new MetricName(this.metricName, this.metricGroup);
	}

	private Metric randomMetric() {
		return randomMetric(constMetricName());
	}

	private Metric randomMetric(MetricName name) {
		Metric metric = mock(Metric.class);
		given(metric.value()).willReturn(this.random.nextDouble());
		given(metric.metricName()).willReturn(name);
		return metric;
	}

	private KafkaMetricContainer randomKafkaMetricContainer() {
		return new KafkaMetricContainer(randomMetric(), this.metricPrefix);
	}
}
