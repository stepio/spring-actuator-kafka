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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import java.util.Random;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

/**
 * Tests for {@link KafkaMetricContainer}.
 *
 * @author Igor Stepanov
 */
public class KafkaMetricContainerTests {

	private Random random;
	private String metricPrefix;
	private String metricGroup;
	private String metricName;

	public KafkaMetricContainerTests() {
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
				.isEqualTo(this.metricPrefix + '.' + this.metricGroup + '.' + this.metricName);
	}

	/**
	 * Tags' values are used in metric's name, ordered by the appropriate keys.
	 */
	@Test
	public void metricName_withConstantValuesAndTags() {
		final String tag1 = "some_dummy_key";
		final String tag2 = "another_dummy_key";
		final String value1 = "some_dummy_val";
		final String value2 = "another_dummy_val";
		assertThat(randomKafkaMetricContainer(new MetricName(this.metricName, this.metricGroup, "Test metric", tag1, value1, tag2, value2))
				.getMetricName())
				.isEqualTo(this.metricPrefix + '.' + value2 + '.' + value1 + '.' + this.metricGroup + '.' + this.metricName);
	}

	/**
	 * Instance of {@link MetricName} cannot be initialized without name.
	 */
	@Test
	public void metricName_withNullName_NullPointerException() {
		assertThatExceptionOfType(NullPointerException.class)
				.isThrownBy(new ThrowableAssert.ThrowingCallable() {
					@Override
					public void call() throws Throwable {
						new KafkaMetricContainer(randomMetric(new MetricName(null, KafkaMetricContainerTests.this.metricGroup)), KafkaMetricContainerTests.this.metricPrefix);
					}
				});
	}

	/**
	 * Instance of {@link MetricName} cannot be initialized without group.
	 */
	@Test
	public void metricName_withNullGroup_NullPointerException() {
		assertThatExceptionOfType(NullPointerException.class)
				.isThrownBy(new ThrowableAssert.ThrowingCallable() {
					@Override
					public void call() throws Throwable {
						new KafkaMetricContainer(randomMetric(new MetricName(KafkaMetricContainerTests.this.metricName, null)), KafkaMetricContainerTests.this.metricPrefix);
					}
				});
	}

	/**
	 * Name's calculation fails for null value of {@link Metric}.
	 */
	@Test
	public void metricName_withNull_NullPointerException() {
		assertThatExceptionOfType(NullPointerException.class)
				.isThrownBy(new ThrowableAssert.ThrowingCallable() {
					@Override
					public void call() throws Throwable {
						randomKafkaMetricContainer().metricName(null);
					}
				});
	}

	protected MetricName constMetricName() {
		return new MetricName(this.metricName, this.metricGroup);
	}

	protected Metric randomMetric() {
		return randomMetric(constMetricName());
	}

	protected Metric randomMetric(MetricName name) {
		Metric metric = mock(Metric.class);
		given(metric.value()).willReturn(this.random.nextDouble());
		given(metric.metricName()).willReturn(name);
		return metric;
	}

	protected KafkaMetricContainer randomKafkaMetricContainer(MetricName name) {
		return new KafkaMetricContainer(randomMetric(name), this.metricPrefix);
	}

	protected KafkaMetricContainer randomKafkaMetricContainer() {
		return randomKafkaMetricContainer(constMetricName());
	}
}
