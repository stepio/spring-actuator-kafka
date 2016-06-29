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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Implementation of Apache Kafka's {@link MetricsReporter}, backed with Spring Boot Actuator.
 * Unfortunately, Kafka's approach makes it unclear how to use Spring's context (if it's somehow possible),
 * so all the job is done with pure Java - cannot use Spring's annotations effectively.
 *
 * @author Igor Stepanov
 */
public class KafkaStatisticsProvider implements MetricsReporter {

	protected static final Logger LOGGER = LoggerFactory.getLogger(KafkaStatisticsProvider.class);

	protected static final String METRICS_GAUGE_SERVICE_IMPL = "kafka.metrics.gauge.service.impl";
	protected static final String METRICS_UPDATE_EXECUTOR_IMPL = "kafka.metrics.update.executor";
	protected static final String METRICS_UPDATE_INTERVAL_PARAM = "kafka.metrics.update.interval";
	protected static final long METRICS_UPDATE_INTERVAL_DEFAULT = 30000;

	protected ConcurrentMap<MetricName, KafkaMetricContainer> configuredMetrics;
	protected ScheduledExecutorService executorService;
	protected boolean closeExecutorService = false;
	protected GaugeService gaugeService; // logically final, no setter - should not be updated once it's initialized
	protected Long updateInterval; // logically final, no setter - should not be updated once it's initialized

	public KafkaStatisticsProvider() {
		LOGGER.info("Constructed an empty object");
	}

	@Override
	public void init(List<KafkaMetric> metrics) {
		for (KafkaMetric metric : metrics) {
			metricChange(metric);
		}
		LOGGER.debug("Initialized {} metrics", metrics.size());
	}

	@Override
	public void metricChange(KafkaMetric metric) {
		KafkaMetricContainer container = new KafkaMetricContainer(metric);
		this.configuredMetrics.put(metric.metricName(), container);
		LOGGER.debug("Metric {} is added/modified", container.getMetricName());
	}

	@Override
	public void metricRemoval(KafkaMetric metric) {
		KafkaMetricContainer container = this.configuredMetrics.remove(metric.metricName());
		if (container != null) {
			LOGGER.debug("Metric {} is removed", container.getMetricName());
		}
	}

	/**
	 * Closing the {@link ScheduledExecutorService} if it's initialized internally.
	 */
	@Override
	public void close() {
		if (this.executorService != null && this.closeExecutorService) {
			this.executorService.shutdown();
			LOGGER.info("Object cleared, executor stopped");
		}
	}

	@Override
	public void configure(Map<String, ?> configs) {
		this.gaugeService = (GaugeService) configs.get(METRICS_GAUGE_SERVICE_IMPL);
		this.executorService = (ScheduledExecutorService) configs.get(METRICS_UPDATE_EXECUTOR_IMPL);
		this.updateInterval = (Long) configs.get(METRICS_UPDATE_INTERVAL_PARAM);
		postConstruct();
	}

	/**
	 * Actually does the configuration of {@link KafkaStatisticsProvider} instance if the appropriate {@link GaugeService} is set.
	 */
	protected void postConstruct() {
		LOGGER.info("Performing initialization to schedule the metrics gathering...");
		this.configuredMetrics = new ConcurrentHashMap<>();
		if (this.executorService == null) {
			this.executorService = Executors.newSingleThreadScheduledExecutor();
			this.closeExecutorService = true;
		}
		if (this.updateInterval == null) {
			this.updateInterval = METRICS_UPDATE_INTERVAL_DEFAULT;
		}
		this.executorService.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					String metricName;
					double metricValue;
					for (ConcurrentMap.Entry<MetricName, KafkaMetricContainer> entry : KafkaStatisticsProvider.this.configuredMetrics.entrySet()) {
						metricName = entry.getValue().getMetricName();
						metricValue = entry.getValue().getValue().value();
						LOGGER.trace("Set metric {} with value {}", metricName, metricValue);
						KafkaStatisticsProvider.this.gaugeService.submit(metricName, metricValue);
					}
				}
				catch (Exception ex) {
					// Javadoc: If any execution of the task encounters an exception, subsequent executions are suppressed
					LOGGER.error("Exception occurred in the scheduled task", ex);
				}
			}
		}, 0, this.updateInterval, TimeUnit.MILLISECONDS);
		LOGGER.info("Initialization complete, metrics updating scheduled with {} ms interval between the updates", this.updateInterval);
	}

	/**
	 * Method for setting Kafka-related properties, required for proper updating of metrics.
	 *
	 * @param configs {@link Map} with Kafka-specific properties, required to initialize the appropriate consumer/producer.
	 * @param gaugeService reference to an instance of Springs {@link GaugeService}, used to set the collected metrics
	 * @param executorService reference to an instance of {@link ScheduledExecutorService}, used to schedule periodic values' recalculation for metrics
	 * @param updateInterval interval for iterating the whole set of tracked metrics to recalculate and resubmit their values
	 */
	public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService, ScheduledExecutorService executorService, Long updateInterval) {
		if (gaugeService == null) {
			throw new NullPointerException("Initializing GaugeService as null is meaningless!");
		}
		configs.put(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, Collections.singletonList(KafkaStatisticsProvider.class.getCanonicalName()));
		configs.put(METRICS_GAUGE_SERVICE_IMPL, gaugeService);
		LOGGER.debug("Set property {} with provided GaugeService instance reference", METRICS_GAUGE_SERVICE_IMPL);
		if (executorService != null) {
			configs.put(METRICS_UPDATE_EXECUTOR_IMPL, executorService);
			LOGGER.debug("Set property {} with provided ScheduledExecutorService instance reference", METRICS_UPDATE_EXECUTOR_IMPL);
		}
		if (updateInterval != null) {
			configs.put(METRICS_UPDATE_INTERVAL_PARAM, updateInterval);
			LOGGER.debug("Set property {} with value {}", METRICS_UPDATE_INTERVAL_PARAM, updateInterval);
		}
	}

	/**
	 * Overloaded method, which sets updateInterval with default value.
	 *
	 * @param configs {@link Map} with Kafka-specific properties, required to initialize the appropriate consumer/producer.
	 * @param gaugeService reference to an instance of Spring's {@link GaugeService}, used to set the collected metrics
	 */
	public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService) {
		configureKafkaMetrics(configs, gaugeService, null, null);
	}
}
