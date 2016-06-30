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
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Tests for {@link KafkaConfigUtils}.
 *
 * @author Igor Stepanov
 */
public class KafkaConfigUtilsTest {

	private AtomicInteger submittingCounter;

	@Before
	public void initCounter() {
		this.submittingCounter = new AtomicInteger();
	}

	@Test
	public void configureKafkaMetrics_withNullMap_NullPointerException() {
		ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
		try {
			try {
				KafkaConfigUtils.configureKafkaMetrics(null, mockGaugeService(), "test");
				new AssertionError("NullPointerException should be thrown!");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(NullPointerException.class);
			}
			try {
				KafkaConfigUtils.configureKafkaMetrics(null, mockGaugeService(), "test", executors, 10L);
				new AssertionError("NullPointerException should be thrown!");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(NullPointerException.class);
			}
		}
		finally {
			executors.shutdown();
		}
	}

	@Test
	public void configureKafkaMetrics_withNullGaugeService_NullPointerException() {
		ScheduledExecutorService executors = Executors.newSingleThreadScheduledExecutor();
		try {
			try {
				KafkaConfigUtils.configureKafkaMetrics(new HashMap<String, Object>(), null, "test");
				new AssertionError("NullPointerException should be thrown!");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(NullPointerException.class);
			}
			try {
				KafkaConfigUtils.configureKafkaMetrics(new HashMap<String, Object>(), null, "test", executors, 10L);
				new AssertionError("NullPointerException should be thrown!");
			}
			catch (Exception ex) {
				assertThat(ex).isInstanceOf(NullPointerException.class);
			}
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
		KafkaConfigUtils.configureKafkaMetrics(config, gaugeService, null);
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
		GaugeService gauge = mock(GaugeService.class);
		willAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				KafkaConfigUtils.LOGGER.info("Called GaugeService.submit with arguments: {}", Arrays.toString(args));
				KafkaConfigUtilsTest.this.submittingCounter.incrementAndGet();
				return null;
			}
		}).given(gauge).submit(anyString(), anyDouble());
		return gauge;
	}
}
