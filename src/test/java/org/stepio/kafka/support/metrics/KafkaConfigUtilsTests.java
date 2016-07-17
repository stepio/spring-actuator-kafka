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
import static org.mockito.BDDMockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.kafka.clients.CommonClientConfigs;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Tests for {@link KafkaConfigUtils}.
 *
 * @author Igor Stepanov
 */
public class KafkaConfigUtilsTests {

	@Test
	public void configureKafkaMetrics_withNullMap_NullPointerException() {
		assertThatExceptionOfType(NullPointerException.class)
				.isThrownBy(new ThrowableAssert.ThrowingCallable() {
					@Override
					public void call() throws Throwable {
						KafkaConfigUtils.configureKafkaMetrics(null, mockGaugeService());
					}
				});
		final ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
		try {
			assertThatExceptionOfType(NullPointerException.class)
					.isThrownBy(new ThrowableAssert.ThrowingCallable() {
						@Override
						public void call() throws Throwable {
							KafkaConfigUtils.configureKafkaMetrics(null, mockGaugeService(), "test", executors, 10L);
						}
					});
		}
		finally {
			executors.shutdown();
		}
	}

	@Test
	public void configureKafkaMetrics_withNullGaugeService_NullPointerException() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(new ThrowableAssert.ThrowingCallable() {
					@Override
					public void call() throws Throwable {
						KafkaConfigUtils.configureKafkaMetrics(new HashMap<String, Object>(), null);
					}
				})
				.withMessageContaining("Initializing GaugeService as null is meaningless!");
		final ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
		try {
			assertThatExceptionOfType(IllegalArgumentException.class)
					.isThrownBy(new ThrowableAssert.ThrowingCallable() {
						@Override
						public void call() throws Throwable {
							KafkaConfigUtils.configureKafkaMetrics(new HashMap<String, Object>(), null, "test", executors, 10L);
						}
					})
					.withMessageContaining("Initializing GaugeService as null is meaningless!");
		}
		finally {
			executors.shutdown();
		}
	}

	@Test
	public void configureKafkaMetrics_withGaugeService() {
		Map<String, Object> config = new HashMap<>();
		assertThat(config).isEmpty();
		GaugeService gaugeService = mockGaugeService();
		KafkaConfigUtils.configureKafkaMetrics(config, gaugeService);
		assertThat(config).hasSize(2);
		assertThat(config.get(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG))
				.asList()
				.contains(KafkaStatisticsProvider.class.getCanonicalName());
		assertThat(config.get(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL)).isSameAs(gaugeService);
		assertThat(config.get(KafkaStatisticsProvider.METRICS_PREFIX_PARAM)).isNull();
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL)).isNull();
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM)).isNull();
	}

	@Test
	public void configureKafkaMetrics_withGaugeServiceAndPrefix() {
		Map<String, Object> config = new HashMap<>();
		assertThat(config).isEmpty();
		GaugeService gaugeService = mockGaugeService();
		String prefix = "test.prefix";
		KafkaConfigUtils.configureKafkaMetrics(config, gaugeService, prefix, null, null);
		assertThat(config).hasSize(3);
		assertThat(config.get(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG))
				.asList()
				.contains(KafkaStatisticsProvider.class.getCanonicalName());
		assertThat(config.get(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL)).isSameAs(gaugeService);
		assertThat(config.get(KafkaStatisticsProvider.METRICS_PREFIX_PARAM)).isEqualTo(prefix);
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL)).isNull();
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM)).isNull();
	}

	@Test
	public void configureKafkaMetrics_withGaugeServiceAndInterval() {
		Map<String, Object> config = new HashMap<>();
		assertThat(config).isEmpty();
		GaugeService gaugeService = mockGaugeService();
		Long universalAnswer = 42L;
		KafkaConfigUtils.configureKafkaMetrics(config, gaugeService, null, null, universalAnswer);
		assertThat(config).hasSize(3);
		assertThat(config.get(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG))
				.asList()
				.contains(KafkaStatisticsProvider.class.getCanonicalName());
		assertThat(config.get(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL)).isSameAs(gaugeService);
		assertThat(config.get(KafkaStatisticsProvider.METRICS_PREFIX_PARAM)).isNull();
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL)).isNull();
		assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM)).isEqualTo(universalAnswer);
	}

	@Test
	public void configureKafkaMetrics_withGaugeServiceAndExecutors() {
		ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
		try {
			Map<String, Object> config = new HashMap<>();
			assertThat(config).isEmpty();
			GaugeService gaugeService = mockGaugeService();
			KafkaConfigUtils.configureKafkaMetrics(config, gaugeService, null, executors, null);
			assertThat(config).hasSize(3);
			assertThat(config.get(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG))
					.asList()
					.contains(KafkaStatisticsProvider.class.getCanonicalName());
			assertThat(config.get(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL)).isSameAs(gaugeService);
			assertThat(config.get(KafkaStatisticsProvider.METRICS_PREFIX_PARAM)).isNull();
			assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL)).isSameAs(executors);
			assertThat(config.get(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM)).isNull();
		}
		finally {
			executors.shutdown();
		}
	}

	private GaugeService mockGaugeService() {
		return mock(GaugeService.class);
	}
}
