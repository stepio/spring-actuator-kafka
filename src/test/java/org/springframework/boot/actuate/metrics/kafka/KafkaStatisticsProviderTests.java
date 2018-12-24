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

package org.springframework.boot.actuate.metrics.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Tests for {@link KafkaStatisticsProvider}. It's a single point of using PowerMock, as
 * it was required to mock final {@link KafkaMetric} for testing of my implementation of
 * {@link org.apache.kafka.common.metrics.MetricsReporter}, which surprisingly depends on
 * {@link KafkaMetric} instead of {@link org.apache.kafka.common.Metric} interface. The
 * same is reported in KAFKA-3923.
 *
 * @author Igor Stepanov
 * @see <a href="https://issues.apache.org/jira/browse/KAFKA-3923">MetricReporter
 * interface depends on final class KafkaMetric instead of Metric interface</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(KafkaMetric.class)
public class KafkaStatisticsProviderTests {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(KafkaStatisticsProvider.class);

	private Random random;

	private String metricGroup;

	private long updateInterval;

	private CountDownLatch latch;

	private GaugeService gaugeService;

	private KafkaStatisticsProvider simpleProvider;

	public KafkaStatisticsProviderTests() {
		this.random = new Random();
		this.metricGroup = "some_dummy_value";
		this.updateInterval = 500;
	}

	@Before
	public void setUp() {
		this.simpleProvider = new KafkaStatisticsProvider();
		Map<String, Object> config = new HashMap<>();
		this.gaugeService = mockGaugeService();
		config.put(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL, this.gaugeService);
		config.put(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM,
				this.updateInterval);
		this.simpleProvider.configure(config);
	}

	@After
	public void tearDown() {
		this.simpleProvider.close();
	}

	/**
	 * Initial set of {@link KafkaMetric} instances starts being tracked with
	 * {@link GaugeService} periodically. Each metric updates {@link GaugeService}
	 * separately.
	 */
	@Test
	public void init_mockMetrics() throws InterruptedException {
		this.latch = new CountDownLatch(2);
		this.simpleProvider.init(Arrays.asList(randomMetric(), randomMetric()));
		assertThat(this.latch.getCount()).isEqualTo(2);
		assertThat(this.latch.await(2 * this.updateInterval, TimeUnit.MILLISECONDS))
				.isTrue();
	}

	/**
	 * After adding new {@link KafkaMetric}, it's value is pushed to {@link GaugeService}
	 * periodically.
	 */
	@Test
	public void metricChange_mockMetric() throws InterruptedException {
		this.latch = new CountDownLatch(1);
		this.simpleProvider.metricChange(randomMetric());
		assertThat(this.latch.getCount()).isEqualTo(1);
		assertThat(this.latch.await(2 * this.updateInterval, TimeUnit.MILLISECONDS))
				.isTrue();
	}

	/**
	 * If {@link KafkaMetric} is removed, {@link GaugeService} is not triggered anymore.
	 */
	@Test
	public void metricRemoval_mockMetric() throws InterruptedException {
		KafkaMetric metric = randomMetric();
		this.simpleProvider.metricChange(metric);
		this.latch = new CountDownLatch(1);
		this.simpleProvider.metricRemoval(metric);
		assertThat(this.latch.getCount()).isEqualTo(1);
		assertThat(this.latch.await(2 * this.updateInterval, TimeUnit.MILLISECONDS))
				.isFalse();
	}

	@Test
	public void close_externalExecutors() {
		KafkaStatisticsProvider customProvider = new KafkaStatisticsProvider();
		Map<String, Object> config = new HashMap<>();
		config.put(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL,
				mockGaugeService());
		try {
			config.put(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL,
					Executors.newSingleThreadScheduledExecutor());
			config.put(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM,
					this.updateInterval);
			customProvider.configure(config);
			assertThat(customProvider.executorService.isShutdown()).isFalse();
			customProvider.close();
			assertThat(customProvider.executorService.isShutdown()).isFalse();
		}
		finally {
			try {
				customProvider.executorService.shutdown();
				assertThat(customProvider.executorService.isShutdown()).isTrue();
			}
			catch (Exception ex) {
				LOGGER.error("Failed to shutdown executor", ex);
			}
		}
	}

	/**
	 * If {@link ScheduledExecutorService} is initialized specifically for current
	 * {@link KafkaStatisticsProvider}, it should be stopped upon closing.
	 */
	@Test
	public void close_internalExecutors() {
		assertThat(this.simpleProvider.executorService.isShutdown()).isFalse();
		this.simpleProvider.close();
		assertThat(this.simpleProvider.executorService.isShutdown()).isTrue();
	}

	/**
	 * Reference to {@link GaugeService} is set properly with provided value.
	 */
	@Test
	public void configure_gaugeService() {
		assertThat(this.simpleProvider.gaugeService).isSameAs(gaugeService);
	}

	/**
	 * Value of updateInterval is set properly with provided value.
	 */
	@Test
	public void configure_customUpdateInterval() {
		assertThat(this.simpleProvider.updateInterval).isEqualTo(updateInterval);
	}

	/**
	 * Value of updateInterval is set properly with default value.
	 */
	@Test
	public void configure_defaultUpdateInterval() {
		KafkaStatisticsProvider customProvider = new KafkaStatisticsProvider();
		Map<String, Object> config = new HashMap<>();
		config.put(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL,
				mockGaugeService());
		customProvider.configure(config);
		assertThat(customProvider.updateInterval)
				.isEqualTo(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_DEFAULT);
	}

	/**
	 * Reference to {@link ScheduledExecutorService} is set properly with provided value.
	 */
	@Test
	public void configure_customExecutorService() {
		ScheduledExecutorService scheduledExecutorService = Executors
				.newSingleThreadScheduledExecutor();
		KafkaStatisticsProvider customProvider = new KafkaStatisticsProvider();
		Map<String, Object> config = new HashMap<>();
		config.put(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL,
				scheduledExecutorService);
		customProvider.configure(config);
		assertThat(customProvider.executorService).isSameAs(scheduledExecutorService);
	}

	/**
	 * Value of prefix is set properly with provided value.
	 */
	@Test
	public void configure_customPrefix() {
		String customPrefix = "test_value_for_prefix";
		KafkaStatisticsProvider customProvider = new KafkaStatisticsProvider();
		Map<String, Object> config = new HashMap<>();
		config.put(KafkaStatisticsProvider.METRICS_PREFIX_PARAM, customPrefix);
		customProvider.configure(config);
		assertThat(customProvider.prefix).isEqualTo(customPrefix);
	}

	private GaugeService mockGaugeService() {
		GaugeService gauge = mock(GaugeService.class);
		willAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				LOGGER.info("Called GaugeService.submit with arguments: {}",
						Arrays.toString(args));
				KafkaStatisticsProviderTests.this.latch.countDown();
				return null;
			}
		}).given(gauge).submit(anyString(), anyDouble());
		return gauge;
	}

	protected MetricName randomMetricName() {
		return new MetricName(UUID.randomUUID().toString(), this.metricGroup);
	}

	protected KafkaMetric randomMetric() {
		return randomMetric(randomMetricName());
	}

	protected KafkaMetric randomMetric(MetricName name) {
		KafkaMetric metric = PowerMockito.mock(KafkaMetric.class);
		PowerMockito.when(metric.value()).thenReturn(this.random.nextDouble());
		PowerMockito.when(metric.metricName()).thenReturn(name);
		return metric;
	}

}
